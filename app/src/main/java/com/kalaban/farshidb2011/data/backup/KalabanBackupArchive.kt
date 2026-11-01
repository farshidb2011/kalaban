package com.kalaban.farshidb2011.data.backup

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.charset.StandardCharsets

/**
 * Single-file Kalaban backup container (`.kalb`).
 *
 * Layout (big-endian):
 * ```
 * 0..3   Magic "KALB"
 * 4..5   Version (uint16) — currently 1
 * 6..7   Flags (uint16) — reserved, 0
 * 8..11  MetaLength (uint32)
 * 12..   Meta UTF-8 JSON (shop data without image bytes)
 *        ImageCount (uint32)
 *        repeated ImageCount times:
 *          ItemId (int64)
 *          MimeLen (uint16) + Mime UTF-8
 *          DataLen (uint32) + raw image bytes
 * ```
 */
object KalabanBackupArchive {
    const val MAGIC = "KALB"
    const val VERSION: Int = 1
    const val FILE_NAME = "kalaban-backup.kalb"

    data class ImageEntry(
        val itemId: Long,
        val mime: String,
        val bytes: ByteArray,
    )

    data class Archive(
        val metaJson: String,
        val images: List<ImageEntry>,
    )

    fun isKalabanArchive(bytes: ByteArray): Boolean {
        if (bytes.size < 12) return false
        return bytes[0] == 'K'.code.toByte() &&
            bytes[1] == 'A'.code.toByte() &&
            bytes[2] == 'L'.code.toByte() &&
            bytes[3] == 'B'.code.toByte()
    }

    fun pack(metaJson: String, images: List<ImageEntry>): ByteArray {
        val metaBytes = metaJson.toByteArray(StandardCharsets.UTF_8)
        val out = ByteArrayOutputStream(metaBytes.size + images.sumOf { 14 + it.mime.length + it.bytes.size } + 16)
        DataOutputStream(out).use { dos ->
            dos.writeBytes(MAGIC)
            dos.writeShort(VERSION)
            dos.writeShort(0) // flags
            dos.writeInt(metaBytes.size)
            dos.write(metaBytes)
            dos.writeInt(images.size)
            for (image in images) {
                val mimeBytes = image.mime.toByteArray(StandardCharsets.UTF_8)
                require(mimeBytes.size <= 0xFFFF) { "mime too long" }
                require(image.bytes.size <= Int.MAX_VALUE) { "image too large" }
                dos.writeLong(image.itemId)
                dos.writeShort(mimeBytes.size)
                dos.write(mimeBytes)
                dos.writeInt(image.bytes.size)
                dos.write(image.bytes)
            }
            dos.flush()
        }
        return out.toByteArray()
    }

    fun unpack(bytes: ByteArray): Archive {
        require(isKalabanArchive(bytes)) { "Not a Kalaban backup archive" }
        DataInputStream(ByteArrayInputStream(bytes)).use { dis ->
            dis.skipBytes(4) // magic
            val version = dis.readUnsignedShort()
            require(version == VERSION) { "Unsupported backup version: $version" }
            dis.readUnsignedShort() // flags
            val metaLen = dis.readInt()
            require(metaLen >= 0 && metaLen <= bytes.size) { "Invalid meta length" }
            val metaBytes = ByteArray(metaLen)
            dis.readFully(metaBytes)
            val metaJson = String(metaBytes, StandardCharsets.UTF_8)
            val imageCount = dis.readInt()
            require(imageCount >= 0) { "Invalid image count" }
            val images = ArrayList<ImageEntry>(imageCount)
            repeat(imageCount) {
                val itemId = dis.readLong()
                val mimeLen = dis.readUnsignedShort()
                val mimeBytes = ByteArray(mimeLen)
                dis.readFully(mimeBytes)
                val dataLen = dis.readInt()
                require(dataLen >= 0) { "Invalid image data length" }
                val data = ByteArray(dataLen)
                dis.readFully(data)
                images += ImageEntry(
                    itemId = itemId,
                    mime = String(mimeBytes, StandardCharsets.UTF_8).ifBlank { "image/jpeg" },
                    bytes = data,
                )
            }
            return Archive(metaJson = metaJson, images = images)
        }
    }
}

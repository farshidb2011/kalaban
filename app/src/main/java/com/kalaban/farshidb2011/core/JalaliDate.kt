package com.kalaban.farshidb2011.core

import java.util.Calendar
import java.util.TimeZone

/** Minimal Jalali (Persian) calendar helpers for sale dates and reminders. */
object JalaliDate {
    data class Date(val year: Int, val month: Int, val day: Int) {
        fun format(): String = "%04d/%02d/%02d".format(year, month, day)

        /** Convert this Jalali date to a Gregorian calendar at local noon. */
        fun toGregorianEpochMs(timeZone: TimeZone = TimeZone.getDefault()): Long {
            val (gy, gm, gd) = toGregorian()
            val cal = Calendar.getInstance(timeZone).apply {
                set(Calendar.YEAR, gy)
                set(Calendar.MONTH, gm - 1)
                set(Calendar.DAY_OF_MONTH, gd)
                set(Calendar.HOUR_OF_DAY, 12)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return cal.timeInMillis
        }

        fun toGregorian(): Triple<Int, Int, Int> {
            val jy = year - 979
            val jm = month - 1
            val jd = day - 1
            var jDayNo = 365 * jy + jy / 33 * 8 + (jy % 33 + 3) / 4
            for (i in 0 until jm) {
                jDayNo += if (i < 6) 31 else 30
            }
            jDayNo += jd
            var gDayNo = jDayNo + 79
            val gy = 1600 + 400 * (gDayNo / 146097)
            gDayNo %= 146097
            var leap = true
            var gy2 = gy
            if (gDayNo >= 36525) {
                gDayNo--
                gy2 += 100 * (gDayNo / 36524)
                gDayNo %= 36524
                if (gDayNo >= 365) gDayNo++ else leap = false
            }
            gy2 += 4 * (gDayNo / 1461)
            gDayNo %= 1461
            if (gDayNo >= 366) {
                leap = false
                gDayNo--
                gy2 += gDayNo / 365
                gDayNo %= 365
            }
            val gDaysInMonth = intArrayOf(0, 31, if (leap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
            var gm = 1
            while (gm <= 12 && gDayNo >= gDaysInMonth[gm]) {
                gDayNo -= gDaysInMonth[gm]
                gm++
            }
            return Triple(gy2, gm, gDayNo + 1)
        }
    }

    fun parse(formatted: String): Date? {
        val parts = formatted.trim().split('/', '-')
        if (parts.size != 3) return null
        val y = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        val d = parts[2].toIntOrNull() ?: return null
        if (m !in 1..12 || d !in 1..31 || y < 1300) return null
        return Date(y, m, d)
    }

    fun today(timeZone: TimeZone = TimeZone.getDefault()): Date {
        val cal = Calendar.getInstance(timeZone)
        return fromGregorian(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
        )
    }

    fun fromEpochMs(epochMs: Long, timeZone: TimeZone = TimeZone.getDefault()): Date {
        val cal = Calendar.getInstance(timeZone).apply { timeInMillis = epochMs }
        return fromGregorian(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
        )
    }

    fun fromGregorian(gy: Int, gm: Int, gd: Int): Date {
        val gDaysInMonth = intArrayOf(0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        val jDaysInMonth = intArrayOf(0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)

        var gy2 = gy - 1600
        var gm2 = gm - 1
        var gd2 = gd - 1

        var gDayNo = 365 * gy2 + (gy2 + 3) / 4 - (gy2 + 99) / 100 + (gy2 + 399) / 400
        for (i in 0 until gm2) gDayNo += gDaysInMonth[i + 1]
        if (gm2 > 1 && ((gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0))) gDayNo += 1
        gDayNo += gd2

        var jDayNo = gDayNo - 79
        val jNp = jDayNo / 12053
        jDayNo %= 12053
        var jy = 979 + 33 * jNp + 4 * (jDayNo / 1461)
        jDayNo %= 1461
        if (jDayNo >= 366) {
            jy += (jDayNo - 1) / 365
            jDayNo = (jDayNo - 1) % 365
        }
        var jm = 0
        var i = 1
        while (i <= 12 && jDayNo >= jDaysInMonth[i]) {
            jDayNo -= jDaysInMonth[i]
            i++
        }
        jm = i
        val jd = jDayNo + 1
        return Date(jy, jm, jd)
    }
}

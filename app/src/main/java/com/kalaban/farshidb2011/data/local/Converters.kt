package com.kalaban.farshidb2011.data.local

import androidx.room.TypeConverter
import com.kalaban.farshidb2011.data.local.entity.PaymentType

class Converters {
    @TypeConverter
    fun toPaymentType(value: String): PaymentType = PaymentType.valueOf(value)

    @TypeConverter
    fun fromPaymentType(value: PaymentType): String = value.name
}

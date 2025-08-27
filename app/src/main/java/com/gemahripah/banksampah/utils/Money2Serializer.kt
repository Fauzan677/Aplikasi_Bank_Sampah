package com.gemahripah.banksampah.utils

import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive

object Money2Serializer : KSerializer<BigDecimal> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("BigDecimalFlexible", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): BigDecimal {
        return when (decoder) {
            is JsonDecoder -> {
                val p = decoder.decodeJsonElement() as JsonPrimitive
                val raw = if (p.isString) p.content else p.content
                BigDecimal(raw)
            }
            else -> BigDecimal(decoder.decodeString())
        }
    }

    override fun serialize(encoder: Encoder, value: BigDecimal) {
        // 2 angka di belakang koma
        val txt = value.setScale(2, RoundingMode.HALF_UP).toPlainString()
        when (encoder) {
            is JsonEncoder -> encoder.encodeJsonElement(JsonPrimitive(txt))
            else -> encoder.encodeString(txt)
        }
    }
}
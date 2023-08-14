package com.sytoss.aiHelper.services.codeCreating

import com.sytoss.aiHelper.bom.codeCreating.ModelType
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object ModelTypeSerializer : KSerializer<ModelType> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ModelType", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ModelType) {
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): ModelType {
        val value = decoder.decodeString()
        return requireNotNull(ModelType.values().find { it.value == value }) {
            "Unknown ModelType value: $value"
        }
    }
}
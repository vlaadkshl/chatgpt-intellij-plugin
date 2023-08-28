package com.sytoss.aiHelper.services.codeCreating

import com.sytoss.aiHelper.bom.codeCreating.CreateResponse
import com.sytoss.aiHelper.bom.codeCreating.ElementType
import com.sytoss.aiHelper.exceptions.PumlNotFoundException
import com.sytoss.aiHelper.exceptions.generationException.ElementNotGeneratedException

object CodeCreatingService {

    suspend fun createBom(pumlContent: String?): CreateResponse {
        return pumlContent?.let { puml ->
            CodeCreatorWithChatServer.generateBomFromPuml(puml)
        } ?: throw PumlNotFoundException()
    }

    suspend fun createDtoFromBom(bomCodes: List<String>): CreateResponse? {
        if (bomCodes.isEmpty()) {
            throw ElementNotGeneratedException(ElementType.BOM)
        }

        return CodeCreatorWithChatServer.generateDtoFromBom(bomCodes)
    }

    suspend fun createDtoFromPuml(pumlContent: String?): CreateResponse {
        return pumlContent?.let { puml ->
            CodeCreatorWithChatServer.generateDtoFromPuml(puml)
        } ?: throw PumlNotFoundException()
    }

    suspend fun createConverters(bomTexts: List<String>, dtoTexts: List<String>): CreateResponse? {
        if (bomTexts.isEmpty() && dtoTexts.isEmpty()) {
            throw ElementNotGeneratedException(ElementType.BOM, ElementType.DTO)
        }

        return CodeCreatorWithChatServer.generateConverters(bomTexts, dtoTexts)
    }
}
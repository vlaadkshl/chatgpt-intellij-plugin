package com.sytoss.aiHelper.services.codeCreating

import com.sytoss.aiHelper.bom.codeCreating.CreateRequest
import com.sytoss.aiHelper.bom.codeCreating.CreateResponse
import com.sytoss.aiHelper.bom.codeCreating.ModelType

object Creators {

    fun createBom(pumlContent: String): CreateResponse? {
        val request = CreateRequest(
            model = ModelType.GPT,
            prompt = """
                    Write java classes for BOM according to the PUML diagram below:
                    $pumlContent
                """.trimIndent(),
            example = """
                    Hotel.java:
                    ```java
                    import lombok.AllArgsConstructor;
                    import lombok.Builder;
                    import lombok.Data;
                    import lombok.NoArgsConstructor;
                    
                    import java.util.List;
                    
                    @Data
                    @Builder
                    @NoArgsConstructor
                    @AllArgsConstructor
                    public class Hotel {
                        private long id;
                    
                        private String address;
                    
                        private String type;
                    
                        private List<Room> rooms;
                    }
                    ```
                    
                    Room.java:
                    ```java
                    import lombok.AllArgsConstructor;
                    import lombok.Builder;
                    import lombok.Data;
                    import lombok.NoArgsConstructor;
                    
                    @Data
                    @Builder
                    @NoArgsConstructor
                    @AllArgsConstructor
                    public class Room {
                        private long id;
                    
                        private String number;
                    
                        private String level;
                    
                        private int capacity;
                    }
                    ```
                """.trimIndent()
        )

        return RequestSender.sendRequest(request)
    }
}
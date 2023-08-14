package com.sytoss.aiHelper.services.codeCreating

import com.intellij.openapi.vfs.VirtualFile
import com.sytoss.aiHelper.bom.codeCreating.CreateRequest
import com.sytoss.aiHelper.bom.codeCreating.CreateResponse
import com.sytoss.aiHelper.bom.codeCreating.ElementType
import com.sytoss.aiHelper.bom.codeCreating.ModelType
import java.nio.file.Files

object Creators {

    fun create(elemsToGenerate: Set<ElementType>, pumlFile: VirtualFile)
            : Map<ElementType, List<CreateResponse.CreateContent>> {
        val result = mutableMapOf<ElementType, List<CreateResponse.CreateContent>>()

        if (elemsToGenerate.contains(ElementType.CONVERTER)
            && (!elemsToGenerate.contains(ElementType.BOM) || !elemsToGenerate.contains(ElementType.DTO))
        ) {
            return result
        }

        if (elemsToGenerate.contains(ElementType.BOM)) {
            val pumlContent = Files.readString(pumlFile.toNioPath())
            val createdResponse = createBom(pumlContent)

            if (createdResponse != null) {
                result[ElementType.BOM] = createdResponse.result
            }
        }

        if (elemsToGenerate.contains(ElementType.DTO)) {
            result[ElementType.BOM]?.let {
                val createdResponse = createDto(it)

                if (createdResponse != null) {
                    result[ElementType.DTO] = createdResponse.result
                }
            }
        }

        if (elemsToGenerate.contains(ElementType.CONVERTER)) {
            result[ElementType.BOM]?.let { boms ->
                result[ElementType.DTO]?.let { dtos ->
                    val createdResponse = createConverters(boms, dtos)

                    if (createdResponse != null) {
                        result[ElementType.CONVERTER] = createdResponse.result
                    }
                }
            }
        }

        return result
    }

    private fun createBom(pumlContent: String): CreateResponse? {
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

    private fun createDto(bomElements: List<CreateResponse.CreateContent>): CreateResponse? {
        val request = CreateRequest(
            model = ModelType.GPT,
            prompt = """
                    Write java classes for DTO according to the BOM classes below:
                    ${bomElements.joinToString(separator = "\n") { it.body }}
                """.trimIndent(),
            example = """
                    CourseDto.java:
                    ```java
                    import jakarta.persistence.*;
                    import lombok.AllArgsConstructor;
                    import lombok.Builder;
                    import lombok.Data;
                    import lombok.NoArgsConstructor;
                    
                    import java.util.List;
                    
                    @Entity
                    @Data
                    @Builder
                    @Table(name = "COURSE")
                    @NoArgsConstructor
                    @AllArgsConstructor
                    public class CourseDto {
                        @Id
                        @GeneratedValue(strategy = GenerationType.IDENTITY)
                        private long id;
                    
                        @Column(name = "name")
                        private String name;
                    }
                    ```
                    
                    StudentDto.java:
                    ```java
                    import jakarta.persistence.*;
                    import lombok.AllArgsConstructor;
                    import lombok.Builder;
                    import lombok.Data;
                    import lombok.NoArgsConstructor;
                    
                    @Entity
                    @Data
                    @Builder
                    @Table(name = "STUDENT")
                    @NoArgsConstructor
                    @AllArgsConstructor
                    public class StudentDto {
                        @Id
                        @GeneratedValue(strategy = GenerationType.IDENTITY)
                        private long id;
                    
                        @Column(name = "name")
                        private String name;
                    
                        @ManyToOne
                        @JoinColumn(name = "course_id")
                        private CourseDto course;
                    }
                    ```
                """.trimIndent()
        )

        return RequestSender.sendRequest(request)
    }

    private fun createConverters(
        boms: List<CreateResponse.CreateContent>,
        dtos: List<CreateResponse.CreateContent>
    ): CreateResponse? {
        val request = CreateRequest(
            model = ModelType.GPT,
            prompt = """
                    Write java classes for Converters from BOM to DTO and vice versa according to the classes below:
                    
                    BOM:
                    ${boms.joinToString(separator = "\n\n") { it.body }}
                    
                    DTO:
                    ${dtos.joinToString(separator = "\n\n") { it.body }}
                """.trimIndent(),
            example = """
                    BookConverter:
                    ```java
                    package com.sytoss.edu.library.converters;

                    import com.sytoss.edu.library.bom.Author;
                    import com.sytoss.edu.library.bom.Book;
                    import com.sytoss.edu.library.dto.AuthorDTO;
                    import com.sytoss.edu.library.dto.BookDTO;
                    import com.sytoss.edu.library.dto.GenreDTO;
                    import lombok.RequiredArgsConstructor;
                    import org.springframework.stereotype.Component;

                    import java.util.HashSet;
                    import java.util.Set;
                    import java.util.stream.Collectors;

                    @Component
                    @RequiredArgsConstructor
                    public class BookConverter {

                        private final AuthorConverter authorConverter;

                        private final GenreConverter genreConverter;

                        public void toDto(Book source, BookDTO destination) {
                            destination.setName(source.getName());
                            destination.setLanguage(source.getLanguage());
                            destination.setYearOfPublishing(source.getYearOfPublishing());
                            AuthorDTO authorDTO = new AuthorDTO();
                            authorConverter.toDto(source.getAuthor(), authorDTO);
                            destination.setAuthor(authorDTO);
                            Set<GenreDTO> genres = new HashSet<>();
                            genreConverter.toDto(source.getGenres(), genres);
                            destination.setGenres(genres);
                        }

                        public void fromDTO(BookDTO source, Book book) {
                            book.setId(source.getId());
                            book.setName(source.getName());
                            book.setLanguage(source.getLanguage());
                            book.setYearOfPublishing(source.getYearOfPublishing());
                            book.setGenres(source.getGenres()
                                    .stream()
                                    .map(genreConverter::fromDTO)
                                    .collect(Collectors.toList()));
                            Author author = new Author();
                            authorConverter.fromDTO(source.getAuthor(), author);
                            book.setAuthor(author);
                        }
                    }
                    ```
                """.trimIndent()
        )

        return RequestSender.sendRequest(request)
    }
}
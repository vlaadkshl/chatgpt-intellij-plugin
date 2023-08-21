package com.sytoss.aiHelper.services.codeCreating

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.Messages
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.util.ui.components.BorderLayoutPanel
import com.sytoss.aiHelper.bom.codeCreating.CreateRequest
import com.sytoss.aiHelper.bom.codeCreating.CreateResponse
import com.sytoss.aiHelper.bom.codeCreating.ModelType
import com.sytoss.aiHelper.services.CommonFields.applicationManager
import com.sytoss.aiHelper.services.CommonFields.dumbService
import com.sytoss.aiHelper.services.UiBuilder
import com.sytoss.aiHelper.ui.components.DefaultConstraints
import com.sytoss.aiHelper.ui.components.JButtonWithListener
import java.awt.FlowLayout
import java.awt.GridBagLayout
import javax.swing.BorderFactory.createEmptyBorder
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.SwingConstants
import kotlin.concurrent.thread

object Creators {
    var isNeedContinue = false

    private fun needsContinue(button: JButton) {
        isNeedContinue = true
        button.isEnabled = false
    }

    private lateinit var retryButton: JButtonWithListener

    private lateinit var editorsResultMap: MutableMap<String, Editor>

    fun create(
        continuable: Boolean,
        tabbedPane: JBTabbedPane,
        tabComponent: BorderLayoutPanel,
        componentIndex: Int,
        generateFun: ((CreateResponse) -> Unit) -> Unit
    ): MutableMap<String, Editor> {
        val editors = mutableMapOf<String, Editor>()

        val loadingLabel = JBLabel("Loading...", AnimatedIcon.Default(), SwingConstants.LEFT)
        try {
            val innerPanel = JPanel(GridBagLayout())
            innerPanel.add(loadingLabel, DefaultConstraints.topLeftColumn)

            val innerPanelWrapper = JPanel(FlowLayout(FlowLayout.LEFT))
            innerPanelWrapper.add(innerPanel)

            val innerPanelScrollable = JBScrollPane(innerPanelWrapper)
            innerPanelScrollable.border = createEmptyBorder()

            val continueButton = JButtonWithListener("Continue") { needsContinue(it.source as JButton) }
            continueButton.isEnabled = false

            retryButton = JButtonWithListener("Retry") {
                thread {
                    retryFun(generateFun, editors, innerPanel, continueButton, loadingLabel)
                }
            }
            retryButton.isEnabled = false

            val buttonsGroup = JPanel(FlowLayout(FlowLayout.LEFT))
            if (continuable) {
                buttonsGroup.add(continueButton)
            }
            buttonsGroup.add(retryButton)

            tabComponent.addToTop(buttonsGroup)
            tabComponent.addToCenter(innerPanelScrollable)

            applicationManager.invokeAndWait {
                tabbedPane.setEnabledAt(componentIndex, true)
                tabbedPane.selectedComponent = tabComponent
            }

            generateFun { response ->
                applicationManager.invokeAndWait {
                    editorsResultMap = UiBuilder.buildCreateClassesPanel(response, innerPanel)
                    editors.putAll(editorsResultMap)
                    continueButton.isEnabled = true
                }
            }
        } catch (e: Throwable) {
            dumbService.smartInvokeLater { Messages.showErrorDialog(e.message, "Error") }
        } finally {
            dumbService.smartInvokeLater {
                loadingLabel.isVisible = false
                retryButton.isEnabled = true
            }
        }

        return editors
    }

    private fun retryFun(
        generateFun: ((CreateResponse) -> Unit) -> Unit,
        editors: MutableMap<String, Editor>,
        innerPanel: JPanel,
        continueButton: JButtonWithListener,
        loadingLabel: JBLabel
    ) {
        try {
            dumbService.smartInvokeLater {
                loadingLabel.isVisible = true
                innerPanel.isVisible = false
            }

            generateFun { response ->
                applicationManager.invokeAndWait {
                    editorsResultMap.keys.forEach { editors.remove(it) }
                    innerPanel.removeAll()
                    innerPanel.isVisible = true

                    editors.putAll(UiBuilder.buildCreateClassesPanel(response, innerPanel))
                    continueButton.isEnabled = true
                }
            }
        } catch (e: Throwable) {
            dumbService.smartInvokeLater { Messages.showErrorDialog(e.message, "Error") }
        } finally {
            dumbService.smartInvokeLater {
                loadingLabel.isVisible = false
                retryButton.isEnabled = true
            }
        }
    }

    fun createBom(pumlContent: String): CreateResponse? {
        val request = CreateRequest(
            model = ModelType.GPT,
            prompt = """
                    Write java classes for BOM according to the PUML diagram below. If there is some connections, that are displayed in PUML file, you need to put them in code.
                    
                    PUML diagram:
                    $pumlContent
                """.trimIndent(),
            example = bomExample
        )

        return RequestSender.sendRequest(request)
    }

    fun createDto(pumlContent: String): CreateResponse? {
        val request = CreateRequest(
            model = ModelType.GPT,
            prompt = """
                    Write java classes for DTO according to the PUML diagram below. If there is some connections, that are displayed in PUML file, you need to put them in code.
                    
                    PUML diagram:
                    $pumlContent
                """.trimIndent(),
            example = dtoExample
        )

        return RequestSender.sendRequest(request)
    }

    fun createDto(bomElements: List<String>): CreateResponse? {
        val request = CreateRequest(
            model = ModelType.GPT,
            prompt = """
                    Create DTO classes according to the BOM classes below:
                    ${bomElements.joinToString(separator = "\n")}
                """.trimIndent(),
            example = dtoExample
        )

        return RequestSender.sendRequest(request)
    }

    fun createConverters(boms: List<String>, dtos: List<String>): CreateResponse? {
        val request = CreateRequest(
            model = ModelType.GPT,
            prompt = """
                    Create Converter classes from BOM to DTO and vice versa according to the classes below:
                    
                    BOMs:
                    ${boms.joinToString(separator = "\n\n")}
                    
                    DTOs:
                    ${dtos.joinToString(separator = "\n\n")}
                """.trimIndent(),
            example = converterExample
        )

        return RequestSender.sendRequest(request)
    }

    private val bomExample = """
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

    private val dtoExample = """
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

    private val converterExample = """
        BookConverter:
        ```java
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
}
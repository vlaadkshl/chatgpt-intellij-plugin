package com.sytoss.aiHelper.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.OnePixelSplitter
import com.sytoss.aiHelper.bom.codeCreating.CreateRequest
import com.sytoss.aiHelper.bom.codeCreating.ModelType
import com.sytoss.aiHelper.services.PumlDiagramChooser
import com.sytoss.aiHelper.services.codeCreating.RequestSender
import com.sytoss.aiHelper.ui.components.DefaultConstraints
import com.sytoss.aiHelper.ui.components.FileChooserCreateComponent
import com.sytoss.aiHelper.ui.components.JButtonWithListener
import com.sytoss.aiHelper.ui.components.ScrollWithInsets
import java.awt.GridBagLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

class CodeCreatingToolWindowContent(private val project: Project) {

    private val mainPanel = JPanel(GridBagLayout())

    val contentPanel = OnePixelSplitter()

    private val pumlChooserBtn = JButtonWithListener("Choose PlantUML file") {
        PumlDiagramChooser.selectFile(it.source as JButton, project)
    }

    private val bomChooser = FileChooserCreateComponent("Create DTO based on BOM", project)

    init {
        contentPanel.firstComponent = ScrollWithInsets { mainPanel }
        addWithConstraints(pumlChooserBtn)
        addWithConstraints(JButtonWithListener("Send Request") {
            val request = CreateRequest(
                model = ModelType.GPT,
                prompt = """
                    Write Java class Library using Lombok annotation with fields id, name, address, specialization, list of books.
                    Write Java class Book using Lombok annotation with id, title, year of publication, number in the library.
                    Write entity for DB LibraryDTO class based on Library class and BookDTO class based on Book class.
                    Use JPA jakarta annotations only for LibraryDTO and BookDTO classes to save in database.
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
                    
                    HotelDTO.java:
                    ```java
                    
                    import jakarta.persistence.*;
                    import lombok.AllArgsConstructor;
                    import lombok.Builder;
                    import lombok.Data;
                    import lombok.NoArgsConstructor;
                    
                    import java.util.List;
                    
                    @Data
                    @Builder
                    @NoArgsConstructor
                    @AllArgsConstructor
                    @Entity
                    @Table(name = "HOTEL")
                    public class HotelDTO {
                        @Id
                        @GeneratedValue(strategy = GenerationType.IDENTITY)
                        private long id;
                    
                        @Column(name = "ADDRESS")
                        private String address;
                    
                        @Column(name = "TYPE")
                        private String type;
                    
                    
                        @OneToMany(fetch = FetchType.EAGER, mappedBy = "hotel", cascade = CascadeType.ALL)
                        private List<RoomDTO> rooms;
                    }
                    ```
                    
                    RoomDTO.java:
                    ```java
                    import jakarta.persistence.*;
                    import lombok.AllArgsConstructor;
                    import lombok.Builder;
                    import lombok.Data;
                    import lombok.NoArgsConstructor;
                    
                    @Data
                    @Builder
                    @NoArgsConstructor
                    @AllArgsConstructor
                    @Entity
                    @Table(name = "ROOM")
                    public class RoomDTO {
                        @Id
                        @GeneratedValue(strategy = GenerationType.IDENTITY)
                        private long id;
                    
                        @Column(name = "NUMBER")
                        private String number;
                    
                        @Column(name = "LEVEL")
                        private String level;
                    
                        @Column(name = "CAPACITY")
                        private int capacity;
                    
                        @ManyToOne
                        @JoinColumn(name = "HOTEL_ID", referencedColumnName = "ID")
                        private HotelDTO hotel;
                    }
                    ```
                """.trimIndent()
            )

            val response = RequestSender.sendRequest(request)
            Messages.showInfoMessage(
                response?.result?.joinToString(separator = "\n\n") { it.body } ?: "No Response.",
                "Response"
            )
        })
        addWithConstraints(bomChooser)

        contentPanel.secondComponent = ScrollWithInsets { JPanel() }
    }

    private fun addWithConstraints(component: JComponent) {
        mainPanel.add(component, DefaultConstraints.topLeftColumn)
    }
}
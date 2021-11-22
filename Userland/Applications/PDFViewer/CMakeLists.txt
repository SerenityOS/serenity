serenity_component(
    PDFViewer
    TARGETS PDFViewer
)

set(SOURCES
    NumericInput.cpp
    OutlineModel.cpp
    PDFViewer.cpp
    PDFViewerWidget.cpp
    SidebarWidget.cpp
    main.cpp
    )

serenity_app(PDFViewer ICON app-pdf-viewer)
target_link_libraries(PDFViewer LibGUI LibPDF LibFileSystemAccessClient LibMain)

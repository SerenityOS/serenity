serenity_component(
    PDFViewer
    TARGETS PDFViewer
)

set(SOURCES
    OutlineModel.cpp
    PDFViewer.cpp
    PDFViewerWidget.cpp
    SidebarWidget.cpp
    ThumbnailsListView.cpp
    ThumbnailsModel.cpp
    main.cpp
    )

serenity_app(PDFViewer ICON app-pdf-viewer)
target_link_libraries(PDFViewer PRIVATE LibCore LibGfx LibGUI LibPDF LibFileSystemAccessClient LibConfig LibMain LibURL)

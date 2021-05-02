set(SOURCES
    PDFViewer.cpp
    PDFViewerWidget.cpp
    main.cpp
)

serenity_app(PDFViewer ICON app-pdf-viewer)
target_link_libraries(PDFViewer LibGUI LibPDF)

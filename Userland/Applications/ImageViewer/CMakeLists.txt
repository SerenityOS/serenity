serenity_component(
    ImageViewer
    RECOMMENDED
    TARGETS ImageViewer
    DEPENDS ImageDecoder
)

set(SOURCES
    main.cpp
        MainWidget.cpp
        ViewWidget.cpp
)

serenity_app(ImageViewer ICON app-image-viewer)
target_link_libraries(ImageViewer PRIVATE LibCore LibDesktop LibFileSystemAccessClient LibGUI LibGfx LibConfig LibImageDecoderClient LibMain LibURL)

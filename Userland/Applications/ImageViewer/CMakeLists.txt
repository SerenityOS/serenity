serenity_component(
    ImageViewer
    RECOMMENDED
    TARGETS ImageViewer
    DEPENDS ImageDecoder
)

set(SOURCES
    main.cpp
        ViewWidget.cpp
)

serenity_app(ImageViewer ICON filetype-image)
target_link_libraries(ImageViewer PRIVATE LibCore LibDesktop LibGUI LibGfx LibImageDecoderClient LibMain)

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
target_link_libraries(ImageViewer LibDesktop LibGUI LibGfx LibImageDecoderClient LibMain)

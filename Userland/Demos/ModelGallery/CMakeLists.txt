serenity_component(
    ModelGallery
    TARGETS ModelGallery
)

set(SOURCES
    main.cpp
    GalleryWidget.cpp
    BasicModel.cpp
)

serenity_app(ModelGallery ICON app-model-gallery)

target_link_libraries(ModelGallery PRIVATE LibCore LibGUI LibGfx LibMain)

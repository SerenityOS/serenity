serenity_component(
    ModelGallery
    TARGETS ModelGallery
)

stringify_gml(./BasicModelTab.gml BasicModelTabGML.h basic_model_tab_gml)

set(SOURCES
    main.cpp
    GalleryWidget.cpp
    BasicModel.cpp
)

set(GENERATED_SOURCES
    BasicModelTabGML.h
)

serenity_app(ModelGallery ICON app-model-gallery)

target_link_libraries(ModelGallery PRIVATE LibCore LibGUI LibGfx LibMain)

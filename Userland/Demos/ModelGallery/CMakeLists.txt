serenity_component(
    ModelGallery
    TARGETS ModelGallery
)

compile_gml(./BasicModelTab.gml BasicModelTabGML.h basic_model_tab_gml)

set(SOURCES
    main.cpp
    GalleryWidget.cpp
    BasicModel.cpp
    BasicModelTabGML.h
)

serenity_app(ModelGallery ICON app-model-gallery)

target_link_libraries(ModelGallery LibGUI LibGfx LibMain)

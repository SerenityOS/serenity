serenity_component(
    MapsSettings
    RECOMMENDED
    TARGETS MapsSettings
)

compile_gml(MapsSettingsWidget.gml MapsSettingsWidgetGML.cpp)

set(SOURCES
    main.cpp
    MapsSettingsWidgetGML.cpp
    MapsSettingsWidget.cpp
)

serenity_app(MapsSettings ICON app-maps)
target_link_libraries(MapsSettings PRIVATE LibConfig LibCore LibGfx LibGUI LibMain LibURL)

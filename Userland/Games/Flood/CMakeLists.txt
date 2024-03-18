serenity_component(
    Flood
    RECOMMENDED
    TARGETS Flood
)

compile_gml(FloodWindow.gml FloodWindowGML.cpp)
compile_gml(SettingsWidget.gml SettingsWidgetGML.cpp)

set(SOURCES
    Board.cpp
    BoardWidget.cpp
    FloodWindowGML.cpp
    SettingsDialog.cpp
    SettingsWidgetGML.cpp
    main.cpp
)

serenity_app(Flood ICON app-flood)
target_link_libraries(Flood PRIVATE LibConfig LibCore LibDesktop LibGfx LibGUI LibMain LibURL)

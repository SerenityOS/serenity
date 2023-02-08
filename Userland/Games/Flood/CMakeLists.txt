serenity_component(
    Flood
    RECOMMENDED
    TARGETS Flood
)

stringify_gml(FloodWindow.gml FloodWindowGML.h flood_window_gml)
stringify_gml(SettingsDialog.gml SettingsDialogGML.h settings_dialog_gml)

set(SOURCES
    Board.cpp
    BoardWidget.cpp
    SettingsDialog.cpp
    main.cpp
)

set(GENERATED_SOURCES
    FloodWindowGML.h
    SettingsDialogGML.h
)

serenity_app(Flood ICON app-flood)
target_link_libraries(Flood PRIVATE LibConfig LibCore LibDesktop LibGfx LibGUI LibMain)

serenity_component(
    2048
    RECOMMENDED
    TARGETS 2048
)

stringify_gml(GameSizeDialog.gml GameSizeDialogGML.h game_size_dialog_gml)
stringify_gml(GameWindow.gml GameWindowGML.h game_window_gml)

set(SOURCES
    BoardView.cpp
    Game.cpp
    GameSizeDialog.cpp
    main.cpp
)

set(GENERATED_SOURCES
    GameSizeDialogGML.h
    GameWindowGML.h
)

serenity_app(2048 ICON app-2048)
target_link_libraries(2048 PRIVATE LibConfig LibCore LibGfx LibGUI LibMain LibDesktop)

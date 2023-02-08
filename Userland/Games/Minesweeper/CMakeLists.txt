serenity_component(
    Minesweeper
    RECOMMENDED
    TARGETS Minesweeper
)

stringify_gml(MinesweeperCustomGameWindow.gml MinesweeperCustomGameWindowGML.h minesweeper_custom_game_window_gml)
stringify_gml(MinesweeperWindow.gml MinesweeperWindowGML.h minesweeper_window_gml)

set(SOURCES
    CustomGameDialog.cpp
    Field.cpp
    main.cpp
)

set(GENERATED_SOURCES
    MinesweeperCustomGameWindowGML.h
    MinesweeperWindowGML.h
)

serenity_app(Minesweeper ICON app-minesweeper)
target_link_libraries(Minesweeper PRIVATE LibCore LibGfx LibGUI LibConfig LibMain LibDesktop)

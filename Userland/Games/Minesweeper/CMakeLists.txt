serenity_component(
    Minesweeper
    RECOMMENDED
    TARGETS Minesweeper
)

compile_gml(MinesweeperCustomGameWindow.gml MinesweeperCustomGameWindowGML.h minesweeper_custom_game_window_gml)

set(SOURCES
    MinesweeperCustomGameWindowGML.h
    CustomGameDialog.cpp
    Field.cpp
    main.cpp
)

serenity_app(Minesweeper ICON app-minesweeper)
target_link_libraries(Minesweeper LibGUI LibConfig LibMain)

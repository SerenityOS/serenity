serenity_component(
    GameOfLife
    RECOMMENDED
    TARGETS GameOfLife
)

compile_gml(GameOfLife.gml GameOfLifeGML.h game_of_life_gml)

set(SOURCES
    main.cpp
    Board.cpp
    BoardWidget.cpp
    GameOfLifeGML.h
    Pattern.cpp
)

serenity_app(GameOfLife ICON app-gameoflife)
target_link_libraries(GameOfLife LibGUI LibMain)

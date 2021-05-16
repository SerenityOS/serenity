compile_gml(GameOfLife.gml GameOfLifeGML.h game_of_life_gml)

set(SOURCES
    Board.cpp
    Pattern.cpp
    BoardWidget.cpp
    PatternWidget.cpp
    GameOfLifeGML.h
    main.cpp
)

serenity_app(GameOfLife ICON app-gameoflife)
target_link_libraries(GameOfLife LibGUI)

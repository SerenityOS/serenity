serenity_component(
    GameOfLife
    RECOMMENDED
    TARGETS GameOfLife
)

stringify_gml(GameOfLife.gml GameOfLifeGML.h game_of_life_gml)

set(SOURCES
    main.cpp
    Board.cpp
    BoardWidget.cpp
    Pattern.cpp
)

set(GENERATED_SOURCES
    GameOfLifeGML.h
)

serenity_app(GameOfLife ICON app-gameoflife)
target_link_libraries(GameOfLife PRIVATE LibCore LibGfx LibGUI LibMain LibDesktop)

serenity_component(
    GameOfLife
    RECOMMENDED
    TARGETS GameOfLife
)

compile_gml(GameOfLife.gml GameOfLifeGML.cpp)

set(SOURCES
    main.cpp
    Board.cpp
    BoardWidget.cpp
    GameOfLifeGML.cpp
    Pattern.cpp
)

serenity_app(GameOfLife ICON app-gameoflife)
target_link_libraries(GameOfLife PRIVATE LibCore LibGfx LibGUI LibMain LibDesktop LibURL)

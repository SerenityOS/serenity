serenity_component(
    Minesweeper
    RECOMMENDED
    TARGETS Minesweeper
)

compile_gml(MainWidget.gml MainWidgetGML.cpp)
compile_gml(CustomGameWidget.gml CustomGameWidgetGML.cpp)

set(SOURCES
    CustomGameDialog.cpp
    CustomGameWidgetGML.cpp
    Field.cpp
    main.cpp
    MainWidgetGML.cpp
)

serenity_app(Minesweeper ICON app-minesweeper)
target_link_libraries(Minesweeper PRIVATE LibCore LibGfx LibGUI LibConfig LibMain LibDesktop LibURL)

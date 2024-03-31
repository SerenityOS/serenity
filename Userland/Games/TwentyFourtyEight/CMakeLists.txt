serenity_component(
    2048
    RECOMMENDED
    TARGETS 2048
)

compile_gml(GameSizeDialog.gml GameSizeDialogGML.cpp)
compile_gml(GameWindowWidget.gml GameWindowWidgetGML.cpp)

set(SOURCES
    BoardView.cpp
    Game.cpp
    GameSizeDialog.cpp
    GameSizeDialogGML.cpp
    GameWindowWidgetGML.cpp
    main.cpp
)

serenity_app(2048 ICON app-2048)
target_link_libraries(2048 PRIVATE LibConfig LibCore LibGfx LibGUI LibMain LibDesktop LibURL)

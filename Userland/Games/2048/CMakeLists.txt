serenity_component(
    2048
    RECOMMENDED
    TARGETS 2048
)

set(SOURCES
    BoardView.cpp
    Game.cpp
    GameSizeDialog.cpp
    main.cpp
)

serenity_app(2048 ICON app-2048)
target_link_libraries(2048 LibConfig LibGUI LibMain)

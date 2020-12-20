set(SOURCES
    BoardView.cpp
    Game.cpp
    GameSizeDialog.cpp
    main.cpp
)

serenity_app(2048 ICON app-2048)
target_link_libraries(2048 LibGUI)

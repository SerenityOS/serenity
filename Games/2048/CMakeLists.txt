set(SOURCES
    BoardView.cpp
    Game.cpp
    GameSizeDialog.cpp
    main.cpp
)

serenity_bin(2048)
target_link_libraries(2048 LibGUI)

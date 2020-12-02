set(SOURCES
    main.cpp
    Game.cpp
    LevelSelectDialog.cpp
)

serenity_bin(Breakout)
target_link_libraries(Breakout LibGUI)

set(SOURCES
    main.cpp
    Game.cpp
)

serenity_bin(Breakout)
target_link_libraries(Breakout LibGUI)

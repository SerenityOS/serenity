set(SOURCES
    main.cpp
    Game.cpp
)

serenity_bin(Pong)
target_link_libraries(Pong LibGUI)

set(SOURCES
    main.cpp
    SnakeGame.cpp
)

serenity_bin(Snake)
target_link_libraries(Snake LibGUI)

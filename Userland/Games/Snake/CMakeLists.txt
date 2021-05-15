set(SOURCES
    main.cpp
    SnakeGame.cpp
)

serenity_app(Snake ICON app-snake)
target_link_libraries(Snake LibGUI)

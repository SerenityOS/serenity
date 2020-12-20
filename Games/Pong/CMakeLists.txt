set(SOURCES
    main.cpp
    Game.cpp
)

serenity_app(Pong ICON app-pong)
target_link_libraries(Pong LibGUI)

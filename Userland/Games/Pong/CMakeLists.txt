serenity_component(
    Pong
    RECOMMENDED
    TARGETS Pong
)

set(SOURCES
    main.cpp
    Game.cpp
)

serenity_app(Pong ICON app-pong)
target_link_libraries(Pong LibGUI LibMain LibDesktop)

serenity_component(
    FlappyBug
    RECOMMENDED
    TARGETS FlappyBug
)

set(SOURCES
    main.cpp
    Game.cpp
)

serenity_app(FlappyBug ICON app-flappybug)
target_link_libraries(FlappyBug PRIVATE LibCore LibGfx LibGUI LibConfig LibMain LibDesktop LibURL)

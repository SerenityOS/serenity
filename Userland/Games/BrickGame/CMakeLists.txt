serenity_component(
    BrickGame
    RECOMMENDED
    TARGETS BrickGame
)

set(SOURCES
    main.cpp
    BrickGame.cpp
)

serenity_app(BrickGame ICON app-brickgame)
target_link_libraries(BrickGame PRIVATE LibGUI LibCore LibGfx LibConfig LibMain LibDesktop LibURL)

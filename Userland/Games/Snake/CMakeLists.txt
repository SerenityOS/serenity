serenity_component(
    Snake
    RECOMMENDED
    TARGETS Snake
)

stringify_gml(Snake.gml SnakeGML.h snake_gml)

set(SOURCES
    Game.cpp
    main.cpp
    Skins/ClassicSkin.cpp
    Skins/ImageSkin.cpp
    Skins/SnakeSkin.cpp
)

set(GENERATED_SOURCES
    SnakeGML.h
)

serenity_app(Snake ICON app-snake)
target_link_libraries(Snake PRIVATE LibCore LibFileSystem LibGfx LibGUI LibConfig LibMain LibDesktop)

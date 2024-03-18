serenity_component(
    Snake
    RECOMMENDED
    TARGETS Snake
)

compile_gml(Snake.gml SnakeGML.cpp)

set(SOURCES
    Game.cpp
    main.cpp
    Skins/ClassicSkin.cpp
    Skins/ImageSkin.cpp
    Skins/SnakeSkin.cpp
    SnakeGML.cpp
)

serenity_app(Snake ICON app-snake)
target_link_libraries(Snake PRIVATE LibCore LibFileSystem LibGfx LibGUI LibConfig LibMain LibDesktop LibURL)

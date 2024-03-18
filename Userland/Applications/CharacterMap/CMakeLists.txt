serenity_component(
    CharacterMap
    RECOMMENDED
    TARGETS CharacterMap
)

stringify_gml(CharacterMapWindow.gml CharacterMapWindowGML.h character_map_window_gml)
stringify_gml(CharacterSearchWindow.gml CharacterSearchWindowGML.h character_search_window_gml)

set(SOURCES
    CharacterMapWidget.cpp
    CharacterSearchWidget.cpp
    SearchCharacters.cpp
    main.cpp
)

set(GENERATED_SOURCES
    CharacterMapWindowGML.h
    CharacterSearchWindowGML.h
)

serenity_app(CharacterMap ICON app-character-map)
target_link_libraries(CharacterMap PRIVATE LibConfig LibCore LibDesktop LibGfx LibGUI LibMain LibUnicode LibURL)

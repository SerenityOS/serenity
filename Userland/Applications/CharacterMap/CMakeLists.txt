serenity_component(
    CharacterMap
    RECOMMENDED
    TARGETS CharacterMap
)

set(SOURCES
    CharacterMapWidget.cpp
    CharacterSearchWidget.cpp
    SearchCharacters.cpp
    main.cpp
)

serenity_app(CharacterMap ICON app-character-map)
target_link_libraries(CharacterMap PRIVATE LibConfig LibCore LibDesktop LibGfx LibGUI LibMain LibUnicode LibURL)

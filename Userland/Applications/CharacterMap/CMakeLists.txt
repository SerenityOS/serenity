serenity_component(
    CharacterMap
    RECOMMENDED
    TARGETS CharacterMap
)

compile_gml(CharacterMapWidget.gml CharacterMapWidgetGML.cpp)
compile_gml(CharacterSearchWidget.gml CharacterSearchWidgetGML.cpp)

set(SOURCES
    CharacterMapWidget.cpp
    CharacterMapWidgetGML.cpp
    CharacterSearchWidget.cpp
    CharacterSearchWidgetGML.cpp
    SearchCharacters.cpp
    main.cpp
)

serenity_app(CharacterMap ICON app-character-map)
target_link_libraries(CharacterMap PRIVATE LibConfig LibCore LibDesktop LibGfx LibGUI LibMain LibUnicode)

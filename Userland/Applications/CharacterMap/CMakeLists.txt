serenity_component(
    CharacterMap
    RECOMMENDED
    TARGETS CharacterMap
)

compile_gml(CharacterMapWindow.gml CharacterMapWindowGML.h character_map_window_gml)
compile_gml(CharacterSearchWindow.gml CharacterSearchWindowGML.h character_search_window_gml)

set(SOURCES
    CharacterMapWidget.cpp
    CharacterMapWindowGML.h
    CharacterSearchWidget.cpp
    CharacterSearchWindowGML.h
    main.cpp
)

serenity_app(CharacterMap ICON app-character-map)
target_link_libraries(CharacterMap LibDesktop LibGUI LibMain LibUnicode)

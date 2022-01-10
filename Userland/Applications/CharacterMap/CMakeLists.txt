serenity_component(
    CharacterMap
    REQUIRED
    TARGETS CharacterMap
)

compile_gml(CharacterMapWindow.gml CharacterMapWindowGML.h character_map_window_gml)

set(SOURCES
    CharacterMapWindowGML.h
    CharacterMapWidget.cpp
    main.cpp
)

serenity_app(CharacterMap ICON app-keyboard-settings)
target_link_libraries(CharacterMap LibGUI LibMain)
link_with_unicode_data(CharacterMap)

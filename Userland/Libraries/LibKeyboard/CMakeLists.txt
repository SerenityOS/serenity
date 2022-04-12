set(SOURCES
    CharacterMap.cpp
    CharacterMapFile.cpp
    Keymap.cpp
)

serenity_lib(LibKeyboard keyboard)
target_link_libraries(LibKeyboard LibCore)

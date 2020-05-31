set(SOURCES
    CharacterMap.cpp
    CharacterMapFile.cpp
)

serenity_lib(LibKeyboard keyboard)
target_link_libraries(LibKeyboard LibCore)

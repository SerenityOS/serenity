include(unicode_data.cmake)

SET(SOURCES
    ${UNICODE_DATA_SOURCES}
    CharacterTypes.cpp
    Locale.cpp
)

serenity_lib(LibUnicode unicode)
target_link_libraries(LibUnicode LibCore)

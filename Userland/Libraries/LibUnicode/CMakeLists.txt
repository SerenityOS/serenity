include(unicode_data.cmake)

SET(SOURCES
    ${UNICODE_DATA_SOURCES}
    CharacterTypes.cpp
)

serenity_lib(LibUnicode unicode)
target_link_libraries(LibUnicode LibCore)

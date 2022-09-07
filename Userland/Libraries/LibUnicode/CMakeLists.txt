include(${SerenityOS_SOURCE_DIR}/Meta/CMake/unicode_data.cmake)

set(SOURCES
    CharacterTypes.cpp
    CurrencyCode.cpp
    Emoji.cpp
    ${UNICODE_DATA_SOURCES}
)

serenity_lib(LibUnicode unicode)
target_link_libraries(LibUnicode LibCore)
target_compile_definitions(LibUnicode PRIVATE ENABLE_UNICODE_DATA=$<BOOL:${ENABLE_UNICODE_DATABASE_DOWNLOAD}>)

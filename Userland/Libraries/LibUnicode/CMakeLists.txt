include(${SerenityOS_SOURCE_DIR}/Meta/CMake/unicode_data.cmake)

SET(SOURCES
    ${UNICODE_DATA_SOURCES}
    CharacterTypes.cpp
    CurrencyCode.cpp
    DateTimeFormat.cpp
    Locale.cpp
    NumberFormat.cpp
)

serenity_lib(LibUnicode unicode)
target_link_libraries(LibUnicode LibCore)
target_compile_definitions(LibUnicode PRIVATE ENABLE_UNICODE_DATA=$<BOOL:${ENABLE_UNICODE_DATABASE_DOWNLOAD}>)

include(${SerenityOS_SOURCE_DIR}/Meta/CMake/unicode_data.cmake)
include(${SerenityOS_SOURCE_DIR}/Meta/CMake/locale_data.cmake)

if (DEFINED UNICODE_DATA_SOURCES)
    set(SOURCES ${UNICODE_DATA_SOURCES} ${LOCALE_DATA_SOURCES})
    serenity_lib(LibUnicodeData unicodedata)
    target_compile_options(LibUnicodeData PRIVATE -g0 -Os -Wno-parentheses-equality)
    target_link_libraries(LibUnicodeData LibCore LibTimeZone)
endif()

set(SOURCES
    CharacterTypes.cpp
    CurrencyCode.cpp
    DateTimeFormat.cpp
    Locale.cpp
    NumberFormat.cpp
    PluralRules.cpp
    RelativeTimeFormat.cpp
)

serenity_lib(LibUnicode unicode)
target_link_libraries(LibUnicode LibCore)
target_compile_definitions(LibUnicode PRIVATE ENABLE_UNICODE_DATA=$<BOOL:${ENABLE_UNICODE_DATABASE_DOWNLOAD}>)

if (DEFINED UNICODE_DATA_SOURCES)
    add_dependencies(LibUnicode LibUnicodeData)
endif()

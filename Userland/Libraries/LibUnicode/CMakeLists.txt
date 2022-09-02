include(${SerenityOS_SOURCE_DIR}/Meta/CMake/unicode_data.cmake)
include(${SerenityOS_SOURCE_DIR}/Meta/CMake/locale_data.cmake)

if (DEFINED LOCALE_DATA_SOURCES)
    set(SOURCES ${LOCALE_DATA_SOURCES})
    serenity_lib(LibLocaleData localedata)
    target_compile_options(LibLocaleData PRIVATE -g0 -Os -Wno-parentheses-equality)
    target_link_libraries(LibLocaleData LibCore LibTimeZone)
endif()

set(SOURCES
    CharacterTypes.cpp
    CurrencyCode.cpp
    DateTimeFormat.cpp
    Locale.cpp
    NumberFormat.cpp
    PluralRules.cpp
    RelativeTimeFormat.cpp
    ${UNICODE_DATA_SOURCES}
)

serenity_lib(LibUnicode unicode)
target_link_libraries(LibUnicode LibCore)
target_compile_definitions(LibUnicode PRIVATE ENABLE_UNICODE_DATA=$<BOOL:${ENABLE_UNICODE_DATABASE_DOWNLOAD}>)

if (DEFINED LOCALE_DATA_SOURCES)
    add_dependencies(LibUnicode LibLocaleData)
endif()

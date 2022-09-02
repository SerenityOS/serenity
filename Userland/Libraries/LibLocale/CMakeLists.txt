include(${SerenityOS_SOURCE_DIR}/Meta/CMake/locale_data.cmake)

if (DEFINED LOCALE_DATA_SOURCES)
    set(SOURCES ${LOCALE_DATA_SOURCES})
    serenity_lib(LibLocaleData localedata)
    target_compile_options(LibLocaleData PRIVATE -g0 -Os -Wno-parentheses-equality)
    target_link_libraries(LibLocaleData LibCore LibTimeZone)
endif()

set(SOURCES
    DateTimeFormat.cpp
    Locale.cpp
    NumberFormat.cpp
    PluralRules.cpp
    RelativeTimeFormat.cpp
)

serenity_lib(LibLocale locale)
target_link_libraries(LibLocale LibCore LibUnicode)
target_compile_definitions(LibLocale PRIVATE ENABLE_UNICODE_DATA=$<BOOL:${ENABLE_UNICODE_DATABASE_DOWNLOAD}>)

if (DEFINED LOCALE_DATA_SOURCES)
    add_dependencies(LibLocale LibLocaleData)
endif()

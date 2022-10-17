include(${SerenityOS_SOURCE_DIR}/Meta/CMake/locale_data.cmake)

if (DEFINED LOCALE_DATA_SOURCES)
    set(SOURCES ${LOCALE_DATA_SOURCES})
    set(GENERATED_SOURCES ${CURRENT_LIB_GENERATED})
    if (SERENITYOS)
        serenity_lib(LibLocaleData localedata)
    else()
        add_library(LibLocaleData OBJECT ${SOURCES})
        serenity_generated_sources(LibLocaleData)
    endif()
    target_compile_options(LibLocaleData PRIVATE -g0 -Os -Wno-parentheses-equality)
    target_link_libraries(LibLocaleData LibCore LibTimeZone)
    unset(GENERATED_SOURCES)
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
    if (SERENITYOS)
        add_dependencies(LibLocale LibLocaleData)
    else()
        target_link_libraries(LibLocale LibLocaleData)
    endif()
endif()

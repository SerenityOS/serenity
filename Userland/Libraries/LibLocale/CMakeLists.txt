include(${SerenityOS_SOURCE_DIR}/Meta/CMake/locale_data.cmake)

if (DEFINED LOCALE_DATA_SOURCES)
    set(SOURCES ${LOCALE_DATA_SOURCES})
    serenity_lib(LibLocaleData localedata)
    target_compile_options(LibLocaleData PRIVATE -g0 -Os -Wno-parentheses-equality)
    target_link_libraries(LibLocaleData LibCore LibTimeZone)
endif()

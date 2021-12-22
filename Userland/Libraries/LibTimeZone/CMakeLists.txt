include(${SerenityOS_SOURCE_DIR}/Meta/CMake/time_zone_data.cmake)

if (DEFINED TIME_ZONE_DATA_SOURCES)
    set(SOURCES ${TIME_ZONE_DATA_SOURCES})
    serenity_lib(LibTimeZoneData timezonedata)
    target_compile_options(LibTimeZoneData PRIVATE -g0 -Os)
    target_link_libraries(LibTimeZoneData LibCore)
endif()

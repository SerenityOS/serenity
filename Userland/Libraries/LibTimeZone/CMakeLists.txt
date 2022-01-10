include(${SerenityOS_SOURCE_DIR}/Meta/CMake/time_zone_data.cmake)

set(SOURCES
    TimeZone.cpp
    ${TIME_ZONE_DATA_SOURCES}
)

serenity_lib(LibTimeZone timezone)
target_link_libraries(LibTimeZone LibCore)
target_compile_definitions(LibTimeZone PRIVATE ENABLE_TIME_ZONE_DATA=$<BOOL:${ENABLE_TIME_ZONE_DATABASE_DOWNLOAD}>)

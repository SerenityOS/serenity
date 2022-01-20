include(${SerenityOS_SOURCE_DIR}/Meta/CMake/time_zone_data.cmake)

set(SOURCES
    TimeZone.cpp
    ${TIME_ZONE_DATA_SOURCES}
)

add_library(LibTimeZone OBJECT ${SOURCES})
target_compile_definitions(LibTimeZone PRIVATE ENABLE_TIME_ZONE_DATA=$<BOOL:${ENABLE_TIME_ZONE_DATABASE_DOWNLOAD}>)

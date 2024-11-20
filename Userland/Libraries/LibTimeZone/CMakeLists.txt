include(${SerenityOS_SOURCE_DIR}/Meta/CMake/time_zone_data.cmake)

set(SOURCES
    ${TIME_ZONE_DATA_SOURCES}
    TimeZone.cpp
)
set(GENERATED_SOURCES ${CURRENT_LIB_GENERATED})

add_library(LibTimeZone OBJECT ${SOURCES})
serenity_generated_sources(LibTimeZone)
target_compile_definitions(LibTimeZone PRIVATE ENABLE_TIME_ZONE_DATA=$<BOOL:${ENABLE_TIME_ZONE_DATABASE_DOWNLOAD}>)

# NOTE: These objects are used by the DynamicLoader, which is always built without coverage instrumentation.
#       We could allow them to be instrumented for coverage if DynamicLoader built its own copy
target_link_libraries(LibTimeZone PRIVATE NoCoverage)

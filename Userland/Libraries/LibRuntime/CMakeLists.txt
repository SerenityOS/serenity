if (CMAKE_SYSTEM_NAME STREQUAL "SerenityOS")
    add_subdirectory(Serenity)
endif()

set(LIBRUNTIME_SOURCES ${LIBRUNTIME_SOURCES} PARENT_SCOPE)

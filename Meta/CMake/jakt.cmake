#
# Builds the jakt bootstrap compiler as a host tool for Lagom to compile files written in jakt
#

include(FetchContent)

FetchContent_Declare(jakt
    GIT_REPOSITORY https://github.com/SerenityOS/jakt.git
    GIT_TAG main
    GIT_SHALLOW TRUE
)

# Allow developers to skip download/update steps with local checkout
if (JAKT_SOURCE_DIR)
    set(FETCHCONTENT_SOURCE_DIR_JAKT ${JAKT_SOURCE_DIR} CACHE PATH "Developer's pre-existing jakt source directory" FORCE)
    message(STATUS "Using pre-existing JAKT_SOURCE_DIR: ${JAKT_SOURCE_DIR}")
endif()

set(JAKT_BUILD_TESTING OFF)
FetchContent_MakeAvailable(jakt)

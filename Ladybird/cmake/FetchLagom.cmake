# Copyright (c) 2021, Andrew Kaster <akaster@serenityos.org>
#
# SPDX-License-Identifier: MIT

# Fetch serenity, so that we can build Lagom from it
FetchContent_Declare(lagom
    GIT_REPOSITORY https://github.com/SerenityOS/serenity.git
    GIT_TAG origin/master
    GIT_SHALLOW TRUE
    SOURCE_DIR serenity
)

# Allow developers to skip download/update steps with local checkout
if (SERENITY_SOURCE_DIR)
    set(FETCHCONTENT_SOURCE_DIR_LAGOM ${SERENITY_SOURCE_DIR} CACHE PATH "Developer's pre-existing serenity source directory" FORCE)
    message(STATUS "Using pre-existing SERENITY_SOURCE_DIR: ${SERENITY_SOURCE_DIR}")
endif()

# Can't use FetchContent_MakeAvailable b/c we want to use the Lagom build, not the main build
# Populate source directory for lagom
FetchContent_GetProperties(lagom)
if (NOT lagom_POPULATED)
    FetchContent_Populate(lagom)
    set(BUILD_LAGOM ON CACHE INTERNAL "Build all Lagom targets")

    # FIXME: Setting target_include_directories on Lagom libraries might make this unecessary?
    include_directories(${lagom_SOURCE_DIR}/Userland/Libraries)
    include_directories(${lagom_SOURCE_DIR})
    include_directories(${lagom_BINARY_DIR})

    # We set EXCLUDE_FROM_ALL to make sure that only required Lagom libraries are built
    add_subdirectory(${lagom_SOURCE_DIR}/Meta/Lagom ${lagom_BINARY_DIR} EXCLUDE_FROM_ALL)
endif()

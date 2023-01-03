# Copyright (c) 2021, Andrew Kaster <akaster@serenityos.org>
#
# SPDX-License-Identifier: MIT

set(BUILD_LAGOM ON CACHE INTERNAL "Build all Lagom targets")

set(LAGOM_SOURCE_DIR "${SERENITY_SOURCE_DIR}/Meta/Lagom")
set(LAGOM_BINARY_DIR "${CMAKE_CURRENT_BINARY_DIR}/Lagom")

# FIXME: Setting target_include_directories on Lagom libraries might make this unnecessary?
include_directories(${SERENITY_SOURCE_DIR})
include_directories(${SERENITY_SOURCE_DIR}/Userland/Libraries)
include_directories(${LAGOM_BINARY_DIR})
include_directories(${LAGOM_BINARY_DIR}/Userland/Services)
include_directories(${LAGOM_BINARY_DIR}/Userland/Libraries)

# We set EXCLUDE_FROM_ALL to make sure that only required Lagom libraries are built
add_subdirectory("${LAGOM_SOURCE_DIR}" "${LAGOM_BINARY_DIR}" EXCLUDE_FROM_ALL)

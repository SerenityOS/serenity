cmake_minimum_required(VERSION 3.22)

if (NOT DEFINED SERENITY_ARCH OR NOT DEFINED SERENITY_SYSROOT)
    message(FATAL_ERROR "SERENITY_ARCH and SERENITY_SYSROOT must be defined")
endif()

# SERENITY_ARCH is used by the included file.
include(Userland/Libraries/LibC/Headers.cmake)

link_libc_headers("${CMAKE_CURRENT_SOURCE_DIR}/Userland/Libraries/LibC" "${SERENITY_SYSROOT}/usr/include")

cmake_minimum_required (VERSION 3.0)
project (Lagom)

set(CMAKE_CXX_COMPILER g++)
set(CMAKE_CXX_FLAGS "")
unset(CMAKE_SYSROOT)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Os -Wall -Wextra -Werror -std=c++17")

if ("${CMAKE_CXX_COMPILER_ID}" STREQUAL "Clang")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wconsumed")
elseif ("${CMAKE_CXX_COMPILER_ID}" STREQUAL "GNU")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wno-expansion-to-defined")
endif()

file(GLOB AK_SOURCES "../../AK/*.cpp")
file(GLOB LIBCORE_SOURCES "../../Libraries/LibCore/*.cpp")

set(SOURCES ${AK_SOURCES} ${LIBCORE_SOURCES})

add_library(lagom ${SOURCES})
target_include_directories(lagom PUBLIC ../../ ../../Libraries)

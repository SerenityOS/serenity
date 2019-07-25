cmake_minimum_required (VERSION 2.6)

set(CMAKE_C_COMPILER clang)
set(CMAKE_CXX_COMPILER clang)
set(CMAKE_CXX_FLAGS "-O2 -Wall -Wextra -Wconsumed -Werror -std=c++17 -fPIC")

project (Lagom)

file(GLOB AK_SOURCES "../AK/*.cpp")
file(GLOB LIBCORE_SOURCES "../Libraries/LibCore/*.cpp")

set(SOURCES ${AK_SOURCES} ${LIBCORE_SOURCES})

include_directories (../)
include_directories (../Libraries/)
add_library(lagom ${SOURCES})

add_executable(TestApp TestApp.cpp)
target_link_libraries(TestApp lagom)
target_link_libraries(TestApp stdc++)

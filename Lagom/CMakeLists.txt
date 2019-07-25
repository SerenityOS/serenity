cmake_minimum_required (VERSION 2.6)

set(CMAKE_C_COMPILER clang)
set(CMAKE_CXX_COMPILER clang)
set(CMAKE_CXX_FLAGS "-O2 -Wall -Wextra -Wconsumed -Werror -std=c++17")

project (Lagom)

file(GLOB SOURCES "../Libraries/LibCore/*.cpp")

set(SOURCES ${SOURCES}
    ../AK/StringImpl.cpp
    ../AK/String.cpp
    ../AK/JsonArray.cpp
    ../AK/JsonValue.cpp
    ../AK/JsonObject.cpp
    ../AK/JsonParser.cpp
    ../AK/StringBuilder.cpp
    ../AK/StringView.cpp
    ../AK/LogStream.cpp
)

include_directories (../)
include_directories (../Libraries/)
add_library(lagom SHARED ${SOURCES})


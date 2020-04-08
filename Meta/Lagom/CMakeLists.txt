cmake_minimum_required (VERSION 3.0)
project (Lagom)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O2 -Wall -Wextra -Werror -std=c++17 -fPIC -g")

if ("${CMAKE_CXX_COMPILER_ID}" STREQUAL "Clang")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wconsumed")

    option(ENABLE_ADDRESS_SANITIZER "Enable address sanitizer testing in gcc/clang" FALSE)
    if (ENABLE_ADDRESS_SANITIZER)
        add_definitions(-fsanitize=address -fno-omit-frame-pointer)
        set(LINKER_FLAGS "${LINKER_FLAGS} -fsanitize=address")
    endif()

    option(ENABLE_MEMORY_SANITIZER "Enable memory sanitizer testing in gcc/clang" FALSE)
    if (ENABLE_MEMORY_SANITIZER)
        add_definitions(-fsanitize=memory -fsanitize-memory-track-origins -fno-omit-frame-pointer)
        set(LINKER_FLAGS "${LINKER_FLAGS} -fsanitize=memory -fsanitize-memory-track-origins")
    endif()

    option(ENABLE_UNDEFINED_SANITIZER "Enable undefined behavior sanitizer testing in gcc/clang" FALSE)
    if (ENABLE_UNDEFINED_SANITIZER)
        add_definitions(-fsanitize=undefined -fno-omit-frame-pointer)
        set(LINKER_FLAGS "${LINKER_FLAGS} -fsanitize=undefined")
    endif()

    option(ENABLE_FUZZER_SANITIZER "Enable fuzzer sanitizer testing in clang" FALSE)
    if (ENABLE_FUZZER_SANITIZER)
        add_definitions(-fsanitize=fuzzer -fno-omit-frame-pointer)
        set(LINKER_FLAGS "${LINKER_FLAGS} -fsanitize=fuzzer-no-link")
    endif()

    set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} ${LINKER_FLAGS}")
    set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} ${LINKER_FLAGS}")
    set(CMAKE_MODULE_LINKER_FLAGS "${CMAKE_MODULE_LINKER_FLAGS} ${LINKER_FLAGS}")

elseif ("${CMAKE_CXX_COMPILER_ID}" STREQUAL "GNU")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wno-expansion-to-defined")
endif()

file(GLOB AK_SOURCES "../../AK/*.cpp")
file(GLOB LIBCORE_SOURCES "../../Libraries/LibCore/*.cpp")
file(GLOB LIBIPC_SOURCES "../../Libraries/LibIPC/*.cpp")
file(GLOB LIBLINE_SOURCES "../../Libraries/LibLine/*.cpp")
file(GLOB LIBJS_SOURCES "../../Libraries/LibJS/*.cpp")
file(GLOB LIBJS_SUBDIR_SOURCES "../../Libraries/LibJS/*/*.cpp")

set(SOURCES ${AK_SOURCES} ${LIBCORE_SOURCES} ${LIBIPC_SOURCES} ${LIBLINE_SOURCES} ${LIBJS_SOURCES} ${LIBJS_SUBDIR_SOURCES})

include_directories (../../)
include_directories (../../Libraries/)
add_library(lagom ${SOURCES})

add_executable(TestApp TestApp.cpp)
target_link_libraries(TestApp lagom)
target_link_libraries(TestApp stdc++)

add_executable(TestJson TestJson.cpp)
target_link_libraries(TestJson lagom)
target_link_libraries(TestJson stdc++)

add_executable(js ../../Userland/js.cpp)
target_link_libraries(js lagom)
target_link_libraries(js stdc++)
target_link_libraries(js pthread)

if (ENABLE_FUZZER_SANITIZER)
    add_subdirectory(Fuzzers)
endif()

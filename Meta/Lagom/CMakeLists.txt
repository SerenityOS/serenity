cmake_minimum_required (VERSION 3.0)
project (Lagom)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wno-unknown-warning-option -O2 -Wall -Wextra -Werror -std=c++2a -fPIC -g -Wno-deprecated-copy")

if ("${CMAKE_CXX_COMPILER_ID}" STREQUAL "Clang")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wconsumed -Wno-overloaded-virtual")

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
file(GLOB LIBELF_SOURCES "../../Libraries/LibELF/*.cpp")
file(GLOB LIBGEMINI_SOURCES "../../Libraries/LibGemini/*.cpp")
file(GLOB LIBGFX_SOURCES "../../Libraries/LibGfx/*.cpp")
file(GLOB LIBHTTP_SOURCES "../../Libraries/LibHTTP/*.cpp")
file(GLOB LIBIPC_SOURCES "../../Libraries/LibIPC/*.cpp")
file(GLOB LIBLINE_SOURCES "../../Libraries/LibLine/*.cpp")
set(LIBM_SOURCES "../../Libraries/LibM/math.cpp")
file(GLOB LIBMARKDOWN_SOURCES "../../Libraries/LibMarkdown/*.cpp")
file(GLOB LIBX86_SOURCES "../../Libraries/LibX86/*.cpp")
file(GLOB LIBJS_SOURCES "../../Libraries/LibJS/*.cpp")
file(GLOB LIBJS_SUBDIR_SOURCES "../../Libraries/LibJS/*/*.cpp")
file(GLOB LIBCOMPRESS_SOURCES "../../Libraries/LibCompress/*.cpp")
file(GLOB LIBCRYPTO_SOURCES "../../Libraries/LibCrypto/*.cpp")
file(GLOB LIBCRYPTO_SUBDIR_SOURCES "../../Libraries/LibCrypto/*/*.cpp")
file(GLOB LIBTLS_SOURCES "../../Libraries/LibTLS/*.cpp")
file(GLOB SHELL_SOURCES "../../Shell/*.cpp")
file(GLOB SHELL_TESTS "../../Shell/Tests/*.sh")

set(LAGOM_CORE_SOURCES ${AK_SOURCES} ${LIBCORE_SOURCES})
set(LAGOM_MORE_SOURCES ${LIBELF_SOURCES} ${LIBIPC_SOURCES} ${LIBLINE_SOURCES} ${LIBJS_SOURCES} ${LIBJS_SUBDIR_SOURCES} ${LIBX86_SOURCES} ${LIBCRYPTO_SOURCES} ${LIBCOMPRESS_SOURCES} ${LIBCRYPTO_SUBDIR_SOURCES} ${LIBTLS_SOURCES} ${LIBMARKDOWN_SOURCES} ${LIBGEMINI_SOURCES} ${LIBGFX_SOURCES} ${LIBHTTP_SOURCES})

include_directories (../../)
include_directories (../../Libraries/)
add_library(LagomCore ${LAGOM_CORE_SOURCES})

if (BUILD_LAGOM)
    add_library(Lagom $<TARGET_OBJECTS:LagomCore> ${LAGOM_MORE_SOURCES})

    add_executable(TestApp TestApp.cpp)
    target_link_libraries(TestApp Lagom)
    target_link_libraries(TestApp stdc++)

    add_executable(TestJson TestJson.cpp)
    target_link_libraries(TestJson Lagom)
    target_link_libraries(TestJson stdc++)

    add_executable(js_lagom ../../Userland/js.cpp)
    set_target_properties(js_lagom PROPERTIES OUTPUT_NAME js)
    target_link_libraries(js_lagom Lagom)
    target_link_libraries(js_lagom stdc++)
    target_link_libraries(js_lagom pthread)

    add_executable(test-js_lagom ../../Userland/test-js.cpp)
    set_target_properties(test-js_lagom PROPERTIES OUTPUT_NAME test-js)
    target_link_libraries(test-js_lagom Lagom)
    target_link_libraries(test-js_lagom stdc++)
    target_link_libraries(test-js_lagom pthread)

    add_executable(test-crypto_lagom ../../Userland/test-crypto.cpp)
    set_target_properties(test-crypto_lagom PROPERTIES OUTPUT_NAME test-crypto)
    target_link_libraries(test-crypto_lagom Lagom)
    target_link_libraries(test-crypto_lagom stdc++)
    add_test(
        NAME Crypto
        COMMAND test-crypto_lagom test -t -s google.com
        WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
    )

    add_executable(disasm_lagom ../../Userland/disasm.cpp)
    set_target_properties(disasm_lagom PROPERTIES OUTPUT_NAME disasm)
    target_link_libraries(disasm_lagom Lagom)
    target_link_libraries(disasm_lagom stdc++)

    add_executable(shell_lagom ${SHELL_SOURCES})
    set_target_properties(shell_lagom PROPERTIES OUTPUT_NAME shell)
    target_link_libraries(shell_lagom Lagom)
    target_link_libraries(shell_lagom stdc++)
    target_link_libraries(shell_lagom pthread)
    foreach(TEST_PATH ${SHELL_TESTS})
        get_filename_component(TEST_NAME ${TEST_PATH} NAME_WE)
        add_test(
            NAME "Shell-${TEST_NAME}"
            COMMAND shell_lagom "${TEST_PATH}"
            WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
        )
    endforeach()
endif()

if (ENABLE_FUZZER_SANITIZER)
    add_subdirectory(Fuzzers)
endif()

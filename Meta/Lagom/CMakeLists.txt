cmake_minimum_required (VERSION 3.0)
project (Lagom)

if (NOT ENABLE_OSS_FUZZ)
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wno-unknown-warning-option -O2 -Wall -Wextra -Werror -std=c++2a -fPIC -g -Wno-deprecated-copy")
else()
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++2a -fPIC -g -Wno-deprecated-copy")
endif()


if ("${CMAKE_CXX_COMPILER_ID}" STREQUAL "Clang")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wconsumed -Wno-overloaded-virtual")

    if (ENABLE_ADDRESS_SANITIZER)
        add_definitions(-fsanitize=address -fno-omit-frame-pointer)
        set(LINKER_FLAGS "${LINKER_FLAGS} -fsanitize=address")
    endif()

    if (ENABLE_MEMORY_SANITIZER)
        add_definitions(-fsanitize=memory -fsanitize-memory-track-origins -fno-omit-frame-pointer)
        set(LINKER_FLAGS "${LINKER_FLAGS} -fsanitize=memory -fsanitize-memory-track-origins")
    endif()

    if (ENABLE_UNDEFINED_SANITIZER)
        add_definitions(-fsanitize=undefined -fno-omit-frame-pointer)
        set(LINKER_FLAGS "${LINKER_FLAGS} -fsanitize=undefined")
    endif()

    if (ENABLE_FUZZER_SANITIZER)
        add_definitions(-fsanitize=fuzzer -fno-omit-frame-pointer)
        set(LINKER_FLAGS "${LINKER_FLAGS} -fsanitize=fuzzer")
    endif()

    set(ORIGINAL_CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS}")
    set(ORIGINAL_CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS}")
    set(ORIGINAL_CMAKE_MODULE_LINKER_FLAGS "${CMAKE_MODULE_LINKER_FLAGS}")

    set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} ${LINKER_FLAGS}")
    set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} ${LINKER_FLAGS}")
    set(CMAKE_MODULE_LINKER_FLAGS "${CMAKE_MODULE_LINKER_FLAGS} ${LINKER_FLAGS}")

elseif ("${CMAKE_CXX_COMPILER_ID}" STREQUAL "GNU")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wno-expansion-to-defined")
endif()

file(GLOB AK_SOURCES CONFIGURE_DEPENDS "../../AK/*.cpp")
file(GLOB LIBREGEX_LIBC_SOURCES "../../Userland/Libraries/LibRegex/C/Regex.cpp")
file(GLOB LIBREGEX_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibRegex/*.cpp")
file(GLOB LIBCORE_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibCore/*.cpp")
file(GLOB LIBELF_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibELF/*.cpp")
# There's no way we can reliably make this cross platform
list(REMOVE_ITEM LIBELF_SOURCES "${CMAKE_CURRENT_SOURCE_DIR}/../../Userland/Libraries/LibELF/DynamicLinker.cpp")


file(GLOB LIBGEMINI_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibGemini/*.cpp")
file(GLOB LIBGFX_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibGfx/*.cpp")
file(GLOB LIBGUI_GML_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibGUI/GML*.cpp")
list(REMOVE_ITEM LIBGUI_GML_SOURCES ../../Userland/Libraries/LibGUI/GMLSyntaxHighlighter.cpp)
file(GLOB LIBHTTP_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibHTTP/*.cpp")
file(GLOB LIBIPC_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibIPC/*.cpp")
file(GLOB LIBLINE_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibLine/*.cpp")
set(LIBM_SOURCES "../../Userland/Libraries/LibM/math.cpp")
file(GLOB LIBMARKDOWN_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibMarkdown/*.cpp")
file(GLOB LIBX86_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibX86/*.cpp")
file(GLOB LIBJS_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibJS/*.cpp")
file(GLOB LIBJS_SUBDIR_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibJS/*/*.cpp")
file(GLOB LIBCOMPRESS_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibCompress/*.cpp")
file(GLOB LIBCRYPTO_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibCrypto/*.cpp")
file(GLOB LIBCRYPTO_SUBDIR_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibCrypto/*/*.cpp")
file(GLOB LIBTLS_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibTLS/*.cpp")
file(GLOB LIBTTF_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibTTF/*.cpp")
file(GLOB SHELL_SOURCES CONFIGURE_DEPENDS "../../Userland/Shell/*.cpp")
file(GLOB SHELL_TESTS CONFIGURE_DEPENDS "../../Userland/Shell/Tests/*.sh")
list(REMOVE_ITEM SHELL_SOURCES ../../Userland/Shell/main.cpp)

set(LAGOM_REGEX_SOURCES ${LIBREGEX_LIBC_SOURCES} ${LIBREGEX_SOURCES})
set(LAGOM_CORE_SOURCES ${AK_SOURCES} ${LIBCORE_SOURCES})
set(LAGOM_MORE_SOURCES ${LIBELF_SOURCES} ${LIBIPC_SOURCES} ${LIBLINE_SOURCES} ${LIBJS_SOURCES} ${LIBJS_SUBDIR_SOURCES} ${LIBX86_SOURCES} ${LIBCRYPTO_SOURCES} ${LIBCOMPRESS_SOURCES} ${LIBCRYPTO_SUBDIR_SOURCES} ${LIBTLS_SOURCES} ${LIBTTF_SOURCES} ${LIBMARKDOWN_SOURCES} ${LIBGEMINI_SOURCES} ${LIBGFX_SOURCES} ${LIBGUI_GML_SOURCES} ${LIBHTTP_SOURCES} ${LAGOM_REGEX_SOURCES} ${SHELL_SOURCES})

# FIXME: This is a hack, because the lagom stuff can be build individually or
#        in combination with the system, we generate two Debug.h files. One in
#        Build/AK/Debug.h and the other in Build/Meta/Lagom/AK/Debug.h.
configure_file(../../AK/Debug.h.in AK/Debug.h @ONLY)
configure_file(../../Kernel/Debug.h.in Kernel/Debug.h @ONLY)

include_directories(../../)
include_directories(../../Userland/)
include_directories(../../Userland/Libraries/)
include_directories(${CMAKE_BINARY_DIR})
add_library(LagomCore ${LAGOM_CORE_SOURCES})

if (BUILD_LAGOM)
    add_library(Lagom $<TARGET_OBJECTS:LagomCore> ${LAGOM_MORE_SOURCES})

    if (NOT ENABLE_OSS_FUZZ AND NOT ENABLE_FUZZER_SANITIZER)
        add_executable(TestApp TestApp.cpp)
        target_link_libraries(TestApp Lagom)
        target_link_libraries(TestApp stdc++)

        add_executable(TestJson TestJson.cpp)
        target_link_libraries(TestJson Lagom)
        target_link_libraries(TestJson stdc++)

        add_executable(adjtime_lagom ../../Userland/Utilities/adjtime.cpp)
        set_target_properties(adjtime_lagom PROPERTIES OUTPUT_NAME adjtime)
        target_link_libraries(adjtime_lagom Lagom)

        add_executable(js_lagom ../../Userland/Utilities/js.cpp)
        set_target_properties(js_lagom PROPERTIES OUTPUT_NAME js)
        target_link_libraries(js_lagom Lagom)
        target_link_libraries(js_lagom stdc++)
        target_link_libraries(js_lagom pthread)

        add_executable(ntpquery_lagom ../../Userland/Utilities/ntpquery.cpp)
        set_target_properties(ntpquery_lagom PROPERTIES OUTPUT_NAME ntpquery)
        target_link_libraries(ntpquery_lagom Lagom)

        add_executable(test-js_lagom ../../Userland/Utilities/test-js.cpp)
        set_target_properties(test-js_lagom PROPERTIES OUTPUT_NAME test-js)
        target_link_libraries(test-js_lagom Lagom)
        target_link_libraries(test-js_lagom stdc++)
        target_link_libraries(test-js_lagom pthread)

        add_executable(test-crypto_lagom ../../Userland/Utilities/test-crypto.cpp)
        set_target_properties(test-crypto_lagom PROPERTIES OUTPUT_NAME test-crypto)
        target_link_libraries(test-crypto_lagom Lagom)
        target_link_libraries(test-crypto_lagom stdc++)
        add_test(
            NAME Crypto
            COMMAND test-crypto_lagom test -t -s google.com --ca-certs-file ../../Base/etc/ca_certs.ini
            WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
        )

        add_executable(test-compress_lagom ../../Userland/Utilities/test-compress.cpp)
        set_target_properties(test-compress_lagom PROPERTIES OUTPUT_NAME test-compress)
        target_link_libraries(test-compress_lagom Lagom)
        target_link_libraries(test-compress_lagom stdc++)

        add_executable(disasm_lagom ../../Userland/Utilities/disasm.cpp)
        set_target_properties(disasm_lagom PROPERTIES OUTPUT_NAME disasm)
        target_link_libraries(disasm_lagom Lagom)
        target_link_libraries(disasm_lagom stdc++)

        add_executable(shell_lagom ../../Userland/Shell/main.cpp)
        set_target_properties(shell_lagom PROPERTIES OUTPUT_NAME shell)
        target_link_libraries(shell_lagom Lagom)
        target_link_libraries(shell_lagom stdc++)
        target_link_libraries(shell_lagom pthread)

        add_executable(gml-format_lagom ../../Userland/Utilities/gml-format.cpp)
        set_target_properties(gml-format_lagom PROPERTIES OUTPUT_NAME gml-format)
        target_link_libraries(gml-format_lagom Lagom)
        target_link_libraries(gml-format_lagom stdc++)

        foreach(TEST_PATH ${SHELL_TESTS})
            get_filename_component(TEST_NAME ${TEST_PATH} NAME_WE)
            add_test(
                NAME "Shell-${TEST_NAME}"
                COMMAND shell_lagom --skip-shellrc "${TEST_PATH}"
                WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/../../Userland/Shell/Tests
            )
            set_tests_properties("Shell-${TEST_NAME}" PROPERTIES
                TIMEOUT 10
                FAIL_REGULAR_EXPRESSION "FAIL"
                PASS_REGULAR_EXPRESSION "PASS")
        endforeach()
    endif()
endif()

if (ENABLE_FUZZER_SANITIZER OR ENABLE_OSS_FUZZ)
    add_subdirectory(Fuzzers)
endif()

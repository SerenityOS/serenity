cmake_minimum_required (VERSION 3.16)

project(
    Lagom
    VERSION 0.0.0
    DESCRIPTION "Host build of SerenityOS libraries and applications"
    HOMEPAGE_URL "https://github.com/SerenityOS/serenity"
    LANGUAGES C CXX
)

option(ENABLE_LAGOM_CCACHE "Enable ccache for Lagom builds" OFF)
option(BUILD_SHARED_LIBS "Build shared libraries instead of static libraries" ON)
if (ENABLE_OSS_FUZZ)
    set(BUILD_SHARED_LIBS OFF) # Don't use shared libraries on oss-fuzz, for ease of integration with their infrastructure
endif()

# This is required for CMake (when invoked for a Lagom-only build) to
# ignore any files downloading during the build, e.g. UnicodeData.txt.
# https://cmake.org/cmake/help/latest/policy/CMP0058.html
cmake_policy(SET CMP0058 NEW)

get_filename_component(
    SERENITY_PROJECT_ROOT "${PROJECT_SOURCE_DIR}/../.."
    ABSOLUTE CACHE
)

find_package(Threads REQUIRED)

if (ENABLE_LAGOM_CCACHE)
    find_program(CCACHE_PROGRAM ccache)
    if(CCACHE_PROGRAM)
        set_property(GLOBAL PROPERTY RULE_LAUNCH_COMPILE "${CCACHE_PROGRAM}")
    endif()
endif()

include(${SERENITY_PROJECT_ROOT}/Meta/CMake/wasm_spec_tests.cmake)

add_compile_options(-Wno-unknown-warning-option -Wno-literal-suffix -Wno-deprecated-copy)
add_compile_options(-O2)
add_compile_options(-Wall -Wextra -Werror)
add_compile_options(-fPIC -g)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

set(CMAKE_SKIP_BUILD_RPATH FALSE)
set(CMAKE_BUILD_WITH_INSTALL_RPATH FALSE)
# See slide 100 of the following ppt :^)
# https://crascit.com/wp-content/uploads/2019/09/Deep-CMake-For-Library-Authors-Craig-Scott-CppCon-2019.pdf
if (NOT APPLE)
    set(CMAKE_INSTALL_RPATH $ORIGIN)
endif()
set(CMAKE_INSTALL_RPATH_USE_LINK_PATH TRUE)

set(CMAKE_INSTALL_MESSAGE NEVER)

if (ENABLE_ADDRESS_SANITIZER)
    add_compile_options(-fsanitize=address -fno-omit-frame-pointer)
    set(LINKER_FLAGS "${LINKER_FLAGS} -fsanitize=address")
endif()

if (ENABLE_MEMORY_SANITIZER)
    add_compile_options(-fsanitize=memory -fsanitize-memory-track-origins -fno-omit-frame-pointer)
    set(LINKER_FLAGS "${LINKER_FLAGS} -fsanitize=memory -fsanitize-memory-track-origins")
endif()

if (ENABLE_UNDEFINED_SANITIZER)
    add_compile_options(-fsanitize=undefined -fno-sanitize=vptr -fno-omit-frame-pointer)
    set(LINKER_FLAGS "${LINKER_FLAGS} -fsanitize=undefined -fno-sanitize=vptr")
endif()

if ("${CMAKE_CXX_COMPILER_ID}" MATCHES "Clang$")
    # Clang's default constexpr-steps limit is 1048576(2^20), GCC doesn't have one
    add_compile_options(-Wno-overloaded-virtual -Wno-user-defined-literals -fconstexpr-steps=16777216)

    if (ENABLE_FUZZER_SANITIZER)
        add_compile_options(-fsanitize=fuzzer -fno-omit-frame-pointer)
        set(LINKER_FLAGS "${LINKER_FLAGS} -fsanitize=fuzzer")
    endif()

elseif ("${CMAKE_CXX_COMPILER_ID}" STREQUAL "GNU")
    add_compile_options(-Wno-expansion-to-defined)
endif()

# These are here to support Fuzzili builds further down the directory stack
set(ORIGINAL_CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS}")
set(ORIGINAL_CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS}")
set(ORIGINAL_CMAKE_MODULE_LINKER_FLAGS "${CMAKE_MODULE_LINKER_FLAGS}")

set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} ${LINKER_FLAGS}")
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} ${LINKER_FLAGS}")
set(CMAKE_MODULE_LINKER_FLAGS "${CMAKE_MODULE_LINKER_FLAGS} ${LINKER_FLAGS}")

# FIXME: This is a hack, because the lagom stuff can be built individually or
#        in combination with the system, we generate two Debug.h files. One in
#        Build/AK/Debug.h and the other in Build/Meta/Lagom/AK/Debug.h.
configure_file(../../AK/Debug.h.in AK/Debug.h @ONLY)
configure_file(../../Kernel/Debug.h.in Kernel/Debug.h @ONLY)

include_directories(../../)
include_directories(../../Userland/)
include_directories(../../Userland/Libraries/)
include_directories(${CMAKE_BINARY_DIR})
include_directories(${CMAKE_CURRENT_BINARY_DIR})

# install rules, think about moving to its own helper cmake file
# Don't install Lagom libs into the target Root/
# FIXME: Remove this check for 4594
if (CMAKE_SOURCE_DIR MATCHES ".*/Lagom")
    include(CMakePackageConfigHelpers)
    include(GNUInstallDirs)

    # find_package(<package>) call for consumers to find this project
    set(package Lagom)

    write_basic_package_version_file(
        "${package}ConfigVersion.cmake"
        COMPATIBILITY SameMajorVersion
    )

    # Allow package maintainers to freely override the path for the configs
    set(Lagom_INSTALL_CMAKEDIR "${CMAKE_INSTALL_DATADIR}/${package}"
        CACHE PATH "CMake package config location relative to the install prefix")
    mark_as_advanced(Lagom_INSTALL_CMAKEDIR)

    install(
        FILES ${SERENITY_PROJECT_ROOT}/Meta/CMake/lagom-install-config.cmake
        DESTINATION "${Lagom_INSTALL_CMAKEDIR}"
        RENAME "${package}Config.cmake"
        COMPONENT Lagom_Development
    )

    install(
        FILES "${PROJECT_BINARY_DIR}/${package}ConfigVersion.cmake"
        DESTINATION "${Lagom_INSTALL_CMAKEDIR}"
        COMPONENT Lagom_Development
    )

    install(
        EXPORT LagomTargets
        NAMESPACE Lagom::
        DESTINATION "${Lagom_INSTALL_CMAKEDIR}"
        COMPONENT Lagom_Development
    )

    # Manually install AK
    install(
        DIRECTORY "${SERENITY_PROJECT_ROOT}/AK"
        COMPONENT Lagom_Development
        DESTINATION ${CMAKE_INSTALL_INCLUDEDIR}
        FILES_MATCHING PATTERN "*.h"
    )
endif()

function(lagom_lib library fs_name)
    cmake_parse_arguments(LAGOM_LIBRARY "" "" "SOURCES;LIBS" ${ARGN})
    set(target_name "Lagom${library}")
    add_library(${target_name} ${LAGOM_LIBRARY_SOURCES})
    # alias for pretty exports
    add_library(Lagom::${library} ALIAS ${target_name})

    set_target_properties(
        ${target_name} PROPERTIES
        VERSION "${PROJECT_VERSION}"
        SOVERSION "${PROJECT_VERSION_MAJOR}"
        EXPORT_NAME ${library}
        OUTPUT_NAME lagom-${fs_name}
    )
    target_link_libraries(${target_name} ${LAGOM_LIBRARY_LIBS})
    if (NOT ${target_name} STREQUAL "LagomCore")
        target_link_libraries(${target_name} LagomCore)
    endif()
    # Don't install Lagom libs into the target Root/
    # FIXME: Remove this check for 4594
    if (CMAKE_SOURCE_DIR MATCHES ".*/Lagom")
        install(
            TARGETS ${target_name}
            EXPORT LagomTargets
            RUNTIME #
                COMPONENT Lagom_Runtime
            LIBRARY #
                COMPONENT Lagom_Runtime
                NAMELINK_COMPONENT Lagom_Development
            ARCHIVE #
                COMPONENT Lagom_Development
            INCLUDES #
                DESTINATION ${CMAKE_INSTALL_INCLUDEDIR}
        )
        install(
            DIRECTORY "${SERENITY_PROJECT_ROOT}/Userland/Libraries/Lib${library}"
            COMPONENT Lagom_Development
            DESTINATION ${CMAKE_INSTALL_INCLUDEDIR}
            FILES_MATCHING PATTERN "*.h"
        )
    endif()
endfunction()

function(lagom_test source)
    cmake_parse_arguments(LAGOM_TEST "" "" "LIBS" ${ARGN})
    get_filename_component(name ${source} NAME_WE)
    add_executable(${name}_lagom ${source})
    set_target_properties(${name}_lagom PROPERTIES OUTPUT_NAME ${name})
    target_link_libraries(${name}_lagom LagomCore LagomTest LagomTestMain ${LAGOM_TEST_LIBS})
    add_test(
            NAME ${name}
            COMMAND ${name}_lagom
    )
endfunction()

# AK/Core
# Note: AK is included in LagomCore for the host build instead of LibC per the target build
file(GLOB AK_SOURCES CONFIGURE_DEPENDS "../../AK/*.cpp")
file(GLOB LIBCORE_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibCore/*.cpp")
lagom_lib(Core core
    SOURCES ${AK_SOURCES} ${LIBCORE_SOURCES}
    LIBS Threads::Threads ${CMAKE_DL_LIBS}
)
if (NOT APPLE)
    target_link_libraries(LagomCore crypt) # Core::Account uses crypt() but it's not in libcrypt on macOS
endif()

if (BUILD_LAGOM)
    # Lagom Libraries

    # Archive
    file(GLOB LIBARCHIVE_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibArchive/*.cpp")
    lagom_lib(Archive archive
        SOURCES ${LIBARCHIVE_SOURCES}
    )

    # Audio
    file(GLOB LIBAUDIO_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibAudio/*.cpp")
    list(REMOVE_ITEM LIBAUDIO_SOURCES "${CMAKE_CURRENT_SOURCE_DIR}/../../Userland/Libraries/LibAudio/ClientConnection.cpp")
    lagom_lib(Audio audio
        SOURCES ${LIBAUDIO_SOURCES}
    )

    # Compress
    file(GLOB LIBCOMPRESS_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibCompress/*.cpp")
    lagom_lib(Compress compress
        SOURCES ${LIBCOMPRESS_SOURCES}
        LIBS LagomCrypto
    )

    # Crypto
    file(GLOB LIBCRYPTO_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibCrypto/*.cpp")
    file(GLOB LIBCRYPTO_SUBDIR_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibCrypto/*/*.cpp")
    file(GLOB LIBCRYPTO_SUBSUBDIR_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibCrypto/*/*/*.cpp")
    lagom_lib(Crypto crypto
        SOURCES ${LIBCRYPTO_SOURCES} ${LIBCRYPTO_SUBDIR_SOURCES} ${LIBCRYPTO_SUBSUBDIR_SOURCES}
    )

    # ELF
    file(GLOB LIBELF_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibELF/*.cpp")
    # There's no way we can reliably make the dymamic loading classes cross platform
    list(FILTER LIBELF_SOURCES EXCLUDE REGEX ".*Dynamic.*.cpp$")
    lagom_lib(ELF elf
        SOURCES ${LIBELF_SOURCES}
    )

    # Gemini
    file(GLOB LIBGEMINI_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibGemini/*.cpp")
    lagom_lib(Gemini gemini
        SOURCES ${LIBGEMINI_SOURCES}
        LIBS LagomTLS
    )

    # GFX
    file(GLOB LIBGFX_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibGfx/*.cpp")
    file(GLOB LIBGFX_TTF_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibGfx/TrueTypeFont/*.cpp")
    lagom_lib(Gfx gfx
        SOURCES ${LIBGFX_SOURCES} ${LIBGFX_TTF_SOURCES}
        LIBS m LagomCompress LagomTextCodec LagomIPC
    )

    # GUI-GML
    file(GLOB LIBGUI_GML_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibGUI/GML*.cpp")
    list(REMOVE_ITEM LIBGUI_GML_SOURCES "${CMAKE_CURRENT_SOURCE_DIR}/../../Userland/Libraries/LibGUI/GMLAutocompleteProvider.cpp")
    list(REMOVE_ITEM LIBGUI_GML_SOURCES "${CMAKE_CURRENT_SOURCE_DIR}/../../Userland/Libraries/LibGUI/GMLSyntaxHighlighter.cpp")
    lagom_lib(GML gml
        SOURCES ${LIBGUI_GML_SOURCES}
    )

    # HTTP
    file(GLOB LIBHTTP_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibHTTP/*.cpp")
    lagom_lib(HTTP http
        SOURCES ${LIBHTTP_SOURCES}
        LIBS LagomCompress LagomTLS
    )

    # IMAP
    file(GLOB LIBIMAP_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibIMAP/*.cpp")
    lagom_lib(IMAP imap
        SOURCES ${LIBIMAP_SOURCES}
        LIBS LagomTLS
    )

    # IPC
    file(GLOB LIBIPC_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibIPC/*.cpp")
    lagom_lib(IPC ipc
        SOURCES ${LIBIPC_SOURCES}
    )

    # JS
    file(GLOB LIBJS_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibJS/*.cpp")
    file(GLOB LIBJS_SUBDIR_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibJS/*/*.cpp")
    file(GLOB LIBJS_SUBSUBDIR_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibJS/*/*/*.cpp")
    list(REMOVE_ITEM LIBJS_SOURCES "${CMAKE_CURRENT_SOURCE_DIR}/../../Userland/Libraries/LibJS/SyntaxHighlighter.cpp")
    lagom_lib(JS js
        SOURCES ${LIBJS_SOURCES} ${LIBJS_SUBDIR_SOURCES} ${LIBJS_SUBSUBDIR_SOURCES}
        LIBS m LagomCrypto LagomRegex LagomUnicode
    )

    # Line
    file(GLOB LIBLINE_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibLine/*.cpp")
    lagom_lib(Line line
        SOURCES ${LIBLINE_SOURCES}
    )

    # Markdown
    file(GLOB LIBMARKDOWN_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibMarkdown/*.cpp")
    lagom_lib(Markdown markdown
        SOURCES ${LIBMARKDOWN_SOURCES}
        LIBS LagomJS
    )

    # Regex
    file(GLOB LIBREGEX_LIBC_SOURCES "../../Userland/Libraries/LibRegex/C/Regex.cpp")
    file(GLOB LIBREGEX_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibRegex/*.cpp")
    lagom_lib(Regex regex
        SOURCES ${LIBREGEX_SOURCES} ${LIBREGEX_LIBC_SOURCES}
        LIBS LagomUnicode
    )

    # Shell
    file(GLOB SHELL_SOURCES CONFIGURE_DEPENDS "../../Userland/Shell/*.cpp")
    list(REMOVE_ITEM SHELL_SOURCES "${CMAKE_CURRENT_SOURCE_DIR}/../../Userland/Shell/SyntaxHighlighter.cpp")
    list(FILTER SHELL_SOURCES EXCLUDE REGEX ".*main.cpp$")
    lagom_lib(Shell shell
        SOURCES ${SHELL_SOURCES}
        LIBS LagomLine LagomRegex
    )

    # SQL
    file(GLOB_RECURSE LIBSQL_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibSQL/*.cpp")
    list(REMOVE_ITEM LIBSQL_SOURCES "${CMAKE_CURRENT_SOURCE_DIR}/../../Userland/Libraries/LibSQL/AST/SyntaxHighlighter.cpp")
    list(REMOVE_ITEM LIBSQL_SOURCES "${CMAKE_CURRENT_SOURCE_DIR}/../../Userland/Libraries/LibSQL/SQLClient.cpp")
    lagom_lib(SQL sql
        SOURCES ${LIBSQL_SOURCES}
    )

    # TextCodec
    file(GLOB LIBTEXTCODEC_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibTextCodec/*.cpp")
    lagom_lib(TextCodec textcodec
        SOURCES ${LIBTEXTCODEC_SOURCES}
    )

    # TLS
    file(GLOB LIBTLS_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibTLS/*.cpp")
    lagom_lib(TLS tls
        SOURCES ${LIBTLS_SOURCES}
        LIBS LagomCrypto
    )

    # Unicode
    # We need to make sure not to build code generators for Fuzzer builds, as they already have their own main.cpp
    if (NOT ENABLE_OSS_FUZZ AND NOT ENABLE_FUZZER_SANITIZER)
        # FIXME: Make this logic smarter in 4594
        if (NOT CMAKE_SOURCE_DIR STREQUAL SERENITY_PROJECT_ROOT)
            set(write_if_different ${CMAKE_CURRENT_SOURCE_DIR}/../write-only-on-difference.sh)
            add_subdirectory(../../Userland/Libraries/LibUnicode/CodeGenerators ${CMAKE_CURRENT_BINARY_DIR}/LibUnicode/CodeGenerators)
        endif()
        include(../../Userland/Libraries/LibUnicode/unicode_data.cmake)
    endif()
    file(GLOB LIBUNICODE_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibUnicode/*.cpp")
    lagom_lib(Unicode unicode
        SOURCES ${LIBUNICODE_SOURCES} ${UNICODE_DATA_SOURCES}
    )

    # WASM
    file(GLOB LIBWASM_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibWasm/*/*.cpp")
    lagom_lib(Wasm wasm
        SOURCES ${LIBWASM_SOURCES}
    )

    # x86
    file(GLOB LIBX86_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibX86/*.cpp")
    lagom_lib(X86 x86
        SOURCES ${LIBX86_SOURCES}
    )

    if (NOT ENABLE_OSS_FUZZ AND NOT ENABLE_FUZZER_SANITIZER)
        # Lagom Examples
        add_executable(TestApp TestApp.cpp)
        target_link_libraries(TestApp LagomCore)

        add_executable(TestJson TestJson.cpp)
        target_link_libraries(TestJson LagomCore)

        # Lagom Utilities
        add_executable(adjtime_lagom ../../Userland/Utilities/adjtime.cpp)
        set_target_properties(adjtime_lagom PROPERTIES OUTPUT_NAME adjtime)
        target_link_libraries(adjtime_lagom LagomCore)

        add_executable(disasm_lagom ../../Userland/Utilities/disasm.cpp)
        set_target_properties(disasm_lagom PROPERTIES OUTPUT_NAME disasm)
        target_link_libraries(disasm_lagom LagomCore LagomELF LagomX86)

        add_executable(gml-format_lagom ../../Userland/Utilities/gml-format.cpp)
        set_target_properties(gml-format_lagom PROPERTIES OUTPUT_NAME gml-format)
        target_link_libraries(gml-format_lagom LagomCore LagomGML)

        add_executable(js_lagom ../../Userland/Utilities/js.cpp)
        set_target_properties(js_lagom PROPERTIES OUTPUT_NAME js)
        target_link_libraries(js_lagom LagomJS LagomLine Threads::Threads)

        add_executable(ntpquery_lagom ../../Userland/Utilities/ntpquery.cpp)
        set_target_properties(ntpquery_lagom PROPERTIES OUTPUT_NAME ntpquery)
        target_link_libraries(ntpquery_lagom LagomCore)

        add_executable(shell_lagom ../../Userland/Shell/main.cpp)
        set_target_properties(shell_lagom PROPERTIES OUTPUT_NAME shell)
        target_link_libraries(shell_lagom LagomCore LagomShell)

        add_executable(wasm_lagom ../../Userland/Utilities/wasm.cpp)
        set_target_properties(wasm_lagom PROPERTIES OUTPUT_NAME wasm)
        target_link_libraries(wasm_lagom LagomCore LagomWasm LagomLine)

        enable_testing()
        # LibTest
        file(GLOB LIBTEST_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibTest/*.cpp")
        list(FILTER LIBTEST_SOURCES EXCLUDE REGEX ".*Main.cpp$")
        add_library(
            LagomTest
            ${LIBTEST_SOURCES}
        )
        target_link_libraries(LagomTest LagomCore)
        set_target_properties(LagomTest PROPERTIES OUTPUT_NAME lagom-test)
        add_library(
            LagomTestMain
            OBJECT
            "${SERENITY_PROJECT_ROOT}/Userland/Libraries/LibTest/TestMain.cpp"
        )

        # LibTest tests from Tests/
        # AK
        file(GLOB AK_TEST_SOURCES CONFIGURE_DEPENDS "../../Tests/AK/*.cpp")
        foreach(source ${AK_TEST_SOURCES})
            lagom_test(${source})
        endforeach()
        set_tests_properties(TestJSON PROPERTIES WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/../../Tests/AK)

        # Core
        lagom_test(../../Tests/LibCore/TestLibCoreIODevice.cpp)
        set_tests_properties(TestLibCoreIODevice PROPERTIES WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/../../Tests/LibCore)

        # Crypto
        file(GLOB LIBCRYPTO_TESTS CONFIGURE_DEPENDS "../../Tests/LibCrypto/*.cpp")
        foreach(source ${LIBCRYPTO_TESTS})
            lagom_test(${source} LIBS LagomCrypto)
        endforeach()

        # Compress
        file(GLOB LIBCOMPRESS_TESTS CONFIGURE_DEPENDS "../../Tests/LibCompress/*.cpp")
        foreach(source ${LIBCOMPRESS_TESTS})
            lagom_test(${source} LIBS LagomCompress)
        endforeach()

        # Regex
        file(GLOB LIBREGEX_TESTS CONFIGURE_DEPENDS "../../Tests/LibRegex/*.cpp")
        # RegexLibC test POSIX <regex.h> and contains many Serenity extensions
        # It is therefore not reasonable to run it on Lagom
        list(REMOVE_ITEM LIBREGEX_TESTS "${CMAKE_CURRENT_SOURCE_DIR}/../../Tests/LibRegex/RegexLibC.cpp")
        foreach(source ${LIBREGEX_TESTS})
            lagom_test(${source} LIBS LagomRegex)
        endforeach()

        # SQL
        file(GLOB LIBSQL_TEST_SOURCES CONFIGURE_DEPENDS "../../Tests/LibSQL/*.cpp")
        foreach(source ${LIBSQL_TEST_SOURCES})
            lagom_test(${source} LIBS LagomSQL)
        endforeach()

        # TLS
        file(GLOB LIBTLS_TESTS CONFIGURE_DEPENDS "../../Tests/LibTLS/*.cpp")
        foreach(source ${LIBTLS_TESTS})
            lagom_test(${source} LIBS LagomTLS)
            set_tests_properties(TestTLSHandshake PROPERTIES WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/../../Tests/LibTLS)
        endforeach()

        # Unicode
        file(GLOB LIBUNICODE_TEST_SOURCES CONFIGURE_DEPENDS "../../Tests/LibUnicode/*.cpp")
        foreach(source ${LIBUNICODE_TEST_SOURCES})
            lagom_test(${source} LIBS LagomUnicode)
        endforeach()

        # JavaScriptTestRunner + LibTest tests
        # test-js
        add_executable(test-js_lagom
            ../../Tests/LibJS/test-js.cpp
            ../../Userland/Libraries/LibTest/JavaScriptTestRunnerMain.cpp)
        set_target_properties(test-js_lagom PROPERTIES OUTPUT_NAME test-js)
        target_link_libraries(test-js_lagom LagomCore LagomTest LagomJS)
        add_test(
            NAME JS
            COMMAND test-js_lagom --show-progress=false
        )
        set_tests_properties(JS PROPERTIES ENVIRONMENT SERENITY_SOURCE_DIR=${SERENITY_PROJECT_ROOT})

        # test-wasm
        add_executable(test-wasm_lagom
            ../../Tests/LibWasm/test-wasm.cpp
            ../../Userland/Libraries/LibTest/JavaScriptTestRunnerMain.cpp)
        set_target_properties(test-wasm_lagom PROPERTIES OUTPUT_NAME test-wasm)
        target_link_libraries(test-wasm_lagom LagomCore LagomTest LagomWasm LagomJS)
        add_test(
            NAME WasmParser
            COMMAND test-wasm_lagom --show-progress=false
        )
        set_tests_properties(WasmParser PROPERTIES
            ENVIRONMENT SERENITY_SOURCE_DIR=${SERENITY_PROJECT_ROOT}
            SKIP_RETURN_CODE 1)

        # Tests that are not LibTest based
        # test-crypto
        add_executable(test-crypto_lagom ../../Userland/Utilities/test-crypto.cpp)
        set_target_properties(test-crypto_lagom PROPERTIES OUTPUT_NAME test-crypto)
        target_link_libraries(test-crypto_lagom LagomCore LagomTLS LagomCrypto LagomLine)
        add_test(
            NAME Crypto
            COMMAND test-crypto_lagom test -t -s google.com --ca-certs-file ../../Base/etc/ca_certs.ini
            WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
        )

        # Shell
        file(GLOB SHELL_TESTS CONFIGURE_DEPENDS "../../Userland/Shell/Tests/*.sh")
        foreach(TEST_PATH ${SHELL_TESTS})
            get_filename_component(TEST_NAME ${TEST_PATH} NAME_WE)
            add_test(
                NAME "Shell-${TEST_NAME}"
                COMMAND shell_lagom --skip-shellrc "${TEST_PATH}"
                WORKING_DIRECTORY ${SERENITY_PROJECT_ROOT}/Userland/Shell/Tests
            )
            set_tests_properties("Shell-${TEST_NAME}" PROPERTIES
                TIMEOUT 10
                FAIL_REGULAR_EXPRESSION "FAIL"
                PASS_REGULAR_EXPRESSION "PASS"
            )
        endforeach()
    endif()
endif()

if (ENABLE_FUZZER_SANITIZER OR ENABLE_OSS_FUZZ)
    add_subdirectory(Fuzzers)
endif()

cmake_minimum_required (VERSION 3.16)

project(
    Lagom
    VERSION 0.0.0
    DESCRIPTION "Host build of SerenityOS libraries and applications"
    HOMEPAGE_URL "https://github.com/SerenityOS/serenity"
    LANGUAGES C CXX
)

if (CMAKE_CXX_COMPILER_ID STREQUAL "GNU" AND CMAKE_CXX_COMPILER_VERSION VERSION_LESS "11")
  message(FATAL_ERROR
      "A GCC version less than 11 was detected (${CMAKE_CXX_COMPILER_VERSION}), this is unsupported.\n"
      "Please re-read the build instructions documentation, and upgrade your host compiler.\n")
endif()

# This is required for CMake (when invoked for a Lagom-only build) to
# ignore any files downloading during the build, e.g. UnicodeData.txt.
# https://cmake.org/cmake/help/latest/policy/CMP0058.html
cmake_policy(SET CMP0058 NEW)

get_filename_component(
    SERENITY_PROJECT_ROOT "${PROJECT_SOURCE_DIR}/../.."
    ABSOLUTE CACHE
)
set(SerenityOS_SOURCE_DIR "${SERENITY_PROJECT_ROOT}" CACHE STRING "")

list(APPEND CMAKE_MODULE_PATH "${SERENITY_PROJECT_ROOT}/Meta/CMake")

if(NOT COMMAND serenity_option)
    macro(serenity_option)
        set(${ARGV})
    endmacro()
endif()

include(check_for_dependencies)
include(lagom_options)

if(ENABLE_ALL_THE_DEBUG_MACROS)
    include(all_the_debug_macros)
endif()

# FIXME: BUILD_SHARED_LIBS has a default of OFF, as it's intended to be set by the
#        user when configuring the project. We should instead change libjs-test262
#        and oss-fuzz to set this option on their end, and enable it by default in
#        Meta/serenity.sh. This is #9867.
option(BUILD_SHARED_LIBS "Build shared libraries instead of static libraries" ON)

find_package(Threads REQUIRED)

if (ENABLE_LAGOM_CCACHE)
    find_program(CCACHE_PROGRAM ccache)
    if(CCACHE_PROGRAM)
        set(CMAKE_C_COMPILER_LAUNCHER "${CCACHE_PROGRAM}" CACHE FILEPATH "Path to a compiler launcher program, e.g. ccache")
        set(CMAKE_CXX_COMPILER_LAUNCHER "${CCACHE_PROGRAM}" CACHE FILEPATH "Path to a compiler launcher program, e.g. ccache")
    endif()
endif()

if (ENABLE_FUZZERS_LIBFUZZER OR ENABLE_FUZZERS_OSSFUZZ)
	set(ENABLE_FUZZERS ON)
endif()

# We need to make sure not to build code generators for Fuzzer builds, as they already have their own main.cpp
# Instead, we import them from a previous install of Lagom. This mandates a two-stage build for fuzzers.
# The same concern goes for cross-compile builds, where we need the tools built for the host
set(BUILD_LAGOM_TOOLS ON)
if (ENABLE_FUZZERS OR CMAKE_CROSSCOMPILING)
    find_package(LagomTools REQUIRED)
    set(BUILD_LAGOM_TOOLS OFF)
endif()

include(wasm_spec_tests)
include(flac_spec_tests)
include(lagom_compile_options)

include(GNUInstallDirs) # make sure to include before we mess w/RPATH

set(CMAKE_SKIP_BUILD_RPATH FALSE)
set(CMAKE_BUILD_WITH_INSTALL_RPATH FALSE)
# See slide 100 of the following ppt :^)
# https://crascit.com/wp-content/uploads/2019/09/Deep-CMake-For-Library-Authors-Craig-Scott-CppCon-2019.pdf
if (APPLE)
    # FIXME: This doesn't work for the full BUILD_LAGOM=ON build, see #10055
    set(CMAKE_MACOSX_RPATH TRUE)
    set(CMAKE_INSTALL_NAME_DIR "@rpath")
    set(CMAKE_INSTALL_RPATH "@loader_path/../lib")
else()
    set(CMAKE_INSTALL_RPATH "$ORIGIN:$ORIGIN/../${CMAKE_INSTALL_LIBDIR}")
endif()
set(CMAKE_INSTALL_RPATH_USE_LINK_PATH TRUE)

set(CMAKE_EXPORT_COMPILE_COMMANDS ON)

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
    add_compile_options(-fsanitize=undefined -fno-omit-frame-pointer)
    set(LINKER_FLAGS "${LINKER_FLAGS} -fsanitize=undefined")
endif()

if (ENABLE_FUZZERS)
    add_compile_options(-fno-omit-frame-pointer)
endif()

if (CMAKE_CXX_COMPILER_ID MATCHES "Clang$")
    # Clang's default constexpr-steps limit is 1048576(2^20), GCC doesn't have one
    add_compile_options(-Wno-overloaded-virtual -Wno-user-defined-literals -fconstexpr-steps=16777216)
    # FIXME: Re-enable this check when the warning stops triggering, or document why we can't stop it from triggering.
    # For now, there is a lot of unused private fields in LibWeb that trigger this that could be removed.
    # See issue #14137 for details
    add_compile_options(-Wno-unused-private-field)

    if (ENABLE_FUZZERS_LIBFUZZER)
        add_compile_options(-fsanitize=fuzzer)
        set(LINKER_FLAGS "${LINKER_FLAGS} -fsanitize=fuzzer")
    endif()

elseif (CMAKE_CXX_COMPILER_ID STREQUAL "GNU")
    add_compile_options(-Wno-expansion-to-defined)
    if (ENABLE_FUZZERS_LIBFUZZER)
        message(FATAL_ERROR
            "Fuzzer Sanitizer (-fsanitize=fuzzer) is only supported for Fuzzer targets with LLVM. "
            "Reconfigure CMake with -DCMAKE_C_COMPILER and -DCMAKE_CXX_COMPILER pointing to a clang-based toolchain "
	    "or build binaries without built-in fuzzing support by setting -DENABLE_FUZZERS instead."
        )
    endif()
endif()

# These are here to support Fuzzili builds further down the directory stack
set(ORIGINAL_CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS}")
set(ORIGINAL_CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS}")
set(ORIGINAL_CMAKE_MODULE_LINKER_FLAGS "${CMAKE_MODULE_LINKER_FLAGS}")

set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} ${LINKER_FLAGS}")
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} ${LINKER_FLAGS}")
set(CMAKE_MODULE_LINKER_FLAGS "${CMAKE_MODULE_LINKER_FLAGS} ${LINKER_FLAGS}")

configure_file(../../AK/Debug.h.in AK/Debug.h @ONLY)

include_directories(../../)
include_directories(../../Userland/)
include_directories(../../Userland/Libraries/)
include_directories(../../Userland/Services)
include_directories(${CMAKE_BINARY_DIR})
include_directories(${CMAKE_CURRENT_BINARY_DIR})
include_directories(${CMAKE_CURRENT_BINARY_DIR}/Userland/Libraries)
include_directories(${CMAKE_CURRENT_BINARY_DIR}/Userland/Services)

# install rules, think about moving to its own helper cmake file
include(CMakePackageConfigHelpers)

# find_package(<package>) call for consumers to find this project
set(package Lagom CACHE STRING "")

# Allow package maintainers to freely override the path for the configs
set(Lagom_INSTALL_CMAKEDIR "${CMAKE_INSTALL_DATADIR}/${package}"
    CACHE PATH "CMake package config location relative to the install prefix")
mark_as_advanced(Lagom_INSTALL_CMAKEDIR)

install(
    FILES "${SERENITY_PROJECT_ROOT}/Meta/CMake/lagom-install-config.cmake"
    DESTINATION "${Lagom_INSTALL_CMAKEDIR}"
    RENAME "${package}Config.cmake"
    COMPONENT Lagom_Development
)

install(
    EXPORT LagomTargets
    NAMESPACE Lagom::
    DESTINATION "${Lagom_INSTALL_CMAKEDIR}"
    COMPONENT Lagom_Development
)

function(lagom_lib target_name fs_name)
    cmake_parse_arguments(LAGOM_LIBRARY "" "LIBRARY_TYPE" "SOURCES;LIBS" ${ARGN})
    string(REPLACE "Lib" "" library ${target_name})
    if (NOT LAGOM_LIBRARY_LIBRARY_TYPE)
        set(LAGOM_LIBRARY_LIBRARY_TYPE "")
    endif()
    add_library(${target_name} ${LAGOM_LIBRARY_LIBRARY_TYPE} ${LAGOM_LIBRARY_SOURCES})
    # Don't make alias when we're going to import a previous build for Tools
    # FIXME: Is there a better way to write this?
    if (NOT ENABLE_FUZZERS AND NOT CMAKE_CROSSCOMPILING)
        # alias for parity with exports
        add_library(Lagom::${library} ALIAS ${target_name})
    endif()

    set_target_properties(
        ${target_name} PROPERTIES
        VERSION "${PROJECT_VERSION}"
        SOVERSION "${PROJECT_VERSION_MAJOR}"
        EXPORT_NAME ${library}
        OUTPUT_NAME lagom-${fs_name}
    )
    target_link_libraries(${target_name} ${LAGOM_LIBRARY_LIBS})
    if (NOT ${target_name} STREQUAL "LibCore")
        target_link_libraries(${target_name} LibCore)
    endif()
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
    # FIXME: Move this to serenity_install_headers
    install(
        DIRECTORY "${SERENITY_PROJECT_ROOT}/Userland/Libraries/Lib${library}"
        COMPONENT Lagom_Development
        DESTINATION ${CMAKE_INSTALL_INCLUDEDIR}
        FILES_MATCHING PATTERN "*.h"
    )
    serenity_generated_sources(${target_name})
endfunction()

function(lagom_test source)
    cmake_parse_arguments(LAGOM_TEST "" "WORKING_DIRECTORY" "LIBS" ${ARGN})
    get_filename_component(name ${source} NAME_WE)
    add_executable(${name} ${source})
    target_link_libraries(${name} LibCore LibTest LibTestMain ${LAGOM_TEST_LIBS})
    add_test(
            NAME ${name}
            COMMAND ${name}
            WORKING_DIRECTORY ${LAGOM_TEST_WORKING_DIRECTORY}
    )
endfunction()

function(serenity_bin name)
    add_executable(${name} ${SOURCES} ${GENERATED_SOURCES})
    add_executable(Lagom::${name} ALIAS ${name})
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
    serenity_generated_sources(${name})
endfunction()

function(serenity_lib name fs_name)
    lagom_lib(${name} ${fs_name} SOURCES ${SOURCES} ${GENERATED_SOURCES})
endfunction()

function(serenity_lib_static name fs_name)
    lagom_lib(${name} ${fs_name} LIBRARY_TYPE STATIC SOURCES ${SOURCES} ${GENERATED_SOURCES})
endfunction()

function(serenity_install_headers dir)
endfunction()

function(serenity_install_sources dir)
endfunction()

macro(add_serenity_subdirectory path)
    add_subdirectory("${SERENITY_PROJECT_ROOT}/${path}" "${CMAKE_CURRENT_BINARY_DIR}/${path}")
endmacro()

add_custom_target(components ALL)
option(BUILD_EVERYTHING "Build all optional components" ON)

if (NOT TARGET all_generated)
    # Meta target to run all code-gen steps in the build.
    add_custom_target(all_generated)
endif()

# Create mostly empty targets for system libraries we don't need to build for Lagom
add_library(LibC INTERFACE)
add_library(LibCrypt INTERFACE)
add_library(LibSystem INTERFACE)
if (NOT APPLE AND NOT ANDROID AND NOT ${CMAKE_SYSTEM_NAME} MATCHES "OpenBSD")
    target_link_libraries(LibCrypt INTERFACE crypt) # LibCore::Account uses crypt() but it's not in libcrypt on macOS
endif()
if (SERENITYOS)
    # Serenity only allows syscalls from LibSystem, so if someone asks for that on Lagom,
    # we need to pass that through to the system's LibSystem.
    target_link_libraries(LibSystem INTERFACE system)
endif()
add_library(NoCoverage INTERFACE)
# "install" these special targets to placate CMake
install(TARGETS LibC LibCrypt LibSystem NoCoverage EXPORT LagomTargets)

# AK/LibCore
# Note: AK is included in LibCore for the host build instead of LibC per the target build
add_serenity_subdirectory(AK)
add_serenity_subdirectory(Userland/Libraries/LibCore)
target_link_libraries(LibCore Threads::Threads)
target_sources(LibCore PRIVATE ${AK_SOURCES})

# LibMain
add_serenity_subdirectory(Userland/Libraries/LibMain)

# LibTimeZone
# This is needed even if Lagom is not enabled because it is depended upon by code generators.
add_serenity_subdirectory(Userland/Libraries/LibTimeZone)
# We need an install rule for LibTimeZone b/c it is a manual OBJECT library instead of serenity_lib
install(TARGETS LibTimeZone EXPORT LagomTargets)

# LibIDL
# This is used by the BindingsGenerator so needs to always be built.
add_serenity_subdirectory(Userland/Libraries/LibIDL)

# Manually install AK headers
install(
    DIRECTORY "${SERENITY_PROJECT_ROOT}/AK"
    COMPONENT Lagom_Development
    DESTINATION ${CMAKE_INSTALL_INCLUDEDIR}
    FILES_MATCHING PATTERN "*.h"
)

# Code Generators and other host tools
if (BUILD_LAGOM_TOOLS)
    add_subdirectory(Tools)
endif()

if (BUILD_LAGOM)
    # Lagom Libraries
    set(lagom_standard_libraries
        Archive
        Audio
        Compress
        Crypto
        DNS
        Gemini
        Gfx
        GL
        GPU
        HTTP
        IMAP
        IPC
        JS
        Line
        Locale
        Markdown
        PDF
        Regex
        SoftGPU
        SQL
        Syntax
        TextCodec
        Threading
        TLS
        Unicode
        Video
        Wasm
        WebSocket
        XML
    )

    if (ENABLE_LAGOM_LIBWEB)
        list(APPEND lagom_standard_libraries Web)

        # WebView
        list(APPEND LIBWEBVIEW_SOURCES "../../Userland/Libraries/LibWebView/DOMTreeModel.cpp")
        list(APPEND LIBWEBVIEW_SOURCES "../../Userland/Libraries/LibWebView/WebContentClient.cpp")

        compile_ipc(${SERENITY_PROJECT_ROOT}/Userland/Services/WebContent/WebContentServer.ipc WebContent/WebContentServerEndpoint.h)
        compile_ipc(${SERENITY_PROJECT_ROOT}/Userland/Services/WebContent/WebContentClient.ipc WebContent/WebContentClientEndpoint.h)

        list(APPEND LIBWEBVIEW_GENERATED_SOURCES WebContent/WebContentClientEndpoint.h)
        list(APPEND LIBWEBVIEW_GENERATED_SOURCES WebContent/WebContentServerEndpoint.h)

        set(GENERATED_SOURCES ${LIBWEBVIEW_GENERATED_SOURCES})
        lagom_lib(LibWebView webview
            SOURCES ${LIBWEBVIEW_SOURCES} ${LIBWEBVIEW_GENERATED_SOURCES}
            LIBS LibGUI LibWeb)
        unset(GENERATED_SOURCES)
    endif()

    # FIXME: Excluding arm64 is a temporary hack to circumvent a build problem
    #        for Lagom on Apple M1
    if (NOT CMAKE_SYSTEM_PROCESSOR MATCHES "arm64")
        # FIXME: Create a LIBELF_SOURCES macro similar to AK
        file(GLOB LIBELF_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibELF/*.cpp")
        # There's no way we can reliably make the dynamic loading classes cross platform
        list(FILTER LIBELF_SOURCES EXCLUDE REGEX ".*Dynamic.*.cpp$")
        lagom_lib(LibELF elf
            SOURCES ${LIBELF_SOURCES}
        )
        list(APPEND lagom_standard_libraries X86)
    endif()

    foreach(lib IN LISTS lagom_standard_libraries)
        add_serenity_subdirectory("Userland/Libraries/Lib${lib}")
    endforeach()

    # GUI
    set(LIBGUI_SOURCES
        GML/Lexer.cpp
        GML/Parser.cpp
        GML/SyntaxHighlighter.cpp
        Icon.cpp
        Model.cpp
        ModelIndex.cpp
    )
    list(TRANSFORM LIBGUI_SOURCES PREPEND "${SERENITY_PROJECT_ROOT}/Userland/Libraries/LibGUI/")
    lagom_lib(LibGUI gui
        SOURCES ${LIBGUI_SOURCES}
        LIBS LibGfx LibSyntax)

    # FIXME: Why doesn't RPATH of the tests handle this properly?
    set_target_properties(LibSoftGPU PROPERTIES LIBRARY_OUTPUT_DIRECTORY ${CMAKE_CURRENT_BINARY_DIR})

    # FIXME: LibLocaleData is an object lib in Lagom, because the weak symbol trick we use on serenity
    #    straight up isn't supposed to work per ELF rules
    target_link_libraries(LibLocale LibTimeZone)
    install(TARGETS LibLocaleData EXPORT LagomTargets)

    add_serenity_subdirectory(Userland/Shell)

    if (NOT ENABLE_FUZZERS AND NOT ENABLE_COMPILER_EXPLORER_BUILD AND NOT ANDROID)
        # Lagom Services
        add_serenity_subdirectory(Userland/Services)

        # Lagom Examples
        add_executable(TestApp TestApp.cpp)
        target_link_libraries(TestApp LibCore)

        add_executable(TestJson TestJson.cpp)
        target_link_libraries(TestJson LibCore)

        # Lagom Utilities
        add_executable(adjtime ../../Userland/Utilities/adjtime.cpp)
        target_link_libraries(adjtime LibCore LibMain)

        # FIXME: Excluding arm64 is a temporary hack to circumvent a build problem
        #        for Lagom on Apple M1
        if (NOT CMAKE_SYSTEM_PROCESSOR MATCHES "arm64")
            add_executable(disasm ../../Userland/Utilities/disasm.cpp)
            target_link_libraries(disasm LibCore LibELF LibX86 LibMain)
        endif()

        add_executable(gml-format ../../Userland/Utilities/gml-format.cpp)
        target_link_libraries(gml-format LibCore LibGUI LibMain)

        if (ENABLE_LAGOM_LIBWEB)
            add_executable(headless-browser ../../Userland/Utilities/headless-browser.cpp)
            target_link_libraries(headless-browser LibWeb LibWebSocket LibHTTP LibJS LibGfx LibMain)
        endif()

        add_executable(js ../../Userland/Utilities/js.cpp)
        target_link_libraries(js LibJS LibLine LibMain LibTextCodec Threads::Threads)

        add_executable(markdown-check ../../Userland/Utilities/markdown-check.cpp)
        target_link_libraries(markdown-check LibMarkdown LibMain)

        add_executable(ntpquery ../../Userland/Utilities/ntpquery.cpp)
        target_link_libraries(ntpquery LibCore LibMain)

        add_executable(test262-runner ../../Tests/LibJS/test262-runner.cpp)
        target_link_libraries(test262-runner LibJS LibCore)

        add_executable(wasm ../../Userland/Utilities/wasm.cpp)
        target_link_libraries(wasm LibCore LibWasm LibLine LibMain)

        add_executable(xml ../../Userland/Utilities/xml.cpp)
        target_link_libraries(xml LibCore LibXML LibMain)

        enable_testing()
        # LibTest
        file(GLOB LIBTEST_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibTest/*.cpp")
        list(FILTER LIBTEST_SOURCES EXCLUDE REGEX ".*Main.cpp$")
        add_library(
            LibTest
            ${LIBTEST_SOURCES}
        )
        target_link_libraries(LibTest LibCore)
        set_target_properties(LibTest PROPERTIES OUTPUT_NAME lagom-test)
        add_library(
            LibTestMain
            OBJECT
            "${SERENITY_PROJECT_ROOT}/Userland/Libraries/LibTest/TestMain.cpp"
        )

        # LibTest tests from Tests/
        # AK
        file(GLOB AK_TEST_SOURCES CONFIGURE_DEPENDS "../../Tests/AK/*.cpp")
        foreach(source ${AK_TEST_SOURCES})
            lagom_test(${source} WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/../../Tests/AK)
        endforeach()

        # LibAudio
        file(GLOB LIBAUDIO_TEST_SOURCES CONFIGURE_DEPENDS "../../Tests/LibAudio/*.cpp")
        foreach(source ${LIBAUDIO_TEST_SOURCES})
            lagom_test(${source} LIBS LibAudio WORKING_DIRECTORY ${FLAC_TEST_PATH})
        endforeach()

        # LibCore
        lagom_test(../../Tests/LibCore/TestLibCoreIODevice.cpp WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/../../Tests/LibCore)

        # Crypto
        file(GLOB LIBCRYPTO_TESTS CONFIGURE_DEPENDS "../../Tests/LibCrypto/*.cpp")
        foreach(source ${LIBCRYPTO_TESTS})
            lagom_test(${source} LIBS LibCrypto)
        endforeach()

        # Compress
        file(COPY "${SERENITY_PROJECT_ROOT}/Tests/LibCompress/brotli-test-files" DESTINATION "./")
        file(GLOB LIBCOMPRESS_TESTS CONFIGURE_DEPENDS "../../Tests/LibCompress/*.cpp")
        foreach(source ${LIBCOMPRESS_TESTS})
            lagom_test(${source} LIBS LibCompress)
        endforeach()

        # GL
        file(COPY "${SERENITY_PROJECT_ROOT}/Tests/LibGL/reference-images" DESTINATION "./")
        file(GLOB LIBGL_TESTS CONFIGURE_DEPENDS "../../Tests/LibGL/*.cpp")
        foreach(source ${LIBGL_TESTS})
            lagom_test(${source} LIBS LibGL LibGPU LibSoftGPU)
        endforeach()

        # Locale
        file(GLOB LIBLOCALE_TEST_SOURCES CONFIGURE_DEPENDS "../../Tests/LibLocale/*.cpp")
        foreach(source ${LIBLOCALE_TEST_SOURCES})
            lagom_test(${source} LIBS LibLocale LibUnicode)
        endforeach()

        # PDF
        file(GLOB LIBPDF_TESTS CONFIGURE_DEPENDS "../../Tests/LibPDF/*.cpp")
        foreach(source ${LIBPDF_TESTS})
            lagom_test(${source} LIBS LibPDF WORKING_DIRECTORY ${SERENITY_PROJECT_ROOT}/Base/home/anon/Documents/pdf/)
        endforeach()

        # Regex
        file(GLOB LIBREGEX_TESTS CONFIGURE_DEPENDS "../../Tests/LibRegex/*.cpp")
        # RegexLibC test POSIX <regex.h> and contains many Serenity extensions
        # It is therefore not reasonable to run it on Lagom
        list(REMOVE_ITEM LIBREGEX_TESTS "${CMAKE_CURRENT_SOURCE_DIR}/../../Tests/LibRegex/RegexLibC.cpp")
        foreach(source ${LIBREGEX_TESTS})
            lagom_test(${source} LIBS LibRegex)
        endforeach()

        # SQL
        file(GLOB LIBSQL_TEST_SOURCES CONFIGURE_DEPENDS "../../Tests/LibSQL/*.cpp")
        foreach(source ${LIBSQL_TEST_SOURCES})
            lagom_test(${source} LIBS LibSQL)
        endforeach()

        # TextCodec
        file(GLOB LIBTEXTCODEC_TESTS CONFIGURE_DEPENDS "../../Tests/LibTextCodec/*.cpp")
        foreach(source ${LIBTEXTCODEC_TESTS})
            lagom_test(${source} LIBS LibTextCodec
                WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/../../Tests/LibTextCodec)
        endforeach()

        # TLS
        file(GLOB LIBTLS_TESTS CONFIGURE_DEPENDS "../../Tests/LibTLS/*.cpp")
        foreach(source ${LIBTLS_TESTS})
            lagom_test(${source} LIBS LibTLS
                WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/../../Tests/LibTLS)
        endforeach()

        # TTF
        file(GLOB LIBTTF_TESTS CONFIGURE_DEPENDS "../../Tests/LibTTF/*.cpp")
        foreach(source ${LIBTTF_TESTS})
            lagom_test(${source} LIBS LibGfx)
        endforeach()

        # LibTimeZone
        file(GLOB LIBTIMEZONE_TEST_SOURCES CONFIGURE_DEPENDS "../../Tests/LibTimeZone/*.cpp")
        foreach(source ${LIBTIMEZONE_TEST_SOURCES})
            lagom_test(${source} LIBS LibTimeZone)

            get_filename_component(target "${source}" NAME_WLE)
            target_compile_definitions("${target}" PRIVATE ENABLE_TIME_ZONE_DATA=$<BOOL:${ENABLE_TIME_ZONE_DATABASE_DOWNLOAD}>)
        endforeach()

        # Unicode
        file(GLOB LIBUNICODE_TEST_SOURCES CONFIGURE_DEPENDS "../../Tests/LibUnicode/*.cpp")
        foreach(source ${LIBUNICODE_TEST_SOURCES})
            lagom_test(${source} LIBS LibUnicode)
        endforeach()

        # Video
        file(GLOB LIBVIDEO_TEST_SOURCES CONFIGURE_DEPENDS "../../Tests/LibVideo/*.cpp")
        foreach(source ${LIBVIDEO_TEST_SOURCES})
            lagom_test(${source} LIBS LibVideo
                WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/../../Tests/LibVideo)
        endforeach()

        # JavaScriptTestRunner + LibTest tests
        # test-js
        add_executable(test-js
            ../../Tests/LibJS/test-js.cpp
            ../../Userland/Libraries/LibTest/JavaScriptTestRunnerMain.cpp)
        target_link_libraries(test-js LibCore LibTest LibJS)
        add_test(
            NAME JS
            COMMAND test-js --show-progress=false
        )
        set_tests_properties(JS PROPERTIES ENVIRONMENT SERENITY_SOURCE_DIR=${SERENITY_PROJECT_ROOT})

        # Extra tests from Tests/LibJS
        lagom_test(../../Tests/LibJS/test-invalid-unicode-js.cpp LIBS LibJS)
        lagom_test(../../Tests/LibJS/test-bytecode-js.cpp LIBS LibJS)
        lagom_test(../../Tests/LibJS/test-value-js.cpp LIBS LibJS)

        # Spreadsheet
        add_executable(test-spreadsheet
                ../../Tests/Spreadsheet/test-spreadsheet.cpp
                ../../Userland/Libraries/LibTest/JavaScriptTestRunnerMain.cpp)
        target_link_libraries(test-spreadsheet LibCore LibTest LibJS)
        add_test(
                NAME Spreadsheet
                COMMAND test-spreadsheet --show-progress=false
        )
        set_tests_properties(Spreadsheet PROPERTIES ENVIRONMENT SERENITY_SOURCE_DIR=${SERENITY_PROJECT_ROOT})


        # Markdown
        include(commonmark_spec)
        file(GLOB LIBMARKDOWN_TEST_SOURCES CONFIGURE_DEPENDS "../../Tests/LibMarkdown/*.cpp")
        foreach(source ${LIBMARKDOWN_TEST_SOURCES})
            lagom_test(${source} LIBS LibMarkdown)
        endforeach()
        set_tests_properties(TestCommonmark PROPERTIES DISABLED YES)

        # test-wasm
        add_executable(test-wasm
            ../../Tests/LibWasm/test-wasm.cpp
            ../../Userland/Libraries/LibTest/JavaScriptTestRunnerMain.cpp)
        target_link_libraries(test-wasm LibCore LibTest LibWasm LibJS)
        add_test(
            NAME WasmParser
            COMMAND test-wasm --show-progress=false
        )
        set_tests_properties(WasmParser PROPERTIES
            ENVIRONMENT SERENITY_SOURCE_DIR=${SERENITY_PROJECT_ROOT}
            SKIP_RETURN_CODE 1)

        # Tests that are not LibTest based
        # Shell
        file(GLOB SHELL_TESTS CONFIGURE_DEPENDS "../../Userland/Shell/Tests/*.sh")
        foreach(TEST_PATH ${SHELL_TESTS})
            get_filename_component(TEST_NAME ${TEST_PATH} NAME_WE)
            add_test(
                NAME "Shell-${TEST_NAME}"
                COMMAND Shell --skip-shellrc "${TEST_PATH}"
                WORKING_DIRECTORY ${SERENITY_PROJECT_ROOT}/Userland/Shell/Tests
            )
            set_tests_properties("Shell-${TEST_NAME}" PROPERTIES
                TIMEOUT 10
                FAIL_REGULAR_EXPRESSION "FAIL"
                PASS_REGULAR_EXPRESSION "PASS"
            )
        endforeach()

        # FIXME: When we are using CMake >= 3.21, the library installations can be replaced with RUNTIME_DEPENDENCIES.
        #        https://cmake.org/cmake/help/latest/command/install.html
        include(get_linked_lagom_libraries.cmake)
        get_linked_lagom_libraries(js js_libraries)

        install(TARGETS js ${js_libraries} COMPONENT js)

        set(CPACK_GENERATOR "TGZ")
        set(CPACK_STRIP_FILES TRUE)
        set(CPACK_ARCHIVE_COMPONENT_INSTALL ON)
        set(CPACK_COMPONENTS_ALL js)
        set(CPACK_PACKAGE_FILE_NAME serenity-js)
        include(CPack)
    endif()
endif()

if (ENABLE_FUZZERS)
    add_subdirectory(Fuzzers)
else()
    export_components("${CMAKE_BINARY_DIR}/components.ini")
endif()

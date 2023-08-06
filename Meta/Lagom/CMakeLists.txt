cmake_minimum_required (VERSION 3.16)

project(
    Lagom
    VERSION 0.0.0
    DESCRIPTION "Host build of SerenityOS libraries and applications"
    HOMEPAGE_URL "https://github.com/SerenityOS/serenity"
    LANGUAGES C CXX
)

if (CMAKE_CXX_COMPILER_ID STREQUAL "GNU" AND CMAKE_CXX_COMPILER_VERSION VERSION_LESS "12")
  message(FATAL_ERROR
      "A GCC version less than 12 was detected (${CMAKE_CXX_COMPILER_VERSION}), this is unsupported.\n"
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

set(SERENITY_CACHE_DIR "${PROJECT_BINARY_DIR}" CACHE STRING "")

list(APPEND CMAKE_MODULE_PATH "${SERENITY_PROJECT_ROOT}/Meta/CMake")

if(NOT COMMAND serenity_option)
    macro(serenity_option)
        set(${ARGV})
    endmacro()
endif()

include(check_for_dependencies)
include(lagom_options NO_POLICY_SCOPE)

if(ENABLE_ALL_THE_DEBUG_MACROS)
    include(all_the_debug_macros)
endif()

# FIXME: Is it worth inventing `serenity_dependent_option` ?
if (ENABLE_LAGOM_LADYBIRD)
    set(ENABLE_LAGOM_LIBWEB ON CACHE BOOL "" FORCE)
endif()

# FIXME: BUILD_SHARED_LIBS has a default of OFF, as it's intended to be set by the
#        user when configuring the project. We should instead change libjs-test262
#        and oss-fuzz to set this option on their end, and enable it by default in
#        Meta/serenity.sh. This is #9867.
option(BUILD_SHARED_LIBS "Build shared libraries instead of static libraries" ON)

find_package(Threads REQUIRED)
# FIXME: This global link libraries is required to workaround linker issues (on some systems)
# from the Ladybird import. See https://github.com/SerenityOS/serenity/issues/16847
link_libraries(Threads::Threads)

if (ENABLE_LAGOM_CCACHE)
    include(setup_ccache)
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

include(flac_spec_tests)
include(lagom_compile_options)

include(GNUInstallDirs) # make sure to include before we mess w/RPATH

# Mirror the structure of the installed tree to ensure that rpaths
# always remain valid.
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY "${PROJECT_BINARY_DIR}/bin")
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY "${PROJECT_BINARY_DIR}/${CMAKE_INSTALL_LIBDIR}")
set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY "${PROJECT_BINARY_DIR}/${CMAKE_INSTALL_LIBDIR}")

set(CMAKE_SKIP_BUILD_RPATH FALSE)
set(CMAKE_BUILD_WITH_INSTALL_RPATH TRUE)
# See slide 100 of the following ppt :^)
# https://crascit.com/wp-content/uploads/2019/09/Deep-CMake-For-Library-Authors-Craig-Scott-CppCon-2019.pdf
if (APPLE)
    set(CMAKE_MACOSX_RPATH TRUE)
    set(CMAKE_INSTALL_NAME_DIR "@rpath")
    set(CMAKE_INSTALL_RPATH "@loader_path/../lib")
else()
    set(CMAKE_INSTALL_RPATH "$ORIGIN:$ORIGIN/../${CMAKE_INSTALL_LIBDIR}")
endif()
set(CMAKE_INSTALL_RPATH_USE_LINK_PATH TRUE)

set(CMAKE_EXPORT_COMPILE_COMMANDS ON)

set(CMAKE_INSTALL_MESSAGE NEVER)

if (EMSCRIPTEN)
    set(CMAKE_EXECUTABLE_SUFFIX ".js")
    add_compile_options(-gsource-map)
    add_link_options(--emrun "SHELL:-s ALLOW_MEMORY_GROWTH")
endif()

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

if (ENABLE_COMPILETIME_FORMAT_CHECK)
    add_compile_definitions(ENABLE_COMPILETIME_FORMAT_CHECK)
endif()

if (ENABLE_LAGOM_MOLD)
    add_link_options(-fuse-ld=mold)
endif()

if (ENABLE_FUZZERS)
    add_compile_options(-fno-omit-frame-pointer)
endif()

if (ENABLE_LAGOM_LADYBIRD AND (ENABLE_FUZZERS OR ENABLE_COMPILER_EXPLORER_BUILD))
    message(FATAL_ERROR
        "Ladybird build not supported for Fuzzers or Compiler Explorer."
        "Disable ENABLE_LAGOM_LADYBIRD and try again."
    )
endif()

CHECK_INCLUDE_FILE(pulse/pulseaudio.h HAVE_PULSEAUDIO)

if (CMAKE_CXX_COMPILER_ID MATCHES "Clang$")
    add_compile_options(-Wno-overloaded-virtual)
    # FIXME: Re-enable this check when the warning stops triggering, or document why we can't stop it from triggering.
    # For now, there is a lot of unused private fields in LibWeb that trigger this that could be removed.
    # See issue #14137 for details
    add_compile_options(-Wno-unused-private-field)

    if (ENABLE_FUZZERS_LIBFUZZER)
        add_compile_options(-fsanitize=fuzzer)
        set(LINKER_FLAGS "${LINKER_FLAGS} -fsanitize=fuzzer")
    endif()

elseif (CMAKE_CXX_COMPILER_ID STREQUAL "GNU")
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
    target_link_libraries(${target_name} PRIVATE ${LAGOM_LIBRARY_LIBS})
    if (NOT ${target_name} STREQUAL "LibCore")
        target_link_libraries(${target_name} PRIVATE LibCore)
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
    target_link_libraries(${name} PRIVATE LibCore LibFileSystem LibTest LibTestMain ${LAGOM_TEST_LIBS})
    add_test(
        NAME ${name}
        COMMAND ${name}
        WORKING_DIRECTORY ${LAGOM_TEST_WORKING_DIRECTORY}
    )
    set_target_properties(${name} PROPERTIES LAGOM_WORKING_DIRECTORY "${LAGOM_TEST_WORKING_DIRECTORY}")
endfunction()

function(serenity_test test_src sub_dir)
    cmake_parse_arguments(PARSE_ARGV 2 SERENITY_TEST "MAIN_ALREADY_DEFINED" "CUSTOM_MAIN" "LIBS")
    # FIXME: Pass MAIN_ALREADY_DEFINED and CUSTOM_MAIN to support tests that use them.
    lagom_test(${test_src} LIBS ${SERENITY_TEST_LIBS} WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR})
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
if (NOT APPLE AND NOT ANDROID AND NOT EMSCRIPTEN AND NOT ${CMAKE_SYSTEM_NAME} MATCHES "OpenBSD")
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
target_link_libraries(LibCore PRIVATE Threads::Threads)
if (${CMAKE_SYSTEM_NAME} MATCHES "NetBSD")
    # NetBSD has its shm_open and shm_unlink functions in librt so we need to link that
    target_link_libraries(LibCore PRIVATE librt.so)
endif()
if (${CMAKE_SYSTEM_NAME} MATCHES "SunOS")
    # Solaris has socket and networking related functions in two extra libraries
    target_link_libraries(LibCore PRIVATE nsl socket)
endif()
target_sources(LibCore PRIVATE ${AK_SOURCES})

# LibMain
add_serenity_subdirectory(Userland/Libraries/LibMain)

# LibFileSystem
# This is needed even if Lagom is not enabled because it is depended upon by code generators.
add_serenity_subdirectory(Userland/Libraries/LibFileSystem)

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
        Cpp
        DNS
        Diff
        Gemini
        Gfx
        GL
        GLSL
        GPU
        HTTP
        IMAP
        IPC
        JS
        Line
        Locale
        Manual
        Markdown
        PDF
        Protocol
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
        list(APPEND LIBWEBVIEW_SOURCES "../../Userland/Libraries/LibWebView/AccessibilityTreeModel.cpp")
        list(APPEND LIBWEBVIEW_SOURCES "../../Userland/Libraries/LibWebView/DOMTreeModel.cpp")
        list(APPEND LIBWEBVIEW_SOURCES "../../Userland/Libraries/LibWebView/StylePropertiesModel.cpp")
        list(APPEND LIBWEBVIEW_SOURCES "../../Userland/Libraries/LibWebView/ViewImplementation.cpp")
        list(APPEND LIBWEBVIEW_SOURCES "../../Userland/Libraries/LibWebView/WebContentClient.cpp")
        list(APPEND LIBWEBVIEW_SOURCES "../../Userland/Libraries/LibWebView/RequestServerAdapter.cpp")
        list(APPEND LIBWEBVIEW_SOURCES "../../Userland/Libraries/LibWebView/WebSocketClientAdapter.cpp")

        compile_ipc(${SERENITY_PROJECT_ROOT}/Userland/Services/WebContent/WebContentServer.ipc WebContent/WebContentServerEndpoint.h)
        compile_ipc(${SERENITY_PROJECT_ROOT}/Userland/Services/WebContent/WebContentClient.ipc WebContent/WebContentClientEndpoint.h)
        compile_ipc(${SERENITY_PROJECT_ROOT}/Userland/Services/WebContent/WebDriverClient.ipc WebContent/WebDriverClientEndpoint.h)
        compile_ipc(${SERENITY_PROJECT_ROOT}/Userland/Services/WebContent/WebDriverServer.ipc WebContent/WebDriverServerEndpoint.h)
        compile_ipc(${SERENITY_PROJECT_ROOT}/Userland/Services/RequestServer/RequestClient.ipc RequestServer/RequestClientEndpoint.h)
        compile_ipc(${SERENITY_PROJECT_ROOT}/Userland/Services/RequestServer/RequestServer.ipc RequestServer/RequestServerEndpoint.h)
        compile_ipc(${SERENITY_PROJECT_ROOT}/Userland/Services/WebSocket/WebSocketClient.ipc WebSocket/WebSocketClientEndpoint.h)
        compile_ipc(${SERENITY_PROJECT_ROOT}/Userland/Services/WebSocket/WebSocketServer.ipc WebSocket/WebSocketServerEndpoint.h)

        list(APPEND LIBWEBVIEW_GENERATED_SOURCES WebContent/WebContentClientEndpoint.h)
        list(APPEND LIBWEBVIEW_GENERATED_SOURCES WebContent/WebContentServerEndpoint.h)
        list(APPEND LIBWEBVIEW_GENERATED_SOURCES WebContent/WebDriverClientEndpoint.h)
        list(APPEND LIBWEBVIEW_GENERATED_SOURCES WebContent/WebDriverServerEndpoint.h)
        list(APPEND LIBWEBVIEW_GENERATED_SOURCES RequestServer/RequestClientEndpoint.h)
        list(APPEND LIBWEBVIEW_GENERATED_SOURCES RequestServer/RequestServerEndpoint.h)
        list(APPEND LIBWEBVIEW_GENERATED_SOURCES WebSocket/WebSocketClientEndpoint.h)
        list(APPEND LIBWEBVIEW_GENERATED_SOURCES WebSocket/WebSocketServerEndpoint.h)

        set(GENERATED_SOURCES ${LIBWEBVIEW_GENERATED_SOURCES})
        lagom_lib(LibWebView webview
            SOURCES ${LIBWEBVIEW_SOURCES} ${LIBWEBVIEW_GENERATED_SOURCES}
            LIBS LibGfx LibGUI LibIPC LibWeb LibProtocol)
        unset(GENERATED_SOURCES)
    endif()

    if (NOT EMSCRIPTEN)
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

    # FIXME: LibLocaleData is an object lib in Lagom, because the weak symbol trick we use on serenity
    #    straight up isn't supposed to work per ELF rules
    target_link_libraries(LibLocale PRIVATE LibTimeZone)
    if (ENABLE_UNICODE_DATABASE_DOWNLOAD)
        install(TARGETS LibLocaleData EXPORT LagomTargets)
    endif()

    add_serenity_subdirectory(Userland/Shell)

    if (NOT ENABLE_FUZZERS AND NOT ENABLE_COMPILER_EXPLORER_BUILD AND NOT ANDROID)
        # Lagom Services
        add_serenity_subdirectory(Userland/Services)

        # Lagom Utilities
        add_executable(abench ../../Userland/Utilities/abench.cpp)
        target_link_libraries(abench LibCore LibMain LibFileSystem LibAudio)

        add_executable(aconv ../../Userland/Utilities/aconv.cpp)
        target_link_libraries(aconv LibCore LibMain LibFileSystem LibAudio)

        if (NOT EMSCRIPTEN)
            add_executable(adjtime ../../Userland/Utilities/adjtime.cpp)
            target_link_libraries(adjtime LibCore LibMain)
        endif()

        # FIXME: Excluding arm64 is a temporary hack to circumvent a build problem
        #        for Lagom on Apple M1
        if (NOT CMAKE_SYSTEM_PROCESSOR MATCHES "arm64" AND NOT EMSCRIPTEN)
            add_executable(disasm ../../Userland/Utilities/disasm.cpp)
            target_link_libraries(disasm LibCore LibELF LibX86 LibMain)
        endif()

        if (NOT EMSCRIPTEN)
            # LibELF is part of LibC in SerenityOS builds, but not in Lagom.
            add_executable(file ../../Userland/Utilities/file.cpp)
            target_link_libraries(file LibAudio LibArchive LibCompress LibCore LibELF LibGfx LibIPC LibMain)
        endif()

        add_executable(gml-format ../../Userland/Utilities/gml-format.cpp)
        target_link_libraries(gml-format LibCore LibGUI LibMain)

        add_executable(gunzip ../../Userland/Utilities/gunzip.cpp)
        target_link_libraries(gunzip LibCompress LibCore LibMain)

        add_executable(gzip ../../Userland/Utilities/gzip.cpp)
        target_link_libraries(gzip LibCompress LibCore LibMain)

        if (ENABLE_LAGOM_LADYBIRD)
            add_serenity_subdirectory(Ladybird)
        endif()

        add_executable(icc ../../Userland/Utilities/icc.cpp)
        target_link_libraries(icc LibCore LibGfx LibMain)

        add_executable(image ../../Userland/Utilities/image.cpp)
        target_link_libraries(image LibCore LibGfx LibMain)

        add_executable(isobmff ../../Userland/Utilities/isobmff.cpp)
        target_link_libraries(isobmff LibCore LibGfx LibMain)

        add_executable(ttfdisasm ../../Userland/Utilities/ttfdisasm.cpp)
        target_link_libraries(ttfdisasm LibGfx LibMain)

        add_executable(js ../../Userland/Utilities/js.cpp)
        target_link_libraries(js LibCrypto LibJS LibLine LibLocale LibMain LibTextCodec Threads::Threads)
        if (EMSCRIPTEN)
            add_executable(libjs Wasm/js_repl.cpp)
            target_link_libraries(libjs LibJS LibLocale LibTimeZone LibUnicode)
            target_link_options(libjs PRIVATE
                    -sEXPORTED_FUNCTIONS=_initialize_repl,_execute
                    -sEXPORTED_RUNTIME_METHODS=allocateUTF8
                    -sERROR_ON_UNDEFINED_SYMBOLS=0
                    -sENVIRONMENT=web)
        endif()

        add_executable(lzcat ../../Userland/Utilities/lzcat.cpp)
        target_link_libraries(lzcat LibCompress LibCore LibMain)

        add_executable(markdown-check ../../Userland/Utilities/markdown-check.cpp)
        target_link_libraries(markdown-check LibFileSystem LibMarkdown LibMain LibManual)

        if (NOT EMSCRIPTEN)
            add_executable(ntpquery ../../Userland/Utilities/ntpquery.cpp)
            target_link_libraries(ntpquery LibCore LibMain)
        endif()

        add_executable(pdf ../../Userland/Utilities/pdf.cpp)
        target_link_libraries(pdf LibCore LibGfx LibPDF LibMain)

        add_executable(sql ../../Userland/Utilities/sql.cpp)
        target_link_libraries(sql LibCore LibFileSystem LibIPC LibLine LibMain LibSQL)

        add_executable(tar ../../Userland/Utilities/tar.cpp)
        target_link_libraries(tar LibArchive LibCompress LibCore LibFileSystem LibMain)

        add_executable(test262-runner ../../Tests/LibJS/test262-runner.cpp)
        target_link_libraries(test262-runner LibJS LibCore LibFileSystem)

        if (CMAKE_SYSTEM_NAME STREQUAL "Linux")
            include(CheckCSourceCompiles)
            # Check for musl's declaration of __assert_fail
            check_c_source_compiles(
                "
                #include <assert.h>
                __attribute__((__noreturn__)) void __assert_fail(char const* assertion, char const* file, int line, char const* function) {}
                int main() {}
                "
                ASSERT_FAIL_HAS_INT
            )
        endif()
        if (ASSERT_FAIL_HAS_INT OR EMSCRIPTEN)
            target_compile_definitions(test262-runner PRIVATE ASSERT_FAIL_HAS_INT)
        endif()

        add_executable(wasm ../../Userland/Utilities/wasm.cpp)
        target_link_libraries(wasm LibCore LibFileSystem LibWasm LibLine LibMain LibJS)

        add_executable(xml ../../Userland/Utilities/xml.cpp)
        target_link_libraries(xml LibCore LibFileSystem LibMain LibXML)

        add_executable(xzcat ../../Userland/Utilities/xzcat.cpp)
        target_link_libraries(xzcat LibCompress LibCore LibMain)

        enable_testing()
        # LibTest
        file(GLOB LIBTEST_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibTest/*.cpp")
        list(FILTER LIBTEST_SOURCES EXCLUDE REGEX ".*Main.cpp$")
        add_library(
            LibTest
            ${LIBTEST_SOURCES}
        )
        target_link_libraries(LibTest PRIVATE LibCore LibFileSystem)
        set_target_properties(LibTest PROPERTIES OUTPUT_NAME lagom-test)
        add_library(
            LibTestMain
            OBJECT
            "${SERENITY_PROJECT_ROOT}/Userland/Libraries/LibTest/TestMain.cpp"
        )

        # LibTest tests from Tests/
        set(TEST_DIRECTORIES
            AK
            LibCrypto
            LibCompress
            LibGL
            LibGfx
            LibLocale
            LibMarkdown
            LibPDF
            LibSQL
            LibTextCodec
            LibTTF
            LibTimeZone
            LibUnicode
            LibVideo
            LibXML
        )
        if (ENABLE_LAGOM_LIBWEB)
            list(APPEND TEST_DIRECTORIES LibWeb)
        endif()

        foreach (dir IN LISTS TEST_DIRECTORIES)
            add_serenity_subdirectory("Tests/${dir}")
        endforeach()

        # LibTLS needs a special working directory to find cacert.pem
        lagom_test(../../Tests/LibTLS/TestTLSHandshake.cpp LibTLS LIBS LibTLS LibCrypto)

        # The FLAC tests need a special working directory to find the test files
        lagom_test(../../Tests/LibAudio/TestFLACSpec.cpp LIBS LibAudio WORKING_DIRECTORY "${FLAC_TEST_PATH}/..")

        lagom_test(../../Tests/LibAudio/TestPlaybackStream.cpp LIBS LibAudio)
        if (HAVE_PULSEAUDIO)
            target_compile_definitions(TestPlaybackStream PRIVATE HAVE_PULSEAUDIO=1)
        endif()

        # LibCore
        if ((LINUX OR APPLE) AND NOT EMSCRIPTEN)
            lagom_test(../../Tests/LibCore/TestLibCoreFileWatcher.cpp)
            lagom_test(../../Tests/LibCore/TestLibCorePromise.cpp LIBS LibThreading)
        endif()

        # RegexLibC test POSIX <regex.h> and contains many Serenity extensions
        # It is therefore not reasonable to run it on Lagom, and we only run the Regex test
        lagom_test(../../Tests/LibRegex/Regex.cpp LIBS LibRegex WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/../../Tests/LibRegex)

        # JavaScriptTestRunner + LibTest tests
        # test-js
        add_executable(test-js
            ../../Tests/LibJS/test-js.cpp
            ../../Userland/Libraries/LibTest/JavaScriptTestRunnerMain.cpp)
        target_link_libraries(test-js LibCore LibFileSystem LibTest LibJS)
        add_test(
            NAME JS
            COMMAND test-js --show-progress=false
        )
        set_tests_properties(JS PROPERTIES ENVIRONMENT SERENITY_SOURCE_DIR=${SERENITY_PROJECT_ROOT})

        add_test(
            NAME JS-AST
            COMMAND test-js --ast --show-progress=false
        )
        set_tests_properties(JS-AST PROPERTIES ENVIRONMENT SERENITY_SOURCE_DIR=${SERENITY_PROJECT_ROOT})

        # Extra tests from Tests/LibJS
        lagom_test(../../Tests/LibJS/test-invalid-unicode-js.cpp LIBS LibJS)
        lagom_test(../../Tests/LibJS/test-value-js.cpp LIBS LibJS)

        # Spreadsheet
        add_executable(test-spreadsheet
                ../../Tests/Spreadsheet/test-spreadsheet.cpp
                ../../Userland/Libraries/LibTest/JavaScriptTestRunnerMain.cpp)
        target_link_libraries(test-spreadsheet LibCore LibFileSystem LibTest LibJS)
        add_test(
                NAME Spreadsheet
                COMMAND test-spreadsheet --show-progress=false
        )
        set_tests_properties(Spreadsheet PROPERTIES ENVIRONMENT SERENITY_SOURCE_DIR=${SERENITY_PROJECT_ROOT})

        # test-wasm
        add_executable(test-wasm
            ../../Tests/LibWasm/test-wasm.cpp
            ../../Userland/Libraries/LibTest/JavaScriptTestRunnerMain.cpp)
        target_link_libraries(test-wasm LibCore LibFileSystem LibTest LibWasm LibJS)
        add_test(
            NAME WasmParser
            COMMAND test-wasm --show-progress=false ${CMAKE_CURRENT_BINARY_DIR}/Userland/Libraries/LibWasm/Tests
        )
        set_tests_properties(WasmParser PROPERTIES
            SKIP_RETURN_CODE 1
            ENVIRONMENT SERENITY_SOURCE_DIR=${SERENITY_PROJECT_ROOT}
        )

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
        if (APPLE)
            if("arm64" IN_LIST CMAKE_OSX_ARCHITECTURES AND "x86_64" IN_LIST CMAKE_OSX_ARCHITECTURES)
                set(CPACK_SYSTEM_NAME "macOS-universal2")
            else()
                set(CPACK_SYSTEM_NAME "macOS-${CMAKE_SYSTEM_PROCESSOR}")
            endif()
        else()
            set(CPACK_SYSTEM_NAME "${CMAKE_SYSTEM_NAME}-${CMAKE_SYSTEM_PROCESSOR}")
        endif()

        set(CPACK_ARCHIVE_JS_FILE_NAME "serenity-js-${CPACK_SYSTEM_NAME}")
        set(CPACK_PACKAGE_FILE_NAME "serenity-js-${CPACK_SYSTEM_NAME}")
        include(CPack)
    endif()
endif()

if (ENABLE_FUZZERS)
    add_subdirectory(Fuzzers)
else()
    export_components("${CMAKE_BINARY_DIR}/components.ini")
endif()

if (NOT "$ENV{LAGOM_TARGET}" STREQUAL "")
    add_custom_target(run-lagom-target
        COMMAND "${CMAKE_COMMAND}"
            -E env "SERENITY_SOURCE_DIR=${SERENITY_PROJECT_ROOT}" "$<TARGET_FILE:$ENV{LAGOM_TARGET}>"
            $ENV{LAGOM_ARGS}

        WORKING_DIRECTORY "$<TARGET_PROPERTY:$ENV{LAGOM_TARGET},LAGOM_WORKING_DIRECTORY>"
        DEPENDS "$<TARGET_FILE:$ENV{LAGOM_TARGET}>"
        USES_TERMINAL
        VERBATIM
    )
endif()

include(ca_certificates_data)

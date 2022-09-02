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
include_directories(${CMAKE_BINARY_DIR}/Libraries)
include_directories(${CMAKE_BINARY_DIR}/Services)

# install rules, think about moving to its own helper cmake file
include(CMakePackageConfigHelpers)

# find_package(<package>) call for consumers to find this project
set(package Lagom CACHE STRING "")

# Allow package maintainers to freely override the path for the configs
set(Lagom_INSTALL_CMAKEDIR "${CMAKE_INSTALL_DATADIR}/${package}"
    CACHE PATH "CMake package config location relative to the install prefix")
mark_as_advanced(Lagom_INSTALL_CMAKEDIR)

configure_file("${SERENITY_PROJECT_ROOT}/Meta/CMake/lagom-install-config.cmake.in" "${package}Config.cmake" @ONLY)
install(
    FILES "${CMAKE_CURRENT_BINARY_DIR}/${package}Config.cmake"
    DESTINATION "${Lagom_INSTALL_CMAKEDIR}"
    COMPONENT Lagom_Development
)

install(
    EXPORT LagomTargets
    NAMESPACE Lagom::
    DESTINATION "${Lagom_INSTALL_CMAKEDIR}"
    COMPONENT Lagom_Development
)

function(lagom_lib library fs_name)
    cmake_parse_arguments(LAGOM_LIBRARY "" "" "SOURCES;LIBS" ${ARGN})
    set(target_name "Lib${library}")
    add_library(${target_name} ${LAGOM_LIBRARY_SOURCES})

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
    install(
        DIRECTORY "${SERENITY_PROJECT_ROOT}/Userland/Libraries/Lib${library}"
        COMPONENT Lagom_Development
        DESTINATION ${CMAKE_INSTALL_INCLUDEDIR}
        FILES_MATCHING PATTERN "*.h"
    )
endfunction()

function(lagom_test source)
    cmake_parse_arguments(LAGOM_TEST "" "WORKING_DIRECTORY" "LIBS" ${ARGN})
    get_filename_component(name ${source} NAME_WE)
    add_executable(${name}_lagom ${source})
    set_target_properties(${name}_lagom PROPERTIES OUTPUT_NAME ${name})
    target_link_libraries(${name}_lagom LibCore LibTest LibTestMain ${LAGOM_TEST_LIBS})
    add_test(
            NAME ${name}
            COMMAND ${name}_lagom
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
endfunction()

function(serenity_lib name fs_name)
    lagom_lib(name fs_name SOURCES ${SOURCES} ${GENERATED_SOURCES})
endfunction()

add_custom_target(components ALL)
option(BUILD_EVERYTHING "Build all optional components" ON)

if (NOT TARGET all_generated)
    # Meta target to run all code-gen steps in the build.
    add_custom_target(all_generated)
endif()

# AK/LibCore
# Note: AK is included in LibCore for the host build instead of LibC per the target build
file(GLOB AK_SOURCES CONFIGURE_DEPENDS "../../AK/*.cpp")
file(GLOB LIBCORE_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibCore/*.cpp")
if (ANDROID)
    list(REMOVE_ITEM LIBCORE_SOURCES "${CMAKE_CURRENT_LIST_DIR}/../../Userland/Libraries/LibCore/Account.cpp")
endif()
lagom_lib(Core core
    SOURCES ${AK_SOURCES} ${LIBCORE_SOURCES}
    LIBS Threads::Threads
)
if (NOT APPLE AND NOT ANDROID AND NOT ${CMAKE_SYSTEM_NAME} MATCHES "OpenBSD")
    target_link_libraries(LibCore crypt) # LibCore::Account uses crypt() but it's not in libcrypt on macOS
endif()

# LibMain
file(GLOB LIBMAIN_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibMain/*.cpp")
lagom_lib(Main main
    SOURCES ${LIBMAIN_SOURCES}
)
# The macOS linker is not happy about LibMain's main() calling an undefined symbol (serenity_main()).
if (APPLE)
    target_link_options(LibMain PRIVATE -undefined dynamic_lookup)
endif()

# LibTimeZone
# This is needed even if Lagom is not enabled because it is depended upon by code generators.
include(time_zone_data)
file(GLOB LIBTIMEZONE_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibTimeZone/*.cpp")
lagom_lib(TimeZone timezone
    SOURCES ${LIBTIMEZONE_SOURCES} ${TIME_ZONE_DATA_SOURCES}
)
target_compile_definitions(LibTimeZone PRIVATE ENABLE_TIME_ZONE_DATA=$<BOOL:${ENABLE_TIME_ZONE_DATABASE_DOWNLOAD}>)

# Manually install AK headers
install(
    DIRECTORY "${SERENITY_PROJECT_ROOT}/AK"
    COMPONENT Lagom_Development
    DESTINATION ${CMAKE_INSTALL_INCLUDEDIR}
    FILES_MATCHING PATTERN "*.h"
)

# Code Generators and other host tools
# We need to make sure not to build code generators for Fuzzer builds, as they already have their own main.cpp
# Instead, we import them from a previous install of Lagom. This mandates a two-stage build for fuzzers.
if (ENABLE_FUZZERS OR CMAKE_CROSSCOMPILING)
    find_package(LagomTools REQUIRED)
else()
    add_subdirectory(Tools)
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
    list(REMOVE_ITEM LIBAUDIO_SOURCES "${CMAKE_CURRENT_SOURCE_DIR}/../../Userland/Libraries/LibAudio/ConnectionToServer.cpp")
    lagom_lib(Audio audio
        SOURCES ${LIBAUDIO_SOURCES}
    )

    # Compress
    file(GLOB LIBCOMPRESS_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibCompress/*.cpp")
    lagom_lib(Compress compress
        SOURCES ${LIBCOMPRESS_SOURCES}
        LIBS LibCrypto
    )

    # Crypto
    file(GLOB LIBCRYPTO_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibCrypto/*.cpp")
    file(GLOB LIBCRYPTO_SUBDIR_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibCrypto/*/*.cpp")
    file(GLOB LIBCRYPTO_SUBSUBDIR_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibCrypto/*/*/*.cpp")
    lagom_lib(Crypto crypto
        SOURCES ${LIBCRYPTO_SOURCES} ${LIBCRYPTO_SUBDIR_SOURCES} ${LIBCRYPTO_SUBSUBDIR_SOURCES}
    )

    file(GLOB LIBDNS_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibDNS/*.cpp")
    lagom_lib(DNS dns
        SOURCES ${LIBDNS_SOURCES}
        LIBS LibIPC
    )

    # ELF
    # FIXME: Excluding arm64 is a temporary hack to circumvent a build problem
    #        for Lagom on Apple M1
    if (NOT CMAKE_SYSTEM_PROCESSOR MATCHES "arm64")
        file(GLOB LIBELF_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibELF/*.cpp")
        # There's no way we can reliably make the dynamic loading classes cross platform
        list(FILTER LIBELF_SOURCES EXCLUDE REGEX ".*Dynamic.*.cpp$")
        lagom_lib(ELF elf
            SOURCES ${LIBELF_SOURCES}
        )
    endif()

    # Gemini
    file(GLOB LIBGEMINI_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibGemini/*.cpp")
    lagom_lib(Gemini gemini
        SOURCES ${LIBGEMINI_SOURCES}
        LIBS LibTLS
    )

    # Gfx
    file(GLOB LIBGFX_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibGfx/*.cpp")
    file(GLOB LIBGFX_FILTER_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibGfx/Filters/*.cpp")
    file(GLOB LIBGFX_FONT_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibGfx/Font/*.cpp")
    file(GLOB LIBGFX_TTF_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibGfx/Font/TrueType/*.cpp")
    file(GLOB LIBGFX_WOFF_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibGfx/Font/WOFF/*.cpp")
    lagom_lib(Gfx gfx
            SOURCES ${LIBGFX_SOURCES} ${LIBGFX_FILTER_SOURCES} ${LIBGFX_FONT_SOURCES} ${LIBGFX_TTF_SOURCES} ${LIBGFX_WOFF_SOURCES}
            LIBS m LibCompress LibTextCodec LibIPC
            )

    # GPU
    file(GLOB LIBGPU_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibGPU/*.cpp")
    lagom_lib(GPU gpu
        SOURCES ${LIBGPU_SOURCES}
        LIBS dl
    )

    # GL
    file(GLOB LIBGL_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibGL/*.cpp")
    file(GLOB LIBGL_TEX_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibGL/Tex/*.cpp")
    lagom_lib(GL gl
        SOURCES ${LIBGL_SOURCES} ${LIBGL_TEX_SOURCES}
        LIBS m LibGfx LibGPU)

    # GUI-GML
    file(GLOB LIBGUI_GML_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibGUI/GML/*.cpp")
    list(REMOVE_ITEM LIBGUI_GML_SOURCES "${CMAKE_CURRENT_SOURCE_DIR}/../../Userland/Libraries/LibGUI/GML/AutocompleteProvider.cpp")
    lagom_lib(GML gml
        SOURCES ${LIBGUI_GML_SOURCES}
        LIBS LibSyntax
    )

    # HTTP
    file(GLOB LIBHTTP_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibHTTP/*.cpp")
    lagom_lib(HTTP http
        SOURCES ${LIBHTTP_SOURCES}
        LIBS LibCompress LibTLS
    )

    # IMAP
    file(GLOB LIBIMAP_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibIMAP/*.cpp")
    lagom_lib(IMAP imap
        SOURCES ${LIBIMAP_SOURCES}
        LIBS LibTLS
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
        LIBS m LibCrypto LibRegex LibUnicode LibTextCodec
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
        LIBS LibJS
    )

    # PDF
    file(GLOB LIBPDF_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibPDF/*.cpp")
    file(GLOB LIBPDF_SUBDIR_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibPDF/*/*.cpp")
    lagom_lib(PDF pdf
       SOURCES ${LIBPDF_SOURCES} ${LIBPDF_SUBDIR_SOURCES}
       LIBS LibGfx LibIPC LibTextCodec
    )

    # Regex
    file(GLOB LIBREGEX_LIBC_SOURCES "../../Userland/Libraries/LibRegex/C/Regex.cpp")
    file(GLOB LIBREGEX_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibRegex/*.cpp")
    lagom_lib(Regex regex
        SOURCES ${LIBREGEX_SOURCES} ${LIBREGEX_LIBC_SOURCES}
        LIBS LibUnicode
    )

    # Shell
    file(GLOB SHELL_SOURCES CONFIGURE_DEPENDS "../../Userland/Shell/*.cpp")
    list(REMOVE_ITEM SHELL_SOURCES "${CMAKE_CURRENT_SOURCE_DIR}/../../Userland/Shell/SyntaxHighlighter.cpp")
    list(FILTER SHELL_SOURCES EXCLUDE REGEX ".*main.cpp$")
    lagom_lib(Shell shell
        SOURCES ${SHELL_SOURCES}
        LIBS LibLine LibRegex
    )

    # SoftGPU
    file(GLOB_RECURSE LIBSOFTGPU_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibSoftGPU/*.cpp")
    lagom_lib(SoftGPU softgpu
        SOURCES ${LIBSOFTGPU_SOURCES}
        LIBS m LibGfx
    )

    # Syntax
    file(GLOB LIBSYNTAX_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibSyntax/*.cpp")
    lagom_lib(Syntax syntax
        SOURCES ${LIBSYNTAX_SOURCES}
    )

    # SQL
    file(GLOB_RECURSE LIBSQL_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibSQL/*.cpp")
    list(REMOVE_ITEM LIBSQL_SOURCES "${CMAKE_CURRENT_SOURCE_DIR}/../../Userland/Libraries/LibSQL/AST/SyntaxHighlighter.cpp")
    list(REMOVE_ITEM LIBSQL_SOURCES "${CMAKE_CURRENT_SOURCE_DIR}/../../Userland/Libraries/LibSQL/SQLClient.cpp")
    lagom_lib(SQL sql
        SOURCES ${LIBSQL_SOURCES}
        LIBS LibRegex
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
        LIBS LibCrypto
    )

    # Unicode
    include(unicode_data)
    include(locale_data)
    file(GLOB LIBUNICODE_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibUnicode/*.cpp")
    file(GLOB LIBLOCALE_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibLocale/*.cpp")
    lagom_lib(Unicode unicode
        SOURCES ${LIBUNICODE_SOURCES} ${UNICODE_DATA_SOURCES} ${LIBLOCALE_SOURCES} ${LOCALE_DATA_SOURCES}
    )
    target_compile_definitions(LibUnicode PRIVATE ENABLE_UNICODE_DATA=$<BOOL:${ENABLE_UNICODE_DATABASE_DOWNLOAD}>)
    target_compile_options(LibUnicode PRIVATE -Wno-parentheses-equality)
    target_link_libraries(LibUnicode LibTimeZone)

    # WASM
    file(GLOB LIBWASM_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibWasm/*/*.cpp")
    lagom_lib(Wasm wasm
        SOURCES ${LIBWASM_SOURCES}
    )

    # Web
    include(libweb_generators)
    file(GLOB LIBWEB_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibWeb/*.cpp")
    file(GLOB LIBWEB_SUBDIR_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibWeb/*/*.cpp")
    file(GLOB LIBWEB_SUBSUBDIR_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibWeb/*/*/*.cpp")
    file(GLOB LIBWEB_SUBSUBSUBDIR_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibWeb/*/*/*/*.cpp")
    list(REMOVE_ITEM LIBWEB_SUBSUBDIR_SOURCES "${CMAKE_CURRENT_SOURCE_DIR}/../../Userland/Libraries/LibWeb/CSS/SyntaxHighlighter/SyntaxHighlighter.cpp")
    list(REMOVE_ITEM LIBWEB_SUBSUBDIR_SOURCES "${CMAKE_CURRENT_SOURCE_DIR}/../../Userland/Libraries/LibWeb/HTML/SyntaxHighlighter/SyntaxHighlighter.cpp")

    generate_css_implementation()

    set(LIBWEB_GENERATED_SOURCES
        LibWeb/CSS/DefaultStyleSheetSource.cpp
        LibWeb/CSS/Enums.cpp
        LibWeb/CSS/MediaFeatureID.cpp
        LibWeb/CSS/PropertyID.cpp
        LibWeb/CSS/QuirksModeStyleSheetSource.cpp
        LibWeb/CSS/TransformFunctions.cpp
        LibWeb/CSS/ValueID.cpp
    )

    lagom_lib(Web web
        SOURCES ${LIBWEB_SOURCES} ${LIBWEB_SUBDIR_SOURCES} ${LIBWEB_SUBSUBDIR_SOURCES} ${LIBWEB_SUBSUBSUBDIR_SOURCES} ${LIBWEB_GENERATED_SOURCES}
        LIBS LibMarkdown LibGemini LibGfx LibGL LibJS LibTextCodec LibWasm LibXML
    )
    generate_js_wrappers(LibWeb)

    # WebSocket
    file(GLOB LIBWEBSOCKET_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibWebSocket/*.cpp")
    file(GLOB LIBWEBSOCKET_SUBDIR_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibWebSocket/*/*.cpp")
    lagom_lib(WebSocket websocket
        SOURCES ${LIBWEBSOCKET_SOURCES} ${LIBWEBSOCKET_SUBDIR_SOURCES}
        LIBS LibCrypto LibTLS
    )

    # x86
    # FIXME: Excluding arm64 is a temporary hack to circumvent a build problem
    #        for Lagom on Apple M1
    if (NOT CMAKE_SYSTEM_PROCESSOR MATCHES "arm64")
        file(GLOB LIBX86_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibX86/*.cpp")
        lagom_lib(X86 x86
            SOURCES ${LIBX86_SOURCES}
        )
    endif()

    file(GLOB LIBXML_SOURCES CONFIGURE_DEPENDS "../../Userland/Libraries/LibXML/*/*.cpp")
    lagom_lib(XML xml
        SOURCES ${LIBXML_SOURCES})

    if (NOT ENABLE_FUZZERS AND NOT ENABLE_COMPILER_EXPLORER_BUILD AND NOT ANDROID)
        # Lagom Services
        add_subdirectory("${SERENITY_PROJECT_ROOT}/Userland/Services" "${CMAKE_CURRENT_BINARY_DIR}/Services")

        # Lagom Examples
        add_executable(TestApp TestApp.cpp)
        target_link_libraries(TestApp LibCore)

        add_executable(TestJson TestJson.cpp)
        target_link_libraries(TestJson LibCore)

        # Lagom Utilities
        add_executable(adjtime_lagom ../../Userland/Utilities/adjtime.cpp)
        set_target_properties(adjtime_lagom PROPERTIES OUTPUT_NAME adjtime)
        target_link_libraries(adjtime_lagom LibCore LibMain)

        # FIXME: Excluding arm64 is a temporary hack to circumvent a build problem
        #        for Lagom on Apple M1
        if (NOT CMAKE_SYSTEM_PROCESSOR MATCHES "arm64")
            add_executable(disasm_lagom ../../Userland/Utilities/disasm.cpp)
            set_target_properties(disasm_lagom PROPERTIES OUTPUT_NAME disasm)
            target_link_libraries(disasm_lagom LibCore LibELF LibX86 LibMain)
        endif()

        add_executable(gml-format_lagom ../../Userland/Utilities/gml-format.cpp)
        set_target_properties(gml-format_lagom PROPERTIES OUTPUT_NAME gml-format)
        target_link_libraries(gml-format_lagom LibCore LibGML LibMain)

        add_executable(headless_browser_lagom ../../Userland/Utilities/headless-browser.cpp)
        set_target_properties(headless_browser_lagom PROPERTIES OUTPUT_NAME headless-browser)
        target_link_libraries(headless_browser_lagom LibWeb LibWebSocket LibHTTP LibJS LibGfx LibMain)

        add_executable(js_lagom ../../Userland/Utilities/js.cpp)
        set_target_properties(js_lagom PROPERTIES OUTPUT_NAME js)
        target_link_libraries(js_lagom LibJS LibLine LibMain Threads::Threads)

        add_executable(markdown-check_lagom ../../Userland/Utilities/markdown-check.cpp)
        set_target_properties(markdown-check_lagom PROPERTIES OUTPUT_NAME markdown-check)
        target_link_libraries(markdown-check_lagom LibMarkdown LibMain)

        add_executable(ntpquery_lagom ../../Userland/Utilities/ntpquery.cpp)
        set_target_properties(ntpquery_lagom PROPERTIES OUTPUT_NAME ntpquery)
        target_link_libraries(ntpquery_lagom LibCore)

        add_executable(shell_lagom ../../Userland/Shell/main.cpp)
        set_target_properties(shell_lagom PROPERTIES OUTPUT_NAME shell)
        target_link_libraries(shell_lagom LibCore LibShell LibMain)

        add_executable(wasm_lagom ../../Userland/Utilities/wasm.cpp)
        set_target_properties(wasm_lagom PROPERTIES OUTPUT_NAME wasm)
        target_link_libraries(wasm_lagom LibCore LibWasm LibLine LibMain)

        add_executable(xml_lagom ../../Userland/Utilities/xml.cpp)
        set_target_properties(xml_lagom PROPERTIES OUTPUT_NAME xml)
        target_link_libraries(xml_lagom LibCore LibXML LibMain)

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
            lagom_test(${source} WORKING_DIRECTORY LIBS LibGL LibGPU LibSoftGPU)
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
            target_compile_definitions("${target}_lagom" PRIVATE ENABLE_TIME_ZONE_DATA=$<BOOL:${ENABLE_TIME_ZONE_DATABASE_DOWNLOAD}>)
        endforeach()

        # Unicode
        file(GLOB LIBUNICODE_TEST_SOURCES CONFIGURE_DEPENDS "../../Tests/LibUnicode/*.cpp")
        foreach(source ${LIBUNICODE_TEST_SOURCES})
            lagom_test(${source} LIBS LibUnicode)
        endforeach()

        # JavaScriptTestRunner + LibTest tests
        # test-js
        add_executable(test-js_lagom
            ../../Tests/LibJS/test-js.cpp
            ../../Userland/Libraries/LibTest/JavaScriptTestRunnerMain.cpp)
        set_target_properties(test-js_lagom PROPERTIES OUTPUT_NAME test-js)
        target_link_libraries(test-js_lagom LibCore LibTest LibJS)
        add_test(
            NAME JS
            COMMAND test-js_lagom --show-progress=false
        )
        set_tests_properties(JS PROPERTIES ENVIRONMENT SERENITY_SOURCE_DIR=${SERENITY_PROJECT_ROOT})

        # Extra tests from Tests/LibJS
        lagom_test(../../Tests/LibJS/test-invalid-unicode-js.cpp LIBS LibJS)
        lagom_test(../../Tests/LibJS/test-bytecode-js.cpp LIBS LibJS)

        # Spreadsheet
        add_executable(test-spreadsheet_lagom
                ../../Tests/Spreadsheet/test-spreadsheet.cpp
                ../../Userland/Libraries/LibTest/JavaScriptTestRunnerMain.cpp)
        set_target_properties(test-spreadsheet_lagom PROPERTIES OUTPUT_NAME test-spreadsheet)
        target_link_libraries(test-spreadsheet_lagom LibCore LibTest LibJS)
        add_test(
                NAME Spreadsheet
                COMMAND test-spreadsheet_lagom --show-progress=false
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
        add_executable(test-wasm_lagom
            ../../Tests/LibWasm/test-wasm.cpp
            ../../Userland/Libraries/LibTest/JavaScriptTestRunnerMain.cpp)
        set_target_properties(test-wasm_lagom PROPERTIES OUTPUT_NAME test-wasm)
        target_link_libraries(test-wasm_lagom LibCore LibTest LibWasm LibJS)
        add_test(
            NAME WasmParser
            COMMAND test-wasm_lagom --show-progress=false
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
                COMMAND shell_lagom --skip-shellrc "${TEST_PATH}"
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
        get_linked_lagom_libraries(js_lagom js_lagom_libraries)

        install(TARGETS js_lagom ${js_lagom_libraries} COMPONENT js)

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

cmake_minimum_required(VERSION 3.16)
project(ladybird-gtk4
  VERSION 1.0.0
  LANGUAGES C CXX
)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)
set(CMAKE_EXPORT_COMPILE_COMMANDS ON)

find_package(PkgConfig)
pkg_check_modules(gtk4 REQUIRED IMPORTED_TARGET gtk4>=4.11.3)
pkg_check_modules(libadwaita REQUIRED IMPORTED_TARGET libadwaita-1>=1.4)
pkg_check_modules(libsoup IMPORTED_TARGET libsoup-3.0)

find_program(GLIB_COMPILE_RESOURCES NAMES glib-compile-resources REQUIRED)

find_program(BLUEPRINT_COMPILER NAMES blueprint-compiler REQUIRED)

set(LAGOM_SOURCE_DIR "${SERENITY_SOURCE_DIR}/Meta/Lagom")
set(LAGOM_BINARY_DIR "${CMAKE_CURRENT_BINARY_DIR}/Lagom")

include("${SERENITY_SOURCE_DIR}/Meta/CMake/lagom_compile_options.cmake")

set(BUILD_LAGOM ON CACHE INTERNAL "")
set(ENABLE_LAGOM_LIBWEB ON CACHE INTERNAL "")
# We set EXCLUDE_FROM_ALL to make sure that only required Lagom libraries are built
add_subdirectory("${LAGOM_SOURCE_DIR}" "${LAGOM_BINARY_DIR}" EXCLUDE_FROM_ALL)
# FIXME: Why doesn't Lagom do this?
include_directories(
    "${SERENITY_SOURCE_DIR}"
    "${SERENITY_SOURCE_DIR}/Userland/Libraries"
    "${SERENITY_SOURCE_DIR}/Userland/Applications"
    "${SERENITY_SOURCE_DIR}/Userland/Services"
    "${LAGOM_BINARY_DIR}"
    "${LAGOM_BINARY_DIR}/Userland/Libraries"
    "${LAGOM_BINARY_DIR}/Userland/Services"
)
add_compile_definitions(AK_DONT_REPLACE_STD)

set(RESOURCE_FILES)
function(add_blueprint source_file ui_file)
    add_custom_command(
        OUTPUT "${ui_file}"
        COMMAND "${BLUEPRINT_COMPILER}"
        ARGS
            compile
            --output "${CMAKE_CURRENT_BINARY_DIR}/${ui_file}"
            "${CMAKE_CURRENT_SOURCE_DIR}/${source_file}"
        DEPENDS "${CMAKE_CURRENT_SOURCE_DIR}/${source_file}"
    )
    list(APPEND RESOURCE_FILES "${ui_file}")
    set(RESOURCE_FILES ${RESOURCE_FILES} PARENT_SCOPE)
endfunction()

add_blueprint(window.blp window.ui)
add_blueprint(shortcuts-dialog.blp shortcuts-dialog.ui)
add_blueprint(tab.blp tab.ui)

set(SOURCE_GRESOURCE_FILE "${CMAKE_CURRENT_SOURCE_DIR}/gresources.xml")
set(GENERATED_GRESOURCE_FILE "${CMAKE_CURRENT_BINARY_DIR}/ladybird-resources.c")

add_custom_command(
    OUTPUT "${GENERATED_GRESOURCE_FILE}"
    COMMAND "${GLIB_COMPILE_RESOURCES}"
    ARGS
        --target "${GENERATED_GRESOURCE_FILE}"
        --generate-source
        --sourcedir "${CMAKE_CURRENT_SOURCE_DIR}"
        --sourcedir "${CMAKE_CURRENT_BINARY_DIR}"
        "${SOURCE_GRESOURCE_FILE}"
    DEPENDS "${SOURCE_GRESOURCE_FILE}" ${RESOURCE_FILES}
)
add_library(resources OBJECT "${GENERATED_GRESOURCE_FILE}")
target_link_libraries(resources PRIVATE PkgConfig::gtk4)
set_target_properties(resources PROPERTIES COMPILE_OPTIONS "")

if (libsoup_FOUND)
    add_compile_definitions(HAVE_LIBSOUP)
endif ()

set(SOURCES
    main.cpp
    Application.cpp
    Window.cpp
    WebView.cpp
    Tab.cpp
    BitmapPaintable.cpp
    ViewImpl.cpp
    EventLoopImplementationGLib.cpp
    "${SERENITY_SOURCE_DIR}/Ladybird/Utilities.cpp"
    "${SERENITY_SOURCE_DIR}/Ladybird/HelperProcess.cpp"
    "${SERENITY_SOURCE_DIR}/Userland/Applications/Browser/CookieJar.cpp"
    "${SERENITY_SOURCE_DIR}/Userland/Applications/Browser/Database.cpp"
    $<TARGET_OBJECTS:resources>
)

add_executable(ladybird ${SOURCES})
target_link_libraries(ladybird PRIVATE PkgConfig::gtk4 PkgConfig::libadwaita Lagom::Core Lagom::WebView Lagom::Gfx Lagom::Web Lagom::IPC Lagom::FileSystem Lagom::SQL Lagom::Protocol)

set(WEBCONTENT_SOURCE_DIR "${SERENITY_SOURCE_DIR}/Userland/Services/WebContent")
set(WEBCONTENT_SOURCES
    "${WEBCONTENT_SOURCE_DIR}/ConnectionFromClient.cpp"
    "${WEBCONTENT_SOURCE_DIR}/ConsoleGlobalEnvironmentExtensions.cpp"
    "${WEBCONTENT_SOURCE_DIR}/PageHost.cpp"
    "${WEBCONTENT_SOURCE_DIR}/WebContentConsoleClient.cpp"
    "${WEBCONTENT_SOURCE_DIR}/WebDriverConnection.cpp"
    "${SERENITY_SOURCE_DIR}/Ladybird/FontPlugin.cpp"
    "${SERENITY_SOURCE_DIR}/Ladybird/HelperProcess.cpp"
    "${SERENITY_SOURCE_DIR}/Ladybird/ImageCodecPlugin.cpp"
    "${SERENITY_SOURCE_DIR}/Ladybird/Utilities.cpp"
    WebContentMain.cpp
)
if (libsoup_FOUND)
    set(WEBCONTENT_SOURCES
        RequestManagerSoup.cpp
        EventLoopImplementationGLib.cpp
        ${WEBCONTENT_SOURCES}
    )
endif()

add_executable(WebContent ${WEBCONTENT_SOURCES})
target_link_libraries(WebContent PRIVATE Lagom::Audio Lagom::Core Lagom::FileSystem Lagom::Gfx Lagom::IPC Lagom::JS Lagom::Main Lagom::Web Lagom::WebSocket Lagom::WebView Lagom::Protocol)
if (libsoup_FOUND)
    target_link_libraries(WebContent PRIVATE PkgConfig::libsoup)
endif()

if (NOT CMAKE_SKIP_INSTALL_RULES)
  include(cmake/InstallRules.cmake)
endif()

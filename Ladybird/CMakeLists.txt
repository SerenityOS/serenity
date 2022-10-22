cmake_minimum_required(VERSION 3.16...3.22)

project(ladybird
    VERSION 0.0.1
    LANGUAGES CXX
    DESCRIPTION "Ladybird Web Browser"
)

include(GNUInstallDirs)

if (ANDROID)
    set(BUILD_SHARED_LIBS OFF)
endif()

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

set(CMAKE_SKIP_BUILD_RPATH FALSE)
set(CMAKE_BUILD_WITH_INSTALL_RPATH FALSE)
# See slide 100 of the following ppt :^)
# https://crascit.com/wp-content/uploads/2019/09/Deep-CMake-For-Library-Authors-Craig-Scott-CppCon-2019.pdf
if (NOT APPLE)
    set(CMAKE_INSTALL_RPATH $ORIGIN;$ORIGIN/../${CMAKE_INSTALL_LIBDIR})
endif()
set(CMAKE_INSTALL_RPATH_USE_LINK_PATH TRUE)

set(CMAKE_EXPORT_COMPILE_COMMANDS ON)

include(cmake/EnableLLD.cmake)

if (ENABLE_ADDRESS_SANITIZER)
    add_compile_options(-fsanitize=address -fno-omit-frame-pointer)
    add_link_options(-fsanitize=address)
endif()

if (ENABLE_MEMORY_SANITIZER)
    add_compile_options(-fsanitize=memory -fsanitize-memory-track-origins -fno-omit-frame-pointer)
    add_link_options(-fsanitize=memory -fsanitize-memory-track-origins)
endif()

if (ENABLE_UNDEFINED_SANITIZER)
    add_compile_options(-fsanitize=undefined -fno-omit-frame-pointer)
    add_link_options(-fsanitize=undefined)
endif()

# Lagom
include(FetchContent)
include(cmake/FetchLagom.cmake)

get_filename_component(
    SERENITY_SOURCE_DIR "${Lagom_SOURCE_DIR}/../.."
    ABSOLUTE
)

# Lagom warnings
include(${Lagom_SOURCE_DIR}/../CMake/lagom_compile_options.cmake)
add_compile_options(-Wno-expansion-to-defined)
add_compile_options(-Wno-user-defined-literals)

set(CMAKE_AUTOMOC ON)
set(CMAKE_AUTORCC ON)
set(CMAKE_AUTOUIC ON)
find_package(Qt6 REQUIRED COMPONENTS Core Widgets Network)

set(BROWSER_SOURCE_DIR ${SERENITY_SOURCE_DIR}/Userland/Applications/Browser/)

set(SOURCES
    ${BROWSER_SOURCE_DIR}/CookieJar.cpp
    BrowserWindow.cpp
    ConsoleWidget.cpp
    History.cpp
    ModelTranslator.cpp
    Settings.cpp
    SettingsDialog.cpp
    Tab.cpp
    Utilities.cpp
    WebContentView.cpp
    main.cpp
)

qt_add_executable(ladybird ${SOURCES}
    MANUAL_FINALIZATION
)
#target_link_libraries(ladybird PRIVATE Qt::Widgets Qt::Network LibWeb LibGUI LibWeb LibWebView LibGL LibSoftGPU LibMain)
target_link_libraries(ladybird PRIVATE Qt::Widgets Qt::Network LibWeb LibWebSocket LibGUI LibWebView LibGL LibSoftGPU LibMain)

target_include_directories(ladybird PRIVATE ${CMAKE_CURRENT_BINARY_DIR})
target_include_directories(ladybird PRIVATE ${SERENITY_SOURCE_DIR}/Userland/Applications/)
target_include_directories(ladybird PRIVATE ${SERENITY_SOURCE_DIR}/Userland/Services/)

set_target_properties(ladybird PROPERTIES
    MACOSX_BUNDLE_GUI_IDENTIFIER org.serenityos.ladybird
    MACOSX_BUNDLE_BUNDLE_VERSION ${PROJECT_VERSION}
    MACOSX_BUNDLE_SHORT_VERSION_STRING ${PROJECT_VERSION_MAJOR}.${PROJECT_VERSION_MINOR}
    MACOSX_BUNDLE_INFO_PLIST "${CMAKE_CURRENT_SOURCE_DIR}/Info.plist"
    MACOSX_BUNDLE TRUE
    WIN32_EXECUTABLE TRUE
)

if (ANDROID)
    include(cmake/AndroidExtras.cmake)
endif()

add_custom_target(run
    COMMAND "${CMAKE_COMMAND}" -E env "SERENITY_SOURCE_DIR=${SERENITY_SOURCE_DIR}" "$<TARGET_FILE:ladybird>"
    USES_TERMINAL
)

add_custom_target(debug
    COMMAND "${CMAKE_COMMAND}" -E env "SERENITY_SOURCE_DIR=${SERENITY_SOURCE_DIR}" gdb -ex "set follow-fork-mode child" "$<TARGET_FILE:ladybird>"
    USES_TERMINAL
)

qt_finalize_executable(ladybird)

add_subdirectory(WebContent)
add_dependencies(ladybird WebContent)

if(NOT CMAKE_SKIP_INSTALL_RULES)
    include(cmake/InstallRules.cmake)
endif()

cmake_minimum_required(VERSION 3.16...3.22)

project(ladybird
    VERSION 0.0.1
    LANGUAGES CXX
    DESCRIPTION "Ladybird Web Browser"
)

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

include(cmake/EnableLLD.cmake)

# Lagom
include(FetchContent)
include(cmake/FetchLagom.cmake)

# Lagom warnings
include(${Lagom_SOURCE_DIR}/../CMake/lagom_compile_options.cmake)
add_compile_options(-Wno-expansion-to-defined)

set(CMAKE_AUTOMOC ON)
find_package(Qt6 REQUIRED COMPONENTS Core Widgets Network)

set(SOURCES
    BrowserWindow.cpp
    RequestManagerQt.cpp
    main.cpp
    WebView.cpp
    History.cpp
    Tab.cpp
)

add_executable(ladybird ${SOURCES})
target_link_libraries(ladybird PRIVATE Qt6::Widgets Qt::Network Lagom::Web Lagom::WebSocket Lagom::Main)

get_filename_component(
    SERENITY_SOURCE_DIR "${Lagom_SOURCE_DIR}/../.."
    ABSOLUTE
)

add_custom_target(run
    COMMAND "${CMAKE_COMMAND}" -E env "SERENITY_SOURCE_DIR=${SERENITY_SOURCE_DIR}" "$<TARGET_FILE:ladybird>"
    USES_TERMINAL
)

add_custom_target(debug
    COMMAND "${CMAKE_COMMAND}" -E env "SERENITY_SOURCE_DIR=${SERENITY_SOURCE_DIR}" gdb "$<TARGET_FILE:ladybird>"
    USES_TERMINAL
)


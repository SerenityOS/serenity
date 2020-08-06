cmake_minimum_required(VERSION 3.0)
project(serenity)
set(CMAKE_CXX_STANDARD 20)

file(GLOB_RECURSE AK_SOURCES "serenity/AK/*.cpp")
file(GLOB_RECURSE APPLICATIONS_SOURCES "serenity/Applications/*.cpp")
file(GLOB_RECURSE BASE_SOURCES "serenity/Base/*.cpp")
file(GLOB_RECURSE DEMOS_SOURCES "serenity/Demos/*.cpp")
file(GLOB_RECURSE DEVTOOLS_SOURCES "serenity/DevTools/*.cpp")
file(GLOB_RECURSE GAMES_SOURCES "serenity/Games/*.cpp")
file(GLOB_RECURSE KERNEL_SOURCES "serenity/Kernel/*.cpp")
file(GLOB_RECURSE LIBRARIES_SOURCES "serenity/Libraries/*.cpp")
file(GLOB_RECURSE MENU_APPLETS_SOURCES "serenity/MenuApplets/*.cpp")
file(GLOB_RECURSE PORTS_SOURCES "serenity/Ports/*.cpp")
file(GLOB_RECURSE SERVERS_SOURCES "serenity/Services/*.cpp")
file(GLOB_RECURSE SHELL_SOURCES "serenity/Shell/*.cpp")
file(GLOB_RECURSE TESTS_SOURCES "serenity/Tests/*.cpp")
file(GLOB_RECURSE TOOLCHAIN_SOURCES "serenity/Toolchain/*.cpp")
file(GLOB_RECURSE USERLAND_SOURCES "serenity/Userland/*.cpp")

set(INCLUDE_DIRS
    "serenity"
    "serenity/Kernel"
    "serenity/Libraries"
    "serenity/Libraries/LibC"
    "serenity/Libraries/LibPthread"
    "serenity/Libraries/LibM"
    "serenity/Services"
    "serenity/Toolchain/Local/i686-pc-serenity/include/c++/10.1.0"
    "serenity/Build/Services"
    "serenity/Build/Libraries")

add_library(serenity
    ${AK_SOURCES}
    ${APPLICATIONS_SOURCES}
    ${BASE_SOURCES}
    ${DEMOS_SOURCES}
    ${DEVTOOLS_SOURCES}
    ${GAMES_SOURCES}
    ${KERNEL_SOURCES}
    ${LIBRARIES_SOURCES}
    ${MENU_APPLETS_SOURCES}
    ${PORTS_SOURCES}
    ${SERVERS_SOURCES}
    ${SHELL_SOURCES}
    ${TESTS_SOURCES}
    ${TOOLCHAIN_SOURCES}
    ${USERLAND_SOURCES})

target_compile_definitions(serenity PRIVATE __serenity__ USERLAND SANITIZE_PTRS DEBUG)
target_include_directories(serenity PRIVATE ${INCLUDE_DIRS})

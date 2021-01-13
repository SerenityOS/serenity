cmake_minimum_required(VERSION 3.0)
project(serenity)
set(CMAKE_CXX_STANDARD 20)

set(SERENITY_ARCH "i686" CACHE STRING "Target architecture for SerenityOS.")

file(GLOB_RECURSE AK_SOURCES "serenity/AK/*.cpp")
file(GLOB_RECURSE APPLICATIONS_SOURCES "serenity/Userland/Applications/*.cpp")
file(GLOB_RECURSE BASE_SOURCES "serenity/Base/*.cpp")
file(GLOB_RECURSE DEMOS_SOURCES "serenity/Userland/Demos/*.cpp")
file(GLOB_RECURSE DEVTOOLS_SOURCES "serenity/Userland/DevTools/*.cpp")
file(GLOB_RECURSE GAMES_SOURCES "serenity/Userland/Games/*.cpp")
file(GLOB_RECURSE KERNEL_SOURCES "serenity/Kernel/*.cpp")
file(GLOB_RECURSE LIBRARIES_SOURCES "serenity/Userland/Libraries/*.cpp")
file(GLOB_RECURSE MENU_APPLETS_SOURCES "serenity/Userland/MenuApplets/*.cpp")
file(GLOB_RECURSE PORTS_SOURCES "serenity/Ports/*.cpp")
file(GLOB_RECURSE SERVICES_SOURCES "serenity/Userland/Services/*.cpp")
file(GLOB_RECURSE SHELL_SOURCES "serenity/Userland/Shell/*.cpp")
file(GLOB_RECURSE TESTS_SOURCES "serenity/Userland/Tests/*.cpp")
file(GLOB_RECURSE TOOLCHAIN_SOURCES "serenity/Toolchain/*.cpp")
file(GLOB_RECURSE USERLAND_SOURCES "serenity/Userland/*.cpp")

set(INCLUDE_DIRS
    "serenity"
    "serenity/Kernel"
    "serenity/Userland/Libraries"
    "serenity/Userland/Libraries/LibC"
    "serenity/Userland/Libraries/LibPthread"
    "serenity/Userland/Libraries/LibM"
    "serenity/Userland/Services"
    "serenity/Toolchain/Local/${SERENITY_ARCH}/${SERENITY_ARCH}-pc-serenity/include/c++/10.2.0"
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
    ${SERVICES_SOURCES}
    ${SHELL_SOURCES}
    ${TESTS_SOURCES}
    ${TOOLCHAIN_SOURCES}
    ${USERLAND_SOURCES})

target_compile_definitions(serenity PRIVATE __serenity__ USERLAND SANITIZE_PTRS DEBUG)
target_include_directories(serenity PRIVATE ${INCLUDE_DIRS})

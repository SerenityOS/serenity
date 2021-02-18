cmake_minimum_required(VERSION 3.16)
project(SerenityOS C CXX ASM)

if(NOT "${CMAKE_BUILD_TYPE}" STREQUAL "")
  message(FATAL
    ": Don't use CMAKE_BUILD_TYPE when building serenity.\n"
    "The default build type is optimized with debug info and asserts enabled,\n"
    "and that's all there is.")
endif()

set(CMAKE_INSTALL_MESSAGE NEVER)

enable_testing()

set(CMAKE_EXPORT_COMPILE_COMMANDS ON)

set(SERENITY_ARCH "i686" CACHE STRING "Target architecture for SerenityOS.")

# Central location for all custom options used in the Serenity build.
option(ENABLE_ADDRESS_SANITIZER "Enable address sanitizer testing in gcc/clang" FALSE)
option(ENABLE_KERNEL_ADDRESS_SANITIZER "Enable kernel address sanitizer testing in gcc/clang" FALSE)
option(ENABLE_MEMORY_SANITIZER "Enable memory sanitizer testing in gcc/clang" FALSE)
option(ENABLE_UNDEFINED_SANITIZER "Enable undefined behavior sanitizer testing in gcc/clang" FALSE)
option(ENABLE_FUZZER_SANITIZER "Enable fuzzer sanitizer testing in clang" FALSE)
option(ENABLE_ALL_THE_DEBUG_MACROS "Enable all debug macros to validate they still compile" FALSE)
option(BUILD_LAGOM "Build parts of the system targeting the host OS for fuzzing/testing" FALSE)

add_custom_target(run
    COMMAND ${CMAKE_SOURCE_DIR}/Meta/run.sh
    USES_TERMINAL
)

# This can currently only be implemented by ordered commands
# as cmake doesn't support inter dependency ordering, and we
# would like to avoid inject dependencies on the existing
# custom commands to allow people to run commands adhoc with
# out forcing re-builds when they might not want them.
add_custom_target(setup-and-run
    COMMAND ${CMAKE_MAKE_PROGRAM} install
    COMMAND ${CMAKE_MAKE_PROGRAM} image
    COMMAND ${CMAKE_MAKE_PROGRAM} run
    USES_TERMINAL
)

add_custom_target(image
    DEPENDS qemu-image
)
add_custom_target(qemu-image
    COMMAND ${CMAKE_COMMAND} -E env "SERENITY_ROOT=${CMAKE_SOURCE_DIR}" "SERENITY_ARCH=${SERENITY_ARCH}" ${CMAKE_SOURCE_DIR}/Meta/build-image-qemu.sh
    BYPRODUCTS ${CMAKE_BINARY_DIR}/_disk_image
    USES_TERMINAL
)
add_custom_target(grub-image
    COMMAND ${CMAKE_COMMAND} -E env "SERENITY_ROOT=${CMAKE_SOURCE_DIR}" "SERENITY_ARCH=${SERENITY_ARCH}" ${CMAKE_SOURCE_DIR}/Meta/build-image-grub.sh
    BYPRODUCTS ${CMAKE_BINARY_DIR}/grub_disk_image
    USES_TERMINAL
)

add_custom_target(lint-shell-scripts
    COMMAND ${CMAKE_SOURCE_DIR}/Meta/lint-shell-scripts.sh
    USES_TERMINAL
)
add_custom_target(check-style
    COMMAND ${CMAKE_SOURCE_DIR}/Meta/check-style.sh
    USES_TERMINAL
)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)


if (${CMAKE_HOST_SYSTEM_NAME} MATCHES SerenityOS)
    # FIXME: Something makes this go crazy and flag unused variables that aren't flagged as such when building with the toolchain.
    #        Disable -Werror for now.
    add_compile_options(-Wno-unknown-warning-option -Wall -Wextra -Wmissing-declarations -Wformat=2 -fdiagnostics-color=always -ftls-model=initial-exec)
else()
    add_compile_options(-Wno-unknown-warning-option -Wall -Wextra -Werror -Wmissing-declarations -Wformat=2 -fdiagnostics-color=always -ftls-model=initial-exec)
endif()

if ("${CMAKE_CXX_COMPILER_ID}" STREQUAL "GNU")
    add_compile_options(-fconcepts)
elseif ("${CMAKE_CXX_COMPILER_ID}" STREQUAL "Clang")
    add_compile_options(-Wno-overloaded-virtual)
endif()

if (ENABLE_ALL_THE_DEBUG_MACROS)
    include(${CMAKE_SOURCE_DIR}/Meta/CMake/all_the_debug_macros.cmake)
endif(ENABLE_ALL_THE_DEBUG_MACROS)

configure_file(AK/Debug.h.in AK/Debug.h @ONLY)
configure_file(Kernel/Debug.h.in Kernel/Debug.h @ONLY)

include_directories(Userland/Libraries)
include_directories(.)
include_directories(${CMAKE_BINARY_DIR})

add_subdirectory(Meta/Lagom)
add_subdirectory(Userland/DevTools/IPCCompiler)
add_subdirectory(Userland/Libraries/LibWeb/CodeGenerators)
add_subdirectory(AK/Tests)
add_subdirectory(Userland/Libraries/LibRegex/Tests)

set(write_if_different ${CMAKE_SOURCE_DIR}/Meta/write-only-on-difference.sh)

find_program(CCACHE_PROGRAM ccache)
if(CCACHE_PROGRAM)
    set_property(GLOBAL PROPERTY RULE_LAUNCH_COMPILE "${CCACHE_PROGRAM}")
endif()

unset(CMAKE_SYSROOT)
set(CMAKE_STAGING_PREFIX ${CMAKE_BINARY_DIR}/Root)
set(CMAKE_INSTALL_PREFIX ${CMAKE_BINARY_DIR}/Root)
set(CMAKE_INSTALL_DATAROOTDIR ${CMAKE_BINARY_DIR}/Root/res)

if (${CMAKE_HOST_SYSTEM_NAME} MATCHES SerenityOS)
    message("Good job on building cmake!")
else()
    set(TOOLCHAIN_PATH ${CMAKE_SOURCE_DIR}/Toolchain/Local/${SERENITY_ARCH}/bin)
    set(TOOLCHAIN_PREFIX ${TOOLCHAIN_PATH}/${SERENITY_ARCH}-pc-serenity-)

    set(CMAKE_C_COMPILER ${TOOLCHAIN_PREFIX}gcc)
    set(CMAKE_CXX_COMPILER ${TOOLCHAIN_PREFIX}g++)
    set(CMAKE_ASM_COMPILER ${TOOLCHAIN_PREFIX}gcc)
    set(CMAKE_LINKER ${TOOLCHAIN_PREFIX}ld)
    set(CMAKE_RANLIB ${TOOLCHAIN_PREFIX}ranlib)
    set(CMAKE_STRIP ${TOOLCHAIN_PREFIX}strip)
    set(CMAKE_AR ${TOOLCHAIN_PREFIX}ar)
endif()

foreach(lang ASM C CXX OBJC OBJCXX)
    unset(CMAKE_${lang}_OSX_COMPATIBILITY_VERSION_FLAG)
    unset(CMAKE_${lang}_OSX_CURRENT_VERSION_FLAG)
    unset(CMAKE_${lang}_LINK_FLAGS)
    unset(CMAKE_SHARED_LIBRARY_CREATE_${lang}_FLAGS)
    unset(CMAKE_SHARED_MODULE_CREATE_${lang}_FLAGS)
    unset(CMAKE_SHARED_MODULE_LOADER_${lang}_FLAG )
    unset(CMAKE_${lang}_OSX_DEPLOYMENT_TARGET_FLAG)
    unset(CMAKE_${lang}_SYSROOT_FLAG)
    if (CMAKE_SYSTEM_NAME MATCHES Darwin)
        ## MacOS Workaround. Don't generate install_name flag when cross compiling
        set(CMAKE_${lang}_CREATE_SHARED_LIBRARY
            "<CMAKE_${lang}_COMPILER> <LANGUAGE_COMPILE_FLAGS> <CMAKE_SHARED_LIBRARY_CREATE_${lang}_FLAGS> <LINK_FLAGS> -o <TARGET> <OBJECTS> <LINK_LIBRARIES>")
    endif()
endforeach()

set(CMAKE_INSTALL_NAME_TOOL "")
set(CMAKE_SHARED_LIBRARY_SUFFIX ".so")
set(CMAKE_SHARED_LIBRARY_CREATE_CXX_FLAGS "-shared -Wl,--hash-style=gnu")
set(CMAKE_CXX_LINK_FLAGS "-Wl,--hash-style=gnu,-z,relro,-z,now")

# We disable it completely because it makes cmake very spammy.
# This will need to be revisited when the Loader supports RPATH/RUN_PATH.
set(CMAKE_SKIP_RPATH TRUE)

add_compile_options(-Os -g1 -fno-exceptions -fstack-protector-strong -Wno-address-of-packed-member -Wundef -Wcast-qual -Wwrite-strings -Wimplicit-fallthrough -Wno-nonnull-compare -Wno-deprecated-copy -Wno-expansion-to-defined)
add_compile_options(-ffile-prefix-map=${CMAKE_SOURCE_DIR}=.)

add_compile_definitions(DEBUG SANITIZE_PTRS)
set(CMAKE_CXX_FLAGS_STATIC "${CMAKE_CXX_FLAGS} -static")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -pie -fpic")

if (DBGLN_NO_COMPILETIME_FORMAT_CHECK)
    add_compile_definitions(DBGLN_NO_COMPILETIME_FORMAT_CHECK)
endif()

add_link_options(--sysroot ${CMAKE_BINARY_DIR}/Root)

include_directories(Userland/Libraries/LibC)
include_directories(Userland/Libraries/LibM)
include_directories(Userland/Libraries/LibSystem)
include_directories(Userland/Services)
include_directories(Userland)
include_directories(${CMAKE_CURRENT_BINARY_DIR})
include_directories(${CMAKE_CURRENT_BINARY_DIR}/Userland/Services)
include_directories(${CMAKE_CURRENT_BINARY_DIR}/Userland/Libraries)
include_directories(${CMAKE_CURRENT_BINARY_DIR}/Userland)

add_subdirectory(AK)
add_subdirectory(Kernel)
add_subdirectory(Userland)

set(PCI_IDS_URL https://pci-ids.ucw.cz/v2.2/pci.ids)
set(PCI_IDS_PATH ${CMAKE_INSTALL_DATAROOTDIR}/pci.ids)
if(NOT EXISTS ${PCI_IDS_PATH})
    message(STATUS "Downloading PCI ID database from ${PCI_IDS_URL}...")
    file(DOWNLOAD ${PCI_IDS_URL} ${PCI_IDS_PATH} INACTIVITY_TIMEOUT 10)
endif()

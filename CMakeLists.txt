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

add_custom_target(run
    COMMAND ${CMAKE_SOURCE_DIR}/Meta/run.sh
    USES_TERMINAL
)

add_custom_target(image
    DEPENDS qemu-image
)
add_custom_target(qemu-image
    COMMAND ${CMAKE_COMMAND} -E env "SERENITY_ROOT=${CMAKE_SOURCE_DIR}" ${CMAKE_SOURCE_DIR}/Meta/build-image-qemu.sh
    BYPRODUCTS ${CMAKE_BINARY_DIR}/_disk_image
    USES_TERMINAL
)
add_custom_target(grub-image
    COMMAND ${CMAKE_COMMAND} -E env "SERENITY_ROOT=${CMAKE_SOURCE_DIR}" ${CMAKE_SOURCE_DIR}/Meta/build-image-grub.sh
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

add_compile_options(-Wno-unknown-warning-option -Wall -Wextra -Werror -Wmissing-declarations -fdiagnostics-color=always -ftls-model=initial-exec)

if ("${CMAKE_CXX_COMPILER_ID}" STREQUAL "GNU")
    add_compile_options(-fconcepts)
elseif ("${CMAKE_CXX_COMPILER_ID}" STREQUAL "Clang")
    add_compile_options(-Wno-overloaded-virtual)
endif()

if (ALL_THE_DEBUG_MACROS)
    include(Meta/all_the_debug_macros.cmake)
endif(ALL_THE_DEBUG_MACROS)

include_directories(Libraries)
include_directories(.)

add_subdirectory(Meta/Lagom)
add_subdirectory(DevTools/IPCCompiler)
add_subdirectory(Libraries/LibWeb/CodeGenerators)
add_subdirectory(AK/Tests)
add_subdirectory(Libraries/LibRegex/Tests)

set(write_if_different ${CMAKE_SOURCE_DIR}/Meta/write-only-on-difference.sh)

function(serenity_install_headers target_name)
    file(GLOB_RECURSE headers RELATIVE ${CMAKE_CURRENT_SOURCE_DIR} "*.h")
    foreach(header ${headers})
        get_filename_component(subdirectory ${header} DIRECTORY)
        install(FILES ${header} DESTINATION usr/include/${target_name}/${subdirectory})
    endforeach()
endfunction()

function(serenity_install_sources target_name)
    file(GLOB_RECURSE sources RELATIVE ${CMAKE_CURRENT_SOURCE_DIR} "*.h" "*.cpp")
    foreach(source ${sources})
        get_filename_component(subdirectory ${source} DIRECTORY)
        install(FILES ${source} DESTINATION usr/src/serenity/${target_name}/${subdirectory})
    endforeach()
endfunction()

function(serenity_generated_sources target_name)
    if(DEFINED GENERATED_SOURCES)
        set_source_files_properties(${GENERATED_SOURCES} PROPERTIES GENERATED 1)
        foreach(generated ${GENERATED_SOURCES})
            get_filename_component(generated_name ${generated} NAME)
            add_dependencies(${target_name} generate_${generated_name})
        endforeach()
    endif()
endfunction()

function(serenity_lib target_name fs_name)
    serenity_install_headers(${target_name})
    serenity_install_sources("Libraries/${target_name}")
    #add_library(${target_name} SHARED ${SOURCES} ${GENERATED_SOURCES})
    add_library(${target_name} SHARED ${SOURCES} ${GENERATED_SOURCES} ${CMAKE_SOURCE_DIR}/Libraries/LibC/crt0_shared.cpp)
    #library_sources("{target_name}" PRIVATE ${CMAKE_SOURCE_DIR}/Libraries/LibC/crt0_shared.cpp)
    install(TARGETS ${target_name} DESTINATION usr/lib)
    set_target_properties(${target_name} PROPERTIES OUTPUT_NAME ${fs_name})
    serenity_generated_sources(${target_name})
endfunction()

function(serenity_shared_lib target_name fs_name)
    serenity_install_headers(${target_name})
    serenity_install_sources("Libraries/${target_name}")
    add_library(${target_name} SHARED ${SOURCES} ${GENERATED_SOURCES})
    install(TARGETS ${target_name} DESTINATION usr/lib)
    set_target_properties(${target_name} PROPERTIES OUTPUT_NAME ${fs_name})
    serenity_generated_sources(${target_name})
endfunction()

function(serenity_libc target_name fs_name)
    serenity_install_headers("")
    serenity_install_sources("Libraries/LibC")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -nostdlib -fpic")
    add_library(${target_name} SHARED ${SOURCES})
    install(TARGETS ${target_name} DESTINATION usr/lib)
    set_target_properties(${target_name} PROPERTIES OUTPUT_NAME ${fs_name})
    target_link_directories(LibC PUBLIC ${CMAKE_CURRENT_BINARY_DIR})
    serenity_generated_sources(${target_name})
endfunction()

function(serenity_libc_static target_name fs_name)
    serenity_install_headers("")
    serenity_install_sources("Libraries/LibC")
    add_library(${target_name} ${SOURCES})
    install(TARGETS ${target_name} ARCHIVE DESTINATION usr/lib)
    set_target_properties(${target_name} PROPERTIES OUTPUT_NAME ${fs_name})
    target_link_directories(${target_name} PUBLIC ${CMAKE_CURRENT_BINARY_DIR})
    serenity_generated_sources(${target_name})
endfunction()

function(serenity_bin target_name)
    add_executable(${target_name} ${SOURCES})
    target_sources(${target_name} PRIVATE ${CMAKE_SOURCE_DIR}/Libraries/LibC/crt0_shared.cpp)
    install(TARGETS ${target_name} RUNTIME DESTINATION bin)
    serenity_generated_sources(${target_name})
endfunction()

function(serenity_app target_name)
    cmake_parse_arguments(SERENITY_APP "" "ICON" "" ${ARGN})

    serenity_bin("${target_name}")
    set(small_icon "${CMAKE_SOURCE_DIR}/Base/res/icons/16x16/${SERENITY_APP_ICON}.png")
    set(medium_icon "${CMAKE_SOURCE_DIR}/Base/res/icons/32x32/${SERENITY_APP_ICON}.png")

    if (EXISTS "${small_icon}")
        embed_resource("${target_name}" serenity_icon_s "${small_icon}")
    endif()
    if (EXISTS "${medium_icon}")
        embed_resource("${target_name}" serenity_icon_m "${medium_icon}")
    endif()

    # TODO: Issue warnings if the app icons don't exist
endfunction()

function(compile_gml source output string_name)
    set(source ${CMAKE_CURRENT_SOURCE_DIR}/${source})
    add_custom_command(
        OUTPUT ${output}
        COMMAND ${write_if_different} ${output} ${CMAKE_SOURCE_DIR}/Meta/text-to-cpp-string.sh ${string_name} ${source}
        VERBATIM
        DEPENDS ${CMAKE_SOURCE_DIR}/Meta/text-to-cpp-string.sh
        MAIN_DEPENDENCY ${source}
    )
    get_filename_component(output_name ${output} NAME)
    add_custom_target(generate_${output_name} DEPENDS ${output})
endfunction()


function(compile_ipc source output)
    set(source ${CMAKE_CURRENT_SOURCE_DIR}/${source})
    add_custom_command(
        OUTPUT ${output}
        COMMAND ${write_if_different} ${output} ${CMAKE_BINARY_DIR}/DevTools/IPCCompiler/IPCCompiler ${source}
        VERBATIM
        DEPENDS IPCCompiler
        MAIN_DEPENDENCY ${source}
    )
    get_filename_component(output_name ${output} NAME)
    add_custom_target(generate_${output_name} DEPENDS ${output})
endfunction()

function(embed_resource target section file)
    get_filename_component(asm_file "${file}" NAME)
    set(asm_file "${CMAKE_CURRENT_BINARY_DIR}/${target}-${section}.s")
    get_filename_component(input_file "${file}" ABSOLUTE)
    add_custom_command(
        OUTPUT "${asm_file}"
        COMMAND "${CMAKE_SOURCE_DIR}/Meta/generate-embedded-resource-assembly.sh" "${asm_file}" "${section}" "${input_file}"
        DEPENDS "${input_file}" "${CMAKE_SOURCE_DIR}/Meta/generate-embedded-resource-assembly.sh"
        COMMENT "Generating ${asm_file}"
    )
    target_sources("${target}" PRIVATE "${asm_file}")
endfunction()

find_program(CCACHE_PROGRAM ccache)
if(CCACHE_PROGRAM)
    set_property(GLOBAL PROPERTY RULE_LAUNCH_COMPILE "${CCACHE_PROGRAM}")
endif()

unset(CMAKE_SYSROOT)
set(CMAKE_STAGING_PREFIX ${CMAKE_BINARY_DIR}/Root)
set(CMAKE_INSTALL_PREFIX ${CMAKE_BINARY_DIR}/Root)
set(CMAKE_INSTALL_DATAROOTDIR ${CMAKE_BINARY_DIR}/Root/res)

set(TOOLCHAIN_PATH ${CMAKE_SOURCE_DIR}/Toolchain/Local/bin)
set(TOOLCHAIN_PREFIX ${TOOLCHAIN_PATH}/i686-pc-serenity-)

set(CMAKE_C_COMPILER ${TOOLCHAIN_PREFIX}gcc)
set(CMAKE_CXX_COMPILER ${TOOLCHAIN_PREFIX}g++)
set(CMAKE_ASM_COMPILER ${TOOLCHAIN_PREFIX}gcc)
set(CMAKE_LINKER ${TOOLCHAIN_PREFIX}ld)
set(CMAKE_RANLIB ${TOOLCHAIN_PREFIX}ranlib)
set(CMAKE_STRIP ${TOOLCHAIN_PREFIX}strip)
set(CMAKE_AR ${TOOLCHAIN_PREFIX}ar)

foreach(lang ASM C CXX OBJC OBJCXX)
    unset(CMAKE_${lang}_OSX_COMPATIBILITY_VERSION_FLAG)
    unset(CMAKE_${lang}_OSX_CURRENT_VERSION_FLAG)
    unset(CMAKE_${lang}_LINK_FLAGS)
    unset(CMAKE_SHARED_LIBRARY_CREATE_${lang}_FLAGS)
    unset(CMAKE_SHARED_MODULE_CREATE_${lang}_FLAGS)
    unset(CMAKE_SHARED_MODULE_LOADER_${lang}_FLAG )
    unset(CMAKE_${lang}_OSX_DEPLOYMENT_TARGET_FLAG)
    unset(CMAKE_${lang}_SYSROOT_FLAG)
    unset(CMAKE_SHARED_LIBRARY_SONAME_${lang}_FLAG)
endforeach()

set(CMAKE_INSTALL_NAME_TOOL "true")
set(CMAKE_SHARED_LIBRARY_SUFFIX ".so")
set(CMAKE_SHARED_LIBRARY_CREATE_CXX_FLAGS "-shared")

#FIXME: -fstack-protector

add_compile_options(-Os -g1 -fno-exceptions -fno-rtti -Wno-address-of-packed-member -Wundef -Wcast-qual -Wwrite-strings -Wimplicit-fallthrough -Wno-nonnull-compare -Wno-deprecated-copy -Wno-expansion-to-defined)
add_compile_options(-ffile-prefix-map=${CMAKE_SOURCE_DIR}=.)

add_compile_definitions(DEBUG SANITIZE_PTRS)
set(CMAKE_CXX_FLAGS_STATIC "${CMAKE_CXX_FLAGS} -static")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -nostartfiles -pie -fpic")

add_link_options(--sysroot ${CMAKE_BINARY_DIR}/Root)

include_directories(Libraries/LibC)
include_directories(Libraries/LibM)
include_directories(Services)
include_directories(${CMAKE_CURRENT_BINARY_DIR})
include_directories(${CMAKE_CURRENT_BINARY_DIR}/Services)
include_directories(${CMAKE_CURRENT_BINARY_DIR}/Libraries)

add_subdirectory(AK)
add_subdirectory(Kernel)
add_subdirectory(Libraries)
add_subdirectory(Services)
add_subdirectory(Applications)
add_subdirectory(Games)
add_subdirectory(DevTools)
add_subdirectory(MenuApplets)
add_subdirectory(Shell)
add_subdirectory(Demos)
add_subdirectory(Userland)

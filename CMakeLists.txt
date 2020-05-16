cmake_minimum_required(VERSION 3.16)
project(SerenityOS C CXX ASM)

enable_testing()

add_custom_target(image
    COMMAND ${CMAKE_COMMAND} -E env "SERENITY_ROOT=${CMAKE_SOURCE_DIR}" ${CMAKE_SOURCE_DIR}/Meta/sync.sh
    BYPRODUCTS ${CMAKE_BINARY_DIR}/_disk_image
    USES_TERMINAL
)

add_custom_target(run
    COMMAND ${CMAKE_SOURCE_DIR}/Meta/run.sh
    USES_TERMINAL
)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -Wextra -Werror -std=c++2a -fdiagnostics-color=always")

include_directories(Libraries)
include_directories(.)

add_subdirectory(Meta/Lagom)
add_subdirectory(DevTools/IPCCompiler)
add_subdirectory(DevTools/FormCompiler)
add_subdirectory(Libraries/LibWeb/CodeGenerators)
add_subdirectory(AK/Tests)

function(serenity_lib_headers target_name)
    file(GLOB_RECURSE headers RELATIVE ${CMAKE_CURRENT_SOURCE_DIR} "*.h")
    foreach(header ${headers})
        get_filename_component(subdirectory ${header} DIRECTORY)
        install(FILES ${header} DESTINATION usr/include/${target_name}/${subdirectory})
    endforeach()
endfunction()

function(serenity_lib target_name fs_name)
    serenity_lib_headers(${target_name})
    add_library(${target_name} ${SOURCES} ${GENERATED_SOURCES})
    install(TARGETS ${target_name} ARCHIVE DESTINATION usr/lib)
    set_target_properties(${target_name} PROPERTIES OUTPUT_NAME ${fs_name})
    if(DEFINED GENERATED_SOURCES)
        set_source_files_properties(${GENERATED_SOURCES} PROPERTIES GENERATED 1)
        foreach(generated ${GENERATED_SOURCES})
            get_filename_component(generated_name ${generated} NAME)
            add_dependencies(${target_name} generate_${generated_name})
        endforeach()
    endif()
endfunction()

function(serenity_libc target_name fs_name)
    serenity_lib_headers("")
    add_library(${target_name} ${SOURCES})
    install(TARGETS ${target_name} ARCHIVE DESTINATION usr/lib)
    set_target_properties(${target_name} PROPERTIES OUTPUT_NAME ${fs_name})
    target_link_directories(LibC PUBLIC ${CMAKE_CURRENT_BINARY_DIR})
endfunction()

function(serenity_bin target_name)
    add_executable(${target_name} ${SOURCES})
    install(TARGETS ${target_name} RUNTIME DESTINATION bin)
endfunction()

function(compile_ipc source output)
    set(source ${CMAKE_CURRENT_SOURCE_DIR}/${source})
    add_custom_command(
        OUTPUT ${output}
        COMMAND IPCCompiler ${source} > ${output}
        VERBATIM
        DEPENDS IPCCompiler
        MAIN_DEPENDENCY ${source}
    )
    get_filename_component(output_name ${output} NAME)
    add_custom_target(generate_${output_name} DEPENDS ${output})
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

#FIXME: -fstack-protector

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Os -Wno-sized-deallocation -fno-sized-deallocation -fno-exceptions -fno-rtti -Wno-address-of-packed-member -Wundef -Wcast-qual -Wwrite-strings -Wimplicit-fallthrough -Wno-nonnull-compare -Wno-deprecated-copy")

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DDEBUG -DSANITIZE_PTRS")
add_link_options(--sysroot ${CMAKE_BINARY_DIR}/Root)

if ("${CMAKE_CXX_COMPILER_ID}" STREQUAL "GNU")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wno-expansion-to-defined")
endif()

include_directories(Libraries/LibC)
include_directories(Services)
include_directories(${CMAKE_CURRENT_BINARY_DIR}/Services)
include_directories(${CMAKE_CURRENT_BINARY_DIR}/Libraries)

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

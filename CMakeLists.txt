cmake_minimum_required(VERSION 3.16)

if (BUILD_CROSS_OS_PHASE2)
set(CMAKE_SYSTEM_NAME Generic)
endif()

if (NOT DEFINED BUILD_HOST_TOOLS_PHASE1)
set(BUILD_HOST_TOOLS_PHASE1 True)
endif()

if (NOT DEFINED BUILD_CROSS_OS_PHASE2)
set(BUILD_CROSS_OS_PHASE2 True)
endif()

project(SerenityOS C CXX ASM)

if(NOT "${CMAKE_BUILD_TYPE}" STREQUAL "")
  message(FATAL
    ": Don't use CMAKE_BUILD_TYPE when building serenity.\n"
    "The default build type is optimized with debug info and asserts enabled,\n"
    "and that's all there is.")
endif()

set(CMAKE_INSTALL_MESSAGE NEVER)

enable_testing()

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

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -Wextra -Werror -std=c++2a -fdiagnostics-color=always")

include_directories(Libraries)
include_directories(.)

if(BUILD_HOST_TOOLS_PHASE1)

add_subdirectory(Meta/Lagom)
add_subdirectory(DevTools/IPCCompiler)
add_subdirectory(DevTools/FormCompiler)
add_subdirectory(Libraries/LibWeb/CodeGenerators)
add_subdirectory(AK/Tests)

else()

# Fake empty Targets needed when building with a two phase build
add_custom_target(IPCCompiler)
add_custom_target(FormCompiler)
add_custom_target(WrapperGenerator)
add_custom_target(Generate_CSS_PropertyID_h)
add_custom_target(Generate_CSS_PropertyID_cpp)

endif()

if(DEFINED BUILD_HOST_TOOLS_LOCATION)
set(IPC_COMPILER_EXECUTABLE "${BUILD_HOST_TOOLS_LOCATION}/DevTools/IPCCompiler/IPCCompiler")
set(LIBWEB_WRAPPER_GENERATOR_EXECUTABLE "${BUILD_HOST_TOOLS_LOCATION}/Libraries/LibWeb/CodeGenerators/WrapperGenerator")
set(LIBWEB_GENERATE_CSS_PROPERTYID_H_EXECUTABLE "${BUILD_HOST_TOOLS_LOCATION}/Libraries/LibWeb/CodeGenerators/Generate_CSS_PropertyID_h")
set(LIBWEB_GENERATE_CSS_PROPERTYID_CPP_EXECUTABLE "${BUILD_HOST_TOOLS_LOCATION}/Libraries/LibWeb/CodeGenerators/Generate_CSS_PropertyID_cpp")
else()
set(IPC_COMPILER_EXECUTABLE "IPCCompiler")
set(LIBWEB_WRAPPER_GENERATOR_EXECUTABLE "WrapperGenerator")
set(LIBWEB_GENERATE_CSS_PROPERTYID_H_EXECUTABLE "Generate_CSS_PropertyID_h")
set(LIBWEB_GENERATE_CSS_PROPERTYID_CPP_EXECUTABLE "Generate_CSS_PropertyID_cpp")
endif()

if (BUILD_CROSS_OS_PHASE2)

function(serenity_install_headers target_name)
    file(GLOB_RECURSE headers RELATIVE ${CMAKE_CURRENT_SOURCE_DIR} "*.h")
    foreach(header ${headers})
        get_filename_component(subdirectory ${header} DIRECTORY)
        install(FILES ${header} DESTINATION usr/include/${target_name}/${subdirectory})
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
    add_library(${target_name} ${SOURCES} ${GENERATED_SOURCES})
    install(TARGETS ${target_name} ARCHIVE DESTINATION usr/lib)
    set_target_properties(${target_name} PROPERTIES OUTPUT_NAME ${fs_name})
    serenity_generated_sources(${target_name})
endfunction()

function(serenity_libc target_name fs_name)
    serenity_install_headers("")
    add_library(${target_name} ${SOURCES})
    install(TARGETS ${target_name} ARCHIVE DESTINATION usr/lib)
    set_target_properties(${target_name} PROPERTIES OUTPUT_NAME ${fs_name})
    target_link_directories(LibC PUBLIC ${CMAKE_CURRENT_BINARY_DIR})
    serenity_generated_sources(${target_name})
endfunction()

function(serenity_bin target_name)
    add_executable(${target_name} ${SOURCES})
    install(TARGETS ${target_name} RUNTIME DESTINATION bin)
    serenity_generated_sources(${target_name})
endfunction()

function(compile_ipc source output)
    set(source ${CMAKE_CURRENT_SOURCE_DIR}/${source})
    add_custom_command(
        OUTPUT ${output}
        COMMAND ${IPC_COMPILER_EXECUTABLE} ${source} > ${output}
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

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Os -Wno-sized-deallocation -fno-sized-deallocation -fno-exceptions -fno-rtti -Wno-address-of-packed-member -Wundef -Wcast-qual -Wwrite-strings -Wimplicit-fallthrough -Wno-nonnull-compare -Wno-deprecated-copy -Wno-expansion-to-defined")

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DDEBUG -DSANITIZE_PTRS")
add_link_options(--sysroot ${CMAKE_BINARY_DIR}/Root)

include_directories(Libraries/LibC)
include_directories(Services)
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

endif()
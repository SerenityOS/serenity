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

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wno-unknown-warning-option -Wall -Wextra -Werror -Wmissing-declarations -std=c++2a -fdiagnostics-color=always")
if ("${CMAKE_CXX_COMPILER_ID}" STREQUAL "GNU")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fconcepts")
elseif ("${CMAKE_CXX_COMPILER_ID}" STREQUAL "Clang")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wno-overloaded-virtual")
endif()

if (ALL_THE_DEBUG_MACROS)
    # As you can see, this enables *ALL THE DEBUG MACROS*!
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DAPIC_DEBUG -DAPIC_SMP_DEBUG -DARP_DEBUG -DAWAVLOADER_DEBUG")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DBBFS_DEBUG -DBXVGA_DEBUG")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DCACHE_DEBUG -DCHTTPJOB_DEBUG -DCNETWORKJOB_DEBUG -DCOMPOSE_DEBUG -DCONTEXT_SWITCH_DEBUG -DCONTIGUOUS_VMOBJECT_DEBUG -DCRYPTO_DEBUG -DCSOCKET_DEBUG")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DDEBUG_CURSOR_TOOL -DDEBUG_GZIP -DDEBUG_HIGHLIGHT_FOCUSED_FRAME -DDEBUG_IO -DDEBUG_ITEM_RECTS -DDEBUG_LOCAL_SOCKET -DDEBUG_MAPPED_FILE -DDEBUG_MARKDOWN -DDEBUG_MENUS -DDEBUG_POLL_SELECT -DDEBUG_SPAM -DDEBUG_STRINGIMPL -DDEBUG_SYNTAX_HIGHLIGHTING -DDEBUG_TEXTEDITOR -DDEFERRED_INVOKE_DEBUG -DDHCPV4CLIENT_DEBUG -DDHCPV4_DEBUG -DDOUBLECLICK_DEBUG -DDRAGDROP_DEBUG -DDWARF_DEBUG -DDYNAMIC_LOAD_DEBUG")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DE1000_DEBUG -DEBR_DEBUG -DEDITOR_DEBUG -DEMOJI_DEBUG -DETHERNET_DEBUG -DETHERNET_VERY_DEBUG -DEVENT_DEBUG -DEVENTLOOP_DEBUG -DEXEC_DEBUG -DEXT2_DEBUG")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DFIFO_DEBUG -DFILL_PATH_DEBUG -DFORK_DEBUG")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DGBOXLAYOUT_DEBUG -DGEMINIJOB_DEBUG -DGENERATE_DEBUG_CODE -DGLOBAL_DTORS_DEBUG -DGMENU_DEBUG -DGPT_DEBUG")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DHEAP_DEBUG -DHEX_DEBUG -DHPET_COMPARATOR_DEBUG -DHPET_DEBUG -DHTML_DEBUG -DHTTPSJOB_DEBUG")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DICMP_DEBUG -DICO_DEBUG -DImage_DEBUG -DIMAGE_DECODER_CLIENT_DEBUG -DIMAGE_DECODER_DEBUG -DIMAGE_LOADER_DEBUG -DINTERPRETER_DEBUG -DINTERRUPT_DEBUG -DIOAPIC_DEBUG -DIPC_DEBUG -DIPV4_DEBUG -DIPV4_SOCKET_DEBUG -DIRC_DEBUG -DIRQ_DEBUG")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DJOB_DEBUG")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DKEYBOARD_DEBUG -DKEYBOARD_SHORTCUTS_DEBUG -DKMALLOC_DEBUG_LARGE_ALLOCATIONS")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DLoader_DEBUG")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DMALLOC_DEBUG -DMASTERPTY_DEBUG -DMBR_DEBUG -DMEMORY_DEBUG -DMENU_DEBUG -DMINIMIZE_ANIMATION_DEBUG -DMM_DEBUG -DMOVE_DEBUG -DMULTIPROCESSOR_DEBUG")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DNETWORK_TASK_DEBUG -DNT_DEBUG")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DOBJECT_DEBUG -DOCCLUSIONS_DEBUG -DOFFD_DEBUG")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DPAGE_FAULT_DEBUG -DPARSER_DEBUG -DPATA_DEBUG -DPATA_DEVICE_DEBUG -DPATH_DEBUG -DPCI_DEBUG -DPNG_DEBUG -DPROCESS_DEBUG -DPROCFS_DEBUG -DPS2MOUSE_DEBUG -DPTHREAD_DEBUG -DPTMX_DEBUG")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DREACHABLE_DEBUG -DRESIZE_DEBUG -DRESOURCE_DEBUG -DROUTING_DEBUG -DRTL8139_DEBUG")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DSAFE_SYSCALL_DEBUG -DSB16_DEBUG -DSCHEDULER_DEBUG -DSCHEDULER_RUNNABLE_DEBUG -DSELECTION_DEBUG -DSERVICE_DEBUG -DSHARED_BUFFER_DEBUG -DSH_DEBUG -DSIGNAL_DEBUG -DSLAVEPTY_DEBUG -DSMP_DEBUG -DSOCKET_DEBUG -DSYSTEM_MENU_DEBUG -DSYSTEMSERVER_DEBUG")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DTCP_DEBUG -DTCP_SOCKET_DEBUG -DTERMCAP_DEBUG -DTERMINAL_DEBUG -DTHREAD_DEBUG -DTLS_DEBUG -DTTY_DEBUG")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DUCI_DEBUG -DUDP_DEBUG -DUPDATE_COALESCING_DEBUG")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DVERY_DEBUG -DVFS_DEBUG -DVMWAREBACKDOOR_DEBUG -DVRA_DEBUG")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DWAITQUEUE_DEBUG -DWEAKABLE_DEBUG -DWINDOWMANAGER_DEBUG -DWSMESSAGELOOP_DEBUG -DWSSCREEN_DEBUG")
    # FIXME: Unbreak debug code for the following:
    # - ACPI_DEBUG
endif(ALL_THE_DEBUG_MACROS)

include_directories(Libraries)
include_directories(.)

add_subdirectory(Meta/Lagom)
add_subdirectory(DevTools/IPCCompiler)
add_subdirectory(Libraries/LibWeb/CodeGenerators)
add_subdirectory(AK/Tests)

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
    add_library(${target_name} ${SOURCES} ${GENERATED_SOURCES})
    install(TARGETS ${target_name} ARCHIVE DESTINATION usr/lib)
    set_target_properties(${target_name} PROPERTIES OUTPUT_NAME ${fs_name})
    serenity_generated_sources(${target_name})
endfunction()

function(serenity_libc target_name fs_name)
    serenity_install_headers("")
    serenity_install_sources("Libraries/LibC")
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

function(compile_json_gui source output string_name)
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

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Os -g1 -fno-exceptions -fno-rtti -Wno-address-of-packed-member -Wundef -Wcast-qual -Wwrite-strings -Wimplicit-fallthrough -Wno-nonnull-compare -Wno-deprecated-copy -Wno-expansion-to-defined")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -ffile-prefix-map=${CMAKE_SOURCE_DIR}=.")

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DDEBUG -DSANITIZE_PTRS")
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

set(PREKERNEL_KERNEL_IMAGE_PATH ${CMAKE_CURRENT_BINARY_DIR}/../Kernel_shared_object)
configure_file(KernelImage.S.in KernelImage.S @ONLY)

set(SOURCES
    init.cpp

    ConfigurationTable.cpp
    DebugOutput.cpp
    GOP.cpp
    Panic.cpp
    Relocation.cpp
    Runtime.cpp
    kmalloc.cpp

    KernelImage.S

    Arch/${SERENITY_ARCH}/Boot.cpp
    Arch/${SERENITY_ARCH}/MMU.cpp

    ../Firmware/EFI/EFI.cpp
    ../Library/MiniStdLib.cpp
    ../Prekernel/UBSanitizer.cpp

    ../../Userland/Libraries/LibELF/Image.cpp
    ../../Userland/Libraries/LibELF/Validation.cpp

    ../../AK/Format.cpp
    ../../AK/StringBuilder.cpp
    ../../AK/StringUtils.cpp
    ../../AK/StringView.cpp
)

if ("${SERENITY_ARCH}" STREQUAL "x86_64")
    set(SOURCES
        ${SOURCES}
        Arch/x86_64/CPUID.cpp
    )
endif()

set_source_files_properties(KernelImage.S PROPERTIES OBJECT_DEPENDS ${PREKERNEL_KERNEL_IMAGE_PATH})

add_compile_definitions(PREKERNEL)

add_executable(EFIPrekernel ${SOURCES})

add_dependencies(EFIPrekernel Kernel)

target_compile_options(EFIPrekernel PRIVATE -fno-threadsafe-statics)
target_link_options(EFIPrekernel PRIVATE LINKER:-T ${CMAKE_CURRENT_SOURCE_DIR}/linker.ld LINKER:--no-dynamic-linker -nostdlib)
set_target_properties(EFIPrekernel PROPERTIES LINK_DEPENDS ${CMAKE_CURRENT_SOURCE_DIR}/linker.ld)

if (CMAKE_CXX_COMPILER_ID STREQUAL "GNU")
    target_link_libraries(EFIPrekernel PRIVATE gcc)

    # Prevent naively implemented string functions (like strlen) from being "optimized" into a call to themselves.
    set_source_files_properties(../Library/MiniStdLib.cpp
        PROPERTIES COMPILE_FLAGS "-fno-tree-loop-distribution -fno-tree-loop-distribute-patterns")
elseif(CMAKE_CXX_COMPILER_ID MATCHES "Clang$")
    target_link_libraries(EFIPrekernel PRIVATE clang_rt.builtins)
endif()

# The PrekernelPEImageGenerator doesn't support RELR relocations.
if (CMAKE_CXX_COMPILER_ID MATCHES "Clang$")
    target_link_options(EFIPrekernel PRIVATE LINKER:-z,nopack-relative-relocs LINKER:--pack-dyn-relocs=none)
elseif("${SERENITY_ARCH}" STREQUAL "x86_64")
    target_link_options(EFIPrekernel PRIVATE LINKER:-z,nopack-relative-relocs)
endif()

add_custom_command(
    OUTPUT ${CMAKE_CURRENT_BINARY_DIR}/Kernel.efi
    COMMAND $<TARGET_FILE:Lagom::PrekernelPEImageGenerator> ${CMAKE_CURRENT_BINARY_DIR}/EFIPrekernel ${CMAKE_CURRENT_BINARY_DIR}/Kernel.efi
    VERBATIM
    DEPENDS EFIPrekernel Lagom::PrekernelPEImageGenerator
)

add_custom_target(generate_kernel_efi_image ALL DEPENDS ${CMAKE_CURRENT_BINARY_DIR}/Kernel.efi)

install(FILES ${CMAKE_CURRENT_BINARY_DIR}/Kernel.efi DESTINATION boot)

# Remove options which the Prekernel environment doesn't support.
get_target_property(EFI_PREKERNEL_COMPILE_OPTIONS EFIPrekernel COMPILE_OPTIONS)
list(REMOVE_ITEM EFI_PREKERNEL_COMPILE_OPTIONS "-fsanitize-coverage=trace-pc")
list(REMOVE_ITEM EFI_PREKERNEL_COMPILE_OPTIONS "-fsanitize=kernel-address")
set_target_properties(EFIPrekernel PROPERTIES COMPILE_OPTIONS "${EFI_PREKERNEL_COMPILE_OPTIONS}")

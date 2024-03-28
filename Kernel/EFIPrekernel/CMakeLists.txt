set(SOURCES
    init.cpp
    ../Library/MiniStdLib.cpp
    ../Prekernel/UBSanitizer.cpp
)

add_executable(EFIPrekernel ${SOURCES})
add_dependencies(EFIPrekernel install_libc_headers)

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
if (CMAKE_CXX_COMPILER_ID MATCHES "Clang$" OR ENABLE_MOLD_LINKER)
    target_link_options(EFIPrekernel PRIVATE LINKER:--pack-dyn-relocs=none)
elseif("${SERENITY_ARCH}" STREQUAL "x86_64")
    target_link_options(EFIPrekernel PRIVATE LINKER:-z,nopack-relative-relocs)
endif()

add_custom_command(
    TARGET EFIPrekernel POST_BUILD
    COMMAND $<TARGET_FILE:Lagom::PrekernelPEImageGenerator> ${CMAKE_CURRENT_BINARY_DIR}/EFIPrekernel ${CMAKE_CURRENT_BINARY_DIR}/Prekernel.efi
    VERBATIM
    DEPENDS Lagom::PrekernelPEImageGenerator
    MAIN_DEPENDENCY ${CMAKE_CURRENT_BINARY_DIR}/EFIPrekernel
    BYPRODUCTS ${CMAKE_CURRENT_BINARY_DIR}/Prekernel.efi
)
install(FILES ${CMAKE_CURRENT_BINARY_DIR}/Prekernel.efi DESTINATION boot)

# Remove options which the Prekernel environment doesn't support.
get_target_property(EFI_PREKERNEL_TARGET_OPTIONS EFIPrekernel COMPILE_OPTIONS)
list(REMOVE_ITEM EFI_PREKERNEL_TARGET_OPTIONS "-fsanitize-coverage=trace-pc")
list(REMOVE_ITEM EFI_PREKERNEL_TARGET_OPTIONS "-fsanitize=kernel-address")
set_target_properties(EFIPrekernel PROPERTIES COMPILE_OPTIONS "${EFI_PREKERNEL_TARGET_OPTIONS}")

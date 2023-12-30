set(SOURCES
    UBSanitizer.cpp
    ../Library/MiniStdLib.cpp
    boot.S
    multiboot.S
    init.cpp
    ../../Userland/Libraries/LibELF/Relocation.cpp
    )

if ("${SERENITY_ARCH}" STREQUAL "x86_64")
    set(PREKERNEL_TARGET Prekernel64)
elseif("${SERENITY_ARCH}" STREQUAL "aarch64")
    message(SEND_ERROR "Prekernel is not needed on aarch64 and should not be compiled!")
endif()

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -static")

add_executable(${PREKERNEL_TARGET} ${SOURCES})
add_dependencies(${PREKERNEL_TARGET} install_libc_headers)
target_compile_options(${PREKERNEL_TARGET} PRIVATE -no-pie -fno-pic -fno-threadsafe-statics)

target_link_options(${PREKERNEL_TARGET} PRIVATE LINKER:-T ${CMAKE_CURRENT_SOURCE_DIR}/linker.ld -nostdlib LINKER:--no-pie)
set_target_properties(${PREKERNEL_TARGET} PROPERTIES LINK_DEPENDS ${CMAKE_CURRENT_SOURCE_DIR}/linker.ld)

if (CMAKE_CXX_COMPILER_ID STREQUAL "GNU")
    target_link_libraries(${PREKERNEL_TARGET} PRIVATE gcc)
elseif(CMAKE_CXX_COMPILER_ID MATCHES "Clang$")
    target_link_libraries(${PREKERNEL_TARGET} PRIVATE clang_rt.builtins)
endif()

add_custom_command(
    TARGET ${PREKERNEL_TARGET} POST_BUILD
    COMMAND ${CMAKE_OBJCOPY} -O elf32-i386 ${CMAKE_CURRENT_BINARY_DIR}/${PREKERNEL_TARGET} ${CMAKE_CURRENT_BINARY_DIR}/Prekernel
    BYPRODUCTS ${CMAKE_CURRENT_BINARY_DIR}/Prekernel
)

install(FILES "${CMAKE_CURRENT_BINARY_DIR}/Prekernel" DESTINATION boot)

# Remove options which the Prekernel environment doesn't support.
get_target_property(PREKERNEL_TARGET_OPTIONS ${PREKERNEL_TARGET} COMPILE_OPTIONS)
list(REMOVE_ITEM PREKERNEL_TARGET_OPTIONS "-fsanitize-coverage=trace-pc")
list(REMOVE_ITEM PREKERNEL_TARGET_OPTIONS "-fsanitize=kernel-address")
set_target_properties(${PREKERNEL_TARGET} PROPERTIES COMPILE_OPTIONS "${PREKERNEL_TARGET_OPTIONS}")

if (ENABLE_KERNEL_ADDRESS_SANITIZER)
    add_compile_definitions(KERNEL_ADDRESS_SANITIZER_ENABLED)
endif()

add_compile_options(-ggdb3)

set(SOURCES
    boot.S
    multiboot.S
    init.cpp
    UBSanitizer.cpp
    ../Kernel/MiniStdLib.cpp
)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS_STATIC}")

set_property(SOURCE boot.S APPEND PROPERTY COMPILE_DEFINITIONS _BOOTLOADER)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wno-unknown-warning-option -Wvla -Wnull-dereference")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fno-rtti -ffreestanding -fbuiltin")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -mno-80387 -mno-mmx -mno-sse -mno-sse2")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fno-asynchronous-unwind-tables")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fstack-protector-strong")
if (NOT ${CMAKE_HOST_SYSTEM_NAME} MATCHES SerenityOS)
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -nostdlib -nostdinc -nostdinc++")
endif()

if ("${SERENITY_ARCH}" STREQUAL "x86_64")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -mcmodel=large -fno-pic -mno-red-zone")
else()
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -pie -fPIE")
endif()

add_compile_definitions(KERNEL)

# HACK: This is a workaround for CLion to grok the kernel sources.
#       It's needed because CLion doesn't understand the way we switch compilers mid-build.
add_compile_definitions(__serenity__)

add_link_options(LINKER:-T ${CMAKE_CURRENT_SOURCE_DIR}/linker.ld -nostdlib)

if (${CMAKE_HOST_SYSTEM_NAME} MATCHES SerenityOS)
    include_directories(/usr/local/include/c++/${GCC_VERSION}/)
else()
    if (NOT EXISTS ${TOOLCHAIN_ROOT}/${SERENITY_ARCH}-pc-serenity/include/c++/${GCC_VERSION}/)
        message(FATAL_ERROR "Toolchain version ${GCC_VERSION} appears to be missing! Please run: Meta/serenity.sh rebuild-toolchain")
    endif()
    include_directories(${TOOLCHAIN_ROOT}/${SERENITY_ARCH}-pc-serenity/include/c++/${GCC_VERSION}/)
    include_directories(${TOOLCHAIN_ROOT}/${SERENITY_ARCH}-pc-serenity/include/c++/${GCC_VERSION}/${SERENITY_ARCH}-pc-serenity/)
    link_directories(${TOOLCHAIN_ROOT}/${SERENITY_ARCH}-pc-serenity/lib)
    link_directories(${TOOLCHAIN_ROOT}/lib/gcc/${SERENITY_ARCH}-pc-serenity/${GCC_VERSION}/)
endif()

if ("${SERENITY_ARCH}" STREQUAL "i686")
    set(BOOTLOADER_TARGET Bootloader32)
else()
    set(BOOTLOADER_TARGET Bootloader64)
endif()

add_executable(${BOOTLOADER_TARGET} ${SOURCES})

set_target_properties(${BOOTLOADER_TARGET} PROPERTIES LINK_DEPENDS ${CMAKE_CURRENT_SOURCE_DIR}/linker.ld)

target_link_libraries(${BOOTLOADER_TARGET} gcc supc++)

add_custom_command(
    TARGET ${BOOTLOADER_TARGET} POST_BUILD
    COMMAND ${TOOLCHAIN_PREFIX}objcopy -O elf32-i386 ${CMAKE_CURRENT_BINARY_DIR}/${BOOTLOADER_TARGET} ${CMAKE_CURRENT_BINARY_DIR}/Bootloader
    BYPRODUCTS ${CMAKE_CURRENT_BINARY_DIR}/Bootloader
)

install(FILES "${CMAKE_CURRENT_BINARY_DIR}/Bootloader" DESTINATION boot)

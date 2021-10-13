set(SOURCES
    UBSanitizer.cpp
    ../MiniStdLib.cpp
)
if ("${SERENITY_ARCH}" STREQUAL "aarch64")
    set(SOURCES
        ${SOURCES}
        Arch/aarch64/BootPPMParser.cpp
        Arch/aarch64/GPIO.cpp
        Arch/aarch64/Framebuffer.cpp
        Arch/aarch64/Mailbox.cpp
        Arch/aarch64/MainIdRegister.cpp
        Arch/aarch64/MMIO.cpp
        Arch/aarch64/Timer.cpp
        Arch/aarch64/UART.cpp
        Arch/aarch64/Utils.cpp

        # Preload specific
        Arch/aarch64/init.cpp
        Arch/aarch64/PrekernelMMU.cpp
        Arch/aarch64/PrekernelExceptions.cpp
        Arch/aarch64/PrekernelCommon.cpp

        # Assembly
        Arch/aarch64/boot.S
        Arch/aarch64/Aarch64_asm_utils.S
        Arch/aarch64/vector_table.S
    )
else()
    set(SOURCES
        ${SOURCES}
        Arch/x86/boot.S
        Arch/x86/multiboot.S
        # FIXME: Eventually, some of these should build on aarch64 too.
        init.cpp
        ../../Userland/Libraries/LibELF/Relocation.cpp
    )
endif()


if ("${SERENITY_ARCH}" STREQUAL "i686")
    set(PREKERNEL_TARGET Prekernel32)
elseif ("${SERENITY_ARCH}" STREQUAL "x86_64")
    set(PREKERNEL_TARGET Prekernel64)
else()
    set(PREKERNEL_TARGET Prekernel)
endif()

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -static")

add_executable(${PREKERNEL_TARGET} ${SOURCES})
target_compile_options(${PREKERNEL_TARGET} PRIVATE -no-pie -fno-pic -fno-threadsafe-statics)

if ("${SERENITY_ARCH}" STREQUAL "aarch64")
    set(PREKERNEL_LINKER_SCRIPT "${CMAKE_CURRENT_SOURCE_DIR}/Arch/aarch64/linker.ld")
else()
    set(PREKERNEL_LINKER_SCRIPT "${CMAKE_CURRENT_SOURCE_DIR}/Arch/x86/linker.ld")
endif()
target_link_options(${PREKERNEL_TARGET} PRIVATE LINKER:-T ${PREKERNEL_LINKER_SCRIPT} -nostdlib LINKER:--no-pie)
set_target_properties(${PREKERNEL_TARGET} PROPERTIES LINK_DEPENDS ${PREKERNEL_LINKER_SCRIPT})

if (CMAKE_CXX_COMPILER_ID STREQUAL "GNU")
    target_link_libraries(${PREKERNEL_TARGET} PRIVATE gcc)
elseif(CMAKE_CXX_COMPILER_ID MATCHES "Clang$")
    target_link_libraries(${PREKERNEL_TARGET} PRIVATE clang_rt.builtins)
endif()

if ("${SERENITY_ARCH}" STREQUAL "i686" OR "${SERENITY_ARCH}" STREQUAL "x86_64")
    add_custom_command(
        TARGET ${PREKERNEL_TARGET} POST_BUILD
        COMMAND ${CMAKE_OBJCOPY} -O elf32-i386 ${CMAKE_CURRENT_BINARY_DIR}/${PREKERNEL_TARGET} ${CMAKE_CURRENT_BINARY_DIR}/Prekernel
        BYPRODUCTS ${CMAKE_CURRENT_BINARY_DIR}/Prekernel
    )
endif()

if ("${SERENITY_ARCH}" STREQUAL "aarch64")
    embed_resource(Prekernel serenity_boot_logo "Arch/aarch64/SerenityLogoRGB.ppm")

    add_custom_command(
        TARGET Prekernel POST_BUILD
        COMMAND ${CMAKE_OBJCOPY} -O binary Prekernel kernel8.img
        BYPRODUCTS ${CMAKE_CURRENT_BINARY_DIR}/kernel8.img
    )
endif()

install(FILES "${CMAKE_CURRENT_BINARY_DIR}/Prekernel" DESTINATION boot)

# Remove options which the Prekernel environment doesn't support.
get_target_property(PREKERNEL_TARGET_OPTIONS ${PREKERNEL_TARGET} COMPILE_OPTIONS)
list(REMOVE_ITEM PREKERNEL_TARGET_OPTIONS "-fsanitize-coverage=trace-pc")
list(REMOVE_ITEM PREKERNEL_TARGET_OPTIONS "-fsanitize=kernel-address")
set_target_properties(${PREKERNEL_TARGET} PROPERTIES COMPILE_OPTIONS "${PREKERNEL_TARGET_OPTIONS}")

# This is here just to not make any changes to Lagom CMakeLists in this commit.
if (NOT SERENITYOS)
    add_library(LibELF INTERFACE)
    serenity_install_headers("LibELF")
    serenity_install_sources("Userland/Libraries/LibELF")

    return()
endif()

set(SOURCES
    Image.cpp
    Validation.cpp
)

serenity_install_headers("LibELF")
serenity_install_sources("Userland/Libraries/LibELF")

if (SERENITYOS)
    list(APPEND SOURCES
        Arch/${SERENITY_ARCH}/entry.S
        Arch/${SERENITY_ARCH}/plt_trampoline.S
        Arch/${SERENITY_ARCH}/tls.cpp
        DynamicLinker.cpp
        DynamicLoader.cpp
        DynamicObject.cpp
        ELFBuild.cpp
        Relocation.cpp
    )

    if (SERENITY_ARCH STREQUAL "aarch64")
        list(APPEND SOURCES
            Arch/aarch64/tls.S
        )
    endif()
endif()

serenity_lib(LibELF elf ${SOURCES})
target_link_libraries(LibELF PRIVATE LibSystem)

set(LOADER_SOURCES
    main.cpp
    misc.cpp
)

file(GLOB AK_SOURCES "../../AK/*.cpp")
file(GLOB ELF_SOURCES "../Libraries/LibELF/*.cpp")
file(GLOB LIBC_SOURCES1 "../Libraries/LibC/*.cpp")
file(GLOB LIBC_SOURCES2 "../Libraries/LibC/*/*.cpp")

if ("${SERENITY_ARCH}" STREQUAL "i686")
    file(GLOB LIBC_SOURCES3 "../Libraries/LibC/arch/i386/*.S")
    set(ELF_SOURCES ${ELF_SOURCES} ../Libraries/LibELF/Arch/i386/plt_trampoline.S)
elseif ("${SERENITY_ARCH}" STREQUAL "x86_64")
    file(GLOB LIBC_SOURCES3 "../Libraries/LibC/arch/x86_64/*.S") # FIXME: this does not exist at this point!
    set(ELF_SOURCES ${ELF_SOURCES} ../Libraries/LibELF/Arch/x86_64/plt_trampoline.S)
endif()

file(GLOB LIBSYSTEM_SOURCES "../Libraries/LibSystem/*.cpp")

if (ENABLE_UNDEFINED_SANITIZER)
    set(LOADER_SOURCES ${LOADER_SOURCES} ../Libraries/LibSanitizer/UBSanitizer.cpp)
endif()

add_definitions(-D_DYNAMIC_LOADER)

set(SOURCES ${LOADER_SOURCES} ${AK_SOURCES} ${ELF_SOURCES} ${LIBC_SOURCES1} ${LIBC_SOURCES2} ${LIBC_SOURCES3} ${LIBSYSTEM_SOURCES})

# FIXME: Remove -fbuilding-libgcc after the next toolchain update
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fbuilding-libgcc -fno-rtti -nostdlib -pie -fpic -DNO_TLS")

set_source_files_properties (../Libraries/LibC/ssp.cpp PROPERTIES COMPILE_FLAGS
    "-fno-stack-protector")

add_executable(Loader.so ${SOURCES})
target_link_libraries(Loader.so gcc)
target_link_options(Loader.so PRIVATE LINKER:--no-dynamic-linker)
install(TARGETS Loader.so RUNTIME DESTINATION usr/lib/)

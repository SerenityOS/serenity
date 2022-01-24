set(LOADER_SOURCES
    main.cpp
    misc.cpp
)

file(GLOB AK_SOURCES "../../AK/*.cpp")
file(GLOB ELF_SOURCES "../Libraries/LibELF/*.cpp")
file(GLOB LIBC_SOURCES1 "../Libraries/LibC/*.cpp")
file(GLOB LIBC_SOURCES2 "../Libraries/LibC/*/*.cpp")

if ("${SERENITY_ARCH}" STREQUAL "aarch64")
    file(GLOB LIBC_SOURCES3 "../Libraries/LibC/arch/aarch64/*.S")
    set(ELF_SOURCES ${ELF_SOURCES} ../Libraries/LibELF/Arch/aarch64/entry.S ../Libraries/LibELF/Arch/aarch64/plt_trampoline.S)
elseif ("${SERENITY_ARCH}" STREQUAL "i686")
    file(GLOB LIBC_SOURCES3 "../Libraries/LibC/arch/i386/*.S")
    set(ELF_SOURCES ${ELF_SOURCES} ../Libraries/LibELF/Arch/i386/entry.S ../Libraries/LibELF/Arch/i386/plt_trampoline.S)
elseif ("${SERENITY_ARCH}" STREQUAL "x86_64")
    file(GLOB LIBC_SOURCES3 "../Libraries/LibC/arch/x86_64/*.S")
    set(ELF_SOURCES ${ELF_SOURCES} ../Libraries/LibELF/Arch/x86_64/entry.S ../Libraries/LibELF/Arch/x86_64/plt_trampoline.S)
endif()

file(GLOB LIBSYSTEM_SOURCES "../Libraries/LibSystem/*.cpp")

if (ENABLE_UNDEFINED_SANITIZER)
    set(LOADER_SOURCES ${LOADER_SOURCES} ../Libraries/LibSanitizer/UBSanitizer.cpp)
endif()

add_definitions(-D_DYNAMIC_LOADER)

set(SOURCES ${LOADER_SOURCES} ${AK_SOURCES} ${ELF_SOURCES} ${LIBC_SOURCES1} ${LIBC_SOURCES2} ${LIBC_SOURCES3} ${LIBSYSTEM_SOURCES})

# FIXME: remove -nodefaultlibs after the next toolchain update
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fno-rtti -nodefaultlibs -nostdlib -pie -fpic -DNO_TLS")

set_source_files_properties (../Libraries/LibC/ssp.cpp PROPERTIES COMPILE_FLAGS
    "-fno-stack-protector")
# Prevent GCC from removing null checks by marking the `FILE*` argument non-null
set_source_files_properties(../Libraries/LibC/stdio.cpp PROPERTIES COMPILE_FLAGS "-fno-builtin-fputc -fno-builtin-fputs -fno-builtin-fwrite")

add_executable(Loader.so ${SOURCES})

if (CMAKE_CXX_COMPILER_ID STREQUAL "GNU")
    target_link_libraries(Loader.so PRIVATE gcc)
elseif (CMAKE_CXX_COMPILER_ID MATCHES "Clang$")
    target_link_libraries(Loader.so PRIVATE clang_rt.builtins)
endif ()

target_link_libraries(Loader.so PRIVATE LibTimeZone)
target_link_options(Loader.so PRIVATE LINKER:--no-dynamic-linker)
install(TARGETS Loader.so RUNTIME DESTINATION usr/lib/)

set(LOADER_SOURCES
    main.cpp
    misc.cpp
)

add_library(DynamicLoader_CompileOptions INTERFACE)
target_compile_options(DynamicLoader_CompileOptions INTERFACE -fno-rtti -fpie)
target_link_options(DynamicLoader_CompileOptions INTERFACE -nostdlib -fpie)
target_compile_definitions(DynamicLoader_CompileOptions INTERFACE NO_TLS)
add_dependencies(DynamicLoader_CompileOptions install_libc_headers)

set(LIBC_SOURCES
    ../Libraries/LibC/sys/archctl.cpp
    ../Libraries/LibC/sys/auxv.cpp
    ../Libraries/LibC/sys/file.cpp
    ../Libraries/LibC/sys/mman.cpp
    ../Libraries/LibC/sys/prctl.cpp
    ../Libraries/LibC/sys/select.cpp
    ../Libraries/LibC/sys/wait.cpp
    ../Libraries/LibC/assert.cpp
    ../Libraries/LibC/ctype.cpp
    ../Libraries/LibC/cxxabi.cpp
    ../Libraries/LibC/dirent.cpp
    ../Libraries/LibC/dlfcn.cpp
    ../Libraries/LibC/fcntl.cpp
    ../Libraries/LibC/fenv.cpp
    ../Libraries/LibC/inttypes.cpp
    ../Libraries/LibC/ioctl.cpp
    ../Libraries/LibC/libcinit.cpp
    ../Libraries/LibC/libgen.cpp
    ../Libraries/LibC/link.cpp
    ../Libraries/LibC/malloc.cpp
    ../Libraries/LibC/math.cpp
    ../Libraries/LibC/poll.cpp
    ../Libraries/LibC/pthread_cond.cpp
    ../Libraries/LibC/pthread_integration.cpp
    ../Libraries/LibC/pthread_once.cpp
    ../Libraries/LibC/pwd.cpp
    ../Libraries/LibC/qsort.cpp
    ../Libraries/LibC/sched.cpp
    ../Libraries/LibC/scanf.cpp
    ../Libraries/LibC/search.cpp
    ../Libraries/LibC/serenity.cpp
    ../Libraries/LibC/signal.cpp
    ../Libraries/LibC/spawn.cpp
    ../Libraries/LibC/ssp.cpp
    ../Libraries/LibC/stat.cpp
    ../Libraries/LibC/stdio.cpp
    ../Libraries/LibC/stdlib.cpp
    ../Libraries/LibC/string.cpp
    ../Libraries/LibC/strings.cpp
    ../Libraries/LibC/termios.cpp
    ../Libraries/LibC/time.cpp
    ../Libraries/LibC/times.cpp
    ../Libraries/LibC/ulimit.cpp
    ../Libraries/LibC/unistd.cpp
    ../Libraries/LibC/utime.cpp
    ../Libraries/LibC/utsname.cpp
    ../Libraries/LibC/wchar.cpp
    ../Libraries/LibC/wctype.cpp
    ../Libraries/LibC/wstdio.cpp)

set(ARCH_FOLDER "${SERENITY_ARCH}")

file(GLOB LIBC_ARCH_SPECIFIC_SOURCES "../Libraries/LibC/arch/${ARCH_FOLDER}/*.S")
set(LIBC_ARCH_SPECIFIC_SOURCES ${LIBC_ARCH_SPECIFIC_SOURCES} "../Libraries/LibC/arch/${ARCH_FOLDER}/fenv.cpp")
if ("${SERENITY_ARCH}" STREQUAL "x86_64")
    set(LIBC_ARCH_SPECIFIC_SOURCES ${LIBC_ARCH_SPECIFIC_SOURCES} "../Libraries/LibC/arch/x86_64/memset.cpp")
endif()

if (ENABLE_UNDEFINED_SANITIZER)
    set(LOADER_SOURCES ${LOADER_SOURCES} ../Libraries/LibSanitizer/UBSanitizer.cpp)
endif()

add_definitions(-D_DYNAMIC_LOADER)

set(SOURCES ${LOADER_SOURCES} ${AK_SOURCES} ${LIBC_SOURCES} ${LIBC_ARCH_SPECIFIC_SOURCES})

if (("${SERENITY_ARCH}" STREQUAL "aarch64") OR ("${SERENITY_ARCH}" STREQUAL "riscv64"))
    # On aarch64 and riscv64 the stack protector would be accessed before the Loader can relocate itself.
    set_source_files_properties(main.cpp PROPERTIES COMPILE_FLAGS "-fno-stack-protector")
endif()

set_source_files_properties(../Libraries/LibC/ssp.cpp PROPERTIES COMPILE_FLAGS "-fno-stack-protector")
set_source_files_properties(../Libraries/LibC/ssp_nonshared.cpp PROPERTIES COMPILE_FLAGS "-fno-stack-protector")
# Prevent GCC from removing null checks by marking the `FILE*` argument non-null
set_source_files_properties(../Libraries/LibC/stdio.cpp PROPERTIES COMPILE_FLAGS "-fno-builtin-fputc -fno-builtin-fputs -fno-builtin-fwrite")

# Prevent naively implemented string functions (like strlen) from being "optimized" into a call to themselves.
if (CMAKE_CXX_COMPILER_ID STREQUAL "GNU")
    set_source_files_properties(../Libraries/LibC/string.cpp ../Libraries/LibC/wchar.cpp
        PROPERTIES COMPILE_FLAGS "-fno-tree-loop-distribution -fno-tree-loop-distribute-patterns")
endif()

add_executable(Loader.so ${SOURCES})
add_dependencies(Loader.so install_libc_headers)

if (CMAKE_CXX_COMPILER_ID STREQUAL "GNU")
    target_link_libraries(Loader.so PRIVATE gcc)
elseif (CMAKE_CXX_COMPILER_ID MATCHES "Clang$")
    target_link_libraries(Loader.so PRIVATE clang_rt.builtins)
endif ()

target_link_libraries(Loader.so PRIVATE DynamicLoader_CompileOptions
    DynamicLoader_LibSystemStatic
    DynamicLoader_LibELF
    LibTimeZone)
target_link_options(Loader.so PRIVATE LINKER:--no-dynamic-linker)
# Don't confuse the coverage results by instrumenting Loader
target_link_libraries(Loader.so PRIVATE NoCoverage)

install(TARGETS Loader.so RUNTIME DESTINATION usr/lib/)

set(LOADER_SOURCES
    main.cpp
    misc.cpp
)

add_library(DynamicLoader_CompileOptions INTERFACE)
target_compile_options(DynamicLoader_CompileOptions INTERFACE -fno-rtti -fpie)
target_link_options(DynamicLoader_CompileOptions INTERFACE -nostdlib -fpie)
target_compile_definitions(DynamicLoader_CompileOptions INTERFACE NO_TLS)
add_dependencies(DynamicLoader_CompileOptions install_libc_headers)

file(GLOB LIBC_SOURCES1 "../Libraries/LibC/*.cpp")
file(GLOB LIBC_SOURCES2 "../Libraries/LibC/*/*.cpp")

set(ARCH_FOLDER "${SERENITY_ARCH}")

file(GLOB LIBC_SOURCES3 "../Libraries/LibC/arch/${ARCH_FOLDER}/*.S")
set(LIBC_SOURCES3 ${LIBC_SOURCES3} "../Libraries/LibC/arch/${ARCH_FOLDER}/fenv.cpp")
if ("${SERENITY_ARCH}" STREQUAL "x86_64")
    set(LIBC_SOURCES3 ${LIBC_SOURCES3} "../Libraries/LibC/arch/x86_64/memset.cpp")
endif()

file(GLOB LIBSYSTEM_SOURCES "../Libraries/LibSystem/*.cpp")

if (ENABLE_UNDEFINED_SANITIZER)
    set(LOADER_SOURCES ${LOADER_SOURCES} ../Libraries/LibSanitizer/UBSanitizer.cpp)
endif()

# pthread requires thread local storage, which DynamicLoader does not have.
list(FILTER LIBC_SOURCES1 EXCLUDE REGEX ".*/LibC/(pthread|semaphore)\\.cpp")

add_definitions(-D_DYNAMIC_LOADER)

set(SOURCES ${LOADER_SOURCES} ${AK_SOURCES} ${LIBC_SOURCES1} ${LIBC_SOURCES2} ${LIBC_SOURCES3} ${LIBSYSTEM_SOURCES})

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

target_link_libraries(Loader.so PRIVATE DynamicLoader_CompileOptions DynamicLoader_LibELF LibTimeZone)
target_link_options(Loader.so PRIVATE LINKER:--no-dynamic-linker)
# Don't confuse the coverage results by instrumenting Loader
target_link_libraries(Loader.so PRIVATE NoCoverage)

install(TARGETS Loader.so RUNTIME DESTINATION usr/lib/)

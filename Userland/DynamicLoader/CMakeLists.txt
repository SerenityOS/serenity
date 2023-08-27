set(LOADER_SOURCES
    main.cpp
    misc.cpp
)

file(GLOB ELF_SOURCES "../Libraries/LibELF/*.cpp")
file(GLOB LIBC_SOURCES1 "../Libraries/LibC/*.cpp")
file(GLOB LIBC_SOURCES2 "../Libraries/LibC/*/*.cpp")

set(ARCH_FOLDER "${SERENITY_ARCH}")

file(GLOB LIBC_SOURCES3 "../Libraries/LibC/arch/${ARCH_FOLDER}/*.S")
set(ELF_SOURCES ${ELF_SOURCES} "../Libraries/LibELF/Arch/${ARCH_FOLDER}/entry.S" "../Libraries/LibELF/Arch/${ARCH_FOLDER}/plt_trampoline.S")
if ("${SERENITY_ARCH}" STREQUAL "x86_64")
    set(LIBC_SOURCES3 ${LIBC_SOURCES3} "../Libraries/LibC/arch/x86_64/memset.cpp")
elseif ("${SERENITY_ARCH}" STREQUAL "aarch64")
    set(ELF_SOURCES ${ELF_SOURCES} "../Libraries/LibELF/Arch/aarch64/tls.S")
endif()

file(GLOB LIBSYSTEM_SOURCES "../Libraries/LibSystem/*.cpp")

if (ENABLE_UNDEFINED_SANITIZER)
    set(LOADER_SOURCES ${LOADER_SOURCES} ../Libraries/LibSanitizer/UBSanitizer.cpp)
endif()

# pthread requires thread local storage, which DynamicLoader does not have.
list(FILTER LIBC_SOURCES1 EXCLUDE REGEX ".*/LibC/(pthread|semaphore)\\.cpp")

add_definitions(-D_DYNAMIC_LOADER)

set(SOURCES ${LOADER_SOURCES} ${AK_SOURCES} ${ELF_SOURCES} ${LIBC_SOURCES1} ${LIBC_SOURCES2} ${LIBC_SOURCES3} ${LIBSYSTEM_SOURCES})

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fno-rtti -nostdlib -pie -fpie -DNO_TLS")

if ("${SERENITY_ARCH}" STREQUAL "aarch64")
    # On aarch64 the stack protector would be accessed before the Loader can relocate itself.
    set_source_files_properties(main.cpp ../Libraries/LibELF/Relocation.cpp PROPERTIES COMPILE_FLAGS "-fno-stack-protector")
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

# Note: Don't confuse the coverage results by instrumenting Loader
target_link_libraries(Loader.so PRIVATE LibTimeZone NoCoverage)
target_link_options(Loader.so PRIVATE LINKER:--no-dynamic-linker)
install(TARGETS Loader.so RUNTIME DESTINATION usr/lib/)

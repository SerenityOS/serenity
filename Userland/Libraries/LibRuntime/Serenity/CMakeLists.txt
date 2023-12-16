set(LIBRUNTIME_SOURCES
    # ExtraLibCDefinitions.o must be passed to the linker before any other object file which has
    # static initializers for malloc/stdio to work in them.
    ExtraLibCDefinitions.cpp

    LibC/errno.cpp
    LibC/malloc.cpp
    LibC/stdio.cpp
    LibC/string.cpp
    LibC/strings.cpp
    Exceptions.cpp
    ItaniumCXXABI.cpp
    Mutex.cpp
    PosixThreadSupport.cpp
    PossiblyThrowingCallback.cpp
    StackProtector.cpp
    System.cpp
)

# Prevent GCC from removing null checks by marking the `FILE*` argument non-null
set_source_files_properties(LibC/stdio.cpp PROPERTIES COMPILE_FLAGS "-fno-builtin-fwrite -fno-builtin-fputc -fno-builtin-fputs")
# Prevent naively implemented string functions (like strlen) from being "optimized" into a call to themselves.
set_source_files_properties(LibC/string.cpp PROPERTIES COMPILE_FLAGS -fno-builtin)
set_source_files_properties(PossiblyThrowingCallback.cpp PROPERTIES COMPILE_FLAGS -fexceptions)
set_source_files_properties(StackProtector PROPERTIES COMPILE_FLAGS -fno-stack-protector)

if (SERENITY_ARCH STREQUAL "x86_64")
    set(LIBRUNTIME_ARCH_SOURCES
        LibC/Arch/x86_64/memset.cpp
        LibC/Arch/x86_64/memset.S
    )
endif()

list(APPEND LIBRUNTIME_SOURCES ${LIBRUNTIME_ARCH_SOURCES})
list(TRANSFORM LIBRUNTIME_SOURCES PREPEND "${CMAKE_CURRENT_SOURCE_DIR}/")
set(LIBRUNTIME_SOURCES ${LIBRUNTIME_SOURCES} PARENT_SCOPE)

# ===== Startup files =====
add_library(crt0 STATIC LibC/crt0.cpp)
add_dependencies(crt0 install_libc_headers)
target_link_libraries(crt0 PRIVATE NoCoverage)
add_custom_command(
    TARGET crt0
    COMMAND "${CMAKE_COMMAND}" -E copy $<TARGET_OBJECTS:crt0> ${CMAKE_INSTALL_PREFIX}/usr/lib/crt0.o
)

add_library(crt0_shared STATIC LibC/crt0_shared.cpp)
add_dependencies(crt0_shared install_libc_headers)
target_link_libraries(crt0_shared PRIVATE NoCoverage)
add_custom_command(
    TARGET crt0_shared
    COMMAND "${CMAKE_COMMAND}" -E copy $<TARGET_OBJECTS:crt0_shared> ${CMAKE_INSTALL_PREFIX}/usr/lib/crt0_shared.o
)

add_library(crti STATIC LibC/Arch/${SERENITY_ARCH}/crti.S)
target_link_libraries(crti PRIVATE NoCoverage)
add_custom_command(
    TARGET crti
    COMMAND "${CMAKE_COMMAND}" -E copy $<TARGET_OBJECTS:crti> ${CMAKE_INSTALL_PREFIX}/usr/lib/crti.o
)

add_library(crtn STATIC LibC/Arch/${SERENITY_ARCH}/crtn.S)
target_link_libraries(crtn PRIVATE NoCoverage)
add_custom_command(
    TARGET crtn
    COMMAND "${CMAKE_COMMAND}" -E copy $<TARGET_OBJECTS:crtn> ${CMAKE_INSTALL_PREFIX}/usr/lib/crtn.o
)

# ===== Static build used for DynamicLoader =====
add_library(DynamicLoaderRuntime ${AK_SOURCES} ${LIBRUNTIME_SOURCES})
add_dependencies(DynamicLoaderRuntime install_libc_headers)

target_compile_options(DynamicLoaderRuntime PUBLIC -fno-rtti)

target_compile_definitions(DynamicLoaderRuntime PRIVATE _DYNAMIC_LOADER NO_TLS)
target_link_libraries(DynamicLoaderRuntime PRIVATE NoCoverage)
if (CMAKE_CXX_COMPILER_ID STREQUAL "GNU")
    target_link_libraries(DynamicLoaderRuntime PRIVATE gcc.a)
elseif (CMAKE_CXX_COMPILER_ID MATCHES "Clang$")
    target_link_libraries(DynamicLoaderRuntime PRIVATE clang_rt.builtins.a)
endif()

# ===== Default dynamic build =====
add_library(LibRuntime SHARED ${LIBRUNTIME_SOURCES} ${AK_SOURCES})
add_dependencies(LibRuntime install_libc_headers crt0 crt0_shared crti crtn)

target_compile_options(LibRuntime PRIVATE -ftls-model=initial-exec)

# FIXME: LibRuntime is not yet prepared for coverage symbols.
target_link_libraries(LibRuntime PRIVATE NoCoverage LibSystem)
if (ENABLE_UNDEFINED_SANITIZER)
    target_link_libraries(LibRuntime PRIVATE LibUBSanitizerStatic)
endif()
target_link_options(LibRuntime PRIVATE -nodefaultlibs -Wl,--no-undefined)
# NOTE: Linking with dynamic versions of compiler/C++ runtime here would result in a dependency cycle.
if (CMAKE_CXX_COMPILER_ID STREQUAL "GNU")
    target_link_libraries(LibRuntime PRIVATE gcc.a supc++.a)
elseif (CMAKE_CXX_COMPILER_ID MATCHES "Clang$")
    target_link_libraries(LibRuntime PRIVATE clang_rt.builtins.a c++abi.a)
endif()

install(TARGETS LibRuntime DESTINATION ${CMAKE_INSTALL_LIBDIR} OPTIONAL)
set_target_properties(LibRuntime PROPERTIES OUTPUT_NAME runtime)

target_link_directories(LibC PUBLIC ${CMAKE_CURRENT_BINARY_DIR})

# ===== Static build for use in ports =====
add_library(LibRuntimeWithoutDeps ${AK_SOURCES} ${LIBRUNTIME_SOURCES})
add_dependencies(LibRuntimeWithoutDeps install_libc_headers LibSystem LibUBSanitizerStatic)

target_compile_options(LibRuntimeWithoutDeps PRIVATE -ftls-model=initial-exec)

target_link_libraries(LibRuntimeWithoutDeps PRIVATE NoCoverage)
if (CMAKE_CXX_COMPILER_ID STREQUAL "GNU")
    target_link_libraries(LibRuntimeWithoutDeps PRIVATE gcc.a supc++.a)
elseif (CMAKE_CXX_COMPILER_ID MATCHES "Clang$")
    target_link_libraries(LibRuntimeWithoutDeps PRIVATE clang_rt.builtins.a c++abi.a)
endif()

add_custom_target(LibRuntimeStatic
    COMMAND ${CMAKE_AR} -x $<TARGET_FILE:LibRuntimeWithoutDeps>
    COMMAND ${CMAKE_AR} -x $<TARGET_FILE:LibSystemStatic>
    COMMAND ${CMAKE_AR} -x $<TARGET_FILE:LibUBSanitizerStatic>
    COMMAND ${CMAKE_AR} -rcs ${CMAKE_CURRENT_BINARY_DIR}/libruntime.a *.o
    WORKING_DIRECTORY ${CMAKE_CURRENT_BINARY_DIR}
    DEPENDS LibRuntimeWithoutDeps LibSystemStatic LibUBSanitizerStatic
)
add_dependencies(LibRuntime LibRuntimeStatic) # In order to trigger build of LibRuntimeStatic
install(FILES ${CMAKE_CURRENT_BINARY_DIR}/libruntime.a DESTINATION ${CMAKE_INSTALL_PREFIX}/usr/lib/)

set(SOURCES
    main.cpp
    misc.cpp
)

add_library(DynamicLoader_CompileOptions INTERFACE)
target_compile_definitions(DynamicLoader_CompileOptions INTERFACE NO_TLS _DYNAMIC_LOADER)
target_compile_options(DynamicLoader_CompileOptions INTERFACE $<$<COMPILE_LANGUAGE:CXX>:-fno-rtti> -fpie -ffunction-sections -fdata-sections)
target_link_options(DynamicLoader_CompileOptions INTERFACE -nolibc -nostdlib++ -nostartfiles -static-libgcc -fpie -Wl,--gc-sections)
target_link_options(DynamicLoader_CompileOptions INTERFACE -fno-sanitize=undefined) # Sanitizer runtime is linked in manually

add_executable(Loader.so ${SOURCES})

# Mixing C and C++ results in linker errors with GCC > 16 and CMake < 4.0.
# The issue is fixed upstream in https://gitlab.kitware.com/cmake/cmake/-/merge_requests/12027.
# FIXME: Remove this workaround once we require a CMake version >= 4.1.
list(REMOVE_ITEM CMAKE_C_IMPLICIT_LINK_LIBRARIES "gcc_s_asneeded")

target_link_libraries(Loader.so PRIVATE DynamicLoader_CompileOptions DynamicLoader_LibELF DynamicLoader_LibCoreArgsParser)
if (ENABLE_UNDEFINED_SANITIZER)
    target_link_libraries(Loader.so PRIVATE DynamicLoader_LibUBSanitizer)
endif()
target_link_options(Loader.so PRIVATE LINKER:--no-dynamic-linker)
# Don't confuse the coverage results by instrumenting Loader
target_link_libraries(Loader.so PRIVATE NoCoverage)

install(TARGETS Loader.so RUNTIME DESTINATION usr/lib/)

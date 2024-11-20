set(SOURCES
    main.cpp
    misc.cpp
)

add_library(DynamicLoader_CompileOptions INTERFACE)
target_compile_definitions(DynamicLoader_CompileOptions INTERFACE NO_TLS _DYNAMIC_LOADER)
target_compile_options(DynamicLoader_CompileOptions INTERFACE -fno-rtti -fpie -ffunction-sections -fdata-sections)
target_link_options(DynamicLoader_CompileOptions INTERFACE -nolibc -nostdlib++ -nostartfiles -static-libgcc -fpie -Wl,--gc-sections)
target_link_options(DynamicLoader_CompileOptions INTERFACE -fno-sanitize=undefined) # Sanitizer runtime is linked in manually

add_executable(Loader.so ${SOURCES})

target_link_libraries(Loader.so PRIVATE DynamicLoader_CompileOptions DynamicLoader_LibELF DynamicLoader_LibCoreArgsParser)
if (ENABLE_UNDEFINED_SANITIZER)
    target_link_libraries(Loader.so PRIVATE DynamicLoader_LibUBSanitizer)
endif()
target_link_options(Loader.so PRIVATE LINKER:--no-dynamic-linker)
# Don't confuse the coverage results by instrumenting Loader
target_link_libraries(Loader.so PRIVATE NoCoverage)

install(TARGETS Loader.so RUNTIME DESTINATION usr/lib/)

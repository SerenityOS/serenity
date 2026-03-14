set(SOURCES
    syscall.cpp
)

serenity_libc(LibSystem system)
target_link_options(LibSystem PRIVATE -nostdlib)

add_library(DynamicLoader_LibSystem STATIC ${SOURCES})
target_link_libraries(DynamicLoader_LibSystem PUBLIC DynamicLoader_CompileOptions)

serenity_static_lib(LibSystemStatic)
target_link_libraries(LibSystemStatic PRIVATE LibCoreStatic LibURLStatic)

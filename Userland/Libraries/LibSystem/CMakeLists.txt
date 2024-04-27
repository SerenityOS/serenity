set(SOURCES
    syscall.cpp
)

serenity_libc(LibSystem system)
add_dependencies(LibSystem install_libc_headers)
target_link_options(LibSystem PRIVATE -nostdlib)

add_library(DynamicLoader_LibSystem STATIC ${SOURCES})
target_link_libraries(DynamicLoader_LibSystem PUBLIC DynamicLoader_CompileOptions)

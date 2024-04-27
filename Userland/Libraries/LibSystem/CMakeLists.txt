set(SOURCES
    syscall.cpp
)

serenity_libc(LibSystem system)
add_dependencies(LibSystem install_libc_headers)
target_link_options(LibSystem PRIVATE -nostdlib)

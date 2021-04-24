set(SOURCES
    dlfcn.cpp
)

serenity_libc(LibDl dl)
target_link_libraries(LibDl LibC)

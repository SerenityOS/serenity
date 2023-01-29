set(SOURCES
    Compiler.cpp
    Linker.cpp
)

serenity_lib(LibGLSL glsl)
target_link_libraries(LibGLSL PRIVATE LibGPU)

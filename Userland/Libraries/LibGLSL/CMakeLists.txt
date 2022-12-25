set(SOURCES
    Compiler.cpp
    Lexer.cpp
    Linker.cpp
    Token.cpp
)

serenity_lib(LibGLSL glsl)
target_link_libraries(LibGLSL PRIVATE LibGPU)

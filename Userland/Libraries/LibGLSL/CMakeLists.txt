set(SOURCES
    AST.cpp
    Compiler.cpp
    Lexer.cpp
    Linker.cpp
    Parser.cpp
    Preprocessor.cpp
    Token.cpp
)

serenity_lib(LibGLSL glsl)
target_link_libraries(LibGLSL PRIVATE LibGPU)

install(DIRECTORY Tests/ DESTINATION home/anon/Tests/glsl-tests)

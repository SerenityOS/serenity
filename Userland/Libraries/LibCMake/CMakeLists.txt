set(SOURCES
    Lexer.cpp
    Token.cpp
)

serenity_lib(LibCMake cmake)
target_link_libraries(LibCMake PRIVATE LibSyntax)

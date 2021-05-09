set(SOURCES
    Lexer.cpp
    Parser.cpp
    SyntaxHighlighter.cpp
    Token.cpp
)

serenity_lib(LibSQL sql)
target_link_libraries(LibSQL LibCore LibSyntax)

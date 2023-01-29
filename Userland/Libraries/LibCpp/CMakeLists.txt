set(SOURCES
    AST.cpp
    Lexer.cpp
    Parser.cpp
    Preprocessor.cpp
    SemanticSyntaxHighlighter.cpp
    SyntaxHighlighter.cpp
    Token.cpp
)

serenity_lib(LibCpp cpp)
target_link_libraries(LibCpp PRIVATE LibSyntax LibDiff)

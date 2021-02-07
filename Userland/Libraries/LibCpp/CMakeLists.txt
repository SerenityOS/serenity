set(SOURCES
    AST.cpp
    Lexer.cpp
    Parser.cpp
    SyntaxHighlighter.cpp
)

serenity_lib(LibCpp cpp)
target_link_libraries(LibCpp LibC)

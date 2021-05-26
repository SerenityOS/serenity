set(SOURCES
    BTree.cpp
    BTreeIterator.cpp
    Database.cpp
    Heap.cpp
    Key.cpp
    Lexer.cpp
    Meta.cpp
    Parser.cpp
    SyntaxHighlighter.cpp
    Token.cpp
    TreeNode.cpp
    Tuple.cpp
    Value.cpp
)

serenity_lib(LibSQL sql)
target_link_libraries(LibSQL LibCore LibSyntax)

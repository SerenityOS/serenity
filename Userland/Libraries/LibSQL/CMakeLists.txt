set(SOURCES
    AST/CreateSchema.cpp
    AST/CreateTable.cpp
    AST/Lexer.cpp
    AST/Parser.cpp
    AST/SyntaxHighlighter.cpp
    AST/Token.cpp
    BTree.cpp
    BTreeIterator.cpp
    Database.cpp
    HashIndex.cpp
    Heap.cpp
    Index.cpp
    Key.cpp
    Meta.cpp
    Row.cpp
    TreeNode.cpp
    Tuple.cpp
    Value.cpp
    )

serenity_lib(LibSQL sql)
target_link_libraries(LibSQL LibCore LibSyntax)

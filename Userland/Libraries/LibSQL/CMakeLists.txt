set(SOURCES
    AST/CreateSchema.cpp
    AST/CreateTable.cpp
    AST/Expression.cpp
    AST/Insert.cpp
    AST/Lexer.cpp
    AST/Parser.cpp
    AST/Select.cpp
    AST/Statement.cpp
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
    Serializer.cpp
    SQLClient.cpp
    TreeNode.cpp
    Tuple.cpp
    Value.cpp
    )

set(GENERATED_SOURCES
    ../../Services/SQLServer/SQLClientEndpoint.h
    ../../Services/SQLServer/SQLServerEndpoint.h
    )

serenity_lib(LibSQL sql)
target_link_libraries(LibSQL LibCore LibSyntax)

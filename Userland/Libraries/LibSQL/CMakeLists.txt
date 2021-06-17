set(SOURCES
        BTree.cpp
        BTreeIterator.cpp
        Database.cpp
        HashIndex.cpp
        Heap.cpp
        Index.cpp
        Key.cpp
        Lexer.cpp
        Meta.cpp
        Parser.cpp
        Row.cpp
        SyntaxHighlighter.cpp
        Token.cpp
        TreeNode.cpp
        Tuple.cpp
        Value.cpp
        )

serenity_lib(LibSQL sql)
target_link_libraries(LibSQL LibCore LibSyntax)

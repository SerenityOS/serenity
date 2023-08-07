set(SOURCES
    AST/CreateSchema.cpp
    AST/CreateTable.cpp
    AST/Delete.cpp
    AST/Describe.cpp
    AST/Expression.cpp
    AST/Insert.cpp
    AST/Lexer.cpp
    AST/Parser.cpp
    AST/Select.cpp
    AST/Statement.cpp
    AST/SyntaxHighlighter.cpp
    AST/Token.cpp
    AST/Update.cpp
    BTree.cpp
    BTreeIterator.cpp
    Database.cpp
    Heap.cpp
    Index.cpp
    Key.cpp
    Meta.cpp
    Result.cpp
    ResultSet.cpp
    Row.cpp
    Serializer.cpp
    SQLClient.cpp
    TreeNode.cpp
    Tuple.cpp
    Value.cpp
)

if (NOT SERENITYOS)
    compile_ipc(../../Services/SQLServer/SQLClient.ipc ../../Services/SQLServer/SQLClientEndpoint.h)
    compile_ipc(../../Services/SQLServer/SQLServer.ipc ../../Services/SQLServer/SQLServerEndpoint.h)
endif()

set(GENERATED_SOURCES
    ../../Services/SQLServer/SQLClientEndpoint.h
    ../../Services/SQLServer/SQLServerEndpoint.h
)

serenity_lib(LibSQL sql)
target_link_libraries(LibSQL PRIVATE LibCore LibFileSystem LibIPC LibSyntax LibRegex)

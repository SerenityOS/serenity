set(SOURCES
    AST.cpp
    Builtin.cpp
    Formatter.cpp
    ImmediateFunctions.cpp
    Job.cpp
    NodeVisitor.cpp
    Parser.cpp
    PosixLexer.cpp
    PosixParser.cpp
    Shell.cpp
)

serenity_lib(LibShellMinimal shellminimal)
target_link_libraries(LibShellMinimal PRIVATE LibCoreMinimal LibCoreBasic LibRegex LibLine LibFileSystem)
target_sources(LibShellMinimal PRIVATE MinimalHighlight.cpp)

serenity_lib(LibShell shell)
target_link_libraries(LibShell PRIVATE LibCore LibRegex LibLine LibFileSystem LibSyntax LibURL)
target_sources(LibShell PRIVATE URLHighlight.cpp)

if (SERENITYOS)
    target_sources(LibShell PRIVATE SyntaxHighlighter.cpp)
    target_link_libraries(LibShell PRIVATE LibCodeComprehension)
endif()

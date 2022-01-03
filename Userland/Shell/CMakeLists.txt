serenity_component(
    Shell
    REQUIRED
    TARGETS Shell
)

set(SOURCES
        AST.cpp
        Builtin.cpp
        Formatter.cpp
        ImmediateFunctions.cpp
        Job.cpp
        NodeVisitor.cpp
        Parser.cpp
        Shell.cpp
        SyntaxHighlighter.cpp
        )

serenity_lib(LibShell shell)
target_link_libraries(LibShell LibCore LibLine LibSyntax LibRegex)

set(SOURCES
        main.cpp
        )

serenity_bin(Shell)
target_link_libraries(Shell LibShell LibMain)

install(DIRECTORY Tests/ DESTINATION usr/Tests/Shell
        PATTERN "Tests/*"
        PERMISSIONS OWNER_EXECUTE OWNER_WRITE OWNER_READ
        GROUP_EXECUTE GROUP_READ
        WORLD_EXECUTE WORLD_READ)

set(SOURCES
    AST.cpp
    Builtin.cpp
    Formatter.cpp
    ImmediateFunctions.cpp
    Job.cpp
    NodeVisitor.cpp
    Parser.cpp
    Shell.cpp
)

serenity_lib(LibShell shell)
target_link_libraries(LibShell LibCore LibLine LibRegex)

set(SOURCES
    main.cpp
)

serenity_bin(Shell)
target_link_libraries(Shell LibShell)

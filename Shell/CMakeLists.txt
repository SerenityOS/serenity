set(SOURCES
    AST.cpp
    Builtin.cpp
    Formatter.cpp
    Job.cpp
    NodeVisitor.cpp
    Parser.cpp
    Shell.cpp
    main.cpp
)

serenity_bin(Shell)
target_link_libraries(Shell LibCore LibLine)

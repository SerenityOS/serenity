set(SOURCES
    AST.cpp
    Builtin.cpp
    Job.cpp
    Parser.cpp
    Shell.cpp
    main.cpp
)

serenity_bin(Shell)
target_link_libraries(Shell LibCore LibLine)

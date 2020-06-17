set(SOURCES
    AST.cpp
    Builtin.cpp
    main.cpp
    Parser.cpp
    Shell.cpp
)

serenity_bin(Shell)
target_link_libraries(Shell LibCore LibLine)

set(SOURCES
    AST.cpp
    Parser.cpp
    Utils.cpp
    main.cpp
)

lagom_tool(ConfigReaderGenerator LIBS LagomMain)
target_compile_options(ConfigReaderGenerator PUBLIC -O0)

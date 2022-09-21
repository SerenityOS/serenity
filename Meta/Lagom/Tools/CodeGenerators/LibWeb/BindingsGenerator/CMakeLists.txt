set(SOURCES
    IDLGenerators.cpp
    main.cpp
)

lagom_tool(BindingsGenerator LIBS LibIDL)
target_compile_options(BindingsGenerator PUBLIC -g)

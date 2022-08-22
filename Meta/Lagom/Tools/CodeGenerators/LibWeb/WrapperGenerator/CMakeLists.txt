set(SOURCES
    IDLGenerators.cpp
    main.cpp
)

lagom_tool(WrapperGenerator LIBS LibIDL)
target_compile_options(WrapperGenerator PUBLIC -g)

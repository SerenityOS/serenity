set(SOURCES "")

lagom_tool(WrapperGenerator SOURCES
    IDLGenerators.cpp
    IDLParser.cpp
    main.cpp)

target_compile_options(WrapperGenerator PUBLIC -g)

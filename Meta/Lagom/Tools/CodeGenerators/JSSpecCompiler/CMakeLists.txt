set(SOURCES
    AST/ASTPrinting.cpp
    Parser/Lexer.cpp
    Parser/ParseError.cpp
    Parser/SpecParser.cpp
    Parser/TextParser.cpp
    Parser/XMLUtils.cpp
    main.cpp
)

lagom_tool(JSSpecCompiler LIBS LibMain LibXML)
target_include_directories(JSSpecCompiler PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})
target_compile_options(JSSpecCompiler PRIVATE -Wno-missing-field-initializers)

set(SOURCES
    AST/AST.cpp
    AST/ASTPrinting.cpp
    Compiler/FunctionCallCanonicalizationPass.cpp
    Compiler/GenericASTPass.cpp
    Compiler/IfBranchMergingPass.cpp
    Compiler/ReferenceResolvingPass.cpp
    Parser/Lexer.cpp
    Parser/ParseError.cpp
    Parser/SpecParser.cpp
    Parser/TextParser.cpp
    Parser/XMLUtils.cpp
    Function.cpp
    main.cpp
)

lagom_tool(JSSpecCompiler LIBS LibMain LibXML)
target_include_directories(JSSpecCompiler PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})
target_compile_options(JSSpecCompiler PRIVATE -Wno-missing-field-initializers)

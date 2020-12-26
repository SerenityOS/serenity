set(SOURCES
    AST.cpp
    Driver.cpp
    IR.cpp
    Lexer.cpp
    Option.cpp
    Parser.cpp
)

include_directories(../../)

serenity_lib(LibCpp cpp)
target_link_libraries(LibCpp LibMiddleEnd LibBackEnd LibCore)

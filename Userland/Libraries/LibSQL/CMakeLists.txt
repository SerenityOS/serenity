set(SOURCES
    Lexer.cpp
    Parser.cpp
    Token.cpp
)

serenity_lib(LibSQL sql)
target_link_libraries(LibSQL LibCore)

add_subdirectory(Tests)

set(SOURCES
    Lexer.cpp
    Token.cpp
)

serenity_lib(LibSQL sql)
target_link_libraries(LibSQL LibCore)

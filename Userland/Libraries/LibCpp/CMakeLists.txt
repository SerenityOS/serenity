set(SOURCES
    Lexer.cpp
)

serenity_lib(LibCpp cpp)
target_link_libraries(LibCpp LibC)

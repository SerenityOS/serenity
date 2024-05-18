set(SOURCES
    Parser.cpp
)

serenity_lib(LibJava java)
target_link_libraries(LibJava PRIVATE LibCore)

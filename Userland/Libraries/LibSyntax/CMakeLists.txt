set(SOURCES
    Highlighter.cpp
)

serenity_lib(LibSyntax syntax)
target_link_libraries(LibSyntax LibC)

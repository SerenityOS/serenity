set(SOURCES
    Highlighter.cpp
)

serenity_lib(LibSyntax syntax)

if (NOT BUILD_LAGOM AND NOT BUILD_LAGOM_TOOLS)
    serenity_lib_static(LibSyntaxStatic syntax)
endif()

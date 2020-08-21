set(SOURCES
    DebugInfo.cpp
    DebugSession.cpp
    Dwarf/AbbreviationsMap.cpp
    Dwarf/CompilationUnit.cpp
    Dwarf/DIE.cpp
    Dwarf/DwarfInfo.cpp
    Dwarf/Expression.cpp
    Dwarf/LineProgram.cpp
    StackFrameUtils.cpp
)

serenity_lib(LibDebug debug)
target_link_libraries(LibDebug LibC)

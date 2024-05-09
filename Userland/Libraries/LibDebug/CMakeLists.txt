set(SOURCES
    DebugInfo.cpp
    DebugSession.cpp
    Dwarf/AbbreviationsMap.cpp
    Dwarf/AddressRanges.cpp
    Dwarf/AttributeValue.cpp
    Dwarf/CompilationUnit.cpp
    Dwarf/DIE.cpp
    Dwarf/DwarfInfo.cpp
    Dwarf/LineProgram.cpp
    ProcessInspector.cpp
)

serenity_lib(LibDebug debug)
target_link_libraries(LibDebug PRIVATE LibCore LibELF LibFileSystem LibRegex)

set(SOURCES
    Instruction.cpp
    Types/Formatter.cpp   
)

serenity_lib(LibX86 x86)
target_link_libraries(LibX86 LibC)

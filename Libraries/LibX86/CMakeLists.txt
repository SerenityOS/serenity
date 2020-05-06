set(SOURCES
    Instruction.cpp
)

serenity_lib(LibX86 x86)
target_link_libraries(LibX86 LibC)

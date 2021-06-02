set(SOURCES
    X86Assembler.cpp
    InstructionBuffer.cpp
)

serenity_lib(LibJIT jit)
target_link_libraries(LibJIT LibC LibCore LibX86)

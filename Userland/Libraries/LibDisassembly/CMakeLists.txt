set(SOURCES
    x86/Instruction.cpp
    riscv64/Encoding.cpp
    riscv64/Formatting.cpp
)

serenity_lib(LibDisassembly disassembly)

set(SOURCES
    x86/Instruction.cpp
    riscv64/Encoding.cpp
    riscv64/Formatting.cpp
    riscv64/Instruction.cpp
    riscv64/IM.cpp
)

serenity_lib(LibDisassembly disassembly)

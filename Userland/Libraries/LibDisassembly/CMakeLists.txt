set(SOURCES
    x86/Instruction.cpp
    riscv64/A.cpp
    riscv64/Encoding.cpp
    riscv64/FD.cpp
    riscv64/Formatting.cpp
    riscv64/Instruction.cpp
    riscv64/IM.cpp
    riscv64/Priviledged.cpp
    riscv64/Zicsr.cpp
)

serenity_lib(LibDisassembly disassembly)

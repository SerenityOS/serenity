#pragma once

#include <AK/Types.h>

extern "C" void interrupt_common_asm_entry();

#define GENERATE_GENERIC_INTERRUPT_HANDLER_ASM_ENTRY(interrupt_vector, isr_number) \
    extern "C" void interrupt_##interrupt_vector##_asm_entry();                    \
    asm(".globl interrupt_" #interrupt_vector "_asm_entry\n"                       \
        "interrupt_" #interrupt_vector "_asm_entry:\n"                             \
        "    pushw $" #isr_number "\n"                                             \
        "    pushw $0\n"                                                           \
        "    jmp interrupt_common_asm_entry\n");

asm(
    ".globl interrupt_common_asm_entry\n"
    "interrupt_common_asm_entry: \n"
    "    pusha\n"
    "    pushl %ds\n"
    "    pushl %es\n"
    "    pushl %fs\n"
    "    pushl %gs\n"
    "    pushl %ss\n"
    "    mov $0x10, %ax\n"
    "    mov %ax, %ds\n"
    "    mov %ax, %es\n"
    "    cld\n"
    "    call handle_interrupt\n"
    "    add $0x4, %esp\n" // "popl %ss"
    "    popl %gs\n"
    "    popl %fs\n"
    "    popl %es\n"
    "    popl %ds\n"
    "    popa\n"
    "    add $0x4, %esp\n"
    "    iret\n");

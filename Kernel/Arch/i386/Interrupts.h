/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/Assertions.h>
#include <AK/Types.h>
#include <Kernel/Arch/i386/CPU.h>

extern "C" void interrupt_common_asm_entry();

#define GENERATE_GENERIC_INTERRUPT_HANDLER_ASM_ENTRY(isr_number) \
    extern "C" void interrupt_##isr_number##_asm_entry();        \
    asm(".globl interrupt_" #isr_number "_asm_entry\n"           \
        "interrupt_" #isr_number "_asm_entry:\n"                 \
        "    pushw $" #isr_number "\n"                           \
        "    pushw $0\n"                                         \
        "    jmp interrupt_common_asm_entry\n");

// clang-format off
asm(
    ".globl interrupt_common_asm_entry\n"
    "interrupt_common_asm_entry: \n"
    "    pusha\n"
    "    pushl %ds\n"
    "    pushl %es\n"
    "    pushl %fs\n"
    "    pushl %gs\n"
    "    pushl %ss\n"
    "    mov $" __STRINGIFY(GDT_SELECTOR_DATA0) ", %ax\n"
    "    mov %ax, %ds\n"
    "    mov %ax, %es\n"
    "    mov $" __STRINGIFY(GDT_SELECTOR_PROC) ", %ax\n"
    "    mov %ax, %fs\n"
    "    pushl %esp \n" // set TrapFrame::regs
    "    subl $" __STRINGIFY(TRAP_FRAME_SIZE - 4) ", %esp \n"
    "    movl %esp, %ebx \n" // save pointer to TrapFrame
    "    pushl %ebx \n"
    "    cld\n"
    "    call enter_trap \n"
    "    movl %ebx, 0(%esp) \n" // push pointer to TrapFrame
    "    call handle_interrupt\n"
    "    movl %ebx, 0(%esp) \n" // push pointer to TrapFrame
    ".globl common_trap_exit \n"
    "common_trap_exit: \n"
         // another thread may have handled this trap at this point, so don't
         // make assumptions about the stack other than there's a TrapFrame
         // and a pointer to it.
    "    call exit_trap \n"
    "    addl $" __STRINGIFY(TRAP_FRAME_SIZE + 4) ", %esp\n" // pop TrapFrame and pointer to it
    ".globl interrupt_common_asm_exit \n"
    "interrupt_common_asm_exit: \n"
    "    addl $4, %esp\n" // pop %ss
    "    popl %gs\n"
    "    popl %fs\n"
    "    popl %es\n"
    "    popl %ds\n"
    "    popa\n"
    "    addl $0x4, %esp\n" // skip exception_code, isr_number
    "    iret\n"
);
// clang-format on

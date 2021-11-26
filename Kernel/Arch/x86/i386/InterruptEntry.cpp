/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/x86/DescriptorTable.h>
#include <Kernel/Arch/x86/TrapFrame.h>

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
    "    mov %ax, %gs\n"
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

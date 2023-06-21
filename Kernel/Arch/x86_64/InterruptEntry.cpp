/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/x86_64/DescriptorTable.h>
#include <Kernel/Arch/x86_64/TrapFrame.h>

// clang-format off
asm(
    ".globl interrupt_common_asm_entry\n"
    "interrupt_common_asm_entry: \n"
    // save all the other registers
    "    pushq %r15\n"
    "    pushq %r14\n"
    "    pushq %r13\n"
    "    pushq %r12\n"
    "    pushq %r11\n"
    "    pushq %r10\n"
    "    pushq %r9\n"
    "    pushq %r8\n"
    "    pushq %rax\n"
    "    pushq %rcx\n"
    "    pushq %rdx\n"
    "    pushq %rbx\n"
    "    pushq %rsp\n"
    "    pushq %rbp\n"
    "    pushq %rsi\n"
    "    pushq %rdi\n"
    "    pushq %rsp \n" /* set TrapFrame::regs */
    "    subq $" __STRINGIFY(TRAP_FRAME_SIZE - 8) ", %rsp \n"
    "    movq %rsp, %rdi \n"
    "    cld\n"
    "    call enter_trap \n"
    "    movq %rsp, %rdi \n"
    "    call handle_interrupt \n"
    ".globl common_trap_exit \n"
    "common_trap_exit: \n"
    // another thread may have handled this trap at this point, so don't
    // make assumptions about the stack other than there's a TrapFrame.
    "    movq %rsp, %rdi \n"
    "    call exit_trap \n"
    "    addq $" __STRINGIFY(TRAP_FRAME_SIZE) ", %rsp\n" // pop TrapFrame
    ".globl interrupt_common_asm_exit \n"
    "interrupt_common_asm_exit: \n"
    "    popq %rdi\n"
    "    popq %rsi\n"
    "    popq %rbp\n"
    "    addq $8, %rsp\n" // skip restoring rsp
    "    popq %rbx\n"
    "    popq %rdx\n"
    "    popq %rcx\n"
    "    popq %rax\n"
    "    popq %r8\n"
    "    popq %r9\n"
    "    popq %r10\n"
    "    popq %r11\n"
    "    popq %r12\n"
    "    popq %r13\n"
    "    popq %r14\n"
    "    popq %r15\n"
    "    addq $0x8, %rsp\n" // skip exception_code, isr_number
    "    iretq\n"
);
// clang-format on

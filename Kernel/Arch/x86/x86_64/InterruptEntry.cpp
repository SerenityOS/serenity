/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/x86/DescriptorTable.h>
#include <Kernel/Arch/x86/TrapFrame.h>

// clang-format off
asm(
    ".globl interrupt_common_asm_entry\n"
    "interrupt_common_asm_entry: \n"
    "    hlt \n" // FIXME
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

/*
 * Copyright (c) 2021, Owen Smith <yeeetari@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/x86/DescriptorTable.h>
#include <Kernel/Arch/x86/Processor.h>
#include <Kernel/Arch/x86/TrapFrame.h>

extern "C" void syscall_entry();
extern "C" [[gnu::naked]] void syscall_entry()
{
    // clang-format off
    asm(
        // Store the user stack, then switch to the kernel stack.
        "    movq %%rsp, %%gs:%c[user_stack] \n"
        "    movq %%gs:%c[kernel_stack], %%rsp \n"

        // Build RegisterState.
        "    pushq $0x1b \n" // User ss
        "    pushq %%gs:%c[user_stack] \n" // User rsp
        "    sti \n" // It's now safe to enable interrupts, but we can't index into gs after this point
        "    pushq %%r11 \n" // The CPU preserves the user rflags in r11
        "    pushq $0x23 \n" // User cs
        "    pushq %%rcx \n" // The CPU preserves the user IP in rcx
        "    pushq $0 \n"
        "    pushq %%r15 \n"
        "    pushq %%r14 \n"
        "    pushq %%r13 \n"
        "    pushq %%r12 \n"
        "    pushq %%r11 \n"
        "    pushq %%r10 \n"
        "    pushq %%r9 \n"
        "    pushq %%r8 \n"
        "    pushq %%rax \n"
        "    pushq %%rcx \n"
        "    pushq %%rdx \n"
        "    pushq %%rbx \n"
        "    pushq %%rsp \n"
        "    pushq %%rbp \n"
        "    pushq %%rsi \n"
        "    pushq %%rdi \n"

        "    pushq %%rsp \n" // TrapFrame::regs
        "    subq $" __STRINGIFY(TRAP_FRAME_SIZE - 8) ", %%rsp \n"
        "    movq %%rsp, %%rdi \n"
        "    call enter_trap_no_irq \n"
        "    movq %%rsp, %%rdi \n"
        "    call syscall_handler \n"
        "    movq %%rsp, %%rdi \n"
        "    call exit_trap \n"
        "    addq $" __STRINGIFY(TRAP_FRAME_SIZE) ", %%rsp \n" // Pop TrapFrame

        "    popq %%rdi \n"
        "    popq %%rsi \n"
        "    popq %%rbp \n"
        "    addq $8, %%rsp \n" // Skip restoring kernel rsp
        "    popq %%rbx \n"
        "    popq %%rdx \n"
        "    popq %%rcx \n"
        "    popq %%rax \n"
        "    popq %%r8 \n"
        "    popq %%r9 \n"
        "    popq %%r10 \n"
        "    popq %%r11 \n"
        "    popq %%r12 \n"
        "    popq %%r13 \n"
        "    popq %%r14 \n"
        "    popq %%r15 \n"
        "    addq $8, %%rsp \n"
        "    popq %%rcx \n"
        "    addq $16, %%rsp \n"

        // Disable interrupts before we restore the user stack pointer. sysret will re-enable interrupts when it restores
        // rflags.
        "    cli \n"
        "    popq %%rsp \n"
        "    sysretq \n"
    :: [user_stack] "i"(Kernel::Processor::user_stack_offset()), [kernel_stack] "i"(Kernel::Processor::kernel_stack_offset()));
    // clang-format on
}

/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StdLibExtras.h>
#include <Kernel/Arch/Processor.h>
#include <Kernel/Arch/x86/TrapFrame.h>
#include <Kernel/Panic.h>
#include <Kernel/Process.h>
#include <Kernel/Random.h>
#include <Kernel/Scheduler.h>
#include <Kernel/Sections.h>
#include <Kernel/Thread.h>

namespace Kernel {

NAKED void thread_context_first_enter(void)
{
    // enter_thread_context returns to here first time a thread is executing
    asm(
        // switch_context will have pushed from_thread and to_thread to our news
        // stack prior to thread_context_first_enter() being called, and the
        // pointer to TrapFrame was the top of the stack before that
        "    popq %rdi \n" // from_thread (argument 0)
        "    popq %rsi \n" // to_thread (argument 1)
        "    popq %rdx \n" // pointer to TrapFrame (argument 2)
        "    cld \n"
        "    call context_first_init \n"
        "    jmp common_trap_exit \n");
};

NAKED void do_assume_context(Thread*, u32)
{
    // clang-format off
    // FIXME: I hope (Thread* thread, u32 flags) aren't compiled away
    asm(
        "    movq %rdi, %r12 \n" // save thread ptr
        "    movq %rsi, %r13 \n" // save flags
        // We're going to call Processor::init_context, so just make sure
        // we have enough stack space so we don't stomp over it
        "    subq $(" __STRINGIFY(16 + REGISTER_STATE_SIZE + TRAP_FRAME_SIZE + 8) "), %rsp \n"
        "    cld \n"
        "    call do_init_context \n"
        "    movq %rax, %rsp \n"                          // move stack pointer to what Processor::init_context set up for us
        "    movq %r12, %rdi \n"                          // to_thread
        "    movq %r12, %rsi \n"                          // from_thread
        "    pushq %r12 \n"                               // to_thread (for thread_context_first_enter)
        "    pushq %r12 \n"                               // from_thread (for thread_context_first_enter)
        "    leaq thread_context_first_enter(%rip), %r12 \n" // should be same as regs.rip
        "    pushq %r12 \n"
        "    jmp enter_thread_context \n");
    // clang-format on
}

StringView Processor::platform_string()
{
    return "x86_64"sv;
}

// FIXME: For the most part this is a copy of the i386-specific function, get rid of the code duplication
FlatPtr Processor::init_context(Thread& thread, bool leave_crit)
{
    VERIFY(is_kernel_mode());
    VERIFY(g_scheduler_lock.is_locked());
    if (leave_crit) {
        // Leave the critical section we set up in in Process::exec,
        // but because we still have the scheduler lock we should end up with 1
        VERIFY(in_critical() == 2);
        m_in_critical = 1; // leave it without triggering anything or restoring flags
    }

    u64 kernel_stack_top = thread.kernel_stack_top();

    // Add a random offset between 0-256 (16-byte aligned)
    kernel_stack_top -= round_up_to_power_of_two(get_fast_random<u8>(), 16);

    u64 stack_top = kernel_stack_top;

    // TODO: handle NT?
    VERIFY((cpu_flags() & 0x24000) == 0); // Assume !(NT | VM)

    auto& regs = thread.regs();
    bool return_to_user = (regs.cs & 3) != 0;

    stack_top -= 1 * sizeof(u64);
    *reinterpret_cast<u64*>(kernel_stack_top - 2 * sizeof(u64)) = FlatPtr(&exit_kernel_thread);

    stack_top -= sizeof(RegisterState);

    // we want to end up 16-byte aligned, %rsp + 8 should be aligned
    stack_top -= sizeof(u64);
    *reinterpret_cast<u64*>(kernel_stack_top - sizeof(u64)) = 0;

    // set up the stack so that after returning from thread_context_first_enter()
    // we will end up either in kernel mode or user mode, depending on how the thread is set up
    // However, the first step is to always start in kernel mode with thread_context_first_enter
    RegisterState& iretframe = *reinterpret_cast<RegisterState*>(stack_top);
    iretframe.rdi = regs.rdi;
    iretframe.rsi = regs.rsi;
    iretframe.rbp = regs.rbp;
    iretframe.rsp = 0;
    iretframe.rbx = regs.rbx;
    iretframe.rdx = regs.rdx;
    iretframe.rcx = regs.rcx;
    iretframe.rax = regs.rax;
    iretframe.r8 = regs.r8;
    iretframe.r9 = regs.r9;
    iretframe.r10 = regs.r10;
    iretframe.r11 = regs.r11;
    iretframe.r12 = regs.r12;
    iretframe.r13 = regs.r13;
    iretframe.r14 = regs.r14;
    iretframe.r15 = regs.r15;
    iretframe.rflags = regs.rflags;
    iretframe.rip = regs.rip;
    iretframe.cs = regs.cs;
    if (return_to_user) {
        iretframe.userspace_rsp = regs.rsp;
        iretframe.userspace_ss = GDT_SELECTOR_DATA3 | 3;
    } else {
        iretframe.userspace_rsp = kernel_stack_top;
        iretframe.userspace_ss = 0;
    }

    // make space for a trap frame
    stack_top -= sizeof(TrapFrame);
    TrapFrame& trap = *reinterpret_cast<TrapFrame*>(stack_top);
    trap.regs = &iretframe;
    trap.prev_irq_level = 0;
    trap.next_trap = nullptr;

    stack_top -= sizeof(u64); // pointer to TrapFrame
    *reinterpret_cast<u64*>(stack_top) = stack_top + 8;

    if constexpr (CONTEXT_SWITCH_DEBUG) {
        if (return_to_user) {
            dbgln("init_context {} ({}) set up to execute at rip={}:{}, rsp={}, stack_top={}, user_top={}",
                thread,
                VirtualAddress(&thread),
                iretframe.cs, regs.rip,
                VirtualAddress(regs.rsp),
                VirtualAddress(stack_top),
                iretframe.userspace_rsp);
        } else {
            dbgln("init_context {} ({}) set up to execute at rip={}:{}, rsp={}, stack_top={}",
                thread,
                VirtualAddress(&thread),
                iretframe.cs, regs.rip,
                VirtualAddress(regs.rsp),
                VirtualAddress(stack_top));
        }
    }

    // make switch_context() always first return to thread_context_first_enter()
    // in kernel mode, so set up these values so that we end up popping iretframe
    // off the stack right after the context switch completed, at which point
    // control is transferred to what iretframe is pointing to.
    regs.rip = FlatPtr(&thread_context_first_enter);
    regs.rsp0 = kernel_stack_top;
    regs.rsp = stack_top;
    regs.cs = GDT_SELECTOR_CODE0;
    return stack_top;
}

void Processor::switch_context(Thread*& from_thread, Thread*& to_thread)
{
    VERIFY(!m_in_irq);
    VERIFY(m_in_critical == 1);
    VERIFY(is_kernel_mode());

    dbgln_if(CONTEXT_SWITCH_DEBUG, "switch_context --> switching out of: {} {}", VirtualAddress(from_thread), *from_thread);

    // m_in_critical is restored in enter_thread_context
    from_thread->save_critical(m_in_critical);

    // clang-format off
    // Switch to new thread context, passing from_thread and to_thread
    // through to the new context using registers rdx and rax
    asm volatile(
        // NOTE: changing how much we push to the stack affects thread_context_first_enter()!
        "pushfq \n"
        "pushq %%rbx \n"
        "pushq %%rcx \n"
        "pushq %%rbp \n"
        "pushq %%rsi \n"
        "pushq %%rdi \n"
        "pushq %%r8 \n"
        "pushq %%r9 \n"
        "pushq %%r10 \n"
        "pushq %%r11 \n"
        "pushq %%r12 \n"
        "pushq %%r13 \n"
        "pushq %%r14 \n"
        "pushq %%r15 \n"
        "movq %%rsp, %[from_rsp] \n"
        "leaq 1f(%%rip), %%rbx \n"
        "movq %%rbx, %[from_rip] \n"
        "movq %[to_rsp0], %%rbx \n"
        "movl %%ebx, %[tss_rsp0l] \n"
        "shrq $32, %%rbx \n"
        "movl %%ebx, %[tss_rsp0h] \n"
        "movq %[to_rsp], %%rsp \n"
        "pushq %[to_thread] \n"
        "pushq %[from_thread] \n"
        "pushq %[to_rip] \n"
        "cld \n"
        "movq 16(%%rsp), %%rsi \n"
        "movq 8(%%rsp), %%rdi \n"
        "jmp enter_thread_context \n"
        "1: \n"
        "popq %%rdx \n"
        "popq %%rax \n"
        "popq %%r15 \n"
        "popq %%r14 \n"
        "popq %%r13 \n"
        "popq %%r12 \n"
        "popq %%r11 \n"
        "popq %%r10 \n"
        "popq %%r9 \n"
        "popq %%r8 \n"
        "popq %%rdi \n"
        "popq %%rsi \n"
        "popq %%rbp \n"
        "popq %%rcx \n"
        "popq %%rbx \n"
        "popfq \n"
        : [from_rsp] "=m" (from_thread->regs().rsp),
        [from_rip] "=m" (from_thread->regs().rip),
        [tss_rsp0l] "=m" (m_tss.rsp0l),
        [tss_rsp0h] "=m" (m_tss.rsp0h),
        "=d" (from_thread), // needed so that from_thread retains the correct value
        "=a" (to_thread) // needed so that to_thread retains the correct value
        : [to_rsp] "g" (to_thread->regs().rsp),
        [to_rsp0] "g" (to_thread->regs().rsp0),
        [to_rip] "c" (to_thread->regs().rip),
        [from_thread] "d" (from_thread),
        [to_thread] "a" (to_thread)
        : "memory", "rbx"
    );
    // clang-format on

    dbgln_if(CONTEXT_SWITCH_DEBUG, "switch_context <-- from {} {} to {} {}", VirtualAddress(from_thread), *from_thread, VirtualAddress(to_thread), *to_thread);
}

UNMAP_AFTER_INIT void Processor::initialize_context_switching(Thread& initial_thread)
{
    VERIFY(initial_thread.process().is_kernel_process());

    auto& regs = initial_thread.regs();
    m_tss.iomapbase = sizeof(m_tss);
    m_tss.rsp0l = regs.rsp0 & 0xffffffff;
    m_tss.rsp0h = regs.rsp0 >> 32;

    m_scheduler_initialized = true;

    // clang-format off
    asm volatile(
        "movq %[new_rsp], %%rsp \n" // switch to new stack
        "pushq %[from_to_thread] \n" // to_thread
        "pushq %[from_to_thread] \n" // from_thread
        "pushq %[new_rip] \n" // save the entry rip to the stack
        "cld \n"
        "pushq %[cpu] \n" // push argument for init_finished before register is clobbered
        "call pre_init_finished \n"
        "pop %%rdi \n" // move argument for init_finished into place
        "call init_finished \n"
        "call post_init_finished \n"
        "movq 24(%%rsp), %%rdi \n" // move pointer to TrapFrame into place
        "call enter_trap_no_irq \n"
        "retq \n"
        :: [new_rsp] "g" (regs.rsp),
        [new_rip] "a" (regs.rip),
        [from_to_thread] "b" (&initial_thread),
        [cpu] "c" ((u64)id())
    );
    // clang-format on

    VERIFY_NOT_REACHED();
}

}

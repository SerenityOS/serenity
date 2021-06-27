/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StdLibExtras.h>
#include <Kernel/Arch/x86/Processor.h>
#include <Kernel/Arch/x86/TrapFrame.h>
#include <Kernel/Panic.h>
#include <Kernel/Process.h>
#include <Kernel/Random.h>
#include <Kernel/Sections.h>
#include <Kernel/Thread.h>

namespace Kernel {

extern "C" void thread_context_first_enter(void);
extern "C" void do_assume_context(Thread* thread, u32 flags);
extern "C" void exit_kernel_thread(void);

// clang-format off
asm(
// enter_thread_context returns to here first time a thread is executing
".globl thread_context_first_enter \n"
"thread_context_first_enter: \n"
// switch_context will have pushed from_thread and to_thread to our new
// stack prior to thread_context_first_enter() being called, and the
// pointer to TrapFrame was the top of the stack before that
"    popq %rdi \n" // from_thread (argument 0)
"    popq %rsi \n" // to_thread (argument 1)
"    popq %rdx \n" // pointer to TrapFrame (argument 2)
"    cld \n"
"    call context_first_init \n"
"    jmp common_trap_exit \n"
);
// clang-format on

#if ARCH(I386)
// clang-format off
asm(
".global do_assume_context \n"
"do_assume_context: \n"
"    movl 4(%esp), %ebx \n"
"    movl 8(%esp), %esi \n"
// We're going to call Processor::init_context, so just make sure
// we have enough stack space so we don't stomp over it
"    subl $(" __STRINGIFY(4 + REGISTER_STATE_SIZE + TRAP_FRAME_SIZE + 4) "), %esp \n"
"    pushl %esi \n"
"    pushl %ebx \n"
"    cld \n"
"    call do_init_context \n"
"    addl $8, %esp \n"
"    movl %eax, %esp \n" // move stack pointer to what Processor::init_context set up for us
"    pushl %ebx \n" // push to_thread
"    pushl %ebx \n" // push from_thread
"    pushl $thread_context_first_enter \n" // should be same as tss.eip
"    jmp enter_thread_context \n"
);
// clang-format on
#endif

String Processor::platform_string() const
{
    // FIXME: other platforms
    return "x86_64";
}

// FIXME: For the most part this is a copy of the i386-specific function, get rid of the code duplication
u32 Processor::init_context(Thread& thread, bool leave_crit)
{
    VERIFY(is_kernel_mode());
    VERIFY(g_scheduler_lock.is_locked());
    if (leave_crit) {
        // Leave the critical section we set up in in Process::exec,
        // but because we still have the scheduler lock we should end up with 1
        m_in_critical--; // leave it without triggering anything or restoring flags
        VERIFY(in_critical() == 1);
    }

    u64 kernel_stack_top = thread.kernel_stack_top();

    // Add a random offset between 0-256 (16-byte aligned)
    kernel_stack_top -= round_up_to_power_of_two(get_fast_random<u8>(), 16);

    u64 stack_top = kernel_stack_top;

    // TODO: handle NT?
    VERIFY((cpu_flags() & 0x24000) == 0); // Assume !(NT | VM)

    auto& regs = thread.regs();
    bool return_to_user = (regs.cs & 3) != 0;

    stack_top -= 2 * sizeof(u64);
    *reinterpret_cast<u64*>(kernel_stack_top - 2 * sizeof(u64)) = regs.rsp;
    *reinterpret_cast<u64*>(kernel_stack_top - 3 * sizeof(u64)) = FlatPtr(&exit_kernel_thread);

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
    iretframe.rflags = regs.rflags;
    iretframe.rip = regs.rip;
    iretframe.cs = regs.cs;
    iretframe.userspace_rsp = kernel_stack_top;
    iretframe.userspace_ss = 0;

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
    return stack_top;
}

void Processor::switch_context(Thread*& from_thread, Thread*& to_thread)
{
    VERIFY(!in_irq());
    VERIFY(m_in_critical == 1);
    VERIFY(is_kernel_mode());

    dbgln_if(CONTEXT_SWITCH_DEBUG, "switch_context --> switching out of: {} {}", VirtualAddress(from_thread), *from_thread);
    from_thread->save_critical(m_in_critical);

    PANIC("Context switching not implemented.");

    dbgln_if(CONTEXT_SWITCH_DEBUG, "switch_context <-- from {} {} to {} {}", VirtualAddress(from_thread), *from_thread, VirtualAddress(to_thread), *to_thread);

    Processor::current().restore_in_critical(to_thread->saved_critical());
}

void Processor::assume_context(Thread& thread, FlatPtr flags)
{
    dbgln_if(CONTEXT_SWITCH_DEBUG, "Assume context for thread {} {}", VirtualAddress(&thread), thread);

    VERIFY_INTERRUPTS_DISABLED();
    Scheduler::prepare_after_exec();
    // in_critical() should be 2 here. The critical section in Process::exec
    // and then the scheduler lock
    VERIFY(Processor::current().in_critical() == 2);

    (void)flags;
    TODO();

    VERIFY_NOT_REACHED();
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

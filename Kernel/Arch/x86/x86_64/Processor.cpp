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

#define ENTER_THREAD_CONTEXT_ARGS_SIZE (2 * 4) //  to_thread, from_thread
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
"    movl 8(%esp), %ebx \n" // save pointer to TrapFrame
"    cld \n"
"    call context_first_init \n"
"    addl $" __STRINGIFY(ENTER_THREAD_CONTEXT_ARGS_SIZE) ", %esp \n"
"    movl %ebx, 0(%esp) \n" // push pointer to TrapFrame
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

    u32 kernel_stack_top = thread.kernel_stack_top();

    // Add a random offset between 0-256 (16-byte aligned)
    kernel_stack_top -= round_up_to_power_of_two(get_fast_random<u8>(), 16);

    u32 stack_top = kernel_stack_top;

    // TODO: handle NT?
    VERIFY((cpu_flags() & 0x24000) == 0); // Assume !(NT | VM)

#if 0
    auto& tss = thread.tss();
    bool return_to_user = (tss.cs & 3) != 0;

    // make room for an interrupt frame
    if (!return_to_user) {
        // userspace_esp and userspace_ss are not popped off by iret
        // unless we're switching back to user mode
        stack_top -= sizeof(RegisterState) - 2 * sizeof(u32);

        // For kernel threads we'll push the thread function argument
        // which should be in tss.esp and exit_kernel_thread as return
        // address.
        stack_top -= 2 * sizeof(u32);
        *reinterpret_cast<u32*>(kernel_stack_top - 2 * sizeof(u32)) = tss.esp;
        *reinterpret_cast<u32*>(kernel_stack_top - 3 * sizeof(u32)) = FlatPtr(&exit_kernel_thread);
    } else {
        stack_top -= sizeof(RegisterState);
    }

    // we want to end up 16-byte aligned, %esp + 4 should be aligned
    stack_top -= sizeof(u32);
    *reinterpret_cast<u32*>(kernel_stack_top - sizeof(u32)) = 0;

    // set up the stack so that after returning from thread_context_first_enter()
    // we will end up either in kernel mode or user mode, depending on how the thread is set up
    // However, the first step is to always start in kernel mode with thread_context_first_enter
    RegisterState& iretframe = *reinterpret_cast<RegisterState*>(stack_top);
    // FIXME: copy state to be recovered through TSS

    // make space for a trap frame
    stack_top -= sizeof(TrapFrame);
    TrapFrame& trap = *reinterpret_cast<TrapFrame*>(stack_top);
    trap.regs = &iretframe;
    trap.prev_irq_level = 0;
    trap.next_trap = nullptr;

    stack_top -= sizeof(u32); // pointer to TrapFrame
    *reinterpret_cast<u32*>(stack_top) = stack_top + 4;

    if constexpr (CONTEXT_SWITCH_DEBUG) {
        if (return_to_user) {
            dbgln("init_context {} ({}) set up to execute at eip={}:{}, esp={}, stack_top={}, user_top={}:{}",
                thread,
                VirtualAddress(&thread),
                iretframe.cs, tss.eip,
                VirtualAddress(tss.esp),
                VirtualAddress(stack_top),
                iretframe.userspace_ss,
                iretframe.userspace_esp);
        } else {
            dbgln("init_context {} ({}) set up to execute at eip={}:{}, esp={}, stack_top={}",
                thread,
                VirtualAddress(&thread),
                iretframe.cs, tss.eip,
                VirtualAddress(tss.esp),
                VirtualAddress(stack_top));
        }
    }

    // make switch_context() always first return to thread_context_first_enter()
    // in kernel mode, so set up these values so that we end up popping iretframe
    // off the stack right after the context switch completed, at which point
    // control is transferred to what iretframe is pointing to.
    tss.eip = FlatPtr(&thread_context_first_enter);
    tss.esp0 = kernel_stack_top;
    tss.esp = stack_top;
    tss.cs = GDT_SELECTOR_CODE0;
    tss.ds = GDT_SELECTOR_DATA0;
    tss.es = GDT_SELECTOR_DATA0;
    tss.gs = GDT_SELECTOR_DATA0;
    tss.ss = GDT_SELECTOR_DATA0;
    tss.fs = GDT_SELECTOR_PROC;
#else
    TODO();
#endif
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

    auto& tss = initial_thread.tss();
    m_tss = tss;
#if 0
    m_tss.esp0 = tss.esp0;
    m_tss.ss0 = GDT_SELECTOR_DATA0;
    // user mode needs to be able to switch to kernel mode:
    m_tss.cs = m_tss.ds = m_tss.es = m_tss.gs = m_tss.ss = GDT_SELECTOR_CODE0 | 3;
    m_tss.fs = GDT_SELECTOR_PROC | 3;
#else
    TODO();
#endif

    m_scheduler_initialized = true;

    // FIXME: Context switching (see i386 impl)

    VERIFY_NOT_REACHED();
}

}

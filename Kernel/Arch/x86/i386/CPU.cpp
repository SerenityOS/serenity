/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/Types.h>

#include <Kernel/Arch/x86/CPU.h>
#include <Kernel/Arch/x86/Processor.h>
#include <Kernel/Arch/x86/TrapFrame.h>
#include <Kernel/KSyms.h>
#include <Kernel/Process.h>
#include <Kernel/Thread.h>

namespace Kernel {

// The compiler can't see the calls to these functions inside assembly.
// Declare them, to avoid dead code warnings.
extern "C" void enter_thread_context(Thread* from_thread, Thread* to_thread) __attribute__((used));
extern "C" void context_first_init(Thread* from_thread, Thread* to_thread, TrapFrame* trap) __attribute__((used));
extern "C" u32 do_init_context(Thread* thread, u32 flags) __attribute__((used));

extern "C" void enter_thread_context(Thread* from_thread, Thread* to_thread)
{
    VERIFY(from_thread == to_thread || from_thread->state() != Thread::Running);
    VERIFY(to_thread->state() == Thread::Running);

    bool has_fxsr = Processor::current().has_feature(CPUFeature::FXSR);
    Processor::set_current_thread(*to_thread);

    auto& from_regs = from_thread->regs();
    auto& to_regs = to_thread->regs();

    if (has_fxsr)
        asm volatile("fxsave %0"
                     : "=m"(from_thread->fpu_state()));
    else
        asm volatile("fnsave %0"
                     : "=m"(from_thread->fpu_state()));

    from_regs.fs = get_fs();
    from_regs.gs = get_gs();
    set_fs(to_regs.fs);
    set_gs(to_regs.gs);

    if (from_thread->process().is_traced())
        read_debug_registers_into(from_thread->debug_register_state());

    if (to_thread->process().is_traced()) {
        write_debug_registers_from(to_thread->debug_register_state());
    } else {
        clear_debug_registers();
    }

    auto& processor = Processor::current();
    auto& tls_descriptor = processor.get_gdt_entry(GDT_SELECTOR_TLS);
    tls_descriptor.set_base(to_thread->thread_specific_data());
    tls_descriptor.set_limit(to_thread->thread_specific_region_size());

    if (from_regs.cr3 != to_regs.cr3)
        write_cr3(to_regs.cr3);

    to_thread->set_cpu(processor.get_id());
    processor.restore_in_critical(to_thread->saved_critical());

    if (has_fxsr)
        asm volatile("fxrstor %0" ::"m"(to_thread->fpu_state()));
    else
        asm volatile("frstor %0" ::"m"(to_thread->fpu_state()));

    // TODO: ioperm?
}

extern "C" void context_first_init([[maybe_unused]] Thread* from_thread, [[maybe_unused]] Thread* to_thread, [[maybe_unused]] TrapFrame* trap)
{
    VERIFY(!are_interrupts_enabled());
    VERIFY(is_kernel_mode());

    dbgln_if(CONTEXT_SWITCH_DEBUG, "switch_context <-- from {} {} to {} {} (context_first_init)", VirtualAddress(from_thread), *from_thread, VirtualAddress(to_thread), *to_thread);

    VERIFY(to_thread == Thread::current());

    Scheduler::enter_current(*from_thread, true);

    // Since we got here and don't have Scheduler::context_switch in the
    // call stack (because this is the first time we switched into this
    // context), we need to notify the scheduler so that it can release
    // the scheduler lock. We don't want to enable interrupts at this point
    // as we're still in the middle of a context switch. Doing so could
    // trigger a context switch within a context switch, leading to a crash.
    Scheduler::leave_on_first_switch(trap->regs->eflags & ~0x200);
}

extern "C" u32 do_init_context(Thread* thread, u32 flags)
{
    VERIFY_INTERRUPTS_DISABLED();
    thread->regs().eflags = flags;
    return Processor::current().init_context(*thread, true);
}

}

#include "Scheduler.h"
#include "Process.h"
#include "system.h"
#include "RTC.h"
#include "i8253.h"
#include <AK/TemporaryChange.h>

//#define LOG_EVERY_CONTEXT_SWITCH
//#define SCHEDULER_DEBUG

static dword time_slice_for(Process::Priority priority)
{
    // One time slice unit == 1ms
    switch (priority) {
    case Process::HighPriority:
        return 50;
    case Process::NormalPriority:
        return 15;
    case Process::LowPriority:
        return 5;
    }
    ASSERT_NOT_REACHED();
}

Process* current;
Process* g_last_fpu_process;
Process* g_finalizer;
static Process* s_colonel_process;

struct TaskRedirectionData {
    word selector;
    TSS32 tss;
};
static TaskRedirectionData s_redirection;
static bool s_active;

bool Scheduler::is_active()
{
    return s_active;
}

bool Scheduler::pick_next()
{
    ASSERT_INTERRUPTS_DISABLED();
    ASSERT(!s_active);

    TemporaryChange<bool> change(s_active, true);

    ASSERT(s_active);

    if (!current) {
        // XXX: The first ever context_switch() goes to the idle process.
        //      This to setup a reliable place we can return to.
        return context_switch(*s_colonel_process);
    }

    // Check and unblock processes whose wait conditions have been met.
    Process::for_each([] (Process& process) {
        if (process.state() == Process::BlockedSleep) {
            if (process.wakeup_time() <= system.uptime)
                process.unblock();
            return true;
        }

        if (process.state() == Process::BlockedWait) {
            process.for_each_child([&process] (Process& child) {
                if (child.state() != Process::Dead)
                    return true;
                if (process.waitee_pid() == -1 || process.waitee_pid() == child.pid()) {
                    process.m_waitee_pid = child.pid();
                    process.unblock();
                    return false;
                }
                return true;
            });
            return true;
        }

        if (process.state() == Process::BlockedRead) {
            ASSERT(process.m_blocked_fd != -1);
            // FIXME: Block until the amount of data wanted is available.
            if (process.m_fds[process.m_blocked_fd].descriptor->can_read(process))
                process.unblock();
            return true;
        }

        if (process.state() == Process::BlockedWrite) {
            ASSERT(process.m_blocked_fd != -1);
            if (process.m_fds[process.m_blocked_fd].descriptor->can_write(process))
                process.unblock();
            return true;
        }

        if (process.state() == Process::BlockedConnect) {
            ASSERT(process.m_blocked_connecting_socket);
            if (process.m_blocked_connecting_socket->is_connected())
                process.unblock();
            return true;
        }

        if (process.state() == Process::BlockedSelect) {
            if (process.wakeup_requested()) {
                process.m_wakeup_requested = false;
                process.unblock();
                return true;
            }
            if (process.m_select_has_timeout) {
                auto now_sec = RTC::now();
                auto now_usec = PIT::ticks_since_boot() % 1000;
                if (now_sec > process.m_select_timeout.tv_sec || (now_sec == process.m_select_timeout.tv_sec && now_usec >= process.m_select_timeout.tv_usec)) {
                    process.unblock();
                    return true;
                }
            }
            for (int fd : process.m_select_read_fds) {
                if (process.m_fds[fd].descriptor->can_read(process)) {
                    process.unblock();
                    return true;
                }
            }
            for (int fd : process.m_select_write_fds) {
                if (process.m_fds[fd].descriptor->can_write(process)) {
                    process.unblock();
                    return true;
                }
            }
            return true;
        }

        if (process.state() == Process::Skip1SchedulerPass) {
            process.set_state(Process::Skip0SchedulerPasses);
            return true;
        }

        if (process.state() == Process::Skip0SchedulerPasses) {
            process.set_state(Process::Runnable);
            return true;
        }

        if (process.state() == Process::Dead) {
            if (current != &process && (!process.ppid() || !Process::from_pid(process.ppid()))) {
                auto name = process.name();
                auto pid = process.pid();
                auto exit_status = Process::reap(process);
                dbgprintf("reaped unparented process %s(%u), exit status: %u\n", name.characters(), pid, exit_status);
            }
            return true;
        }

        if (process.state() == Process::Dying) {
            ASSERT(g_finalizer);
            if (g_finalizer->state() == Process::BlockedLurking)
                g_finalizer->unblock();
            return true;
        }

        return true;
    });

    // Dispatch any pending signals.
    // FIXME: Do we really need this to be a separate pass over the process list?
    Process::for_each_living([] (auto& process) {
        if (!process.has_unmasked_pending_signals())
            return true;
        // We know how to interrupt blocked processes, but if they are just executing
        // at some random point in the kernel, let them continue. They'll be in userspace
        // sooner or later and we can deliver the signal then.
        // FIXME: Maybe we could check when returning from a syscall if there's a pending
        //        signal and dispatch it then and there? Would that be doable without the
        //        syscall effectively being "interrupted" despite having completed?
        if (process.in_kernel() && !process.is_blocked() && !process.is_stopped())
            return true;
        // NOTE: dispatch_one_pending_signal() may unblock the process.
        bool was_blocked = process.is_blocked();
        if (process.dispatch_one_pending_signal() == ShouldUnblockProcess::No)
            return true;
        if (was_blocked) {
            dbgprintf("Unblock %s(%u) due to signal\n", process.name().characters(), process.pid());
            process.m_was_interrupted_while_blocked = true;
            process.unblock();
        }
        return true;
    });

#ifdef SCHEDULER_DEBUG
    dbgprintf("Scheduler choices:\n");
    for (auto* process = g_processes->head(); process; process = process->next()) {
        //if (process->state() == Process::BlockedWait || process->state() == Process::BlockedSleep)
//            continue;
        dbgprintf("[K%x] % 12s %s(%u) @ %w:%x\n", process, to_string(process->state()), process->name().characters(), process->pid(), process->tss().cs, process->tss().eip);
    }
#endif

    auto* previous_head = g_processes->head();
    for (;;) {
        // Move head to tail.
        g_processes->append(g_processes->remove_head());
        auto* process = g_processes->head();

        if (process->state() == Process::Runnable || process->state() == Process::Running) {
#ifdef SCHEDULER_DEBUG
            dbgprintf("switch to %s(%u) @ %w:%x\n", process->name().characters(), process->pid(), process->tss().cs, process->tss().eip);
#endif
            return context_switch(*process);
        }

        if (process == previous_head) {
            // Back at process_head, nothing wants to run. Send in the colonel!
            return context_switch(*s_colonel_process);
        }
    }
}

bool Scheduler::donate_to(Process* beneficiary, const char* reason)
{
    (void)reason;
    unsigned ticks_left = current->ticks_left();
    if (!beneficiary || beneficiary->state() != Process::Runnable || ticks_left <= 1) {
        return yield();
    }

    unsigned ticks_to_donate = min(ticks_left - 1, time_slice_for(beneficiary->priority()));
#ifdef SCHEDULER_DEBUG
    dbgprintf("%s(%u) donating %u ticks to %s(%u), reason=%s\n", current->name().characters(), current->pid(), ticks_to_donate, beneficiary->name().characters(), beneficiary->pid(), reason);
#endif
    context_switch(*beneficiary);
    beneficiary->set_ticks_left(ticks_to_donate);
    switch_now();
    return 0;
}

bool Scheduler::yield()
{
    InterruptDisabler disabler;
    ASSERT(current);
    //dbgprintf("%s<%u> yield()\n", current->name().characters(), current->pid());

    if (!pick_next())
        return 1;

    //dbgprintf("yield() jumping to new process: %x (%s)\n", current->far_ptr().selector, current->name().characters());
    switch_now();
    return 0;
}

void Scheduler::pick_next_and_switch_now()
{
    bool someone_wants_to_run = pick_next();
    ASSERT(someone_wants_to_run);
    switch_now();
}

void Scheduler::switch_now()
{
    Descriptor& descriptor = get_gdt_entry(current->selector());
    descriptor.type = 9;
    flush_gdt();
    asm("sti\n"
        "ljmp *(%%eax)\n"
        ::"a"(&current->far_ptr())
    );
}

bool Scheduler::context_switch(Process& process)
{
    process.set_ticks_left(time_slice_for(process.priority()));
    process.did_schedule();

    if (current == &process)
        return false;

    if (current) {
        // If the last process hasn't blocked (still marked as running),
        // mark it as runnable for the next round.
        if (current->state() == Process::Running)
            current->set_state(Process::Runnable);

#ifdef LOG_EVERY_CONTEXT_SWITCH
        dbgprintf("Scheduler: %s(%u) -> %s(%u) %w:%x\n",
                  current->name().characters(), current->pid(),
                  process.name().characters(), process.pid(),
                  process.tss().cs, process.tss().eip);
#endif
    }

    current = &process;
    process.set_state(Process::Running);

#ifdef COOL_GLOBALS
    g_cool_globals->current_pid = process.pid();
#endif

    if (!process.selector()) {
        process.set_selector(gdt_alloc_entry());
        auto& descriptor = get_gdt_entry(process.selector());
        descriptor.set_base(&process.tss());
        descriptor.set_limit(0xffff);
        descriptor.dpl = 0;
        descriptor.segment_present = 1;
        descriptor.granularity = 1;
        descriptor.zero = 0;
        descriptor.operation_size = 1;
        descriptor.descriptor_type = 0;
    }

    auto& descriptor = get_gdt_entry(process.selector());
    descriptor.type = 11; // Busy TSS
    flush_gdt();
    return true;
}

static void initialize_redirection()
{
    auto& descriptor = get_gdt_entry(s_redirection.selector);
    descriptor.set_base(&s_redirection.tss);
    descriptor.set_limit(0xffff);
    descriptor.dpl = 0;
    descriptor.segment_present = 1;
    descriptor.granularity = 1;
    descriptor.zero = 0;
    descriptor.operation_size = 1;
    descriptor.descriptor_type = 0;
    descriptor.type = 9;
    flush_gdt();
}

void Scheduler::prepare_for_iret_to_new_process()
{
    auto& descriptor = get_gdt_entry(s_redirection.selector);
    descriptor.type = 9;
    s_redirection.tss.backlink = current->selector();
    load_task_register(s_redirection.selector);
}

void Scheduler::prepare_to_modify_tss(Process& process)
{
    // This ensures that a currently running process modifying its own TSS
    // in order to yield() and end up somewhere else doesn't just end up
    // right after the yield().
    if (current == &process)
        load_task_register(s_redirection.selector);
}

Process* Scheduler::colonel()
{
    return s_colonel_process;
}

void Scheduler::initialize()
{
    s_redirection.selector = gdt_alloc_entry();
    initialize_redirection();
    s_colonel_process = Process::create_kernel_process("colonel", nullptr);
    load_task_register(s_redirection.selector);
}

void Scheduler::timer_tick(RegisterDump& regs)
{
    if (!current)
        return;

    system.uptime++;

    if (current->tick())
        return;

    current->tss().gs = regs.gs;
    current->tss().fs = regs.fs;
    current->tss().es = regs.es;
    current->tss().ds = regs.ds;
    current->tss().edi = regs.edi;
    current->tss().esi = regs.esi;
    current->tss().ebp = regs.ebp;
    current->tss().ebx = regs.ebx;
    current->tss().edx = regs.edx;
    current->tss().ecx = regs.ecx;
    current->tss().eax = regs.eax;
    current->tss().eip = regs.eip;
    current->tss().cs = regs.cs;
    current->tss().eflags = regs.eflags;

    // Compute process stack pointer.
    // Add 12 for CS, EIP, EFLAGS (interrupt mechanic)
    current->tss().esp = regs.esp + 12;
    current->tss().ss = regs.ss;

    if ((current->tss().cs & 3) != 0) {
        current->tss().ss = regs.ss_if_crossRing;
        current->tss().esp = regs.esp_if_crossRing;
    }

    if (!pick_next())
        return;
    prepare_for_iret_to_new_process();

    // Set the NT (nested task) flag.
    asm(
        "pushf\n"
        "orl $0x00004000, (%esp)\n"
        "popf\n"
    );
}

#include "Scheduler.h"
#include "Process.h"
#include "system.h"

//#define LOG_EVERY_CONTEXT_SWITCH
//#define SCHEDULER_DEBUG

static const dword time_slice = 5; // *10 = 50ms

Process* current;
static Process* s_colonel_process;

struct TaskRedirectionData {
    word selector;
    TSS32 tss;
};
static TaskRedirectionData s_redirection;

bool Scheduler::pick_next()
{
    ASSERT_INTERRUPTS_DISABLED();

    if (!current) {
        // XXX: The first ever context_switch() goes to the idle process.
        //      This to setup a reliable place we can return to.
        return context_switch(*s_colonel_process);
    }

    // Check and unblock processes whose wait conditions have been met.
    Process::for_each([] (auto& process) {
        if (process.state() == Process::BlockedSleep) {
            if (process.wakeupTime() <= system.uptime)
                process.unblock();
            return true;
        }

        if (process.state() == Process::BlockedWait) {
            process.for_each_child([&process] (Process& child) {
                if (child.state() != Process::Dead)
                    return true;
                if (process.waitee() == -1 || process.waitee() == child.pid()) {
                    process.m_waitee_status = (child.m_termination_status << 8) | child.m_termination_signal;
                    process.m_waitee = child.pid();
                    process.unblock();
                    return false;
                }
                return true;
            });
            return true;
        }

        if (process.state() == Process::BlockedRead) {
            ASSERT(process.m_fdBlockedOnRead != -1);
            // FIXME: Block until the amount of data wanted is available.
            if (process.m_fds[process.m_fdBlockedOnRead].descriptor->hasDataAvailableForRead())
                process.unblock();
            return true;
        }

        if (process.state() == Process::BlockedWrite) {
            ASSERT(process.m_blocked_fd != -1);
            if (process.m_fds[process.m_blocked_fd].descriptor->can_write())
                process.unblock();
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
            if (current != &process && !Process::from_pid(process.ppid()))
                Process::reap(process);
            return true;
        }

        return true;
    });

    // Dispatch any pending signals.
    // FIXME: Do we really need this to be a separate pass over the process list?
    Process::for_each_not_in_state(Process::Dead, [] (auto& process) {
        if (!process.has_unmasked_pending_signals())
            return true;
        // We know how to interrupt blocked processes, but if they are just executing
        // at some random point in the kernel, let them continue. They'll be in userspace
        // sooner or later and we can deliver the signal then.
        // FIXME: Maybe we could check when returning from a syscall if there's a pending
        //        signal and dispatch it then and there? Would that be doable without the
        //        syscall effectively being "interrupted" despite having completed?
        if (process.in_kernel() && !process.is_blocked())
            return true;
        if (!process.dispatch_one_pending_signal())
            return true;
        if (process.is_blocked()) {
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
        dbgprintf("% 12s %s(%u) @ %w:%x\n", toString(process->state()), process->name().characters(), process->pid(), process->tss().cs, process->tss().eip);
    }
#endif

    auto* prevHead = g_processes->head();
    for (;;) {
        // Move head to tail.
        g_processes->append(g_processes->removeHead());
        auto* process = g_processes->head();

        if (process->state() == Process::Runnable || process->state() == Process::Running) {
#ifdef SCHEDULER_DEBUG
            dbgprintf("switch to %s(%u) @ %w:%x\n", process->name().characters(), process->pid(), process->tss().cs, process->tss().eip);
#endif
            return context_switch(*process);
        }

        if (process == prevHead) {
            // Back at process_head, nothing wants to run. Send in the colonel!
            return context_switch(*s_colonel_process);
        }
    }
}

bool Scheduler::yield()
{
    if (!current) {
        kprintf("PANIC: sched_yield() with !current");
        HANG;
    }

    //dbgprintf("%s<%u> yield()\n", current->name().characters(), current->pid());

    InterruptDisabler disabler;
    if (!pick_next())
        return 1;

    //dbgprintf("yield() jumping to new process: %x (%s)\n", current->farPtr().selector, current->name().characters());
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
    Descriptor& descriptor = getGDTEntry(current->selector());
    descriptor.type = 9;
    flushGDT();
    asm("sti\n"
        "ljmp *(%%eax)\n"
        ::"a"(&current->farPtr())
    );
}

bool Scheduler::context_switch(Process& process)
{
    process.set_ticks_left(time_slice);
    process.did_schedule();

    if (current == &process)
        return false;

    if (current) {
        // If the last process hasn't blocked (still marked as running),
        // mark it as runnable for the next round.
        if (current->state() == Process::Running)
            current->set_state(Process::Runnable);

#ifdef LOG_EVERY_CONTEXT_SWITCH
        dbgprintf("Scheduler: %s(%u) -> %s(%u)\n", current->name().characters(), current->pid(), process.name().characters(), process.pid());
#endif
    }

    current = &process;
    process.set_state(Process::Running);

#ifdef COOL_GLOBALS
    g_cool_globals->current_pid = process.pid();
#endif

    if (!process.selector()) {
        process.setSelector(gdt_alloc_entry());
        auto& descriptor = getGDTEntry(process.selector());
        descriptor.setBase(&process.tss());
        descriptor.setLimit(0xffff);
        descriptor.dpl = 0;
        descriptor.segment_present = 1;
        descriptor.granularity = 1;
        descriptor.zero = 0;
        descriptor.operation_size = 1;
        descriptor.descriptor_type = 0;
    }

    auto& descriptor = getGDTEntry(process.selector());
    descriptor.type = 11; // Busy TSS
    flushGDT();
    return true;
}

int sched_yield()
{
    return Scheduler::yield();
}

static void initialize_redirection()
{
    auto& descriptor = getGDTEntry(s_redirection.selector);
    descriptor.setBase(&s_redirection.tss);
    descriptor.setLimit(0xffff);
    descriptor.dpl = 0;
    descriptor.segment_present = 1;
    descriptor.granularity = 1;
    descriptor.zero = 0;
    descriptor.operation_size = 1;
    descriptor.descriptor_type = 0;
    descriptor.type = 9;
    flushGDT();
}

void Scheduler::prepare_for_iret_to_new_process()
{
    auto& descriptor = getGDTEntry(s_redirection.selector);
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

void Scheduler::initialize()
{
    memset(&s_redirection, 0, sizeof(s_redirection));
    s_redirection.selector = gdt_alloc_entry();
    initialize_redirection();
    s_colonel_process = Process::create_kernel_process(nullptr, "colonel");
    current = nullptr;
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

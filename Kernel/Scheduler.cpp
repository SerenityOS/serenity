#include "Scheduler.h"
#include "Process.h"
#include "RTC.h"
#include "i8253.h"
#include <AK/TemporaryChange.h>
#include <Kernel/Alarm.h>
#include <Kernel/FileSystem/FileDescriptor.h>
#include <Kernel/Devices/PCSpeaker.h>

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
    case Process::IdlePriority:
        return 1;
    }
    ASSERT_NOT_REACHED();
}

Thread* current;
Thread* g_last_fpu_thread;
Thread* g_finalizer;
static Process* s_colonel_process;
qword g_uptime;
static qword s_beep_timeout;

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

void Scheduler::beep()
{
    PCSpeaker::tone_on(440);
    s_beep_timeout = g_uptime + 100;
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
        return context_switch(s_colonel_process->main_thread());
    }

    struct timeval now;
    kgettimeofday(now);
    auto now_sec = now.tv_sec;
    auto now_usec = now.tv_usec;

    // Check and unblock threads whose wait conditions have been met.
    Thread::for_each_nonrunnable([&] (Thread& thread) {
        auto& process = thread.process();

        if (thread.state() == Thread::BlockedSleep) {
            if (thread.wakeup_time() <= g_uptime)
                thread.unblock();
            return IterationDecision::Continue;
        }

        if (thread.state() == Thread::BlockedWait) {
            process.for_each_child([&] (Process& child) {
                if (!child.is_dead())
                    return true;
                if (thread.waitee_pid() == -1 || thread.waitee_pid() == child.pid()) {
                    thread.m_waitee_pid = child.pid();
                    thread.unblock();
                    return false;
                }
                return true;
            });
            return IterationDecision::Continue;
        }

        if (thread.state() == Thread::BlockedRead) {
            ASSERT(thread.m_blocked_descriptor);
            // FIXME: Block until the amount of data wanted is available.
            if (thread.m_blocked_descriptor->can_read())
                thread.unblock();
            return IterationDecision::Continue;
        }

        if (thread.state() == Thread::BlockedWrite) {
            ASSERT(thread.m_blocked_descriptor != -1);
            if (thread.m_blocked_descriptor->can_write())
                thread.unblock();
            return IterationDecision::Continue;
        }

        if (thread.state() == Thread::BlockedConnect) {
            auto& descriptor = *thread.m_blocked_descriptor;
            auto& socket = *descriptor.socket();
            if (socket.is_connected())
                thread.unblock();
            return IterationDecision::Continue;
        }

        if (thread.state() == Thread::BlockedReceive) {
            auto& descriptor = *thread.m_blocked_descriptor;
            auto& socket = *descriptor.socket();
            // FIXME: Block until the amount of data wanted is available.
            bool timed_out = now_sec > socket.receive_deadline().tv_sec || (now_sec == socket.receive_deadline().tv_sec && now_usec >= socket.receive_deadline().tv_usec);
            if (timed_out || descriptor.can_read()) {
                thread.unblock();
                return IterationDecision::Continue;
            }
            return IterationDecision::Continue;
        }

        if (thread.state() == Thread::BlockedSelect) {
            if (thread.m_select_has_timeout) {
                if (now_sec > thread.m_select_timeout.tv_sec || (now_sec == thread.m_select_timeout.tv_sec && now_usec >= thread.m_select_timeout.tv_usec)) {
                    thread.unblock();
                    return IterationDecision::Continue;
                }
            }
            for (int fd : thread.m_select_read_fds) {
                if (process.m_fds[fd].descriptor->can_read()) {
                    thread.unblock();
                    return IterationDecision::Continue;
                }
            }
            for (int fd : thread.m_select_write_fds) {
                if (process.m_fds[fd].descriptor->can_write()) {
                    thread.unblock();
                    return IterationDecision::Continue;
                }
            }
            return IterationDecision::Continue;
        }

        if (thread.state() == Thread::BlockedSnoozing) {
            if (thread.m_snoozing_alarm->is_ringing()) {
                thread.m_snoozing_alarm = nullptr;
                thread.unblock();
            }
            return IterationDecision::Continue;
        }

        if (thread.state() == Thread::Skip1SchedulerPass) {
            thread.set_state(Thread::Skip0SchedulerPasses);
            return IterationDecision::Continue;
        }

        if (thread.state() == Thread::Skip0SchedulerPasses) {
            thread.set_state(Thread::Runnable);
            return IterationDecision::Continue;
        }

        if (thread.state() == Thread::Dying) {
            ASSERT(g_finalizer);
            if (g_finalizer->state() == Thread::BlockedLurking)
                g_finalizer->unblock();
            return IterationDecision::Continue;
        }

        return IterationDecision::Continue;
    });

    Process::for_each([&] (Process& process) {
        if (process.is_dead()) {
            if (current != &process.main_thread() && (!process.ppid() || !Process::from_pid(process.ppid()))) {
                auto name = process.name();
                auto pid = process.pid();
                auto exit_status = Process::reap(process);
                dbgprintf("reaped unparented process %s(%u), exit status: %u\n", name.characters(), pid, exit_status);
            }
        }
        return true;
    });

    // Dispatch any pending signals.
    // FIXME: Do we really need this to be a separate pass over the process list?
    Thread::for_each_living([] (Thread& thread) {
        if (!thread.has_unmasked_pending_signals())
            return true;
        // FIXME: It would be nice if the Scheduler didn't have to worry about who is "current"
        //        For now, avoid dispatching signals to "current" and do it in a scheduling pass
        //        while some other process is interrupted. Otherwise a mess will be made.
        if (&thread == current)
            return true;
        // We know how to interrupt blocked processes, but if they are just executing
        // at some random point in the kernel, let them continue. They'll be in userspace
        // sooner or later and we can deliver the signal then.
        // FIXME: Maybe we could check when returning from a syscall if there's a pending
        //        signal and dispatch it then and there? Would that be doable without the
        //        syscall effectively being "interrupted" despite having completed?
        if (thread.in_kernel() && !thread.is_blocked() && !thread.is_stopped())
            return true;
        // NOTE: dispatch_one_pending_signal() may unblock the process.
        bool was_blocked = thread.is_blocked();
        if (thread.dispatch_one_pending_signal() == ShouldUnblockThread::No)
            return true;
        if (was_blocked) {
            dbgprintf("Unblock %s(%u) due to signal\n", thread.process().name().characters(), thread.pid());
            thread.m_was_interrupted_while_blocked = true;
            thread.unblock();
        }
        return true;
    });

#ifdef SCHEDULER_DEBUG
    dbgprintf("Non-runnables:\n");
    for (auto* thread = g_nonrunnable_threads->head(); thread; thread = thread->next()) {
        auto* process = &thread->process();
        dbgprintf("[K%x] % 12s %s(%u:%u) @ %w:%x\n", process, to_string(thread->state()), process->name().characters(), process->pid(), thread->tid(), thread->tss().cs, thread->tss().eip);
    }

    dbgprintf("Runnables:\n");
    for (auto* thread = g_runnable_threads->head(); thread; thread = thread->next()) {
        auto* process = &thread->process();
        dbgprintf("[K%x] % 12s %s(%u:%u) @ %w:%x\n", process, to_string(thread->state()), process->name().characters(), process->pid(), thread->tid(), thread->tss().cs, thread->tss().eip);
    }
#endif

    if (g_runnable_threads->is_empty())
        return context_switch(s_colonel_process->main_thread());

    auto* previous_head = g_runnable_threads->head();
    for (;;) {
        // Move head to tail.
        g_runnable_threads->append(g_runnable_threads->remove_head());
        auto* thread = g_runnable_threads->head();

        if (!thread->process().is_being_inspected() && (thread->state() == Thread::Runnable || thread->state() == Thread::Running)) {
#ifdef SCHEDULER_DEBUG
            kprintf("switch to %s(%u:%u) @ %w:%x\n", thread->process().name().characters(), thread->process().pid(), thread->tid(), thread->tss().cs, thread->tss().eip);
#endif
            return context_switch(*thread);
        }

        if (thread == previous_head) {
            // Back at process_head, nothing wants to run. Send in the colonel!
            return context_switch(s_colonel_process->main_thread());
        }
    }
}

bool Scheduler::donate_to(Thread* beneficiary, const char* reason)
{
    InterruptDisabler disabler;
    if (!Thread::is_thread(beneficiary))
        return false;

    (void)reason;
    unsigned ticks_left = current->ticks_left();
    if (!beneficiary || beneficiary->state() != Thread::Runnable || ticks_left <= 1)
        return yield();

    unsigned ticks_to_donate = min(ticks_left - 1, time_slice_for(beneficiary->process().priority()));
#ifdef SCHEDULER_DEBUG
    dbgprintf("%s(%u:%u) donating %u ticks to %s(%u:%u), reason=%s\n", current->process().name().characters(), current->pid(), current->tid(), ticks_to_donate, beneficiary->process().name().characters(), beneficiary->pid(), beneficiary->tid(), reason);
#endif
    context_switch(*beneficiary);
    beneficiary->set_ticks_left(ticks_to_donate);
    switch_now();
    return false;
}

bool Scheduler::yield()
{
    InterruptDisabler disabler;
    ASSERT(current);
//    dbgprintf("%s(%u:%u) yield()\n", current->process().name().characters(), current->pid(), current->tid());

    if (!pick_next())
        return false;

//    dbgprintf("yield() jumping to new process: sel=%x, %s(%u:%u)\n", current->far_ptr().selector, current->process().name().characters(), current->pid(), current->tid());
    switch_now();
    return true;
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

bool Scheduler::context_switch(Thread& thread)
{
    thread.set_ticks_left(time_slice_for(thread.process().priority()));
    thread.did_schedule();

    if (current == &thread)
        return false;

    if (current) {
        // If the last process hasn't blocked (still marked as running),
        // mark it as runnable for the next round.
        if (current->state() == Thread::Running)
            current->set_state(Thread::Runnable);

#ifdef LOG_EVERY_CONTEXT_SWITCH
        dbgprintf("Scheduler: %s(%u:%u) -> %s(%u:%u) %w:%x\n",
                  current->process().name().characters(), current->process().pid(), current->tid(),
                  thread.process().name().characters(), thread.process().pid(), thread.tid(),
                  thread.tss().cs, thread.tss().eip);
#endif
    }

    current = &thread;
    thread.set_state(Thread::Running);

    if (!thread.selector()) {
        thread.set_selector(gdt_alloc_entry());
        auto& descriptor = get_gdt_entry(thread.selector());
        descriptor.set_base(&thread.tss());
        descriptor.set_limit(0xffff);
        descriptor.dpl = 0;
        descriptor.segment_present = 1;
        descriptor.granularity = 1;
        descriptor.zero = 0;
        descriptor.operation_size = 1;
        descriptor.descriptor_type = 0;
    }

    auto& descriptor = get_gdt_entry(thread.selector());
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

void Scheduler::prepare_to_modify_tss(Thread& thread)
{
    // This ensures that a currently running process modifying its own TSS
    // in order to yield() and end up somewhere else doesn't just end up
    // right after the yield().
    if (current == &thread)
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
    // Make sure the colonel uses a smallish time slice.
    s_colonel_process->set_priority(Process::IdlePriority);
    load_task_register(s_redirection.selector);
}

void Scheduler::timer_tick(RegisterDump& regs)
{
    if (!current)
        return;

    ++g_uptime;

    if (s_beep_timeout && g_uptime > s_beep_timeout) {
        PCSpeaker::tone_off();
        s_beep_timeout = 0;
    }

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

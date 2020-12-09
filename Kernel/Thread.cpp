/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/Demangle.h>
#include <AK/StringBuilder.h>
#include <AK/Time.h>
#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/KSyms.h>
#include <Kernel/Process.h>
#include <Kernel/Profiling.h>
#include <Kernel/Scheduler.h>
#include <Kernel/Thread.h>
#include <Kernel/ThreadTracer.h>
#include <Kernel/TimerQueue.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/PageDirectory.h>
#include <Kernel/VM/ProcessPagingScope.h>
#include <LibC/signal_numbers.h>
#include <LibELF/Loader.h>

//#define SIGNAL_DEBUG
//#define THREAD_DEBUG

namespace Kernel {

Thread::Thread(NonnullRefPtr<Process> process)
    : m_process(move(process))
    , m_name(m_process->name())
{
    if (m_process->m_thread_count.fetch_add(1, AK::MemoryOrder::memory_order_relaxed) == 0) {
        // First thread gets TID == PID
        m_tid = m_process->pid().value();
    } else {
        m_tid = Process::allocate_pid().value();
    }
#ifdef THREAD_DEBUG
    dbg() << "Created new thread " << m_process->name() << "(" << m_process->pid().value() << ":" << m_tid.value() << ")";
#endif
    set_default_signal_dispositions();
    m_fpu_state = (FPUState*)kmalloc_aligned<16>(sizeof(FPUState));
    reset_fpu_state();
    memset(&m_tss, 0, sizeof(m_tss));
    m_tss.iomapbase = sizeof(TSS32);

    // Only IF is set when a process boots.
    m_tss.eflags = 0x0202;

    if (m_process->is_kernel_process()) {
        m_tss.cs = GDT_SELECTOR_CODE0;
        m_tss.ds = GDT_SELECTOR_DATA0;
        m_tss.es = GDT_SELECTOR_DATA0;
        m_tss.fs = GDT_SELECTOR_PROC;
        m_tss.ss = GDT_SELECTOR_DATA0;
        m_tss.gs = 0;
    } else {
        m_tss.cs = GDT_SELECTOR_CODE3 | 3;
        m_tss.ds = GDT_SELECTOR_DATA3 | 3;
        m_tss.es = GDT_SELECTOR_DATA3 | 3;
        m_tss.fs = GDT_SELECTOR_DATA3 | 3;
        m_tss.ss = GDT_SELECTOR_DATA3 | 3;
        m_tss.gs = GDT_SELECTOR_TLS | 3;
    }

    m_tss.cr3 = m_process->page_directory().cr3();

    m_kernel_stack_region = MM.allocate_kernel_region(default_kernel_stack_size, String::format("Kernel Stack (Thread %d)", m_tid.value()), Region::Access::Read | Region::Access::Write, false, true);
    m_kernel_stack_region->set_stack(true);
    m_kernel_stack_base = m_kernel_stack_region->vaddr().get();
    m_kernel_stack_top = m_kernel_stack_region->vaddr().offset(default_kernel_stack_size).get() & 0xfffffff8u;

    if (m_process->is_kernel_process()) {
        m_tss.esp = m_tss.esp0 = m_kernel_stack_top;
    } else {
        // Ring 3 processes get a separate stack for ring 0.
        // The ring 3 stack will be assigned by exec().
        m_tss.ss0 = GDT_SELECTOR_DATA0;
        m_tss.esp0 = m_kernel_stack_top;
    }

    // We need to add another reference if we could successfully create
    // all the resources needed for this thread. The reason for this is that
    // we don't want to delete this thread after dropping the reference,
    // it may still be running or scheduled to be run.
    // The finalizer is responsible for dropping this reference once this
    // thread is ready to be cleaned up.
    ref();

    if (m_process->pid() != 0)
        Scheduler::init_thread(*this);
}

Thread::~Thread()
{
    {
        // We need to explicitly remove ourselves from the thread list
        // here. We may get pre-empted in the middle of destructing this
        // thread, which causes problems if the thread list is iterated.
        // Specifically, if this is the last thread of a process, checking
        // block conditions would access m_process, which would be in
        // the middle of being destroyed.
        ScopedSpinLock lock(g_scheduler_lock);
        g_scheduler_data->thread_list_for_state(m_state).remove(*this);
    }
}

void Thread::unblock_from_blocker(Blocker& blocker)
{
    auto do_unblock = [&]() {
        ScopedSpinLock scheduler_lock(g_scheduler_lock);
        ScopedSpinLock block_lock(m_block_lock);
        if (m_blocker != &blocker)
            return;
        if (!should_be_stopped() && !is_stopped())
            unblock();
    };
    if (Processor::current().in_irq()) {
        Processor::current().deferred_call_queue([do_unblock = move(do_unblock), self = make_weak_ptr()]() {
            if (auto this_thread = self.strong_ref())
                do_unblock();
        });
    } else {
        do_unblock();
    }
}

void Thread::unblock(u8 signal)
{
    ASSERT(!Processor::current().in_irq());
    ASSERT(g_scheduler_lock.own_lock());
    ASSERT(m_block_lock.own_lock());
    if (m_state != Thread::Blocked)
        return;
    ASSERT(m_blocker);
    if (signal != 0) {
        if (!m_blocker->can_be_interrupted() && !m_should_die)
            return;
        m_blocker->set_interrupted_by_signal(signal);
    }
    m_blocker = nullptr;
    if (Thread::current() == this) {
        set_state(Thread::Running);
        return;
    }
    ASSERT(m_state != Thread::Runnable && m_state != Thread::Running);
    set_state(Thread::Runnable);
}

void Thread::set_should_die()
{
    if (m_should_die) {
#ifdef THREAD_DEBUG
        dbg() << *this << " Should already die";
#endif
        return;
    }
    ScopedCritical critical;

    // Remember that we should die instead of returning to
    // the userspace.
    ScopedSpinLock lock(g_scheduler_lock);
    m_should_die = true;

    // NOTE: Even the current thread can technically be in "Stopped"
    // state! This is the case when another thread sent a SIGSTOP to
    // it while it was running and it calls e.g. exit() before
    // the scheduler gets involved again.
    if (is_stopped()) {
        // If we were stopped, we need to briefly resume so that
        // the kernel stacks can clean up. We won't ever return back
        // to user mode, though
        ASSERT(!process().is_stopped());
        resume_from_stopped();
    }
    if (is_blocked()) {
        ScopedSpinLock block_lock(m_block_lock);
        if (m_blocker) {
            // We're blocked in the kernel.
            m_blocker->set_interrupted_by_death();
            unblock();
        }
    }
}

void Thread::die_if_needed()
{
    ASSERT(Thread::current() == this);

    if (!m_should_die)
        return;

    unlock_process_if_locked();

    ScopedCritical critical;
    set_should_die();

    // Flag a context switch. Because we're in a critical section,
    // Scheduler::yield will actually only mark a pending scontext switch
    // Simply leaving the critical section would not necessarily trigger
    // a switch.
    Scheduler::yield();

    // Now leave the critical section so that we can also trigger the
    // actual context switch
    u32 prev_flags;
    Processor::current().clear_critical(prev_flags, false);
    dbg() << "die_if_needed returned from clear_critical!!! in irq: " << Processor::current().in_irq();
    // We should never get here, but the scoped scheduler lock
    // will be released by Scheduler::context_switch again
    ASSERT_NOT_REACHED();
}

void Thread::exit(void* exit_value)
{
    ASSERT(Thread::current() == this);
    m_join_condition.thread_did_exit(exit_value);
    set_should_die();
    unlock_process_if_locked();
    die_if_needed();
}

void Thread::yield_while_not_holding_big_lock()
{
    ASSERT(!g_scheduler_lock.own_lock());
    u32 prev_flags;
    u32 prev_crit = Processor::current().clear_critical(prev_flags, true);
    Scheduler::yield();
    // NOTE: We may be on a different CPU now!
    Processor::current().restore_critical(prev_crit, prev_flags);
}

void Thread::yield_without_holding_big_lock()
{
    ASSERT(!g_scheduler_lock.own_lock());
    bool did_unlock = unlock_process_if_locked();
    // NOTE: Even though we call Scheduler::yield here, unless we happen
    // to be outside of a critical section, the yield will be postponed
    // until leaving it in relock_process.
    Scheduler::yield();
    relock_process(did_unlock);
}

bool Thread::unlock_process_if_locked()
{
    return process().big_lock().force_unlock_if_locked();
}

void Thread::lock_process()
{
    process().big_lock().lock();
}

void Thread::relock_process(bool did_unlock)
{
    // Clearing the critical section may trigger the context switch
    // flagged by calling Scheduler::donate_to or Scheduler::yield
    // above. We have to do it this way because we intentionally
    // leave the critical section here to be able to switch contexts.
    u32 prev_flags;
    u32 prev_crit = Processor::current().clear_critical(prev_flags, true);

    if (did_unlock) {
        // We've unblocked, relock the process if needed and carry on.
        process().big_lock().lock();
    }

    // NOTE: We may be on a different CPU now!
    Processor::current().restore_critical(prev_crit, prev_flags);
}

auto Thread::sleep(clockid_t clock_id, const timespec& duration, timespec* remaining_time) -> BlockResult
{
    ASSERT(state() == Thread::Running);
    return Thread::current()->block<Thread::SleepBlocker>(nullptr, Thread::BlockTimeout(false, &duration, nullptr, clock_id), remaining_time);
}

auto Thread::sleep_until(clockid_t clock_id, const timespec& deadline) -> BlockResult
{
    ASSERT(state() == Thread::Running);
    return Thread::current()->block<Thread::SleepBlocker>(nullptr, Thread::BlockTimeout(true, &deadline, nullptr, clock_id));
}

const char* Thread::state_string() const
{
    switch (state()) {
    case Thread::Invalid:
        return "Invalid";
    case Thread::Runnable:
        return "Runnable";
    case Thread::Running:
        return "Running";
    case Thread::Dying:
        return "Dying";
    case Thread::Dead:
        return "Dead";
    case Thread::Stopped:
        return "Stopped";
    case Thread::Blocked: {
        ScopedSpinLock block_lock(m_block_lock);
        ASSERT(m_blocker != nullptr);
        return m_blocker->state_string();
    }
    }
    klog() << "Thread::state_string(): Invalid state: " << state();
    ASSERT_NOT_REACHED();
    return nullptr;
}

void Thread::finalize()
{
    ASSERT(Thread::current() == g_finalizer);
    ASSERT(Thread::current() != this);

#ifdef LOCK_DEBUG
    ASSERT(!m_lock.own_lock());
    if (lock_count() > 0) {
        dbg() << "Thread " << *this << " leaking " << lock_count() << " Locks!";
        ScopedSpinLock list_lock(m_holding_locks_lock);
        for (auto& info : m_holding_locks_list)
            dbg() << " - " << info.lock->name() << " @ " << info.lock << " locked at " << info.file << ":" << info.line << " count: " << info.count;
        ASSERT_NOT_REACHED();
    }
#endif

    {
        ScopedSpinLock lock(g_scheduler_lock);
#ifdef THREAD_DEBUG
        dbg() << "Finalizing thread " << *this;
#endif
        set_state(Thread::State::Dead);
        m_join_condition.thread_finalizing();
    }

    if (m_dump_backtrace_on_finalization)
        dbg() << backtrace_impl();

    kfree_aligned(m_fpu_state);

    auto thread_cnt_before = m_process->m_thread_count.fetch_sub(1, AK::MemoryOrder::memory_order_acq_rel);

    ASSERT(thread_cnt_before != 0);
    if (thread_cnt_before == 1)
        process().finalize();
}

void Thread::finalize_dying_threads()
{
    ASSERT(Thread::current() == g_finalizer);
    Vector<Thread*, 32> dying_threads;
    {
        ScopedSpinLock lock(g_scheduler_lock);
        for_each_in_state(Thread::State::Dying, [&](Thread& thread) {
            if (thread.is_finalizable())
                dying_threads.append(&thread);
            return IterationDecision::Continue;
        });
    }
    for (auto* thread : dying_threads) {
        thread->finalize();

        // This thread will never execute again, drop the running reference
        // NOTE: This may not necessarily drop the last reference if anything
        //       else is still holding onto this thread!
        thread->unref();
    }
}

bool Thread::tick()
{
    ++m_ticks;
    if (tss().cs & 3)
        ++m_process->m_ticks_in_user;
    else
        ++m_process->m_ticks_in_kernel;
    return --m_ticks_left;
}

void Thread::check_dispatch_pending_signal()
{
    auto result = DispatchSignalResult::Continue;
    {
        ScopedSpinLock scheduler_lock(g_scheduler_lock);
        if (pending_signals_for_state()) {
            ScopedSpinLock lock(m_lock);
            result = dispatch_one_pending_signal();
        }
    }

    switch (result) {
    case DispatchSignalResult::Yield:
        yield_while_not_holding_big_lock();
        break;
    case DispatchSignalResult::Terminate:
        process().die();
        break;
    default:
        break;
    }
}

bool Thread::has_pending_signal(u8 signal) const
{
    ScopedSpinLock lock(g_scheduler_lock);
    return pending_signals_for_state() & (1 << (signal - 1));
}

u32 Thread::pending_signals() const
{
    ScopedSpinLock lock(g_scheduler_lock);
    return pending_signals_for_state();
}

u32 Thread::pending_signals_for_state() const
{
    ASSERT(g_scheduler_lock.own_lock());
    constexpr u32 stopped_signal_mask = (1 << (SIGCONT - 1)) | (1 << (SIGKILL - 1)) | (1 << (SIGTRAP - 1));
    return m_state != Stopped ? m_pending_signals : m_pending_signals & stopped_signal_mask;
}

void Thread::send_signal(u8 signal, [[maybe_unused]] Process* sender)
{
    ASSERT(signal < 32);
    ScopedSpinLock scheduler_lock(g_scheduler_lock);

    // FIXME: Figure out what to do for masked signals. Should we also ignore them here?
    if (should_ignore_signal(signal)) {
#ifdef SIGNAL_DEBUG
        dbg() << "Signal " << signal << " was ignored by " << process();
#endif
        return;
    }

#ifdef SIGNAL_DEBUG
    if (sender)
        dbg() << "Signal: " << *sender << " sent " << signal << " to " << process();
    else
        dbg() << "Signal: Kernel sent " << signal << " to " << process();
#endif

    m_pending_signals |= 1 << (signal - 1);
    m_have_any_unmasked_pending_signals.store(pending_signals_for_state() & ~m_signal_mask, AK::memory_order_release);

    if (m_state == Stopped) {
        ScopedSpinLock lock(m_lock);
        if (pending_signals_for_state()) {
#ifdef SIGNAL_DEBUG
            dbg() << "Signal: Resuming stopped " << *this << " to deliver signal " << signal;
#endif
            resume_from_stopped();
        }
    } else {
        ScopedSpinLock block_lock(m_block_lock);
#ifdef SIGNAL_DEBUG
        dbg() << "Signal: Unblocking " << *this << " to deliver signal " << signal;
#endif
        unblock(signal);
    }
}

u32 Thread::update_signal_mask(u32 signal_mask)
{
    ScopedSpinLock lock(g_scheduler_lock);
    auto previous_signal_mask = m_signal_mask;
    m_signal_mask = signal_mask;
    m_have_any_unmasked_pending_signals.store(pending_signals_for_state() & ~m_signal_mask, AK::memory_order_release);
    return previous_signal_mask;
}

u32 Thread::signal_mask() const
{
    ScopedSpinLock lock(g_scheduler_lock);
    return m_signal_mask;
}

u32 Thread::signal_mask_block(sigset_t signal_set, bool block)
{
    ScopedSpinLock lock(g_scheduler_lock);
    auto previous_signal_mask = m_signal_mask;
    if (block)
        m_signal_mask &= ~signal_set;
    else
        m_signal_mask |= signal_set;
    m_have_any_unmasked_pending_signals.store(pending_signals_for_state() & ~m_signal_mask, AK::memory_order_release);
    return previous_signal_mask;
}

void Thread::clear_signals()
{
    ScopedSpinLock lock(g_scheduler_lock);
    m_signal_mask = 0;
    m_pending_signals = 0;
    m_have_any_unmasked_pending_signals.store(false, AK::memory_order_release);
}

// Certain exceptions, such as SIGSEGV and SIGILL, put a
// thread into a state where the signal handler must be
// invoked immediately, otherwise it will continue to fault.
// This function should be used in an exception handler to
// ensure that when the thread resumes, it's executing in
// the appropriate signal handler.
void Thread::send_urgent_signal_to_self(u8 signal)
{
    ASSERT(Thread::current() == this);
    DispatchSignalResult result;
    {
        ScopedSpinLock lock(g_scheduler_lock);
        result = dispatch_signal(signal);
    }
    if (result == DispatchSignalResult::Yield)
        yield_without_holding_big_lock();
}

DispatchSignalResult Thread::dispatch_one_pending_signal()
{
    ASSERT(m_lock.own_lock());
    u32 signal_candidates = pending_signals_for_state() & ~m_signal_mask;
    if (signal_candidates == 0)
        return DispatchSignalResult::Continue;

    u8 signal = 1;
    for (; signal < 32; ++signal) {
        if (signal_candidates & (1 << (signal - 1))) {
            break;
        }
    }
    return dispatch_signal(signal);
}

DispatchSignalResult Thread::try_dispatch_one_pending_signal(u8 signal)
{
    ASSERT(signal != 0);
    ScopedSpinLock scheduler_lock(g_scheduler_lock);
    ScopedSpinLock lock(m_lock);
    u32 signal_candidates = pending_signals_for_state() & ~m_signal_mask;
    if (!(signal_candidates & (1 << (signal - 1))))
        return DispatchSignalResult::Continue;
    return dispatch_signal(signal);
}

enum class DefaultSignalAction {
    Terminate,
    Ignore,
    DumpCore,
    Stop,
    Continue,
};

static DefaultSignalAction default_signal_action(u8 signal)
{
    ASSERT(signal && signal < NSIG);

    switch (signal) {
    case SIGHUP:
    case SIGINT:
    case SIGKILL:
    case SIGPIPE:
    case SIGALRM:
    case SIGUSR1:
    case SIGUSR2:
    case SIGVTALRM:
    case SIGSTKFLT:
    case SIGIO:
    case SIGPROF:
    case SIGTERM:
        return DefaultSignalAction::Terminate;
    case SIGCHLD:
    case SIGURG:
    case SIGWINCH:
    case SIGINFO:
        return DefaultSignalAction::Ignore;
    case SIGQUIT:
    case SIGILL:
    case SIGTRAP:
    case SIGABRT:
    case SIGBUS:
    case SIGFPE:
    case SIGSEGV:
    case SIGXCPU:
    case SIGXFSZ:
    case SIGSYS:
        return DefaultSignalAction::DumpCore;
    case SIGCONT:
        return DefaultSignalAction::Continue;
    case SIGSTOP:
    case SIGTSTP:
    case SIGTTIN:
    case SIGTTOU:
        return DefaultSignalAction::Stop;
    }
    ASSERT_NOT_REACHED();
}

bool Thread::should_ignore_signal(u8 signal) const
{
    ASSERT(signal < 32);
    auto& action = m_signal_action_data[signal];
    if (action.handler_or_sigaction.is_null())
        return default_signal_action(signal) == DefaultSignalAction::Ignore;
    if (action.handler_or_sigaction.as_ptr() == SIG_IGN)
        return true;
    return false;
}

bool Thread::has_signal_handler(u8 signal) const
{
    ASSERT(signal < 32);
    auto& action = m_signal_action_data[signal];
    return !action.handler_or_sigaction.is_null();
}

static bool push_value_on_user_stack(u32* stack, u32 data)
{
    *stack -= 4;
    return copy_to_user((u32*)*stack, &data);
}

void Thread::resume_from_stopped()
{
    ASSERT(is_stopped());
    ASSERT(m_stop_state != State::Invalid);
    ASSERT(g_scheduler_lock.own_lock());
    if (m_stop_state == Blocked) {
        ScopedSpinLock block_lock(m_block_lock);
        if (m_blocker) {
            // Hasn't been unblocked yet
            set_state(Blocked, 0);
        } else {
            // Was unblocked while stopped
            set_state(Runnable);
        }
    } else {
        set_state(m_stop_state, 0);
    }
}

DispatchSignalResult Thread::dispatch_signal(u8 signal)
{
    ASSERT_INTERRUPTS_DISABLED();
    ASSERT(g_scheduler_lock.own_lock());
    ASSERT(signal > 0 && signal <= 32);
    ASSERT(process().is_user_process());
    ASSERT(this == Thread::current());

#ifdef SIGNAL_DEBUG
    klog() << "signal: dispatch signal " << signal << " to " << *this << " state: " << state_string();
#endif

    if (m_state == Invalid || !is_initialized()) {
        // Thread has barely been created, we need to wait until it is
        // at least in Runnable state and is_initialized() returns true,
        // which indicates that it is fully set up an we actually have
        // a register state on the stack that we can modify
        return DispatchSignalResult::Deferred;
    }

    //    if (is_stopped() && signal != SIGCONT && signal != SIGKILL && signal != SIGTRAP) {
    //#ifdef SIGNAL_DEBUG
    //        klog() << "signal: " << *this << " is stopped, will handle signal " << signal << " when resumed";
    //#endif
    //        return DispatchSignalResult::Deferred;
    //    }
    //    if (is_blocked()) {
    //#ifdef SIGNAL_DEBUG
    //        klog() << "signal: " << *this << " is blocked, will handle signal " << signal << " when unblocking";
    //#endif
    //        return DispatchSignalResult::Deferred;
    //    }

    auto& action = m_signal_action_data[signal];
    // FIXME: Implement SA_SIGINFO signal handlers.
    ASSERT(!(action.flags & SA_SIGINFO));

    // Mark this signal as handled.
    m_pending_signals &= ~(1 << (signal - 1));
    m_have_any_unmasked_pending_signals.store(m_pending_signals & ~m_signal_mask, AK::memory_order_release);

    auto& process = this->process();
    auto tracer = process.tracer();
    if (signal == SIGSTOP || (tracer && default_signal_action(signal) == DefaultSignalAction::DumpCore)) {
#ifdef SIGNAL_DEBUG
        dbg() << "signal: signal " << signal << " stopping thread " << *this;
#endif
        set_state(State::Stopped, signal);
        return DispatchSignalResult::Yield;
    }

    if (signal == SIGCONT) {
#ifdef SIGNAL_DEBUG
        dbg() << "signal: SIGCONT resuming " << *this;
#endif
    } else {
        if (tracer) {
            // when a thread is traced, it should be stopped whenever it receives a signal
            // the tracer is notified of this by using waitpid()
            // only "pending signals" from the tracer are sent to the tracee
            if (!tracer->has_pending_signal(signal)) {
#ifdef SIGNAL_DEBUG
                dbg() << "signal: " << signal << " stopping " << *this << " for tracer";
#endif
                set_state(Stopped, signal);
                return DispatchSignalResult::Yield;
            }
            tracer->unset_signal(signal);
        }
    }

    auto handler_vaddr = action.handler_or_sigaction;
    if (handler_vaddr.is_null()) {
        switch (default_signal_action(signal)) {
        case DefaultSignalAction::Stop:
            set_state(Stopped, signal);
            return DispatchSignalResult::Yield;
        case DefaultSignalAction::DumpCore:
            process.for_each_thread([](auto& thread) {
                thread.set_dump_backtrace_on_finalization();
                return IterationDecision::Continue;
            });
            [[fallthrough]];
        case DefaultSignalAction::Terminate:
            m_process->terminate_due_to_signal(signal);
            return DispatchSignalResult::Terminate;
        case DefaultSignalAction::Ignore:
            ASSERT_NOT_REACHED();
        case DefaultSignalAction::Continue:
            return DispatchSignalResult::Continue;
        }
        ASSERT_NOT_REACHED();
    }

    if (handler_vaddr.as_ptr() == SIG_IGN) {
#ifdef SIGNAL_DEBUG
        klog() << "signal: " << *this << " ignored signal " << signal;
#endif
        return DispatchSignalResult::Continue;
    }

    ProcessPagingScope paging_scope(m_process);

    u32 old_signal_mask = m_signal_mask;
    u32 new_signal_mask = action.mask;
    if (action.flags & SA_NODEFER)
        new_signal_mask &= ~(1 << (signal - 1));
    else
        new_signal_mask |= 1 << (signal - 1);

    m_signal_mask |= new_signal_mask;
    m_have_any_unmasked_pending_signals.store(m_pending_signals & ~m_signal_mask, AK::memory_order_release);

    auto setup_stack = [&](RegisterState& state) {
        u32* stack = &state.userspace_esp;
        u32 old_esp = *stack;
        u32 ret_eip = state.eip;
        u32 ret_eflags = state.eflags;

#ifdef SIGNAL_DEBUG
        klog() << "signal: setting up user stack to return to eip: " << String::format("%p", ret_eip) << " esp: " << String::format("%p", old_esp);
#endif

        // Align the stack to 16 bytes.
        // Note that we push 56 bytes (4 * 14) on to the stack,
        // so we need to account for this here.
        u32 stack_alignment = (*stack - 56) % 16;
        *stack -= stack_alignment;

        push_value_on_user_stack(stack, ret_eflags);

        push_value_on_user_stack(stack, ret_eip);
        push_value_on_user_stack(stack, state.eax);
        push_value_on_user_stack(stack, state.ecx);
        push_value_on_user_stack(stack, state.edx);
        push_value_on_user_stack(stack, state.ebx);
        push_value_on_user_stack(stack, old_esp);
        push_value_on_user_stack(stack, state.ebp);
        push_value_on_user_stack(stack, state.esi);
        push_value_on_user_stack(stack, state.edi);

        // PUSH old_signal_mask
        push_value_on_user_stack(stack, old_signal_mask);

        push_value_on_user_stack(stack, signal);
        push_value_on_user_stack(stack, handler_vaddr.get());
        push_value_on_user_stack(stack, 0); //push fake return address

        ASSERT((*stack % 16) == 0);
    };

    // We now place the thread state on the userspace stack.
    // Note that we use a RegisterState.
    // Conversely, when the thread isn't blocking the RegisterState may not be
    // valid (fork, exec etc) but the tss will, so we use that instead.
    auto& regs = get_register_dump_from_stack();
    setup_stack(regs);
    regs.eip = g_return_to_ring3_from_signal_trampoline.get();

#ifdef SIGNAL_DEBUG
    klog() << "signal: Okay, " << *this << " {" << state_string() << "} has been primed with signal handler " << String::format("%w", m_tss.cs) << ":" << String::format("%x", m_tss.eip) << " to deliver " << signal;
#endif
    return DispatchSignalResult::Continue;
}

void Thread::set_default_signal_dispositions()
{
    // FIXME: Set up all the right default actions. See signal(7).
    memset(&m_signal_action_data, 0, sizeof(m_signal_action_data));
    m_signal_action_data[SIGCHLD].handler_or_sigaction = VirtualAddress(SIG_IGN);
    m_signal_action_data[SIGWINCH].handler_or_sigaction = VirtualAddress(SIG_IGN);
}

bool Thread::push_value_on_stack(FlatPtr value)
{
    m_tss.esp -= 4;
    FlatPtr* stack_ptr = (FlatPtr*)m_tss.esp;
    return copy_to_user(stack_ptr, &value);
}

RegisterState& Thread::get_register_dump_from_stack()
{
    return *(RegisterState*)(kernel_stack_top() - sizeof(RegisterState));
}

KResultOr<u32> Thread::make_userspace_stack_for_main_thread(Vector<String> arguments, Vector<String> environment, Vector<AuxiliaryValue> auxiliary_values)
{
    auto* region = m_process->allocate_region(VirtualAddress(), default_userspace_stack_size, "Stack (Main thread)", PROT_READ | PROT_WRITE, false);
    if (!region)
        return KResult(-ENOMEM);
    region->set_stack(true);

    FlatPtr new_esp = region->vaddr().offset(default_userspace_stack_size).get();

    auto push_on_new_stack = [&new_esp](u32 value) {
        new_esp -= 4;
        Userspace<u32*> stack_ptr = new_esp;
        return copy_to_user(stack_ptr, &value);
    };

    auto push_aux_value_on_new_stack = [&new_esp](auxv_t value) {
        new_esp -= sizeof(auxv_t);
        Userspace<auxv_t*> stack_ptr = new_esp;
        return copy_to_user(stack_ptr, &value);
    };

    auto push_string_on_new_stack = [&new_esp](const String& string) {
        new_esp -= round_up_to_power_of_two(string.length() + 1, 4);
        Userspace<u32*> stack_ptr = new_esp;
        return copy_to_user(stack_ptr, string.characters(), string.length() + 1);
    };

    Vector<FlatPtr> argv_entries;
    for (auto& argument : arguments) {
        push_string_on_new_stack(argument);
        argv_entries.append(new_esp);
    }

    Vector<FlatPtr> env_entries;
    for (auto& variable : environment) {
        push_string_on_new_stack(variable);
        env_entries.append(new_esp);
    }

    for (auto& value : auxiliary_values) {
        if (!value.optional_string.is_empty()) {
            push_string_on_new_stack(value.optional_string);
            value.auxv.a_un.a_ptr = (void*)new_esp;
        }
    }

    for (ssize_t i = auxiliary_values.size() - 1; i >= 0; --i) {
        auto& value = auxiliary_values[i];
        push_aux_value_on_new_stack(value.auxv);
    }

    push_on_new_stack(0);
    for (ssize_t i = env_entries.size() - 1; i >= 0; --i)
        push_on_new_stack(env_entries[i]);
    FlatPtr envp = new_esp;

    push_on_new_stack(0);
    for (ssize_t i = argv_entries.size() - 1; i >= 0; --i)
        push_on_new_stack(argv_entries[i]);
    FlatPtr argv = new_esp;

    // NOTE: The stack needs to be 16-byte aligned.
    new_esp -= new_esp % 16;

    push_on_new_stack((FlatPtr)envp);
    push_on_new_stack((FlatPtr)argv);
    push_on_new_stack((FlatPtr)argv_entries.size());
    push_on_new_stack(0);

    return new_esp;
}

RefPtr<Thread> Thread::clone(Process& process)
{
    auto clone = adopt(*new Thread(process));
    memcpy(clone->m_signal_action_data, m_signal_action_data, sizeof(m_signal_action_data));
    clone->m_signal_mask = m_signal_mask;
    memcpy(clone->m_fpu_state, m_fpu_state, sizeof(FPUState));
    clone->m_thread_specific_data = m_thread_specific_data;
    clone->m_thread_specific_region_size = m_thread_specific_region_size;
    return clone;
}

void Thread::set_state(State new_state, u8 stop_signal)
{
    State previous_state;
    ASSERT(g_scheduler_lock.own_lock());
    if (new_state == m_state)
        return;

    {
        ScopedSpinLock thread_lock(m_lock);
        previous_state = m_state;
        if (previous_state == Invalid) {
            // If we were *just* created, we may have already pending signals
            if (has_unmasked_pending_signals()) {
                dbg() << "Dispatch pending signals to new thread " << *this;
                dispatch_one_pending_signal();
            }
        }

        m_state = new_state;
#ifdef THREAD_DEBUG
        dbg() << "Set Thread " << *this << " state to " << state_string();
#endif
    }

    if (m_process->pid() != 0) {
        update_state_for_thread(previous_state);
        ASSERT(g_scheduler_data->has_thread(*this));
    }

    if (previous_state == Stopped) {
        m_stop_state = State::Invalid;
        auto& process = this->process();
        if (process.set_stopped(false) == true) {
            process.for_each_thread([&](auto& thread) {
                if (&thread == this || !thread.is_stopped())
                    return IterationDecision::Continue;
#ifdef THREAD_DEBUG
                dbg() << "Resuming peer thread " << thread;
#endif
                thread.resume_from_stopped();
                return IterationDecision::Continue;
            });
            process.unblock_waiters(Thread::WaitBlocker::UnblockFlags::Continued);
        }
    }

    if (m_state == Stopped) {
        // We don't want to restore to Running state, only Runnable!
        m_stop_state = previous_state != Running ? previous_state : Runnable;
        auto& process = this->process();
        if (process.set_stopped(true) == false) {
            process.for_each_thread([&](auto& thread) {
                if (&thread == this || thread.is_stopped())
                    return IterationDecision::Continue;
#ifdef THREAD_DEBUG
                dbg() << "Stopping peer thread " << thread;
#endif
                thread.set_state(Stopped, stop_signal);
                return IterationDecision::Continue;
            });
            process.unblock_waiters(Thread::WaitBlocker::UnblockFlags::Stopped, stop_signal);
        }
    } else if (m_state == Dying) {
        ASSERT(previous_state != Blocked);
        if (this != Thread::current() && is_finalizable()) {
            // Some other thread set this thread to Dying, notify the
            // finalizer right away as it can be cleaned up now
            Scheduler::notify_finalizer();
        }
    }
}

void Thread::update_state_for_thread(Thread::State previous_state)
{
    ASSERT_INTERRUPTS_DISABLED();
    ASSERT(g_scheduler_data);
    ASSERT(g_scheduler_lock.own_lock());
    auto& previous_list = g_scheduler_data->thread_list_for_state(previous_state);
    auto& list = g_scheduler_data->thread_list_for_state(state());

    if (&previous_list != &list) {
        previous_list.remove(*this);
    }

    if (list.contains(*this))
        return;

    list.append(*this);
}

String Thread::backtrace()
{
    return backtrace_impl();
}

struct RecognizedSymbol {
    u32 address;
    const KernelSymbol* symbol { nullptr };
};

static bool symbolicate(const RecognizedSymbol& symbol, const Process& process, StringBuilder& builder, Process::ELFBundle* elf_bundle)
{
    if (!symbol.address)
        return false;

    bool mask_kernel_addresses = !process.is_superuser();
    if (!symbol.symbol) {
        if (!is_user_address(VirtualAddress(symbol.address))) {
            builder.append("0xdeadc0de\n");
        } else {
            if (elf_bundle && elf_bundle->elf_loader->has_symbols())
                builder.appendf("%p  %s\n", symbol.address, elf_bundle->elf_loader->symbolicate(symbol.address).characters());
            else
                builder.appendf("%p\n", symbol.address);
        }
        return true;
    }
    unsigned offset = symbol.address - symbol.symbol->address;
    if (symbol.symbol->address == g_highest_kernel_symbol_address && offset > 4096) {
        builder.appendf("%p\n", mask_kernel_addresses ? 0xdeadc0de : symbol.address);
    } else {
        builder.appendf("%p  %s +%u\n", mask_kernel_addresses ? 0xdeadc0de : symbol.address, demangle(symbol.symbol->name).characters(), offset);
    }
    return true;
}

String Thread::backtrace_impl()
{
    Vector<RecognizedSymbol, 128> recognized_symbols;

    auto& process = const_cast<Process&>(this->process());
    OwnPtr<Process::ELFBundle> elf_bundle;
    if (!Processor::current().in_irq()) {
        // If we're handling IRQs we can't really safely symbolicate
        elf_bundle = process.elf_bundle();
    }
    auto stack_trace = Processor::capture_stack_trace(*this);
    ASSERT(!g_scheduler_lock.own_lock());
    ProcessPagingScope paging_scope(process);
    for (auto& frame : stack_trace) {
        if (is_user_range(VirtualAddress(frame), sizeof(FlatPtr) * 2)) {
            recognized_symbols.append({ frame, symbolicate_kernel_address(frame) });
        } else {
            recognized_symbols.append({ frame, symbolicate_kernel_address(frame) });
        }
    }

    StringBuilder builder;
    for (auto& symbol : recognized_symbols) {
        if (!symbolicate(symbol, process, builder, elf_bundle.ptr()))
            break;
    }
    return builder.to_string();
}

Vector<FlatPtr> Thread::raw_backtrace(FlatPtr ebp, FlatPtr eip) const
{
    InterruptDisabler disabler;
    auto& process = const_cast<Process&>(this->process());
    ProcessPagingScope paging_scope(process);
    Vector<FlatPtr, Profiling::max_stack_frame_count> backtrace;
    backtrace.append(eip);
    FlatPtr stack_ptr_copy;
    FlatPtr stack_ptr = (FlatPtr)ebp;
    while (stack_ptr) {
        void* fault_at;
        if (!safe_memcpy(&stack_ptr_copy, (void*)stack_ptr, sizeof(FlatPtr), fault_at))
            break;
        FlatPtr retaddr;
        if (!safe_memcpy(&retaddr, (void*)(stack_ptr + sizeof(FlatPtr)), sizeof(FlatPtr), fault_at))
            break;
        backtrace.append(retaddr);
        if (backtrace.size() == Profiling::max_stack_frame_count)
            break;
        stack_ptr = stack_ptr_copy;
    }
    return backtrace;
}

KResult Thread::make_thread_specific_region(Badge<Process>)
{
    size_t thread_specific_region_alignment = max(process().m_master_tls_alignment, alignof(ThreadSpecificData));
    m_thread_specific_region_size = align_up_to(process().m_master_tls_size, thread_specific_region_alignment) + sizeof(ThreadSpecificData);
    auto* region = process().allocate_region({}, m_thread_specific_region_size, "Thread-specific", PROT_READ | PROT_WRITE, true);
    if (!region)
        return KResult(-ENOMEM);
    SmapDisabler disabler;
    auto* thread_specific_data = (ThreadSpecificData*)region->vaddr().offset(align_up_to(process().m_master_tls_size, thread_specific_region_alignment)).as_ptr();
    auto* thread_local_storage = (u8*)((u8*)thread_specific_data) - align_up_to(process().m_master_tls_size, process().m_master_tls_alignment);
    m_thread_specific_data = VirtualAddress(thread_specific_data);
    thread_specific_data->self = thread_specific_data;
    if (process().m_master_tls_size)
        memcpy(thread_local_storage, process().m_master_tls_region.unsafe_ptr()->vaddr().as_ptr(), process().m_master_tls_size);
    return KSuccess;
}

const LogStream& operator<<(const LogStream& stream, const Thread& value)
{
    return stream << value.process().name() << "(" << value.pid().value() << ":" << value.tid().value() << ")";
}

RefPtr<Thread> Thread::from_tid(ThreadID tid)
{
    RefPtr<Thread> found_thread;
    ScopedSpinLock lock(g_scheduler_lock);
    Thread::for_each([&](auto& thread) {
        if (thread.tid() == tid) {
            found_thread = &thread;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });
    return found_thread;
}

void Thread::reset_fpu_state()
{
    memcpy(m_fpu_state, &Processor::current().clean_fpu_state(), sizeof(FPUState));
}

bool Thread::should_be_stopped() const
{
    return process().is_stopped();
}

}

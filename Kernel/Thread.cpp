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
    if (m_process->m_thread_count.fetch_add(1, AK::MemoryOrder::memory_order_acq_rel) == 0) {
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
    ASSERT(!m_joiner);
}

void Thread::unblock()
{
    ASSERT(m_lock.own_lock());
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
    {
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
            resume_from_stopped();
        } else if (state() == Queued) {
            // m_queue can only be accessed safely if g_scheduler_lock is held!
            if (m_queue) {
                m_queue->dequeue(*this);
                m_queue = nullptr;
                // Wake the thread
                wake_from_queue();
            }
        }
    }

    if (is_blocked()) {
        ScopedSpinLock lock(m_lock);
        ASSERT(m_blocker != nullptr);
        // We're blocked in the kernel.
        m_blocker->set_interrupted_by_death();
        unblock();
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
    dbg() << "die_if_needed returned form clear_critical!!! in irq: " << Processor::current().in_irq();
    // We should never get here, but the scoped scheduler lock
    // will be released by Scheduler::context_switch again
    ASSERT_NOT_REACHED();
}

void Thread::yield_without_holding_big_lock()
{
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

u64 Thread::sleep(u64 ticks)
{
    ASSERT(state() == Thread::Running);
    u64 wakeup_time = g_uptime + ticks;
    auto ret = Thread::current()->block<Thread::SleepBlocker>(nullptr, wakeup_time);
    if (wakeup_time > g_uptime) {
        ASSERT(ret.was_interrupted());
    }
    return wakeup_time;
}

u64 Thread::sleep_until(u64 wakeup_time)
{
    ASSERT(state() == Thread::Running);
    auto ret = Thread::current()->block<Thread::SleepBlocker>(nullptr, wakeup_time);
    if (wakeup_time > g_uptime)
        ASSERT(ret.was_interrupted());
    return wakeup_time;
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
    case Thread::Queued:
        return "Queued";
    case Thread::Blocked: {
        ScopedSpinLock lock(m_lock);
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

#ifdef THREAD_DEBUG
    dbg() << "Finalizing thread " << *this;
#endif
    set_state(Thread::State::Dead);

    if (auto* joiner = m_joiner.exchange(nullptr, AK::memory_order_acq_rel)) {
        // Notify joiner that we exited
        static_cast<JoinBlocker*>(joiner->m_blocker)->joinee_exited(m_exit_value);
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

bool Thread::has_pending_signal(u8 signal) const
{
    ScopedSpinLock lock(g_scheduler_lock);
    return m_pending_signals & (1 << (signal - 1));
}

u32 Thread::pending_signals() const
{
    ScopedSpinLock lock(g_scheduler_lock);
    return m_pending_signals;
}

void Thread::send_signal(u8 signal, [[maybe_unused]] Process* sender)
{
    ASSERT(signal < 32);
    ScopedSpinLock lock(g_scheduler_lock);

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
    m_have_any_unmasked_pending_signals.store(m_pending_signals & ~m_signal_mask, AK::memory_order_release);
}

u32 Thread::update_signal_mask(u32 signal_mask)
{
    ScopedSpinLock lock(g_scheduler_lock);
    auto previous_signal_mask = m_signal_mask;
    m_signal_mask = signal_mask;
    m_have_any_unmasked_pending_signals.store(m_pending_signals & ~m_signal_mask, AK::memory_order_release);
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
    m_have_any_unmasked_pending_signals.store(m_pending_signals & ~m_signal_mask, AK::memory_order_release);
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
    ScopedSpinLock lock(g_scheduler_lock);
    if (dispatch_signal(signal) == ShouldUnblockThread::No)
        Scheduler::yield();
}

ShouldUnblockThread Thread::dispatch_one_pending_signal()
{
    ASSERT(m_lock.own_lock());
    u32 signal_candidates = m_pending_signals & ~m_signal_mask;
    ASSERT(signal_candidates);

    u8 signal = 1;
    for (; signal < 32; ++signal) {
        if (signal_candidates & (1 << (signal - 1))) {
            break;
        }
    }
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
    set_state(m_stop_state);
    m_stop_state = State::Invalid;
    // make sure SemiPermanentBlocker is unblocked
    if (m_state != Thread::Runnable && m_state != Thread::Running) {
        ScopedSpinLock lock(m_lock);
        if (m_blocker && m_blocker->is_reason_signal())
            unblock();
    }
}

ShouldUnblockThread Thread::dispatch_signal(u8 signal)
{
    ASSERT_INTERRUPTS_DISABLED();
    ASSERT(g_scheduler_lock.own_lock());
    ASSERT(signal > 0 && signal <= 32);
    ASSERT(process().is_user_process());

#ifdef SIGNAL_DEBUG
    klog() << "signal: dispatch signal " << signal << " to " << *this;
#endif

    if (m_state == Invalid || !is_initialized()) {
        // Thread has barely been created, we need to wait until it is
        // at least in Runnable state and is_initialized() returns true,
        // which indicates that it is fully set up an we actually have
        // a register state on the stack that we can modify
        return ShouldUnblockThread::No;
    }

    auto& action = m_signal_action_data[signal];
    // FIXME: Implement SA_SIGINFO signal handlers.
    ASSERT(!(action.flags & SA_SIGINFO));

    // Mark this signal as handled.
    m_pending_signals &= ~(1 << (signal - 1));
    m_have_any_unmasked_pending_signals.store(m_pending_signals & ~m_signal_mask, AK::memory_order_release);

    if (signal == SIGSTOP) {
        if (!is_stopped()) {
            m_stop_signal = SIGSTOP;
            set_state(State::Stopped);
        }
        return ShouldUnblockThread::No;
    }

    if (signal == SIGCONT && is_stopped()) {
        resume_from_stopped();
    } else {
        auto* thread_tracer = tracer();
        if (thread_tracer != nullptr) {
            // when a thread is traced, it should be stopped whenever it receives a signal
            // the tracer is notified of this by using waitpid()
            // only "pending signals" from the tracer are sent to the tracee
            if (!thread_tracer->has_pending_signal(signal)) {
                m_stop_signal = signal;
                // make sure SemiPermanentBlocker is unblocked
                ScopedSpinLock lock(m_lock);
                if (m_blocker && m_blocker->is_reason_signal())
                    unblock();
                set_state(Stopped);
                return ShouldUnblockThread::No;
            }
            thread_tracer->unset_signal(signal);
        }
    }

    auto handler_vaddr = action.handler_or_sigaction;
    if (handler_vaddr.is_null()) {
        switch (default_signal_action(signal)) {
        case DefaultSignalAction::Stop:
            m_stop_signal = signal;
            set_state(Stopped);
            return ShouldUnblockThread::No;
        case DefaultSignalAction::DumpCore:
            process().for_each_thread([](auto& thread) {
                thread.set_dump_backtrace_on_finalization();
                return IterationDecision::Continue;
            });
            [[fallthrough]];
        case DefaultSignalAction::Terminate:
            m_process->terminate_due_to_signal(signal);
            return ShouldUnblockThread::No;
        case DefaultSignalAction::Ignore:
            ASSERT_NOT_REACHED();
        case DefaultSignalAction::Continue:
            return ShouldUnblockThread::Yes;
        }
        ASSERT_NOT_REACHED();
    }

    if (handler_vaddr.as_ptr() == SIG_IGN) {
#ifdef SIGNAL_DEBUG
        klog() << "signal: " << *this << " ignored signal " << signal;
#endif
        return ShouldUnblockThread::Yes;
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
    return ShouldUnblockThread::Yes;
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

void Thread::set_state(State new_state)
{
    ScopedSpinLock lock(g_scheduler_lock);
    if (new_state == m_state)
        return;

    if (new_state == Blocked) {
        // we should always have a Blocker while blocked
        ASSERT(m_blocker != nullptr);
    }

    auto previous_state = m_state;
    if (previous_state == Invalid) {
        // If we were *just* created, we may have already pending signals
        ScopedSpinLock thread_lock(m_lock);
        if (has_unmasked_pending_signals()) {
            dbg() << "Dispatch pending signals to new thread " << *this;
            dispatch_one_pending_signal();
        }
    }

    if (new_state == Stopped) {
        m_stop_state = m_state;
    }

    m_state = new_state;
#ifdef THREAD_DEBUG
    dbg() << "Set Thread " << *this << " state to " << state_string();
#endif

    if (m_process->pid() != 0) {
        update_state_for_thread(previous_state);
        ASSERT(g_scheduler_data->has_thread(*this));
    }

    if (m_state == Dying) {
        ASSERT(previous_state != Queued);
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
    ProcessPagingScope paging_scope(process);

    // To prevent a context switch involving this thread, which may happen
    // on another processor, we need to acquire the scheduler lock while
    // walking the stack
    {
        ScopedSpinLock lock(g_scheduler_lock);
        FlatPtr stack_ptr, eip;
        if (Processor::get_context_frame_ptr(*this, stack_ptr, eip)) {
            recognized_symbols.append({ eip, symbolicate_kernel_address(eip) });
            while (stack_ptr) {
                FlatPtr retaddr;

                if (is_user_range(VirtualAddress(stack_ptr), sizeof(FlatPtr) * 2)) {
                    if (!copy_from_user(&retaddr, &((FlatPtr*)stack_ptr)[1]))
                        break;
                    recognized_symbols.append({ retaddr, symbolicate_kernel_address(retaddr) });
                    if (!copy_from_user(&stack_ptr, (FlatPtr*)stack_ptr))
                        break;
                } else {
                    void* fault_at;
                    if (!safe_memcpy(&retaddr, &((FlatPtr*)stack_ptr)[1], sizeof(FlatPtr), fault_at))
                        break;
                    recognized_symbols.append({ retaddr, symbolicate_kernel_address(retaddr) });
                    if (!safe_memcpy(&stack_ptr, (FlatPtr*)stack_ptr, sizeof(FlatPtr), fault_at))
                        break;
                }
            }
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
        memcpy(thread_local_storage, process().m_master_tls_region->vaddr().as_ptr(), process().m_master_tls_size);
    return KSuccess;
}

const LogStream& operator<<(const LogStream& stream, const Thread& value)
{
    return stream << value.process().name() << "(" << value.pid().value() << ":" << value.tid().value() << ")";
}

Thread::BlockResult Thread::wait_on(WaitQueue& queue, const char* reason, timeval* timeout, Atomic<bool>* lock, RefPtr<Thread> beneficiary)
{
    auto* current_thread = Thread::current();
    TimerId timer_id {};
    bool did_unlock;

    {
        ScopedCritical critical;
        // We need to be in a critical section *and* then also acquire the
        // scheduler lock. The only way acquiring the scheduler lock could
        // block us is if another core were to be holding it, in which case
        // we need to wait until the scheduler lock is released again
        {
            ScopedSpinLock sched_lock(g_scheduler_lock);
            // m_queue can only be accessed safely if g_scheduler_lock is held!
            m_queue = &queue;
            if (!queue.enqueue(*current_thread)) {
                // The WaitQueue was already requested to wake someone when
                // nobody was waiting. So return right away as we shouldn't
                // be waiting

                // The API contract guarantees we return with interrupts enabled,
                // regardless of how we got called
                critical.set_interrupt_flag_on_destruction(true);

                return BlockResult::NotBlocked;
            }

            did_unlock = unlock_process_if_locked();
            if (lock)
                *lock = false;
            set_state(State::Queued);
            m_wait_reason = reason;

            if (timeout) {
                timer_id = TimerQueue::the().add_timer(*timeout, [&]() {
                    wake_from_queue();
                });
            }

            // Yield and wait for the queue to wake us up again.
            if (beneficiary)
                Scheduler::donate_to(beneficiary, reason);
            else
                Scheduler::yield();
        }

        // We've unblocked, relock the process if needed and carry on.
        relock_process(did_unlock);

        // This looks counter productive, but we may not actually leave
        // the critical section we just restored. It depends on whether
        // we were in one while being called.
        if (current_thread->should_die()) {
            // We're being unblocked so that we can clean up. We shouldn't
            // be in Dying state until we're about to return back to user mode
            ASSERT(current_thread->state() == Thread::Running);
#ifdef THREAD_DEBUG
            dbg() << "Dying thread " << *current_thread << " was unblocked";
#endif
        }
    }

    BlockResult result(BlockResult::WokeNormally);
    {
        // To be able to look at m_wait_queue_node we once again need the
        // scheduler lock, which is held when we insert into the queue
        ScopedSpinLock sched_lock(g_scheduler_lock);

        if (m_queue) {
            ASSERT(m_queue == &queue);
            // If our thread was still in the queue, we timed out
            m_queue = nullptr;
            if (queue.dequeue(*current_thread))
                result = BlockResult::InterruptedByTimeout;
        } else {
            // Our thread was already removed from the queue. The only
            // way this can happen if someone else is trying to kill us.
            // In this case, the queue should not contain us anymore.
            result = BlockResult::InterruptedByDeath;
        }

        // Make sure we cancel the timer if woke normally.
        if (timeout && !result.was_interrupted())
            TimerQueue::the().cancel_timer(timer_id);
    }

    // The API contract guarantees we return with interrupts enabled,
    // regardless of how we got called
    sti();
    return result;
}

void Thread::wake_from_queue()
{
    ScopedSpinLock lock(g_scheduler_lock);
    ASSERT(state() == State::Queued);
    m_wait_reason = nullptr;
    if (this != Thread::current())
        set_state(State::Runnable);
    else
        set_state(State::Running);
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

void Thread::start_tracing_from(ProcessID tracer)
{
    m_tracer = ThreadTracer::create(tracer);
}

void Thread::stop_tracing()
{
    m_tracer = nullptr;
}

void Thread::tracer_trap(const RegisterState& regs)
{
    ASSERT(m_tracer.ptr());
    m_tracer->set_regs(regs);
    send_urgent_signal_to_self(SIGTRAP);
}

const Thread::Blocker& Thread::blocker() const
{
    ASSERT(m_lock.own_lock());
    ASSERT(m_blocker);
    return *m_blocker;
}

}

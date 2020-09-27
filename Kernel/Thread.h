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

#pragma once

#include <AK/Function.h>
#include <AK/IntrusiveList.h>
#include <AK/Optional.h>
#include <AK/OwnPtr.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <AK/WeakPtr.h>
#include <AK/Weakable.h>
#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/Forward.h>
#include <Kernel/KResult.h>
#include <Kernel/Scheduler.h>
#include <Kernel/ThreadTracer.h>
#include <Kernel/UnixTypes.h>
#include <LibC/fd_set.h>
#include <LibELF/AuxiliaryVector.h>

namespace Kernel {

enum class ShouldUnblockThread {
    No = 0,
    Yes
};

struct SignalActionData {
    VirtualAddress handler_or_sigaction;
    u32 mask { 0 };
    int flags { 0 };
};

struct ThreadSpecificData {
    ThreadSpecificData* self;
};

#define THREAD_PRIORITY_MIN 1
#define THREAD_PRIORITY_LOW 10
#define THREAD_PRIORITY_NORMAL 30
#define THREAD_PRIORITY_HIGH 50
#define THREAD_PRIORITY_MAX 99

#define THREAD_AFFINITY_DEFAULT 0xffffffff

class Thread
    : public RefCounted<Thread>
    , public Weakable<Thread> {
    AK_MAKE_NONCOPYABLE(Thread);
    AK_MAKE_NONMOVABLE(Thread);

    friend class Process;
    friend class Scheduler;

public:
    inline static Thread* current()
    {
        return Processor::current().current_thread();
    }

    explicit Thread(NonnullRefPtr<Process>);
    ~Thread();

    static RefPtr<Thread> from_tid(ThreadID);
    static void finalize_dying_threads();

    ThreadID tid() const { return m_tid; }
    ProcessID pid() const;

    void set_priority(u32 p) { m_priority = p; }
    u32 priority() const { return m_priority; }

    void set_priority_boost(u32 boost) { m_priority_boost = boost; }
    u32 priority_boost() const { return m_priority_boost; }

    u32 effective_priority() const;

    KResult try_join(Thread& joiner)
    {
        if (&joiner == this)
            return KResult(-EDEADLK);

        ScopedSpinLock lock(m_lock);
        if (!m_is_joinable || state() == Dead)
            return KResult(-EINVAL);

        Thread* expected = nullptr;
        if (!m_joiner.compare_exchange_strong(expected, &joiner, AK::memory_order_acq_rel))
            return KResult(-EINVAL);

        // From this point on the thread is no longer joinable by anyone
        // else. It also means that if the join is timed, it becomes
        // detached when a timeout happens.
        m_is_joinable = false;
        return KSuccess;
    }

    void join_done()
    {
        // To avoid possible deadlocking, this function must not acquire
        // m_lock. This deadlock could occur if the joiner times out
        // almost at the same time as this thread, and calls into this
        // function to clear the joiner.
        m_joiner.store(nullptr, AK::memory_order_release);
    }

    void detach()
    {
        ScopedSpinLock lock(m_lock);
        m_is_joinable = false;
    }

    bool is_joinable() const
    {
        ScopedSpinLock lock(m_lock);
        return m_is_joinable;
    }

    Process& process() { return m_process; }
    const Process& process() const { return m_process; }

    String backtrace();
    Vector<FlatPtr> raw_backtrace(FlatPtr ebp, FlatPtr eip) const;

    String name() const
    {
        // Because the name can be changed, we can't return a const
        // reference here. We must make a copy
        ScopedSpinLock lock(m_lock);
        return m_name;
    }
    void set_name(const StringView& s)
    {
        ScopedSpinLock lock(m_lock);
        m_name = s;
    }
    void set_name(String&& name)
    {
        ScopedSpinLock lock(m_lock);
        m_name = move(name);
    }

    void finalize();

    enum State : u8 {
        Invalid = 0,
        Runnable,
        Running,
        Dying,
        Dead,
        Stopped,
        Blocked,
        Queued,
    };

    class Blocker {
    public:
        virtual ~Blocker() { }
        virtual bool should_unblock(Thread&) = 0;
        virtual const char* state_string() const = 0;
        virtual bool is_reason_signal() const { return false; }
        virtual timespec* override_timeout(timespec* timeout) { return timeout; }
        virtual void was_unblocked() { }
        void set_interrupted_by_death()
        {
            ScopedSpinLock lock(m_lock);
            m_was_interrupted_by_death = true;
        }
        bool was_interrupted_by_death() const
        {
            ScopedSpinLock lock(m_lock);
            return m_was_interrupted_by_death;
        }
        void set_interrupted_by_signal()
        {
            ScopedSpinLock lock(m_lock);
            m_was_interrupted_while_blocked = true;
        }
        bool was_interrupted_by_signal() const
        {
            ScopedSpinLock lock(m_lock);
            return m_was_interrupted_while_blocked;
        }

    protected:
        mutable RecursiveSpinLock m_lock;

    private:
        bool m_was_interrupted_while_blocked { false };
        bool m_was_interrupted_by_death { false };
        friend class Thread;
    };

    class JoinBlocker final : public Blocker {
    public:
        explicit JoinBlocker(Thread& joinee, KResult& try_join_result, void*& joinee_exit_value);
        virtual bool should_unblock(Thread&) override;
        virtual const char* state_string() const override { return "Joining"; }
        virtual void was_unblocked() override;
        void joinee_exited(void* value);

    private:
        Thread* m_joinee;
        void*& m_joinee_exit_value;
        bool m_join_error { false };
    };

    class FileDescriptionBlocker : public Blocker {
    public:
        const FileDescription& blocked_description() const;

    protected:
        explicit FileDescriptionBlocker(const FileDescription&);

    private:
        NonnullRefPtr<FileDescription> m_blocked_description;
    };

    class AcceptBlocker final : public FileDescriptionBlocker {
    public:
        explicit AcceptBlocker(const FileDescription&);
        virtual bool should_unblock(Thread&) override;
        virtual const char* state_string() const override { return "Accepting"; }
    };

    class ConnectBlocker final : public FileDescriptionBlocker {
    public:
        explicit ConnectBlocker(const FileDescription&);
        virtual bool should_unblock(Thread&) override;
        virtual const char* state_string() const override { return "Connecting"; }
    };

    class WriteBlocker final : public FileDescriptionBlocker {
    public:
        explicit WriteBlocker(const FileDescription&);
        virtual bool should_unblock(Thread&) override;
        virtual const char* state_string() const override { return "Writing"; }
        virtual timespec* override_timeout(timespec*) override;

    private:
        timespec m_deadline;
    };

    class ReadBlocker final : public FileDescriptionBlocker {
    public:
        explicit ReadBlocker(const FileDescription&);
        virtual bool should_unblock(Thread&) override;
        virtual const char* state_string() const override { return "Reading"; }
        virtual timespec* override_timeout(timespec*) override;

    private:
        timespec m_deadline;
    };

    class ConditionBlocker final : public Blocker {
    public:
        ConditionBlocker(const char* state_string, Function<bool()>&& condition);
        virtual bool should_unblock(Thread&) override;
        virtual const char* state_string() const override { return m_state_string; }

    private:
        Function<bool()> m_block_until_condition;
        const char* m_state_string { nullptr };
    };

    class SleepBlocker final : public Blocker {
    public:
        explicit SleepBlocker(u64 wakeup_time);
        virtual bool should_unblock(Thread&) override;
        virtual const char* state_string() const override { return "Sleeping"; }

    private:
        u64 m_wakeup_time { 0 };
    };

    class SelectBlocker final : public Blocker {
    public:
        typedef Vector<int, FD_SETSIZE> FDVector;
        SelectBlocker(const FDVector& read_fds, const FDVector& write_fds, const FDVector& except_fds);
        virtual bool should_unblock(Thread&) override;
        virtual const char* state_string() const override { return "Selecting"; }

    private:
        const FDVector& m_select_read_fds;
        const FDVector& m_select_write_fds;
        const FDVector& m_select_exceptional_fds;
    };

    class WaitBlocker final : public Blocker {
    public:
        WaitBlocker(int wait_options, ProcessID& waitee_pid);
        virtual bool should_unblock(Thread&) override;
        virtual const char* state_string() const override { return "Waiting"; }

    private:
        int m_wait_options { 0 };
        ProcessID& m_waitee_pid;
    };

    class SemiPermanentBlocker final : public Blocker {
    public:
        enum class Reason {
            Signal,
        };

        SemiPermanentBlocker(Reason reason);
        virtual bool should_unblock(Thread&) override;
        virtual const char* state_string() const override
        {
            switch (m_reason) {
            case Reason::Signal:
                return "Signal";
            }
            ASSERT_NOT_REACHED();
        }
        virtual bool is_reason_signal() const override { return m_reason == Reason::Signal; }

    private:
        Reason m_reason;
    };

    void did_schedule() { ++m_times_scheduled; }
    u32 times_scheduled() const { return m_times_scheduled; }

    void resume_from_stopped();

    bool is_stopped() const { return m_state == Stopped; }
    bool is_blocked() const { return m_state == Blocked; }
    bool has_blocker() const
    {
        ASSERT(m_lock.own_lock());
        return m_blocker != nullptr;
    }
    const Blocker& blocker() const;

    u32 cpu() const { return m_cpu.load(AK::MemoryOrder::memory_order_consume); }
    void set_cpu(u32 cpu) { m_cpu.store(cpu, AK::MemoryOrder::memory_order_release); }
    u32 affinity() const { return m_cpu_affinity; }
    void set_affinity(u32 affinity) { m_cpu_affinity = affinity; }

    u32 stack_ptr() const { return m_tss.esp; }

    RegisterState& get_register_dump_from_stack();

    TSS32& tss() { return m_tss; }
    const TSS32& tss() const { return m_tss; }
    State state() const { return m_state; }
    const char* state_string() const;
    u32 ticks() const { return m_ticks; }

    VirtualAddress thread_specific_data() const { return m_thread_specific_data; }
    size_t thread_specific_region_size() const { return m_thread_specific_region_size; }

    u64 sleep(u64 ticks);
    u64 sleep_until(u64 wakeup_time);

    class BlockResult {
    public:
        enum Type {
            WokeNormally,
            NotBlocked,
            InterruptedBySignal,
            InterruptedByDeath,
            InterruptedByTimeout,
        };

        BlockResult() = delete;

        BlockResult(Type type)
            : m_type(type)
        {
        }

        bool operator==(Type type) const
        {
            return m_type == type;
        }

        bool was_interrupted() const
        {
            switch (m_type) {
            case InterruptedBySignal:
            case InterruptedByDeath:
            case InterruptedByTimeout:
                return true;
            default:
                return false;
            }
        }

    private:
        Type m_type;
    };

    template<typename T, class... Args>
    [[nodiscard]] BlockResult block(timespec* timeout, Args&&... args)
    {
        T t(forward<Args>(args)...);

        ScopedSpinLock lock(m_lock);
        // We should never be blocking a blocked (or otherwise non-active) thread.
        ASSERT(state() == Thread::Running);
        ASSERT(m_blocker == nullptr);

        if (t.should_unblock(*this)) {
            // Don't block if the wake condition is already met
            return BlockResult::NotBlocked;
        }

        m_blocker = &t;
        m_blocker_timeout = t.override_timeout(timeout);
        set_state(Thread::Blocked);

        // Release our lock
        lock.unlock();

        // Yield to the scheduler, and wait for us to resume unblocked.
        yield_without_holding_big_lock();

        // Acquire our lock again
        lock.lock();

        // We should no longer be blocked once we woke up
        ASSERT(state() != Thread::Blocked);

        // Remove ourselves...
        m_blocker = nullptr;
        m_blocker_timeout = nullptr;

        // Notify the blocker that we are no longer blocking. It may need
        // to clean up now while we're still holding m_lock
        t.was_unblocked();

        if (t.was_interrupted_by_death())
            return BlockResult::InterruptedByDeath;

        if (t.was_interrupted_by_signal())
            return BlockResult::InterruptedBySignal;

        return BlockResult::WokeNormally;
    }

    [[nodiscard]] BlockResult block_until(const char* state_string, Function<bool()>&& condition)
    {
        return block<ConditionBlocker>(nullptr, state_string, move(condition));
    }

    BlockResult wait_on(WaitQueue& queue, const char* reason, timeval* timeout = nullptr, Atomic<bool>* lock = nullptr, RefPtr<Thread> beneficiary = {});
    void wake_from_queue();

    void unblock();

    // Tell this thread to unblock if needed,
    // gracefully unwind the stack and die.
    void set_should_die();
    bool should_die() const { return m_should_die; }
    void die_if_needed();

    bool tick();
    void set_ticks_left(u32 t) { m_ticks_left = t; }
    u32 ticks_left() const { return m_ticks_left; }

    u32 kernel_stack_base() const { return m_kernel_stack_base; }
    u32 kernel_stack_top() const { return m_kernel_stack_top; }

    void set_state(State);

    bool is_initialized() const { return m_initialized; }
    void set_initialized(bool initialized) { m_initialized = initialized; }

    void send_urgent_signal_to_self(u8 signal);
    void send_signal(u8 signal, Process* sender);
    void consider_unblock(time_t now_sec, long now_usec);

    u32 update_signal_mask(u32 signal_mask);
    u32 signal_mask_block(sigset_t signal_set, bool block);
    u32 signal_mask() const;
    void clear_signals();

    void set_dump_backtrace_on_finalization() { m_dump_backtrace_on_finalization = true; }

    ShouldUnblockThread dispatch_one_pending_signal();
    ShouldUnblockThread dispatch_signal(u8 signal);
    bool has_unmasked_pending_signals() const { return m_have_any_unmasked_pending_signals.load(AK::memory_order_consume); }
    void terminate_due_to_signal(u8 signal);
    bool should_ignore_signal(u8 signal) const;
    bool has_signal_handler(u8 signal) const;
    bool has_pending_signal(u8 signal) const;
    u32 pending_signals() const;

    FPUState& fpu_state() { return *m_fpu_state; }

    void set_default_signal_dispositions();
    bool push_value_on_stack(FlatPtr);

    KResultOr<u32> make_userspace_stack_for_main_thread(Vector<String> arguments, Vector<String> environment, Vector<AuxiliaryValue>);

    KResult make_thread_specific_region(Badge<Process>);

    unsigned syscall_count() const { return m_syscall_count; }
    void did_syscall() { ++m_syscall_count; }
    unsigned inode_faults() const { return m_inode_faults; }
    void did_inode_fault() { ++m_inode_faults; }
    unsigned zero_faults() const { return m_zero_faults; }
    void did_zero_fault() { ++m_zero_faults; }
    unsigned cow_faults() const { return m_cow_faults; }
    void did_cow_fault() { ++m_cow_faults; }

    unsigned file_read_bytes() const { return m_file_read_bytes; }
    unsigned file_write_bytes() const { return m_file_write_bytes; }

    void did_file_read(unsigned bytes)
    {
        m_file_read_bytes += bytes;
    }

    void did_file_write(unsigned bytes)
    {
        m_file_write_bytes += bytes;
    }

    unsigned unix_socket_read_bytes() const { return m_unix_socket_read_bytes; }
    unsigned unix_socket_write_bytes() const { return m_unix_socket_write_bytes; }

    void did_unix_socket_read(unsigned bytes)
    {
        m_unix_socket_read_bytes += bytes;
    }

    void did_unix_socket_write(unsigned bytes)
    {
        m_unix_socket_write_bytes += bytes;
    }

    unsigned ipv4_socket_read_bytes() const { return m_ipv4_socket_read_bytes; }
    unsigned ipv4_socket_write_bytes() const { return m_ipv4_socket_write_bytes; }

    void did_ipv4_socket_read(unsigned bytes)
    {
        m_ipv4_socket_read_bytes += bytes;
    }

    void did_ipv4_socket_write(unsigned bytes)
    {
        m_ipv4_socket_write_bytes += bytes;
    }

    const char* wait_reason() const
    {
        return m_wait_reason;
    }

    void set_active(bool active)
    {
        m_is_active.store(active, AK::memory_order_release);
    }

    bool is_finalizable() const
    {
        // We can't finalize as long as this thread is still running
        // Note that checking for Running state here isn't sufficient
        // as the thread may not be in Running state but switching out.
        // m_is_active is set to false once the context switch is
        // complete and the thread is not executing on any processor.
        if (m_is_active.load(AK::memory_order_consume))
            return false;
        // We can't finalize until the thread is either detached or
        // a join has started. We can't make m_is_joinable atomic
        // because that would introduce a race in try_join.
        ScopedSpinLock lock(m_lock);
        return !m_is_joinable;
    }

    RefPtr<Thread> clone(Process&);

    template<typename Callback>
    static IterationDecision for_each_in_state(State, Callback);
    template<typename Callback>
    static IterationDecision for_each_living(Callback);
    template<typename Callback>
    static IterationDecision for_each(Callback);

    static bool is_runnable_state(Thread::State state)
    {
        return state == Thread::State::Running || state == Thread::State::Runnable;
    }

    static constexpr u32 default_kernel_stack_size = 65536;
    static constexpr u32 default_userspace_stack_size = 4 * MiB;

    ThreadTracer* tracer() { return m_tracer.ptr(); }
    void start_tracing_from(ProcessID tracer);
    void stop_tracing();
    void tracer_trap(const RegisterState&);

    RecursiveSpinLock& get_lock() const { return m_lock; }

private:
    IntrusiveListNode m_runnable_list_node;
    IntrusiveListNode m_wait_queue_node;

private:
    friend struct SchedulerData;
    friend class WaitQueue;
    bool unlock_process_if_locked();
    void relock_process(bool did_unlock);
    String backtrace_impl();
    void reset_fpu_state();

    mutable RecursiveSpinLock m_lock;
    NonnullRefPtr<Process> m_process;
    ThreadID m_tid { -1 };
    TSS32 m_tss;
    Atomic<u32> m_cpu { 0 };
    u32 m_cpu_affinity { THREAD_AFFINITY_DEFAULT };
    u32 m_ticks { 0 };
    u32 m_ticks_left { 0 };
    u32 m_times_scheduled { 0 };
    u32 m_pending_signals { 0 };
    u32 m_signal_mask { 0 };
    u32 m_kernel_stack_base { 0 };
    u32 m_kernel_stack_top { 0 };
    OwnPtr<Region> m_kernel_stack_region;
    VirtualAddress m_thread_specific_data;
    size_t m_thread_specific_region_size { 0 };
    SignalActionData m_signal_action_data[32];
    Blocker* m_blocker { nullptr };
    timespec* m_blocker_timeout { nullptr };
    const char* m_wait_reason { nullptr };
    WaitQueue* m_queue { nullptr };

    Atomic<bool> m_is_active { false };
    bool m_is_joinable { true };
    Atomic<Thread*> m_joiner { nullptr };
    void* m_exit_value { nullptr };

    unsigned m_syscall_count { 0 };
    unsigned m_inode_faults { 0 };
    unsigned m_zero_faults { 0 };
    unsigned m_cow_faults { 0 };

    unsigned m_file_read_bytes { 0 };
    unsigned m_file_write_bytes { 0 };

    unsigned m_unix_socket_read_bytes { 0 };
    unsigned m_unix_socket_write_bytes { 0 };

    unsigned m_ipv4_socket_read_bytes { 0 };
    unsigned m_ipv4_socket_write_bytes { 0 };

    FPUState* m_fpu_state { nullptr };
    State m_state { Invalid };
    String m_name;
    u32 m_priority { THREAD_PRIORITY_NORMAL };
    u32 m_extra_priority { 0 };
    u32 m_priority_boost { 0 };

    u8 m_stop_signal { 0 };
    State m_stop_state { Invalid };

    bool m_dump_backtrace_on_finalization { false };
    bool m_should_die { false };
    bool m_initialized { false };
    Atomic<bool> m_have_any_unmasked_pending_signals { false };

    OwnPtr<ThreadTracer> m_tracer;

    void yield_without_holding_big_lock();
    void update_state_for_thread(Thread::State previous_state);
};

template<typename Callback>
inline IterationDecision Thread::for_each_living(Callback callback)
{
    ASSERT_INTERRUPTS_DISABLED();
    return Thread::for_each([callback](Thread& thread) -> IterationDecision {
        if (thread.state() != Thread::State::Dead && thread.state() != Thread::State::Dying)
            return callback(thread);
        return IterationDecision::Continue;
    });
}

template<typename Callback>
inline IterationDecision Thread::for_each(Callback callback)
{
    ASSERT_INTERRUPTS_DISABLED();
    ScopedSpinLock lock(g_scheduler_lock);
    auto ret = Scheduler::for_each_runnable(callback);
    if (ret == IterationDecision::Break)
        return ret;
    return Scheduler::for_each_nonrunnable(callback);
}

template<typename Callback>
inline IterationDecision Thread::for_each_in_state(State state, Callback callback)
{
    ASSERT_INTERRUPTS_DISABLED();
    ScopedSpinLock lock(g_scheduler_lock);
    auto new_callback = [=](Thread& thread) -> IterationDecision {
        if (thread.state() == state)
            return callback(thread);
        return IterationDecision::Continue;
    };
    if (is_runnable_state(state))
        return Scheduler::for_each_runnable(new_callback);
    return Scheduler::for_each_nonrunnable(new_callback);
}

const LogStream& operator<<(const LogStream&, const Thread&);

struct SchedulerData {
    typedef IntrusiveList<Thread, &Thread::m_runnable_list_node> ThreadList;

    ThreadList m_runnable_threads;
    ThreadList m_nonrunnable_threads;

    bool has_thread(Thread& thread) const
    {
        return m_runnable_threads.contains(thread) || m_nonrunnable_threads.contains(thread);
    }

    ThreadList& thread_list_for_state(Thread::State state)
    {
        if (Thread::is_runnable_state(state))
            return m_runnable_threads;
        return m_nonrunnable_threads;
    }
};

template<typename Callback>
inline IterationDecision Scheduler::for_each_runnable(Callback callback)
{
    ASSERT_INTERRUPTS_DISABLED();
    ASSERT(g_scheduler_lock.own_lock());
    auto& tl = g_scheduler_data->m_runnable_threads;
    for (auto it = tl.begin(); it != tl.end();) {
        auto& thread = *it;
        it = ++it;
        if (callback(thread) == IterationDecision::Break)
            return IterationDecision::Break;
    }

    return IterationDecision::Continue;
}

template<typename Callback>
inline IterationDecision Scheduler::for_each_nonrunnable(Callback callback)
{
    ASSERT_INTERRUPTS_DISABLED();
    ASSERT(g_scheduler_lock.own_lock());
    auto& tl = g_scheduler_data->m_nonrunnable_threads;
    for (auto it = tl.begin(); it != tl.end();) {
        auto& thread = *it;
        it = ++it;
        if (callback(thread) == IterationDecision::Break)
            return IterationDecision::Break;
    }

    return IterationDecision::Continue;
}

}

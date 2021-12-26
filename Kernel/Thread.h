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
#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/Forward.h>
#include <Kernel/KResult.h>
#include <Kernel/Scheduler.h>
#include <Kernel/UnixTypes.h>
#include <LibC/fd_set.h>

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

class Thread {
    friend class Process;
    friend class Scheduler;

public:
    static Thread* current;

    explicit Thread(Process&);
    ~Thread();

    static Thread* from_tid(int);
    static void initialize();
    static void finalize_dying_threads();

    static Vector<Thread*> all_threads();
    static bool is_thread(void*);

    int tid() const { return m_tid; }
    int pid() const;

    void set_priority(u32 p) { m_priority = p; }
    u32 priority() const { return m_priority; }

    void set_priority_boost(u32 boost) { m_priority_boost = boost; }
    u32 priority_boost() const { return m_priority_boost; }

    u32 effective_priority() const;

    void set_joinable(bool j) { m_is_joinable = j; }
    bool is_joinable() const { return m_is_joinable; }

    Process& process() { return m_process; }
    const Process& process() const { return m_process; }

    String backtrace(ProcessInspectionHandle&) const;
    Vector<uintptr_t> raw_backtrace(uintptr_t ebp) const;

    const String& name() const { return m_name; }
    void set_name(StringView s) { m_name = s; }

    void finalize();

    enum State : u8 {
        Invalid = 0,
        Runnable,
        Running,
        Skip1SchedulerPass,
        Skip0SchedulerPasses,
        Dying,
        Dead,
        Stopped,
        Blocked,
        Queued,
    };

    class Blocker {
    public:
        virtual ~Blocker() {}
        virtual bool should_unblock(Thread&, time_t now_s, long us) = 0;
        virtual const char* state_string() const = 0;
        void set_interrupted_by_death() { m_was_interrupted_by_death = true; }
        bool was_interrupted_by_death() const { return m_was_interrupted_by_death; }
        void set_interrupted_by_signal() { m_was_interrupted_while_blocked = true; }
        bool was_interrupted_by_signal() const { return m_was_interrupted_while_blocked; }

    private:
        bool m_was_interrupted_while_blocked { false };
        bool m_was_interrupted_by_death { false };
        friend class Thread;
    };

    class JoinBlocker final : public Blocker {
    public:
        explicit JoinBlocker(Thread& joinee, void*& joinee_exit_value);
        virtual bool should_unblock(Thread&, time_t now_s, long us) override;
        virtual const char* state_string() const override { return "Joining"; }
        void set_joinee_exit_value(void* value) { m_joinee_exit_value = value; }

    private:
        Thread& m_joinee;
        void*& m_joinee_exit_value;
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
        virtual bool should_unblock(Thread&, time_t, long) override;
        virtual const char* state_string() const override { return "Accepting"; }
    };

    class ConnectBlocker final : public FileDescriptionBlocker {
    public:
        explicit ConnectBlocker(const FileDescription&);
        virtual bool should_unblock(Thread&, time_t, long) override;
        virtual const char* state_string() const override { return "Connecting"; }
    };

    class WriteBlocker final : public FileDescriptionBlocker {
    public:
        explicit WriteBlocker(const FileDescription&);
        virtual bool should_unblock(Thread&, time_t, long) override;
        virtual const char* state_string() const override { return "Writing"; }

    private:
        Optional<timeval> m_deadline;
    };

    class ReadBlocker final : public FileDescriptionBlocker {
    public:
        explicit ReadBlocker(const FileDescription&);
        virtual bool should_unblock(Thread&, time_t, long) override;
        virtual const char* state_string() const override { return "Reading"; }

    private:
        Optional<timeval> m_deadline;
    };

    class ConditionBlocker final : public Blocker {
    public:
        ConditionBlocker(const char* state_string, Function<bool()>&& condition);
        virtual bool should_unblock(Thread&, time_t, long) override;
        virtual const char* state_string() const override { return m_state_string; }

    private:
        Function<bool()> m_block_until_condition;
        const char* m_state_string { nullptr };
    };

    class SleepBlocker final : public Blocker {
    public:
        explicit SleepBlocker(u64 wakeup_time);
        virtual bool should_unblock(Thread&, time_t, long) override;
        virtual const char* state_string() const override { return "Sleeping"; }

    private:
        u64 m_wakeup_time { 0 };
    };

    class SelectBlocker final : public Blocker {
    public:
        typedef Vector<int, FD_SETSIZE> FDVector;
        SelectBlocker(const timeval& tv, bool select_has_timeout, const FDVector& read_fds, const FDVector& write_fds, const FDVector& except_fds);
        virtual bool should_unblock(Thread&, time_t, long) override;
        virtual const char* state_string() const override { return "Selecting"; }

    private:
        timeval m_select_timeout;
        bool m_select_has_timeout { false };
        const FDVector& m_select_read_fds;
        const FDVector& m_select_write_fds;
        const FDVector& m_select_exceptional_fds;
    };

    class WaitBlocker final : public Blocker {
    public:
        WaitBlocker(int wait_options, pid_t& waitee_pid);
        virtual bool should_unblock(Thread&, time_t, long) override;
        virtual const char* state_string() const override { return "Waiting"; }

    private:
        int m_wait_options { 0 };
        pid_t& m_waitee_pid;
    };

    class SemiPermanentBlocker final : public Blocker {
    public:
        enum class Reason {
            Signal,
        };

        SemiPermanentBlocker(Reason reason);
        virtual bool should_unblock(Thread&, time_t, long) override;
        virtual const char* state_string() const override
        {
            switch (m_reason) {
            case Reason::Signal:
                return "Signal";
            }
            ASSERT_NOT_REACHED();
        }

    private:
        Reason m_reason;
    };

    void did_schedule() { ++m_times_scheduled; }
    u32 times_scheduled() const { return m_times_scheduled; }

    bool is_stopped() const { return m_state == Stopped; }
    bool is_blocked() const { return m_state == Blocked; }
    bool in_kernel() const { return (m_tss.cs & 0x03) == 0; }

    u32 frame_ptr() const { return m_tss.ebp; }
    u32 stack_ptr() const { return m_tss.esp; }

    RegisterState& get_register_dump_from_stack();

    u16 selector() const { return m_far_ptr.selector; }
    TSS32& tss() { return m_tss; }
    const TSS32& tss() const { return m_tss; }
    State state() const { return m_state; }
    const char* state_string() const;
    u32 ticks() const { return m_ticks; }

    VirtualAddress thread_specific_data() const { return m_thread_specific_data; }

    u64 sleep(u32 ticks);
    u64 sleep_until(u64 wakeup_time);

    enum class BlockResult {
        WokeNormally,
        InterruptedBySignal,
        InterruptedByDeath,
    };

    template<typename T, class... Args>
    [[nodiscard]] BlockResult block(Args&&... args)
    {
        // We should never be blocking a blocked (or otherwise non-active) thread.
        ASSERT(state() == Thread::Running);
        ASSERT(m_blocker == nullptr);

        T t(forward<Args>(args)...);
        m_blocker = &t;
        set_state(Thread::Blocked);

        // Yield to the scheduler, and wait for us to resume unblocked.
        yield_without_holding_big_lock();

        // We should no longer be blocked once we woke up
        ASSERT(state() != Thread::Blocked);

        // Remove ourselves...
        m_blocker = nullptr;

        if (t.was_interrupted_by_signal())
            return BlockResult::InterruptedBySignal;

        if (t.was_interrupted_by_death())
            return BlockResult::InterruptedByDeath;

        return BlockResult::WokeNormally;
    }

    [[nodiscard]] BlockResult block_until(const char* state_string, Function<bool()>&& condition)
    {
        return block<ConditionBlocker>(state_string, move(condition));
    }

    void wait_on(WaitQueue& queue, Atomic<bool>* lock = nullptr, Thread* beneficiary = nullptr, const char* reason = nullptr);
    void wake_from_queue();

    void unblock();

    // Tell this thread to unblock if needed,
    // gracefully unwind the stack and die.
    void set_should_die();
    void die_if_needed();

    const FarPtr& far_ptr() const { return m_far_ptr; }

    bool tick();
    void set_ticks_left(u32 t) { m_ticks_left = t; }
    u32 ticks_left() const { return m_ticks_left; }

    u32 kernel_stack_base() const { return m_kernel_stack_base; }
    u32 kernel_stack_top() const { return m_kernel_stack_top; }

    void set_selector(u16 s) { m_far_ptr.selector = s; }
    void set_state(State);

    void send_urgent_signal_to_self(u8 signal);
    void send_signal(u8 signal, Process* sender);
    void consider_unblock(time_t now_sec, long now_usec);

    void set_dump_backtrace_on_finalization() { m_dump_backtrace_on_finalization = true; }

    ShouldUnblockThread dispatch_one_pending_signal();
    ShouldUnblockThread dispatch_signal(u8 signal);
    bool has_unmasked_pending_signals() const;
    void terminate_due_to_signal(u8 signal);
    bool should_ignore_signal(u8 signal) const;
    bool has_signal_handler(u8 signal) const;

    FPUState& fpu_state() { return *m_fpu_state; }

    void set_default_signal_dispositions();
    void push_value_on_stack(uintptr_t);

    u32 make_userspace_stack_for_main_thread(Vector<String> arguments, Vector<String> environment);

    void make_thread_specific_region(Badge<Process>);

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

    Thread* clone(Process&);

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
    static constexpr u32 default_userspace_stack_size = 4 * MB;

private:
    IntrusiveListNode m_runnable_list_node;
    IntrusiveListNode m_wait_queue_node;

private:
    friend class SchedulerData;
    friend class WaitQueue;
    bool unlock_process_if_locked();
    void relock_process();
    String backtrace_impl() const;
    void reset_fpu_state();

    Process& m_process;
    int m_tid { -1 };
    TSS32 m_tss;
    FarPtr m_far_ptr;
    u32 m_ticks { 0 };
    u32 m_ticks_left { 0 };
    u32 m_times_scheduled { 0 };
    u32 m_pending_signals { 0 };
    u32 m_signal_mask { 0 };
    u32 m_kernel_stack_base { 0 };
    u32 m_kernel_stack_top { 0 };
    OwnPtr<Region> m_kernel_stack_region;
    VirtualAddress m_thread_specific_data;
    SignalActionData m_signal_action_data[32];
    Blocker* m_blocker { nullptr };

    bool m_is_joinable { true };
    Thread* m_joiner { nullptr };
    Thread* m_joinee { nullptr };
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

    bool m_dump_backtrace_on_finalization { false };
    bool m_should_die { false };

    void yield_without_holding_big_lock();
};

HashTable<Thread*>& thread_table();

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
    auto ret = Scheduler::for_each_runnable(callback);
    if (ret == IterationDecision::Break)
        return ret;
    return Scheduler::for_each_nonrunnable(callback);
}

template<typename Callback>
inline IterationDecision Thread::for_each_in_state(State state, Callback callback)
{
    ASSERT_INTERRUPTS_DISABLED();
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
    auto& tl = g_scheduler_data->m_nonrunnable_threads;
    for (auto it = tl.begin(); it != tl.end();) {
        auto& thread = *it;
        it = ++it;
        if (callback(thread) == IterationDecision::Break)
            return IterationDecision::Break;
    }

    return IterationDecision::Continue;
}

u16 thread_specific_selector();
Descriptor& thread_specific_descriptor();

}

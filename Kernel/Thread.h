#pragma once

#include <AK/AKString.h>
#include <AK/Function.h>
#include <AK/IntrusiveList.h>
#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <AK/Vector.h>
#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/KResult.h>
#include <Kernel/Scheduler.h>
#include <Kernel/UnixTypes.h>
#include <Kernel/VM/Region.h>
#include <LibC/fd_set.h>

class Alarm;
class FileDescription;
class Process;
class ProcessInspectionHandle;
class Region;

enum class ShouldUnblockThread {
    No = 0,
    Yes
};

struct SignalActionData {
    VirtualAddress handler_or_sigaction;
    u32 mask { 0 };
    int flags { 0 };
};

class Thread {
    friend class Process;
    friend class Scheduler;

public:
    explicit Thread(Process&);
    ~Thread();

    static void initialize();
    static void finalize_dying_threads();

    static Vector<Thread*> all_threads();
    static bool is_thread(void*);

    int tid() const { return m_tid; }
    int pid() const;

    Process& process() { return m_process; }
    const Process& process() const { return m_process; }

    String backtrace(ProcessInspectionHandle&) const;

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
    };

    class Blocker {
    public:
        virtual ~Blocker() {}
        virtual bool should_unblock(Thread&, time_t now_s, long us) = 0;
        virtual const char* state_string() const = 0;
        void set_interrupted_by_signal() { m_was_interrupted_while_blocked = true; }
        bool was_interrupted_by_signal() const { return m_was_interrupted_while_blocked; }

    private:
        bool m_was_interrupted_while_blocked { false };
        friend class Thread;
        IntrusiveListNode m_blocker_list_node;
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

    class ReceiveBlocker final : public FileDescriptionBlocker {
    public:
        explicit ReceiveBlocker(const FileDescription&);
        virtual bool should_unblock(Thread&, time_t, long) override;
        virtual const char* state_string() const override { return "Receiving"; }
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
    };

    class ReadBlocker final : public FileDescriptionBlocker {
    public:
        explicit ReadBlocker(const FileDescription&);
        virtual bool should_unblock(Thread&, time_t, long) override;
        virtual const char* state_string() const override { return "Reading"; }
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
            Lurking,
            Signal,
        };

        SemiPermanentBlocker(Reason reason);
        virtual bool should_unblock(Thread&, time_t, long) override;
        virtual const char* state_string() const override
        {
            switch (m_reason) {
            case Reason::Lurking:
                return "Lurking";
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

    u16 selector() const { return m_far_ptr.selector; }
    TSS32& tss() { return m_tss; }
    const TSS32& tss() const { return m_tss; }
    State state() const { return m_state; }
    const char* state_string() const;
    u32 ticks() const { return m_ticks; }

    u64 sleep(u32 ticks);

    enum class BlockResult {
        WokeNormally,
        InterruptedBySignal,
    };

    template<typename T, class... Args>
    [[nodiscard]] BlockResult block(Args&&... args)
    {
        // We should never be blocking a blocked (or otherwise non-active) thread.
        ASSERT(state() == Thread::Running);

        T t(AK::forward<Args>(args)...);
        m_blockers.prepend(t);

        // Enter blocked state.
        set_state(Thread::Blocked);

        // Yield to the scheduler, and wait for us to resume unblocked.
        block_helper();

        // We should no longer be blocked once we woke up
        ASSERT(state() != Thread::Blocked);

        // We should be the first blocker, otherwise we're waking up in a
        // different order than we are waiting: scheduler bug?
        ASSERT(m_blockers.first() == &t);

        // Remove ourselves...
        m_blockers.remove(t);

        // If there are still pending blockers that need to be waited for, then
        // set state back to Blocked, for the next one to be handled.
        // Otherwise, we're good now, and done with blocking state.
        if (!m_blockers.is_empty()) {
            if (!m_blockers.first()->was_interrupted_by_signal())
                set_state(Thread::Blocked);
        }

        if (t.was_interrupted_by_signal())
            return BlockResult::InterruptedBySignal;

        return BlockResult::WokeNormally;
    };

    [[nodiscard]] BlockResult block_until(const char* state_string, Function<bool()>&& condition)
    {
        return block<ConditionBlocker>(state_string, move(condition));
    }

    void unblock();

    const FarPtr& far_ptr() const { return m_far_ptr; }

    bool tick();
    void set_ticks_left(u32 t) { m_ticks_left = t; }
    u32 ticks_left() const { return m_ticks_left; }

    u32 kernel_stack_base() const { return m_kernel_stack_base; }
    u32 kernel_stack_top() const { return m_kernel_stack_top; }
    u32 kernel_stack_for_signal_handler_base() const { return m_kernel_stack_for_signal_handler_region ? m_kernel_stack_for_signal_handler_region->vaddr().get() : 0; }

    void set_selector(u16 s) { m_far_ptr.selector = s; }
    void set_state(State);

    void send_signal(u8 signal, Process* sender);
    void consider_unblock(time_t now_sec, long now_usec);

    void set_dump_backtrace_on_finalization() { m_dump_backtrace_on_finalization = true; }

    ShouldUnblockThread dispatch_one_pending_signal();
    ShouldUnblockThread dispatch_signal(u8 signal);
    bool has_unmasked_pending_signals() const;
    void terminate_due_to_signal(u8 signal);
    bool should_ignore_signal(u8 signal) const;

    FPUState& fpu_state() { return *m_fpu_state; }
    bool has_used_fpu() const { return m_has_used_fpu; }
    void set_has_used_fpu(bool b) { m_has_used_fpu = b; }

    void set_default_signal_dispositions();
    void push_value_on_user_stack(RegisterDump&, u32);
    void push_value_on_stack(u32);
    void make_userspace_stack_for_main_thread(Vector<String> arguments, Vector<String> environment);
    void make_userspace_stack_for_secondary_thread(void* argument);

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

private:
    IntrusiveListNode m_runnable_list_node;

private:
    friend class SchedulerData;
    String backtrace_impl() const;
    Process& m_process;
    int m_tid { -1 };
    TSS32 m_tss;
    OwnPtr<TSS32> m_tss_to_resume_kernel;
    FarPtr m_far_ptr;
    u32 m_ticks { 0 };
    u32 m_ticks_left { 0 };
    u32 m_times_scheduled { 0 };
    u32 m_pending_signals { 0 };
    u32 m_signal_mask { 0 };
    u32 m_kernel_stack_base { 0 };
    u32 m_kernel_stack_top { 0 };
    RefPtr<Region> m_userspace_stack_region;
    RefPtr<Region> m_kernel_stack_region;
    RefPtr<Region> m_kernel_stack_for_signal_handler_region;
    SignalActionData m_signal_action_data[32];
    Region* m_signal_stack_user_region { nullptr };
    IntrusiveList<Blocker, &Blocker::m_blocker_list_node> m_blockers;
    FPUState* m_fpu_state { nullptr };
    State m_state { Invalid };
    bool m_has_used_fpu { false };
    bool m_dump_backtrace_on_finalization { false };

    void block_helper();
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

inline const LogStream& operator<<(const LogStream& stream, const Thread& value)
{
    return stream << "Thread{" << &value << "}(" << value.pid() << ":" << value.tid() << ")";
}

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

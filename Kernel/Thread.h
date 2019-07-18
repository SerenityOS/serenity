#pragma once

#include <AK/AKString.h>
#include <AK/Function.h>
#include <AK/InlineLinkedList.h>
#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <AK/Vector.h>
#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/KResult.h>
#include <Kernel/UnixTypes.h>
#include <Kernel/VM/Region.h>

class Alarm;
class FileDescription;
class Process;
class Region;
class Thread;

enum class ShouldUnblockThread {
    No = 0,
    Yes
};

struct SignalActionData {
    VirtualAddress handler_or_sigaction;
    u32 mask { 0 };
    int flags { 0 };
};

extern InlineLinkedList<Thread>* g_runnable_threads;
extern InlineLinkedList<Thread>* g_nonrunnable_threads;

class Thread : public InlineLinkedListNode<Thread> {
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

        __Begin_Blocked_States__,
        BlockedLurking,
        BlockedWait,
        BlockedSignal,
        BlockedCondition,
        __End_Blocked_States__
    };

    class ThreadBlocker {
    public:
        virtual ~ThreadBlocker() {}
        virtual bool should_unblock(Thread&, time_t now_s, long us) = 0;
    };

    class ThreadBlockerFileDescription : public ThreadBlocker {
    public:
        ThreadBlockerFileDescription(const RefPtr<FileDescription>& description);
        RefPtr<FileDescription> blocked_description() const;

    private:
        RefPtr<FileDescription> m_blocked_description;
    };

    class ThreadBlockerAccept : public ThreadBlockerFileDescription {
    public:
        ThreadBlockerAccept(const RefPtr<FileDescription>& description);
        virtual bool should_unblock(Thread&, time_t, long) override;
    };

    class ThreadBlockerReceive : public ThreadBlockerFileDescription {
    public:
        ThreadBlockerReceive(const RefPtr<FileDescription>& description);
        virtual bool should_unblock(Thread&, time_t, long) override;
    };

    class ThreadBlockerConnect : public ThreadBlockerFileDescription {
    public:
        ThreadBlockerConnect(const RefPtr<FileDescription>& description);
        virtual bool should_unblock(Thread&, time_t, long) override;
    };

    class ThreadBlockerWrite : public ThreadBlockerFileDescription {
    public:
        ThreadBlockerWrite(const RefPtr<FileDescription>& description);
        virtual bool should_unblock(Thread&, time_t, long) override;
    };

    class ThreadBlockerRead : public ThreadBlockerFileDescription {
    public:
        ThreadBlockerRead(const RefPtr<FileDescription>& description);
        virtual bool should_unblock(Thread&, time_t, long) override;
    };

    class ThreadBlockerCondition : public ThreadBlocker {
    public:
        ThreadBlockerCondition(Function<bool()> &condition);
        virtual bool should_unblock(Thread&, time_t, long) override;

    private:
        Function<bool()> m_block_until_condition;
    };

    class ThreadBlockerSleep : public ThreadBlocker {
    public:
        ThreadBlockerSleep(u64 wakeup_time);
        virtual bool should_unblock(Thread&, time_t, long) override;

    private:
        u64 m_wakeup_time { 0 };
    };

    class ThreadBlockerSelect : public ThreadBlocker {
    public:
        ThreadBlockerSelect(const timeval& tv, bool select_has_timeout, const Vector<int>& read_fds, const Vector<int>& write_fds, const Vector<int>& except_fds);
        virtual bool should_unblock(Thread&, time_t, long) override;

    private:
        timeval m_select_timeout;
        bool m_select_has_timeout { false };
        const Vector<int>& m_select_read_fds;
        const Vector<int>& m_select_write_fds;
        const Vector<int>& m_select_exceptional_fds;
    };

    void did_schedule() { ++m_times_scheduled; }
    u32 times_scheduled() const { return m_times_scheduled; }

    bool is_stopped() const { return m_state == Stopped; }
    bool is_blocked() const
    {
        return m_state > __Begin_Blocked_States__ && m_state < __End_Blocked_States__;
    }
    bool in_kernel() const { return (m_tss.cs & 0x03) == 0; }

    u32 frame_ptr() const { return m_tss.ebp; }
    u32 stack_ptr() const { return m_tss.esp; }

    u16 selector() const { return m_far_ptr.selector; }
    TSS32& tss() { return m_tss; }
    State state() const { return m_state; }
    u32 ticks() const { return m_ticks; }
    pid_t waitee_pid() const { return m_waitee_pid; }

    u64 sleep(u32 ticks);
    void block(Thread::State);
    void block(ThreadBlocker& blocker);
    void unblock();

    void block_until(Function<bool()>&&);
    KResult wait_for_connect(FileDescription&);

    const FarPtr& far_ptr() const { return m_far_ptr; }

    bool tick();
    void set_ticks_left(u32 t) { m_ticks_left = t; }
    u32 ticks_left() const { return m_ticks_left; }

    u32 kernel_stack_base() const { return m_kernel_stack_base; }
    u32 kernel_stack_for_signal_handler_base() const { return m_kernel_stack_for_signal_handler_region ? m_kernel_stack_for_signal_handler_region->vaddr().get() : 0; }

    void set_selector(u16 s) { m_far_ptr.selector = s; }
    void set_state(State);

    void send_signal(u8 signal, Process* sender);
    void consider_unblock(time_t now_sec, long now_usec);

    ShouldUnblockThread dispatch_one_pending_signal();
    ShouldUnblockThread dispatch_signal(u8 signal);
    bool has_unmasked_pending_signals() const;
    void terminate_due_to_signal(u8 signal);
    bool should_ignore_signal(u8 signal) const;

    FPUState& fpu_state() { return *m_fpu_state; }
    bool has_used_fpu() const { return m_has_used_fpu; }
    void set_has_used_fpu(bool b) { m_has_used_fpu = b; }

    void set_default_signal_dispositions();
    void push_value_on_stack(u32);
    void make_userspace_stack_for_main_thread(Vector<String> arguments, Vector<String> environment);
    void make_userspace_stack_for_secondary_thread(void* argument);

    Thread* clone(Process&);

    // For InlineLinkedList
    Thread* m_prev { nullptr };
    Thread* m_next { nullptr };

    InlineLinkedList<Thread>* thread_list() { return m_thread_list; }
    void set_thread_list(InlineLinkedList<Thread>*);

    template<typename Callback>
    static void for_each_in_state(State, Callback);
    template<typename Callback>
    static void for_each_living(Callback);
    template<typename Callback>
    static void for_each_runnable(Callback);
    template<typename Callback>
    static void for_each_nonrunnable(Callback);
    template<typename Callback>
    static void for_each(Callback);

    static bool is_runnable_state(Thread::State state)
    {
        return state == Thread::State::Running || state == Thread::State::Runnable;
    }

    static InlineLinkedList<Thread>* thread_list_for_state(Thread::State state)
    {
        if (is_runnable_state(state))
            return g_runnable_threads;
        return g_nonrunnable_threads;
    }

private:
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
    RefPtr<Region> m_kernel_stack_region;
    RefPtr<Region> m_kernel_stack_for_signal_handler_region;
    pid_t m_waitee_pid { -1 };
    int m_wait_options { 0 };
    SignalActionData m_signal_action_data[32];
    Region* m_signal_stack_user_region { nullptr };
    OwnPtr<ThreadBlocker> m_blocker;
    FPUState* m_fpu_state { nullptr };
    InlineLinkedList<Thread>* m_thread_list { nullptr };
    State m_state { Invalid };
    bool m_has_used_fpu { false };
    bool m_was_interrupted_while_blocked { false };
};

HashTable<Thread*>& thread_table();

const char* to_string(Thread::State);

template<typename Callback>
inline void Thread::for_each_in_state(State state, Callback callback)
{
    ASSERT_INTERRUPTS_DISABLED();
    for (auto* thread = thread_list_for_state(state)->head(); thread;) {
        auto* next_thread = thread->next();
        if (thread->state() == state)
            callback(*thread);
        thread = next_thread;
    }
}

template<typename Callback>
inline void Thread::for_each_living(Callback callback)
{
    ASSERT_INTERRUPTS_DISABLED();
    for (auto* thread = g_runnable_threads->head(); thread;) {
        auto* next_thread = thread->next();
        if (thread->state() != Thread::State::Dead && thread->state() != Thread::State::Dying)
            callback(*thread);
        thread = next_thread;
    }
    for (auto* thread = g_nonrunnable_threads->head(); thread;) {
        auto* next_thread = thread->next();
        if (thread->state() != Thread::State::Dead && thread->state() != Thread::State::Dying)
            callback(*thread);
        thread = next_thread;
    }
}

template<typename Callback>
inline void Thread::for_each(Callback callback)
{
    ASSERT_INTERRUPTS_DISABLED();
    for_each_runnable(callback);
    for_each_nonrunnable(callback);
}

template<typename Callback>
inline void Thread::for_each_runnable(Callback callback)
{
    ASSERT_INTERRUPTS_DISABLED();
    for (auto* thread = g_runnable_threads->head(); thread;) {
        auto* next_thread = thread->next();
        if (callback(*thread) == IterationDecision::Break)
            return;
        thread = next_thread;
    }
}

template<typename Callback>
inline void Thread::for_each_nonrunnable(Callback callback)
{
    ASSERT_INTERRUPTS_DISABLED();
    for (auto* thread = g_nonrunnable_threads->head(); thread;) {
        auto* next_thread = thread->next();
        if (callback(*thread) == IterationDecision::Break)
            return;
        thread = next_thread;
    }
}

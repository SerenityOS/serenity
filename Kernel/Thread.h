#pragma once

#include <AK/AKString.h>
#include <AK/InlineLinkedList.h>
#include <AK/OwnPtr.h>
#include <AK/RetainPtr.h>
#include <AK/Vector.h>
#include <Kernel/KResult.h>
#include <Kernel/VirtualAddress.h>
#include <Kernel/UnixTypes.h>
#include <Kernel/VM/Region.h>
#include <Kernel/i386.h>

class Alarm;
class FileDescription;
class Process;
class Region;
class Thread;

enum class ShouldUnblockThread
{
    No = 0,
    Yes
};

struct SignalActionData {
    VirtualAddress handler_or_sigaction;
    dword mask { 0 };
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

    enum State : byte
    {
        Invalid = 0,
        Runnable,
        Running,
        Skip1SchedulerPass,
        Skip0SchedulerPasses,
        Dying,
        Dead,
        Stopped,
        BlockedLurking,
        BlockedSleep,
        BlockedWait,
        BlockedRead,
        BlockedWrite,
        BlockedSignal,
        BlockedSelect,
        BlockedConnect,
        BlockedReceive,
        BlockedSnoozing,
    };

    void did_schedule() { ++m_times_scheduled; }
    dword times_scheduled() const { return m_times_scheduled; }

    bool is_stopped() const { return m_state == Stopped; }
    bool is_blocked() const
    {
        return m_state == BlockedSleep || m_state == BlockedWait || m_state == BlockedRead || m_state == BlockedWrite || m_state == BlockedSignal || m_state == BlockedSelect;
    }
    bool in_kernel() const { return (m_tss.cs & 0x03) == 0; }

    dword frame_ptr() const { return m_tss.ebp; }
    dword stack_ptr() const { return m_tss.esp; }

    word selector() const { return m_far_ptr.selector; }
    TSS32& tss() { return m_tss; }
    State state() const { return m_state; }
    dword ticks() const { return m_ticks; }
    pid_t waitee_pid() const { return m_waitee_pid; }

    void sleep(dword ticks);
    void block(Thread::State);
    void block(Thread::State, FileDescription&);
    void unblock();

    void set_wakeup_time(qword t) { m_wakeup_time = t; }
    qword wakeup_time() const { return m_wakeup_time; }
    void snooze_until(Alarm&);
    KResult wait_for_connect(FileDescription&);

    const FarPtr& far_ptr() const { return m_far_ptr; }

    bool tick();
    void set_ticks_left(dword t) { m_ticks_left = t; }
    dword ticks_left() const { return m_ticks_left; }

    dword kernel_stack_base() const { return m_kernel_stack_base; }
    dword kernel_stack_for_signal_handler_base() const { return m_kernel_stack_for_signal_handler_region ? m_kernel_stack_for_signal_handler_region->vaddr().get() : 0; }

    void set_selector(word s) { m_far_ptr.selector = s; }
    void set_state(State);

    void send_signal(byte signal, Process* sender);

    ShouldUnblockThread dispatch_one_pending_signal();
    ShouldUnblockThread dispatch_signal(byte signal);
    bool has_unmasked_pending_signals() const;
    void terminate_due_to_signal(byte signal);

    FPUState& fpu_state() { return *m_fpu_state; }
    bool has_used_fpu() const { return m_has_used_fpu; }
    void set_has_used_fpu(bool b) { m_has_used_fpu = b; }

    void set_default_signal_dispositions();
    void push_value_on_stack(dword);
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
    dword m_ticks { 0 };
    dword m_ticks_left { 0 };
    qword m_wakeup_time { 0 };
    dword m_times_scheduled { 0 };
    dword m_pending_signals { 0 };
    dword m_signal_mask { 0 };
    dword m_kernel_stack_base { 0 };
    RetainPtr<Region> m_kernel_stack_region;
    RetainPtr<Region> m_kernel_stack_for_signal_handler_region;
    pid_t m_waitee_pid { -1 };
    RetainPtr<FileDescription> m_blocked_descriptor;
    timeval m_select_timeout;
    SignalActionData m_signal_action_data[32];
    Region* m_signal_stack_user_region { nullptr };
    Alarm* m_snoozing_alarm { nullptr };
    Vector<int> m_select_read_fds;
    Vector<int> m_select_write_fds;
    Vector<int> m_select_exceptional_fds;
    FPUState* m_fpu_state { nullptr };
    InlineLinkedList<Thread>* m_thread_list { nullptr };
    State m_state { Invalid };
    bool m_select_has_timeout { false };
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
        if (callback(*thread) == IterationDecision::Abort)
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
        if (callback(*thread) == IterationDecision::Abort)
            return;
        thread = next_thread;
    }
}

#pragma once

#include <Kernel/i386.h>
#include <Kernel/KResult.h>
#include <Kernel/LinearAddress.h>
#include <Kernel/UnixTypes.h>
#include <AK/AKString.h>
#include <AK/InlineLinkedList.h>
#include <AK/RetainPtr.h>
#include <AK/Vector.h>

class Alarm;
class Process;
class Region;
class Socket;

enum class ShouldUnblockThread { No = 0, Yes };

struct SignalActionData {
    LinearAddress handler_or_sigaction;
    dword mask { 0 };
    int flags { 0 };
    LinearAddress restorer;
};

class Thread : public InlineLinkedListNode<Thread> {
    friend class Process;
    friend class Scheduler;
public:
    explicit Thread(Process&);
    ~Thread();

    static void initialize();
    static void finalize_dying_threads();

    static Vector<Thread*> all_threads();

    int tid() const { return m_tid; }
    int pid() const;

    Process& process() { return m_process; }
    const Process& process() const { return m_process; }

    void finalize();

    enum State {
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
    dword stack_top() const { return m_tss.ss == 0x10 ? m_stack_top0 : m_stack_top3; }

    word selector() const { return m_far_ptr.selector; }
    TSS32& tss() { return m_tss; }
    State state() const { return m_state; }
    dword ticks() const { return m_ticks; }
    pid_t waitee_pid() const { return m_waitee_pid; }

    void sleep(dword ticks);
    void block(Thread::State);
    void unblock();

    void set_wakeup_time(qword t) { m_wakeup_time = t; }
    qword wakeup_time() const { return m_wakeup_time; }
    void snooze_until(Alarm&);
    KResult wait_for_connect(Socket&);

    const FarPtr& far_ptr() const { return m_far_ptr; }

    bool tick();
    void set_ticks_left(dword t) { m_ticks_left = t; }
    dword ticks_left() const { return m_ticks_left; }

    dword kernel_stack_base() const { return (dword)m_kernel_stack; }
    dword kernel_stack_for_signal_handler_base() const { return (dword)m_kernel_stack_for_signal_handler; }

    void set_selector(word s) { m_far_ptr.selector = s; }
    void set_state(State s) { m_state = s; }

    void send_signal(byte signal, Process* sender);

    ShouldUnblockThread dispatch_one_pending_signal();
    ShouldUnblockThread dispatch_signal(byte signal);
    bool has_unmasked_pending_signals() const;
    void terminate_due_to_signal(byte signal);

    FPUState& fpu_state() { return *m_fpu_state; }
    bool has_used_fpu() const { return m_has_used_fpu; }
    void set_has_used_fpu(bool b) { m_has_used_fpu = b; }

    void set_blocked_socket(Socket* socket) { m_blocked_socket = socket; }

    void set_default_signal_dispositions();
    void push_value_on_stack(dword);
    void make_userspace_stack_for_main_thread(Vector<String> arguments, Vector<String> environment);
    void make_userspace_stack_for_secondary_thread(void* argument);

    Thread* clone(Process&);

    // For InlineLinkedList
    Thread* m_prev { nullptr };
    Thread* m_next { nullptr };

    template<typename Callback> static void for_each_in_state(State, Callback);
    template<typename Callback> static void for_each_living(Callback);
    template<typename Callback> static void for_each(Callback);

private:
    Process& m_process;
    int m_tid { -1 };
    TSS32 m_tss;
    TSS32 m_tss_to_resume_kernel;
    FarPtr m_far_ptr;
    dword m_ticks { 0 };
    dword m_ticks_left { 0 };
    dword m_stack_top0 { 0 };
    dword m_stack_top3 { 0 };
    qword m_wakeup_time { 0 };
    dword m_times_scheduled { 0 };
    dword m_pending_signals { 0 };
    dword m_signal_mask { 0 };
    void* m_kernel_stack { nullptr };
    void* m_kernel_stack_for_signal_handler { nullptr };
    pid_t m_waitee_pid { -1 };
    int m_blocked_fd { -1 };
    timeval m_select_timeout;
    SignalActionData m_signal_action_data[32];
    RetainPtr<Socket> m_blocked_socket;
    Region* m_signal_stack_user_region { nullptr };
    Alarm* m_snoozing_alarm { nullptr };
    Vector<int> m_select_read_fds;
    Vector<int> m_select_write_fds;
    Vector<int> m_select_exceptional_fds;
    State m_state { Invalid };
    FPUState* m_fpu_state { nullptr };
    bool m_select_has_timeout { false };
    bool m_has_used_fpu { false };
    bool m_was_interrupted_while_blocked { false };
};

extern InlineLinkedList<Thread>* g_threads;

const char* to_string(Thread::State);

template<typename Callback>
inline void Thread::for_each_in_state(State state, Callback callback)
{
    ASSERT_INTERRUPTS_DISABLED();
    for (auto* thread = g_threads->head(); thread;) {
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
    for (auto* thread = g_threads->head(); thread;) {
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
    for (auto* thread = g_threads->head(); thread;) {
        auto* next_thread = thread->next();
        if (callback(*thread) == IterationDecision::Abort)
            return;
        thread = next_thread;
    }
}


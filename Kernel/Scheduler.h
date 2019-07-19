#pragma once

#include <AK/Assertions.h>
#include <AK/Types.h>
#include <AK/Function.h>
#include <AK/IntrusiveList.h>

class Process;
class Thread;
struct RegisterDump;

extern Thread* current;
extern Thread* g_last_fpu_thread;
extern Thread* g_finalizer;
extern u64 g_uptime;

class Scheduler {
public:
    static void initialize();
    static void timer_tick(RegisterDump&);
    static bool pick_next();
    static void pick_next_and_switch_now();
    static void switch_now();
    static bool yield();
    static bool donate_to(Thread*, const char* reason);
    static bool context_switch(Thread&);
    static void prepare_to_modify_tss(Thread&);
    static Process* colonel();
    static bool is_active();
    static void beep();

    template<typename Callback>
    static inline IterationDecision for_each_runnable(Callback callback)
    {
        return for_each_runnable_func([callback](Thread& thread) {
            return callback(thread);
        });
    }

    template<typename Callback>
    static inline IterationDecision for_each_nonrunnable(Callback callback)
    {
        return for_each_nonrunnable_func([callback](Thread& thread) {
            return callback(thread);
        });
    }

    static void init_thread(Thread& thread);
    static void update_state_for_thread(Thread& thread);

private:
    static void prepare_for_iret_to_new_process();
    static IterationDecision for_each_runnable_func(Function<IterationDecision(Thread&)>&& callback);
    static IterationDecision for_each_nonrunnable_func(Function<IterationDecision(Thread&)>&& callback);

};


#pragma once

#include <AK/Assertions.h>
#include <AK/Types.h>

class Process;
class Thread;
struct RegisterDump;

extern Thread* current;
extern Thread* g_last_fpu_thread;
extern Thread* g_finalizer;
extern qword g_uptime;

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

private:
    static void prepare_for_iret_to_new_process();
};

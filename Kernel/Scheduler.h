#pragma once

#include <AK/Assertions.h>

class Process;
struct RegisterDump;

extern Process* current;
extern Process* g_last_fpu_process;
extern Process* g_finalizer;

class Scheduler {
public:
    static void initialize();
    static void timer_tick(RegisterDump&);
    static bool pick_next();
    static void pick_next_and_switch_now();
    static void switch_now();
    static bool yield();
    static bool donate_to(Process*, const char* reason);
    static bool context_switch(Process&);
    static void prepare_to_modify_tss(Process&);
    static Process* colonel();
    static bool is_active();
private:
    static void prepare_for_iret_to_new_process();
};

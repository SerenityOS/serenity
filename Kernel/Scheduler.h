#pragma once

#include <AK/Assertions.h>
class Process;

extern Process* current;

class Scheduler {
public:
    static void initialize();
    static bool pick_next();
    static void pick_next_and_switch_now();
    static void switch_now();
    static bool yield();
    static bool context_switch(Process&);
    static void prepare_for_iret_to_new_process();
    static void prepare_to_modify_own_tss();
};

int sched_yield();

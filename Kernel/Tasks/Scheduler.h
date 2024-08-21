/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Assertions.h>
#include <AK/Function.h>
#include <AK/IntrusiveList.h>
#include <AK/Types.h>
#include <Kernel/Forward.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Time/TimeManagement.h>
#include <Kernel/UnixTypes.h>

namespace Kernel {

struct RegisterState;

extern Thread* g_finalizer;
extern WaitQueue* g_finalizer_wait_queue;
extern Atomic<bool> g_finalizer_has_work;
extern RecursiveSpinlock<LockRank::None> g_scheduler_lock;

struct TotalTimeScheduled {
    u64 total { 0 };
    u64 total_kernel { 0 };
};

class Scheduler {
public:
    static void initialize();
    static Thread* create_ap_idle_thread(u32 cpu);
    static void set_idle_thread(Thread* idle_thread);
    static void timer_tick();
    [[noreturn]] static void start();
    static void pick_next();
    static void yield();
    static void context_switch(Thread*);
    static void enter_current(Thread& prev_thread);
    static void leave_on_first_switch(InterruptsState);
    static void prepare_after_exec();
    static void prepare_for_idle_loop();
    static Process* colonel();
    static void idle_loop(void*);
    static void invoke_async();
    static void notify_finalizer();
    static Thread& pull_next_runnable_thread();
    static Thread* peek_next_runnable_thread();
    static bool dequeue_runnable_thread(Thread&, bool = false);
    static void enqueue_runnable_thread(Thread&);
    static void dump_scheduler_state(bool = false);
    static bool is_initialized();
    static TotalTimeScheduled get_total_time_scheduled();
    static void add_time_scheduled(u64, bool);
};

}

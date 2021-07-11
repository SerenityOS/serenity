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
#include <Kernel/SpinLock.h>
#include <Kernel/Time/TimeManagement.h>
#include <Kernel/UnixTypes.h>

namespace Kernel {

struct RegisterState;

extern Thread* g_finalizer;
extern WaitQueue* g_finalizer_wait_queue;
extern Atomic<bool> g_finalizer_has_work;
extern RecursiveSpinLock g_scheduler_lock;

class Scheduler {
public:
    static void initialize();
    static Thread* create_ap_idle_thread(u32 cpu);
    static void set_idle_thread(Thread* idle_thread);
    static void timer_tick(const RegisterState&);
    [[noreturn]] static void start();
    static bool pick_next();
    static bool yield();
    static void yield_from_critical();
    static bool context_switch(Thread*);
    static void enter_current(Thread& prev_thread, bool is_first);
    static void leave_on_first_switch(u32 flags);
    static void prepare_after_exec();
    static void prepare_for_idle_loop();
    static Process* colonel();
    static void idle_loop(void*);
    static void invoke_async();
    static void notify_finalizer();
    static Thread& pull_next_runnable_thread();
    static bool dequeue_runnable_thread(Thread&, bool = false);
    static void queue_runnable_thread(Thread&);
    static void dump_scheduler_state();
    static bool is_initialized();
};

}

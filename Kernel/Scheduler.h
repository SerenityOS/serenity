/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/Assertions.h>
#include <AK/Function.h>
#include <AK/IntrusiveList.h>
#include <AK/Types.h>
#include <Kernel/SpinLock.h>
#include <Kernel/Time/TimeManagement.h>
#include <Kernel/UnixTypes.h>

namespace Kernel {

class Process;
class Thread;
class WaitQueue;
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
    static bool donate_to_and_switch(Thread*, const char* reason);
    static bool donate_to(RefPtr<Thread>&, const char* reason);
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
};

}

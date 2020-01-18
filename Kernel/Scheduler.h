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
#include <AK/Types.h>
#include <AK/Function.h>
#include <AK/IntrusiveList.h>

class Process;
class Thread;
class WaitQueue;
struct RegisterDump;
struct SchedulerData;

extern Thread* current;
extern Thread* g_finalizer;
extern Thread* g_colonel;
extern WaitQueue* g_finalizer_wait_queue;
extern u64 g_uptime;
extern SchedulerData* g_scheduler_data;

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
    static void idle_loop();
    static void stop_idling();

    template<typename Callback>
    static inline IterationDecision for_each_runnable(Callback);

    template<typename Callback>
    static inline IterationDecision for_each_nonrunnable(Callback);

    static void init_thread(Thread& thread);
    static void update_state_for_thread(Thread& thread);

private:
    static void prepare_for_iret_to_new_process();
};

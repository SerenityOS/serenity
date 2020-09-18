/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
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

#include <AK/Queue.h>
#include <LibThread/BackgroundAction.h>
#include <LibThread/Lock.h>
#include <LibThread/Thread.h>

static LibThread::Lockable<Queue<Function<void()>>>* s_all_actions;
static LibThread::Thread* s_background_thread;

static int background_thread_func()
{
    while (true) {
        Function<void()> work_item;
        {
            LOCKER(s_all_actions->lock());

            if (!s_all_actions->resource().is_empty())
                work_item = s_all_actions->resource().dequeue();
        }
        if (work_item)
            work_item();
        else
            sleep(1);
    }

    ASSERT_NOT_REACHED();
}

static void init()
{
    s_all_actions = new LibThread::Lockable<Queue<Function<void()>>>();
    s_background_thread = &LibThread::Thread::construct(background_thread_func).leak_ref();
    s_background_thread->set_name("Background thread");
    s_background_thread->start();
}

LibThread::Lockable<Queue<Function<void()>>>& LibThread::BackgroundActionBase::all_actions()
{
    if (s_all_actions == nullptr)
        init();
    return *s_all_actions;
}

LibThread::Thread& LibThread::BackgroundActionBase::background_thread()
{
    if (s_background_thread == nullptr)
        init();
    return *s_background_thread;
}

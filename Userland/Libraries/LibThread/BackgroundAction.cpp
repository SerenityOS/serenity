/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Queue.h>
#include <LibThread/BackgroundAction.h>
#include <LibThread/Lock.h>
#include <LibThread/Thread.h>

static LibThread::Lockable<Queue<Function<void()>>>* s_all_actions;
static LibThread::Thread* s_background_thread;

static intptr_t background_thread_func()
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

    VERIFY_NOT_REACHED();
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

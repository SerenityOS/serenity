/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Queue.h>
#include <LibThreading/BackgroundAction.h>
#include <LibThreading/Lock.h>
#include <LibThreading/Thread.h>

static Threading::Lockable<Queue<Function<void()>>>* s_all_actions;
static Threading::Thread* s_background_thread;

static intptr_t background_thread_func()
{
    while (true) {
        Function<void()> work_item;
        {
            Threading::Locker locker(s_all_actions->lock());

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
    s_all_actions = new Threading::Lockable<Queue<Function<void()>>>();
    s_background_thread = &Threading::Thread::construct(background_thread_func).leak_ref();
    s_background_thread->set_name("Background thread");
    s_background_thread->start();
}

Threading::Lockable<Queue<Function<void()>>>& Threading::BackgroundActionBase::all_actions()
{
    if (s_all_actions == nullptr)
        init();
    return *s_all_actions;
}

Threading::Thread& Threading::BackgroundActionBase::background_thread()
{
    if (s_background_thread == nullptr)
        init();
    return *s_background_thread;
}

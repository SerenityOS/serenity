/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Queue.h>
#include <LibThreading/BackgroundAction.h>
#include <LibThreading/Lock.h>
#include <LibThreading/Thread.h>
#include <unistd.h>

static Threading::Lockable<Queue<Function<void()>>>* s_all_actions;
static Threading::Thread* s_background_thread;
static int s_notify_pipe_fds[2];

static intptr_t background_thread_func()
{
    while (true) {
        char buffer[1];
        auto nread = read(s_notify_pipe_fds[0], buffer, sizeof(buffer));
        if (nread < 0) {
            perror("read");
            _exit(1);
        }

        Vector<Function<void()>> work_items;
        {
            Threading::Locker locker(s_all_actions->lock());

            while (!s_all_actions->resource().is_empty()) {
                work_items.append(s_all_actions->resource().dequeue());
            }
        }

        for (auto& work_item : work_items)
            work_item();
    }

    VERIFY_NOT_REACHED();
}

static void init()
{
    if (pipe(s_notify_pipe_fds) < 0) {
        perror("pipe");
        _exit(1);
    }
    s_all_actions = new Threading::Lockable<Queue<Function<void()>>>();
    s_background_thread = &Threading::Thread::construct(background_thread_func).leak_ref();
    s_background_thread->set_name("Background thread");
    s_background_thread->start();
}

Threading::Thread& Threading::BackgroundActionBase::background_thread()
{
    if (s_background_thread == nullptr)
        init();
    return *s_background_thread;
}

void Threading::BackgroundActionBase::enqueue_work(Function<void()> work)
{
    if (s_all_actions == nullptr)
        init();
    Locker locker(s_all_actions->lock());
    s_all_actions->resource().enqueue(move(work));
    char ch = 'x';
    if (write(s_notify_pipe_fds[1], &ch, sizeof(ch)) < 0) {
        perror("write");
        _exit(1);
    }
}

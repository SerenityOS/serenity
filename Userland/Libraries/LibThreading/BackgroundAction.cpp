/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Queue.h>
#include <LibThreading/BackgroundAction.h>
#include <LibThreading/Mutex.h>
#include <LibThreading/Thread.h>
#include <unistd.h>

static pthread_mutex_t s_mutex = PTHREAD_MUTEX_INITIALIZER;
static pthread_cond_t s_condition = PTHREAD_COND_INITIALIZER;
static Queue<Function<void()>>* s_all_actions;
static Threading::Thread* s_background_thread;

static intptr_t background_thread_func()
{
    Vector<Function<void()>> actions;
    while (true) {

        pthread_mutex_lock(&s_mutex);

        while (s_all_actions->is_empty())
            pthread_cond_wait(&s_condition, &s_mutex);

        while (!s_all_actions->is_empty())
            actions.append(s_all_actions->dequeue());

        pthread_mutex_unlock(&s_mutex);

        for (auto& action : actions)
            action();

        actions.clear();
    }
}

static void init()
{
    s_all_actions = new Queue<Function<void()>>;
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

    pthread_mutex_lock(&s_mutex);
    s_all_actions->enqueue(move(work));
    pthread_cond_broadcast(&s_condition);
    pthread_mutex_unlock(&s_mutex);
}

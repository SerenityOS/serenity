/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Atomic.h>
#include <AK/NeverDestroyed.h>
#include <AK/Vector.h>
#include <bits/pthread_integration.h>
#include <errno.h>
#include <sched.h>
#include <unistd.h>

namespace {

// Most programs don't need this, no need to incur an extra mutex lock/unlock on them
static Atomic<bool> g_did_touch_atfork { false };
static pthread_mutex_t g_atfork_list_mutex __PTHREAD_MUTEX_INITIALIZER;
static NeverDestroyed<Vector<void (*)(void), 4>> g_atfork_prepare_list;
static NeverDestroyed<Vector<void (*)(void), 4>> g_atfork_child_list;
static NeverDestroyed<Vector<void (*)(void), 4>> g_atfork_parent_list;

}

extern "C" {
void __pthread_fork_prepare(void)
{
    if (!g_did_touch_atfork.load())
        return;

    __pthread_mutex_lock(&g_atfork_list_mutex);
    for (auto entry : g_atfork_prepare_list.get())
        entry();
    __pthread_mutex_unlock(&g_atfork_list_mutex);
}

void __pthread_fork_child(void)
{
    if (!g_did_touch_atfork.load())
        return;

    __pthread_mutex_lock(&g_atfork_list_mutex);
    for (auto entry : g_atfork_child_list.get())
        entry();
    __pthread_mutex_unlock(&g_atfork_list_mutex);
}

void __pthread_fork_parent(void)
{
    if (!g_did_touch_atfork.load())
        return;

    __pthread_mutex_lock(&g_atfork_list_mutex);
    for (auto entry : g_atfork_parent_list.get())
        entry();
    __pthread_mutex_unlock(&g_atfork_list_mutex);
}

void __pthread_fork_atfork_register_prepare(void (*func)(void))
{
    g_did_touch_atfork.store(true);

    __pthread_mutex_lock(&g_atfork_list_mutex);
    g_atfork_prepare_list->append(func);
    __pthread_mutex_unlock(&g_atfork_list_mutex);
}

void __pthread_fork_atfork_register_parent(void (*func)(void))
{
    g_did_touch_atfork.store(true);

    __pthread_mutex_lock(&g_atfork_list_mutex);
    g_atfork_parent_list->append(func);
    __pthread_mutex_unlock(&g_atfork_list_mutex);
}

void __pthread_fork_atfork_register_child(void (*func)(void))
{
    g_did_touch_atfork.store(true);

    __pthread_mutex_lock(&g_atfork_list_mutex);
    g_atfork_child_list->append(func);
    __pthread_mutex_unlock(&g_atfork_list_mutex);
}

int __pthread_self()
{
    return gettid();
}

int pthread_self() __attribute__((weak, alias("__pthread_self")));

int __pthread_mutex_lock(pthread_mutex_t* mutex)
{
    pthread_t this_thread = __pthread_self();
    for (;;) {
        u32 expected = 0;
        if (!AK::atomic_compare_exchange_strong(&mutex->lock, expected, 1u, AK::memory_order_acquire)) {
            if (mutex->type == __PTHREAD_MUTEX_RECURSIVE && mutex->owner == this_thread) {
                mutex->level++;
                return 0;
            }
            sched_yield();
            continue;
        }
        mutex->owner = this_thread;
        mutex->level = 0;
        return 0;
    }
}

int pthread_mutex_lock(pthread_mutex_t*) __attribute__((weak, alias("__pthread_mutex_lock")));

int __pthread_mutex_unlock(pthread_mutex_t* mutex)
{
    if (mutex->type == __PTHREAD_MUTEX_RECURSIVE && mutex->level > 0) {
        mutex->level--;
        return 0;
    }
    mutex->owner = 0;
    AK::atomic_store(&mutex->lock, 0u, AK::memory_order_release);
    return 0;
}

int pthread_mutex_unlock(pthread_mutex_t*) __attribute__((weak, alias("__pthread_mutex_unlock")));

int __pthread_mutex_trylock(pthread_mutex_t* mutex)
{
    u32 expected = 0;
    if (!AK::atomic_compare_exchange_strong(&mutex->lock, expected, 1u, AK::memory_order_acquire)) {
        if (mutex->type == __PTHREAD_MUTEX_RECURSIVE && mutex->owner == pthread_self()) {
            mutex->level++;
            return 0;
        }
        return EBUSY;
    }
    mutex->owner = pthread_self();
    mutex->level = 0;
    return 0;
}

int pthread_mutex_trylock(pthread_mutex_t* mutex) __attribute__((weak, alias("__pthread_mutex_trylock")));

int __pthread_mutex_init(pthread_mutex_t* mutex, const pthread_mutexattr_t* attributes)
{
    mutex->lock = 0;
    mutex->owner = 0;
    mutex->level = 0;
    mutex->type = attributes ? attributes->type : __PTHREAD_MUTEX_NORMAL;
    return 0;
}

int pthread_mutex_init(pthread_mutex_t*, const pthread_mutexattr_t*) __attribute__((weak, alias("__pthread_mutex_init")));
}

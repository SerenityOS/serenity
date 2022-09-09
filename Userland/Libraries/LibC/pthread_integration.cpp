/*
 * Copyright (c) 2021, the SerenityOS developers.
 * Copyright (c) 2022, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Atomic.h>
#include <AK/NeverDestroyed.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <bits/pthread_integration.h>
#include <errno.h>
#include <pthread.h>
#include <sched.h>
#include <serenity.h>
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

    pthread_mutex_lock(&g_atfork_list_mutex);
    for (auto entry : g_atfork_prepare_list.get())
        entry();
    pthread_mutex_unlock(&g_atfork_list_mutex);
}

void __pthread_fork_child(void)
{
    if (!g_did_touch_atfork.load())
        return;

    pthread_mutex_lock(&g_atfork_list_mutex);
    for (auto entry : g_atfork_child_list.get())
        entry();
    pthread_mutex_unlock(&g_atfork_list_mutex);
}

void __pthread_fork_parent(void)
{
    if (!g_did_touch_atfork.load())
        return;

    pthread_mutex_lock(&g_atfork_list_mutex);
    for (auto entry : g_atfork_parent_list.get())
        entry();
    pthread_mutex_unlock(&g_atfork_list_mutex);
}

void __pthread_fork_atfork_register_prepare(void (*func)(void))
{
    g_did_touch_atfork.store(true);

    pthread_mutex_lock(&g_atfork_list_mutex);
    g_atfork_prepare_list->append(func);
    pthread_mutex_unlock(&g_atfork_list_mutex);
}

void __pthread_fork_atfork_register_parent(void (*func)(void))
{
    g_did_touch_atfork.store(true);

    pthread_mutex_lock(&g_atfork_list_mutex);
    g_atfork_parent_list->append(func);
    pthread_mutex_unlock(&g_atfork_list_mutex);
}

void __pthread_fork_atfork_register_child(void (*func)(void))
{
    g_did_touch_atfork.store(true);

    pthread_mutex_lock(&g_atfork_list_mutex);
    g_atfork_child_list->append(func);
    pthread_mutex_unlock(&g_atfork_list_mutex);
}

// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_self.html
int pthread_self()
{
    return gettid();
}

static constexpr u32 MUTEX_UNLOCKED = 0;
static constexpr u32 MUTEX_LOCKED_NO_NEED_TO_WAKE = 0x40000000;
static constexpr u32 MUTEX_NEED_TO_WAKE = 0x80000000;
static constexpr u32 MUTEX_LOCKED_NEED_TO_WAKE = MUTEX_LOCKED_NO_NEED_TO_WAKE | MUTEX_NEED_TO_WAKE;
static constexpr u32 MUTEX_OWNER_MASK = 0x3FFFFFFF;

// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_mutex_init.html
int pthread_mutex_init(pthread_mutex_t* mutex, pthread_mutexattr_t const* attributes)
{
    mutex->lock = 0;
    mutex->level = 0;
    mutex->type = attributes ? attributes->type : __PTHREAD_MUTEX_NORMAL;
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_mutex_trylock.html
int pthread_mutex_trylock(pthread_mutex_t* mutex)
{
    u32 expected = MUTEX_UNLOCKED;
    u32 desired = MUTEX_LOCKED_NO_NEED_TO_WAKE;
    if (mutex->type == __PTHREAD_MUTEX_RECURSIVE)
        desired |= pthread_self();
    bool exchanged = AK::atomic_compare_exchange_strong(&mutex->lock, expected, desired, AK::memory_order_acquire);

    if (exchanged) [[likely]] {
        mutex->level = 0;
        return 0;
    } else if (mutex->type == __PTHREAD_MUTEX_RECURSIVE) {
        pthread_t owner = expected & MUTEX_OWNER_MASK;
        if (owner == pthread_self()) {
            // We already own the mutex!
            mutex->level++;
            return 0;
        }
    }
    return EBUSY;
}

// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_mutex_lock.html
int pthread_mutex_lock(pthread_mutex_t* mutex)
{
    // Fast path: attempt to claim the mutex without waiting.
    u32 value = MUTEX_UNLOCKED;
    u32 desired = MUTEX_LOCKED_NO_NEED_TO_WAKE;
    if (mutex->type == __PTHREAD_MUTEX_RECURSIVE)
        desired |= pthread_self();
    bool exchanged = AK::atomic_compare_exchange_strong(&mutex->lock, value, desired, AK::memory_order_acquire);
    if (exchanged) [[likely]] {
        mutex->level = 0;
        return 0;
    } else if (mutex->type == __PTHREAD_MUTEX_RECURSIVE) {
        pthread_t owner = value & MUTEX_OWNER_MASK;
        if (owner == pthread_self()) {
            // We already own the mutex!
            mutex->level++;
            return 0;
        }
    }

    // Slow path: wait, record the fact that we're going to wait, and always
    // remember to wake the next thread up once we release the mutex.
    value = AK::atomic_fetch_or(&mutex->lock, MUTEX_NEED_TO_WAKE, AK::memory_order_acquire);

    desired |= MUTEX_NEED_TO_WAKE;
    if (value == MUTEX_UNLOCKED) {
        AK::atomic_store(&mutex->lock, desired, AK::memory_order_relaxed);
    } else {
        do {
            futex_wait(&mutex->lock, value, nullptr, 0, false);
            value = MUTEX_UNLOCKED;
        } while (!AK::atomic_compare_exchange_strong(&mutex->lock, value, desired, AK::memory_order_acquire));
    }

    mutex->level = 0;
    return 0;
}

int __pthread_mutex_lock_pessimistic_np(pthread_mutex_t* mutex)
{
    // Same as pthread_mutex_lock(), but always set MUTEX_LOCKED_NEED_TO_WAKE,
    // and also don't bother checking for already owning the mutex recursively,
    // because we know we don't. Used in the condition variable implementation.
    u32 value = AK::atomic_fetch_or(&mutex->lock, MUTEX_NEED_TO_WAKE, AK::memory_order_acquire);
    u32 desired = MUTEX_LOCKED_NEED_TO_WAKE;
    if (mutex->type == __PTHREAD_MUTEX_RECURSIVE)
        desired |= pthread_self();
    if (value == MUTEX_UNLOCKED) {
        AK::atomic_store(&mutex->lock, desired, AK::memory_order_relaxed);
    } else {
        do {
            futex_wait(&mutex->lock, value, nullptr, 0, false);
            value = MUTEX_UNLOCKED;
        } while (!AK::atomic_compare_exchange_strong(&mutex->lock, value, desired, AK::memory_order_acquire));
    }

    mutex->level = 0;
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_mutex_unlock.html
int pthread_mutex_unlock(pthread_mutex_t* mutex)
{
    if (mutex->type == __PTHREAD_MUTEX_RECURSIVE && mutex->level > 0) {
        mutex->level--;
        return 0;
    }

    u32 value = AK::atomic_exchange(&mutex->lock, MUTEX_UNLOCKED, AK::memory_order_release);
    if (value & MUTEX_NEED_TO_WAKE) [[unlikely]] {
        int rc = futex_wake(&mutex->lock, 1, false);
        VERIFY(rc >= 0);
    }

    return 0;
}
}

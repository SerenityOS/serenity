/*
 * Copyright (c) 2021, the SerenityOS developers.
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

#include <AK/Atomic.h>
#include <AK/NeverDestroyed.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <bits/pthread_integration.h>
#include <sched.h>
#include <sys/types.h>
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

int __pthread_mutex_lock(void* mutexp)
{
    auto* mutex = reinterpret_cast<pthread_mutex_t*>(mutexp);
    auto& atomic = reinterpret_cast<Atomic<u32>&>(mutex->lock);
    pthread_t this_thread = __pthread_self();
    for (;;) {
        u32 expected = false;
        if (!atomic.compare_exchange_strong(expected, true, AK::memory_order_acq_rel)) {
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

int __pthread_mutex_unlock(void* mutexp)
{
    auto* mutex = reinterpret_cast<pthread_mutex_t*>(mutexp);
    if (mutex->type == __PTHREAD_MUTEX_RECURSIVE && mutex->level > 0) {
        mutex->level--;
        return 0;
    }
    mutex->owner = 0;
    mutex->lock = 0;
    return 0;
}

int __pthread_mutex_init(void* mutexp, const void* attrp)
{
    auto* mutex = reinterpret_cast<pthread_mutex_t*>(mutexp);
    auto* attributes = reinterpret_cast<const pthread_mutexattr_t*>(attrp);
    mutex->lock = 0;
    mutex->owner = 0;
    mutex->level = 0;
    mutex->type = attributes ? attributes->type : __PTHREAD_MUTEX_NORMAL;
    return 0;
}
}

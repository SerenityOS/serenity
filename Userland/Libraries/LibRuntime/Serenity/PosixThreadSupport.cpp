/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/Atomic.h>
#include <AK/NeverDestroyed.h>
#include <AK/Vector.h>
#include <LibRuntime/Mutex.h>
#include <LibRuntime/Serenity/PosixThreadSupport.h>
#include <LibRuntime/Serenity/PossiblyThrowingCallback.h>

namespace Runtime {

namespace {
constexpr size_t CALLBACK_COUNT = to_underlying(CallbackType::__Count);

Mutex g_callback_mutex;
Atomic<bool> g_did_touch[CALLBACK_COUNT];
NeverDestroyed<Vector<void (*)(), 4>> g_callback_list[CALLBACK_COUNT];
}

void run_pthread_callbacks(CallbackType type)
{
    if (!g_did_touch[to_underlying(type)])
        return;
    MutexLocker lock(g_callback_mutex);
    for (auto entry : *g_callback_list[to_underlying(type)])
        run_possibly_throwing_callback(entry);
}

void register_pthread_callback(CallbackType type, void (*callback)())
{
    g_did_touch[to_underlying(type)] = true;
    MutexLocker lock(g_callback_mutex);
    g_callback_list[to_underlying(type)]->append(callback);
}

}

[[gnu::weak]] void __pthread_key_destroy_for_current_thread()
{
}

[[gnu::weak]] void __pthread_maybe_cancel()
{
}

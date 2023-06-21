/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Atomic.h>
#include <errno.h>
#include <pthread.h>
#include <unistd.h>

#ifndef _DYNAMIC_LOADER
extern "C" {

static constexpr int max_keys = PTHREAD_KEYS_MAX;

struct KeyTable {
    KeyDestructor destructors[max_keys] { nullptr };
    int next { 0 };
    pthread_mutex_t mutex = PTHREAD_MUTEX_INITIALIZER;
};

struct SpecificTable {
    void* values[max_keys] { nullptr };
};

static KeyTable s_keys;

__thread SpecificTable t_specifics;

// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_key_create.html
int pthread_key_create(pthread_key_t* key, KeyDestructor destructor)
{
    int ret = 0;
    pthread_mutex_lock(&s_keys.mutex);
    if (s_keys.next >= max_keys) {
        ret = EAGAIN;
    } else {
        *key = s_keys.next++;
        s_keys.destructors[*key] = destructor;
        ret = 0;
    }
    pthread_mutex_unlock(&s_keys.mutex);
    return ret;
}

// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_key_delete.html
int pthread_key_delete(pthread_key_t key)
{
    if (key < 0 || key >= max_keys)
        return EINVAL;
    pthread_mutex_lock(&s_keys.mutex);
    s_keys.destructors[key] = nullptr;
    pthread_mutex_unlock(&s_keys.mutex);
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_getspecific.html
void* pthread_getspecific(pthread_key_t key)
{
    if (key < 0)
        return nullptr;
    if (key >= max_keys)
        return nullptr;
    return t_specifics.values[key];
}

// https://pubs.opengroup.org/onlinepubs/009695399/functions/pthread_setspecific.html
int pthread_setspecific(pthread_key_t key, void const* value)
{
    if (key < 0)
        return EINVAL;
    if (key >= max_keys)
        return EINVAL;

    t_specifics.values[key] = const_cast<void*>(value);
    return 0;
}

void __pthread_key_destroy_for_current_thread()
{
    // This function will either be called during exit_thread, for a pthread, or
    // during global program shutdown for the main thread.

    pthread_mutex_lock(&s_keys.mutex);
    size_t num_used_keys = s_keys.next;

    // Dr. POSIX accounts for weird key destructors setting their own key again.
    // Or even, setting other unrelated keys? Odd, but whatever the Doc says goes.

    for (size_t destruct_iteration = 0; destruct_iteration < PTHREAD_DESTRUCTOR_ITERATIONS; ++destruct_iteration) {
        bool any_nonnull_destructors = false;
        for (size_t key_index = 0; key_index < num_used_keys; ++key_index) {
            void* value = exchange(t_specifics.values[key_index], nullptr);

            if (value && s_keys.destructors[key_index]) {
                any_nonnull_destructors = true;
                (*s_keys.destructors[key_index])(value);
            }
        }
        if (!any_nonnull_destructors)
            break;
    }
    pthread_mutex_unlock(&s_keys.mutex);
}
}
#endif

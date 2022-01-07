/*
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 * Copyright (c) 2021, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/Atomic.h>
#include <AK/Types.h>
#include <errno.h>
#include <semaphore.h>
#include <serenity.h>

// Whether sem_wait() or sem_post() is responsible for waking any sleeping
// threads.
static constexpr u32 POST_WAKES = 1 << 31;

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/sem_open.html
sem_t* sem_open(const char*, int, ...)
{
    errno = ENOSYS;
    return nullptr;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/sem_close.html
int sem_close(sem_t*)
{
    errno = ENOSYS;
    return -1;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/sem_unlink.html
int sem_unlink(const char*)
{
    errno = ENOSYS;
    return -1;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/sem_init.html
int sem_init(sem_t* sem, int shared, unsigned int value)
{
    if (shared) {
        errno = ENOSYS;
        return -1;
    }

    if (value > SEM_VALUE_MAX) {
        errno = EINVAL;
        return -1;
    }

    sem->value = value;
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/sem_destroy.html
int sem_destroy(sem_t*)
{
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/sem_getvalue.html
int sem_getvalue(sem_t* sem, int* sval)
{
    u32 value = AK::atomic_load(&sem->value, AK::memory_order_relaxed);
    *sval = value & ~POST_WAKES;
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/sem_post.html
int sem_post(sem_t* sem)
{
    u32 value = AK::atomic_fetch_add(&sem->value, 1u, AK::memory_order_release);
    // Fast path: no need to wake.
    if (!(value & POST_WAKES)) [[likely]]
        return 0;

    // Pass the responsibility for waking more threads if more slots become
    // available later to sem_wait() in the thread we're about to wake, as
    // opposed to further sem_post() calls that free up those slots.
    value = AK::atomic_fetch_and(&sem->value, ~POST_WAKES, AK::memory_order_relaxed);
    // Check if another sem_post() call has handled it already.
    if (!(value & POST_WAKES)) [[likely]]
        return 0;
    int rc = futex_wake(&sem->value, 1);
    VERIFY(rc >= 0);
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/sem_trywait.html
int sem_trywait(sem_t* sem)
{
    u32 value = AK::atomic_load(&sem->value, AK::memory_order_relaxed);
    u32 count = value & ~POST_WAKES;
    if (count == 0)
        return EAGAIN;
    // Decrement the count without touching the flag.
    u32 desired = (count - 1) | (value & POST_WAKES);
    bool exchanged = AK::atomic_compare_exchange_strong(&sem->value, value, desired, AK::memory_order_acquire);
    if (exchanged) [[likely]]
        return 0;
    else
        return EAGAIN;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/sem_wait.html
int sem_wait(sem_t* sem)
{
    return sem_timedwait(sem, nullptr);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/sem_timedwait.html
int sem_timedwait(sem_t* sem, const struct timespec* abstime)
{
    u32 value = AK::atomic_load(&sem->value, AK::memory_order_relaxed);
    bool responsible_for_waking = false;

    while (true) {
        u32 count = value & ~POST_WAKES;
        if (count > 0) [[likely]] {
            // It looks like there are some free slots.
            u32 whether_post_wakes = value & POST_WAKES;
            bool going_to_wake = false;
            if (responsible_for_waking && !whether_post_wakes) {
                // If we have ourselves been woken up previously, and the
                // POST_WAKES flag is not set, that means some more slots might
                // be available now, and it's us who has to wake up additional
                // threads.
                if (count > 1) [[unlikely]]
                    going_to_wake = true;
                // Pass the responsibility for waking up further threads back to
                // sem_post() calls. In particular, we don't want the threads
                // we're about to wake to try to wake anyone else.
                whether_post_wakes = POST_WAKES;
            }
            // Now, try to commit this.
            u32 desired = (count - 1) | whether_post_wakes;
            bool exchanged = AK::atomic_compare_exchange_strong(&sem->value, value, desired, AK::memory_order_acquire);
            if (!exchanged) [[unlikely]]
                // Re-evaluate.
                continue;
            if (going_to_wake) [[unlikely]] {
                int rc = futex_wake(&sem->value, count - 1);
                VERIFY(rc >= 0);
            }
            return 0;
        }
        // We're probably going to sleep, so attempt to set the flag. We do not
        // commit to sleeping yet, though, as setting the flag may fail and
        // cause us to reevaluate what we're doing.
        if (value == 0) {
            bool exchanged = AK::atomic_compare_exchange_strong(&sem->value, value, POST_WAKES, AK::memory_order_relaxed);
            if (!exchanged) [[unlikely]]
                // Re-evaluate.
                continue;
            value = POST_WAKES;
        }
        // At this point, we're committed to sleeping.
        responsible_for_waking = true;
        futex_wait(&sem->value, value, abstime, CLOCK_REALTIME);
        // This is the state we will probably see upon being waked:
        value = 1;
    }
}

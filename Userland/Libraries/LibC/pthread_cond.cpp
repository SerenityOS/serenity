/*
 * Copyright (c) 2019, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/Atomic.h>
#include <AK/Types.h>
#include <bits/pthread_cancel.h>
#include <errno.h>
#include <pthread.h>
#include <serenity.h>
#include <sys/types.h>
#include <time.h>

// Condition variable attributes.

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/pthread_condattr_init.html
int pthread_condattr_init(pthread_condattr_t* attr)
{
    attr->clockid = CLOCK_MONOTONIC_COARSE;
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/pthread_condattr_destroy.html
int pthread_condattr_destroy(pthread_condattr_t*)
{
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/pthread_condattr_getclock.html
int pthread_condattr_getclock(pthread_condattr_t* attr, clockid_t* clock)
{
    *clock = attr->clockid;
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/pthread_condattr_setclock.html
int pthread_condattr_setclock(pthread_condattr_t* attr, clockid_t clock)
{
    switch (clock) {
    case CLOCK_REALTIME:
    case CLOCK_REALTIME_COARSE:
    case CLOCK_MONOTONIC:
    case CLOCK_MONOTONIC_COARSE:
    case CLOCK_MONOTONIC_RAW:
        attr->clockid = clock;
        return 0;
    default:
        return EINVAL;
    }
}

// Condition variables.

// cond->value is the generation number (number of times the variable has been
// signaled) multiplied by INCREMENT, or'ed with the NEED_TO_WAKE flags. It's
// done this way instead of putting the flags into the high bits because the
// sequence number can easily overflow, which is completely fine but should not
// cause it to corrupt the flags.
static constexpr u32 NEED_TO_WAKE_ONE = 1;
static constexpr u32 NEED_TO_WAKE_ALL = 2;
static constexpr u32 INCREMENT = 4;

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/pthread_cond_init.html
int pthread_cond_init(pthread_cond_t* cond, pthread_condattr_t const* attr)
{
    cond->mutex = nullptr;
    cond->value = 0;
    cond->clockid = attr ? attr->clockid : CLOCK_REALTIME_COARSE;
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/pthread_cond_destroy.html
int pthread_cond_destroy(pthread_cond_t*)
{
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/pthread_cond_wait.html
int pthread_cond_wait(pthread_cond_t* cond, pthread_mutex_t* mutex)
{
    return pthread_cond_timedwait(cond, mutex, nullptr);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/pthread_cond_timedwait.html
int pthread_cond_timedwait(pthread_cond_t* cond, pthread_mutex_t* mutex, const struct timespec* abstime)
{
    __pthread_maybe_cancel();

    // Save the mutex this condition variable is associated with. We don't (yet)
    // support changing this mutex once set.
    pthread_mutex_t* old_mutex = AK::atomic_exchange(&cond->mutex, mutex, AK::memory_order_relaxed);
    if (old_mutex && old_mutex != mutex)
        TODO();

    // Fetch the current value, and record that we're about to wait. Fetching
    // the current value has to be done while we hold the mutex, because the
    // value might change as soon as we unlock it.
    u32 value = AK::atomic_fetch_or(&cond->value, NEED_TO_WAKE_ONE | NEED_TO_WAKE_ALL, AK::memory_order_release) | NEED_TO_WAKE_ONE | NEED_TO_WAKE_ALL;
    pthread_mutex_unlock(mutex);
    int rc = futex_wait(&cond->value, value, abstime, cond->clockid, false);
    if (rc < 0 && errno != EAGAIN)
        return errno;

    // We might have been re-queued onto the mutex while we were sleeping. Take
    // the pessimistic locking path.
    __pthread_mutex_lock_pessimistic_np(mutex);
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/pthread_cond_signal.html
int pthread_cond_signal(pthread_cond_t* cond)
{
    // Increment the generation.
    u32 value = AK::atomic_fetch_add(&cond->value, INCREMENT, AK::memory_order_relaxed);
    // Fast path: nobody's waiting (or at least, nobody has to be woken).
    if (!(value & NEED_TO_WAKE_ONE)) [[likely]]
        return 0;

    // Wake someone, and clear the NEED_TO_WAKE_ONE flag if there was nobody for
    // us to wake, to take the fast path the next time. Since we only learn
    // whether there has been somebody waiting or not after we have tried to
    // wake them, it would make sense for us to clear the flag after trying to
    // wake someone up and seeing there was nobody waiting; but that would race
    // with somebody else setting the flag. Therefore, we do it like this:
    // attempt to clear the flag first...
    value = AK::atomic_fetch_and(&cond->value, ~NEED_TO_WAKE_ONE, AK::memory_order_relaxed);
    // ...check if it was already cleared by someone else...
    if (!(value & NEED_TO_WAKE_ONE)) [[likely]]
        return 0;
    // ...try to wake someone...
    int rc = futex_wake(&cond->value, 1, false);
    VERIFY(rc >= 0);
    // ...and if we have woken someone, put the flag back.
    if (rc > 0)
        AK::atomic_fetch_or(&cond->value, NEED_TO_WAKE_ONE, AK::memory_order_relaxed);

    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/pthread_cond_broadcast.html
int pthread_cond_broadcast(pthread_cond_t* cond)
{
    // Increment the generation.
    u32 value = AK::atomic_fetch_add(&cond->value, INCREMENT, AK::memory_order_relaxed);
    // Fast path: nobody's waiting (or at least, nobody has to be woken).
    if (!(value & NEED_TO_WAKE_ALL)) [[likely]]
        return 0;

    AK::atomic_fetch_and(&cond->value, ~(NEED_TO_WAKE_ONE | NEED_TO_WAKE_ALL), AK::memory_order_acquire);

    pthread_mutex_t* mutex = AK::atomic_load(&cond->mutex, AK::memory_order_relaxed);
    VERIFY(mutex);

    int rc = futex(&cond->value, FUTEX_REQUEUE | FUTEX_PRIVATE_FLAG, -1, nullptr, &mutex->lock, INT_MAX);
    VERIFY(rc >= 0);
    return 0;
}

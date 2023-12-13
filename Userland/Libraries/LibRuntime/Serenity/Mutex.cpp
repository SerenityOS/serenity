/*
 * Copyright (c) 2021, the SerenityOS developers.
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Atomic.h>
#include <Kernel/API/POSIX/futex.h>
#include <LibRuntime/Mutex.h>
#include <LibRuntime/System.h>
#include <LibSystem/syscall.h>

namespace Runtime {

ErrorOr<void> futex_wait(u32* userspace_address, u32 value, Optional<RelativeOrAbsoluteTimeout> timeout, bool process_shared)
{
    int futex_op = FUTEX_WAIT;

    timespec timeout_as_timespec;

    if (timeout.has_value()) {
        timeout->visit(
            [&](MonotonicTime const& timeout_value) {
                // MonotonicTime::to_timespec is private, what makes sense in general but is
                // inconvenient here.
                timeout_as_timespec = {
                    .tv_sec = timeout_value.truncated_seconds(),
                    .tv_nsec = timeout_value.nanoseconds_within_second(),
                };
            },
            [&](auto const& timeout_value) {
                timeout_as_timespec = timeout_value.to_timespec();
            });

        if (!timeout->has<Duration>()) {
            // NOTE: FUTEX_WAIT takes a relative timeout, so use FUTEX_WAIT_BITSET instead!
            futex_op = FUTEX_WAIT_BITSET;
        }
        if (timeout->has<UnixDateTime>())
            futex_op |= FUTEX_CLOCK_REALTIME;
    }

    if (!process_shared)
        futex_op |= FUTEX_PRIVATE_FLAG;

    Syscall::SC_futex_params params {
        .userspace_address = userspace_address,
        .futex_op = futex_op,
        .val = value,
        .timeout = &timeout_as_timespec,
        .userspace_address2 = nullptr,
        .val3 = FUTEX_BITSET_MATCH_ANY,
    };
    int rc = static_cast<int>(syscall(SC_futex, &params));

    VERIFY(rc <= 0);
    if (rc != 0)
        return Error::from_syscall("futex"sv, -rc);
    return {};
}

ErrorOr<int> futex_wake(u32* userspace_address, u32 count, bool process_shared)
{
    int futex_op = FUTEX_WAKE;
    if (!process_shared)
        futex_op |= FUTEX_PRIVATE_FLAG;

    Syscall::SC_futex_params params {
        .userspace_address = userspace_address,
        .futex_op = futex_op,
        .val = count,
        .timeout = nullptr,
        .userspace_address2 = nullptr,
        .val3 = 0,
    };

    int rc = static_cast<int>(syscall(SC_futex, &params));
    if (rc < 0)
        return Error::from_syscall("futex"sv, -rc);
    return rc;
}

static constexpr u32 MUTEX_UNLOCKED = 0;
static constexpr u32 MUTEX_LOCKED_NO_NEED_TO_WAKE = 1;
static constexpr u32 MUTEX_LOCKED_NEED_TO_WAKE = 2;

bool Mutex::try_lock()
{
    u32 expected = MUTEX_UNLOCKED;
    bool exchanged = AK::atomic_compare_exchange_strong(&m_lock, expected, MUTEX_LOCKED_NO_NEED_TO_WAKE, AK::memory_order_acquire);

    if (exchanged) [[likely]] {
        if (m_type == MutexType::Recursive)
            AK::atomic_store(&m_owner, gettid(), AK::memory_order_relaxed);
        m_level = 0;
        return true;
    } else if (m_type == MutexType::Recursive) {
        pthread_t owner = AK::atomic_load(&m_owner, AK::memory_order_relaxed);
        if (owner == gettid()) {
            // We already own the mutex!
            m_level++;
            return true;
        }
    }
    return false;
}

void Mutex::lock()
{
    // Fast path: attempt to claim the mutex without waiting.
    u32 value = MUTEX_UNLOCKED;
    bool exchanged = AK::atomic_compare_exchange_strong(&m_lock, value, MUTEX_LOCKED_NO_NEED_TO_WAKE, AK::memory_order_acquire);
    if (exchanged) [[likely]] {
        if (m_type == MutexType::Recursive)
            AK::atomic_store(&m_owner, gettid(), AK::memory_order_relaxed);
        m_level = 0;
        return;
    } else if (m_type == MutexType::Recursive) {
        pthread_t owner = AK::atomic_load(&m_owner, AK::memory_order_relaxed);
        if (owner == gettid()) {
            // We already own the mutex!
            m_level++;
            return;
        }
    }

    // Slow path: wait, record the fact that we're going to wait, and always
    // remember to wake the next thread up once we release the mutex.
    if (value != MUTEX_LOCKED_NEED_TO_WAKE)
        value = AK::atomic_exchange(&m_lock, MUTEX_LOCKED_NEED_TO_WAKE, AK::memory_order_acquire);

    while (value != MUTEX_UNLOCKED) {
        (void)futex_wait(&m_lock, MUTEX_LOCKED_NEED_TO_WAKE);
        value = AK::atomic_exchange(&m_lock, MUTEX_LOCKED_NEED_TO_WAKE, AK::memory_order_acquire);
    }

    if (m_type == MutexType::Recursive)
        AK::atomic_store(&m_owner, gettid(), AK::memory_order_relaxed);
    m_level = 0;
}

void Mutex::unlock()
{
    if (m_type == MutexType::Recursive && m_level > 0) {
        m_level--;
        return;
    }

    if (m_type == MutexType::Recursive)
        AK::atomic_store(&m_owner, 0, AK::memory_order_relaxed);

    u32 value = AK::atomic_exchange(&m_lock, MUTEX_UNLOCKED, AK::memory_order_release);
    if (value == MUTEX_LOCKED_NEED_TO_WAKE) [[unlikely]]
        MUST(futex_wake(&m_lock, 1));
}

}

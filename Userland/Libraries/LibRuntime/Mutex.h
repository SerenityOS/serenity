/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/Noncopyable.h>
#include <AK/Time.h>

namespace Runtime {

using RelativeOrAbsoluteTimeout = Variant<Duration, UnixDateTime, MonotonicTime>;

ErrorOr<void> futex_wait(u32* userspace_address, u32 value, Optional<RelativeOrAbsoluteTimeout> timeout = {}, bool process_shared = false);
ErrorOr<int> futex_wake(u32* userspace_address, u32 count, bool process_shared = false);

enum class MutexType : int {
    Normal = 0,
    Recursive = 1,
};

struct MutexAttributes {
    MutexType type = MutexType::Normal;
};

class Mutex {
    AK_MAKE_NONCOPYABLE(Mutex);
    AK_MAKE_NONMOVABLE(Mutex);

public:
    constexpr Mutex(MutexAttributes attributes = {})
        : m_type(attributes.type)
    {
    }

    bool try_lock();
    void lock();
    void unlock();

private:
    u32 m_lock = 0;
    int m_owner = 0;
    int m_level = 0;
    MutexType m_type = MutexType::Normal;
};

class MutexLocker {
    AK_MAKE_NONCOPYABLE(MutexLocker);
    AK_MAKE_NONMOVABLE(MutexLocker);

public:
    ALWAYS_INLINE explicit MutexLocker(Mutex& mutex)
        : m_mutex(mutex)
    {
        lock();
    }
    ALWAYS_INLINE ~MutexLocker()
    {
        unlock();
    }
    ALWAYS_INLINE void unlock() { m_mutex.unlock(); }
    ALWAYS_INLINE void lock() { m_mutex.lock(); }

private:
    Mutex& m_mutex;
};

}

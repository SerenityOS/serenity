/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StdLibExtras.h>
#include <Kernel/Locking/Mutex.h>

namespace Kernel {

template<typename T, LockMode LockingMode>
class LockedResource {
    AK_MAKE_NONCOPYABLE(LockedResource);

public:
    LockedResource(T* value, Mutex& mutex, LockLocation const& location)
        : m_value(value)
        , m_mutex_locker(mutex, LockingMode, location)
    {
    }

    ALWAYS_INLINE T const* operator->() const { return m_value; }
    ALWAYS_INLINE T const& operator*() const { return *m_value; }

    ALWAYS_INLINE T* operator->() requires(!IsConst<T>) { return m_value; }
    ALWAYS_INLINE T& operator*() requires(!IsConst<T>) { return *m_value; }

    ALWAYS_INLINE T const* get() const { return m_value; }
    ALWAYS_INLINE T* get() requires(!IsConst<T>) { return m_value; }

private:
    T* m_value;
    MutexLocker m_mutex_locker;
};

class MutexContendedResource {
    template<typename, LockMode>
    friend class LockedResource;

    AK_MAKE_NONCOPYABLE(MutexContendedResource);
    AK_MAKE_NONMOVABLE(MutexContendedResource);

public:
    MutexContendedResource() = default;

protected:
    mutable Mutex m_mutex;
};

}

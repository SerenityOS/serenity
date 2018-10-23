#pragma once

#include "Types.h"

namespace AK {

static inline dword CAS(volatile dword* mem, dword newval, dword oldval)
{
    dword ret;
    asm volatile(
        "cmpxchgl %2, %1"
        :"=a"(ret), "=m"(*mem)
        :"r"(newval), "m"(*mem), "0"(oldval));
    return ret;
}

class SpinLock {
public:
    SpinLock() { }
    ~SpinLock() { unlock(); }

    void lock()
    {
        for (;;) {
            if (CAS(&m_lock, 1, 0) == 1)
                return;
        }
    }

    void unlock()
    {
        // barrier();
        m_lock = 0;
    }

    void init()
    {
        m_lock = 0;
    }

private:
    volatile dword m_lock { 0 };
};

class Locker {
public:
    explicit Locker(SpinLock& l) : m_lock(l) { m_lock.lock(); }
    ~Locker() { unlock(); }
    void unlock() { m_lock.unlock(); }

private:
    SpinLock& m_lock;
};

}

using AK::SpinLock;
using AK::Locker;


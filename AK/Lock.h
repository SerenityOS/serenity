#pragma once

#include "Assertions.h"
#include "Types.h"
#include "i386.h"
#include <Kernel/Scheduler.h>

class Process;
extern Process* current;

#ifndef KERNEL
#error This thing is kernel-only right now.
#endif

namespace AK {

static inline dword CAS(volatile dword* mem, dword newval, dword oldval)
{
    dword ret;
    asm volatile(
        "cmpxchgl %2, %1"
        :"=a"(ret), "+m"(*mem)
        :"r"(newval), "0"(oldval)
        :"cc", "memory");
    return ret;
}

class Lock {
public:
    Lock() { }
    ~Lock() { }

    void lock();
    void unlock();

private:
    volatile dword m_lock { 0 };
    dword m_level { 0 };
    Process* m_holder { nullptr };
};

class Locker {
public:
    ALWAYS_INLINE explicit Locker(Lock& l) : m_lock(l) { lock(); }
    ALWAYS_INLINE ~Locker() { unlock(); }
    ALWAYS_INLINE void unlock() { m_lock.unlock(); }
    ALWAYS_INLINE void lock() { m_lock.lock(); }

private:
    Lock& m_lock;
};

inline void Lock::lock()
{
    ASSERT_INTERRUPTS_ENABLED();
    ASSERT(!Scheduler::is_active());
    for (;;) {
        if (CAS(&m_lock, 1, 0) == 0) {
            if (!m_holder || m_holder == current) {
                m_holder = current;
                ++m_level;
                memory_barrier();
                m_lock = 0;
                return;
            }
            m_lock = 0;
        }
        Scheduler::yield();
    }
}

inline void Lock::unlock()
{
    for (;;) {
        if (CAS(&m_lock, 1, 0) == 0) {
            ASSERT(m_holder == current);
            ASSERT(m_level);
            --m_level;
            if (m_level) {
                memory_barrier();
                m_lock = 0;
                return;
            }
            m_holder = nullptr;
            memory_barrier();
            m_lock = 0;
            return;
        }
        Scheduler::yield();
    }
}

#define LOCKER(lock) Locker locker(lock)

}

using AK::Lock;
using AK::Locker;

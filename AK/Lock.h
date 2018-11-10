#pragma once

#include "Assertions.h"
#include "Types.h"

#ifdef SERENITY
#include "i386.h"
int sched_yield();
#else
#include <sched.h>
typedef int InterruptDisabler;
#endif

//#define DEBUG_LOCKS

void log_try_lock(const char*);
void log_locked(const char*);
void log_unlocked(const char*);

namespace AK {

static inline dword CAS(volatile dword* mem, dword newval, dword oldval)
{
    dword ret;
    asm volatile(
        "cmpxchgl %2, %1"
        :"=a"(ret), "+m"(*mem)
        :"r"(newval), "0"(oldval)
        :"memory");
    return ret;
}

class SpinLock {
public:
    SpinLock() { }
    ~SpinLock() { }

    void lock(const char* func = nullptr)
    {
        (void)func;
#ifdef DEBUG_LOCKS
        {
            InterruptDisabler dis;
            log_try_lock(func);
        }
#endif
        for (;;) {
            if (CAS(&m_lock, 1, 0) == 0) {
#ifdef DEBUG_LOCKS
                InterruptDisabler dis;
                log_locked(func);
#endif
                return;
            }
            sched_yield();
        }
    }

    void unlock(const char* func = nullptr)
    {
        (void)func;
        // barrier();
        ASSERT(m_lock);
        m_lock = 0;
#ifdef DEBUG_LOCKS
        InterruptDisabler dis;
        log_unlocked(func);
#endif
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
    explicit Locker(SpinLock& l, const char* func) : m_lock(l), m_func(func) { lock(); }
    ~Locker() { unlock(); }
    void unlock() { m_lock.unlock(m_func); }
    void lock() { m_lock.lock(m_func); }

private:
    SpinLock& m_lock;
    const char* m_func { nullptr };
};

#define LOCKER(lock) Locker locker(lock, __FUNCTION__)

}

using AK::SpinLock;
using AK::Locker;


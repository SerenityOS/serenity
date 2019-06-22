#pragma once

#ifdef __serenity__

#include <AK/Assertions.h>
#include <AK/Types.h>
#include <unistd.h>

#define memory_barrier() asm volatile("" :: \
                                          : "memory")

static inline dword CAS(volatile dword* mem, dword newval, dword oldval)
{
    dword ret;
    asm volatile(
        "cmpxchgl %2, %1"
        : "=a"(ret), "+m"(*mem)
        : "r"(newval), "0"(oldval)
        : "cc", "memory");
    return ret;
}

class CLock {
public:
    CLock() {}
    ~CLock() {}

    void lock();
    void unlock();

private:
    volatile dword m_lock { 0 };
    dword m_level { 0 };
    int m_holder { -1 };
};

class CLocker {
public:
    [[gnu::always_inline]] inline explicit CLocker(CLock& l)
        : m_lock(l)
    {
        lock();
    }
    [[gnu::always_inline]] inline ~CLocker() { unlock(); }
    [[gnu::always_inline]] inline void unlock() { m_lock.unlock(); }
    [[gnu::always_inline]] inline void lock() { m_lock.lock(); }

private:
    CLock& m_lock;
};

[[gnu::always_inline]] inline void CLock::lock()
{
    int tid = gettid();
    for (;;) {
        if (CAS(&m_lock, 1, 0) == 0) {
            if (m_holder == -1 || m_holder == tid) {
                m_holder = tid;
                ++m_level;
                memory_barrier();
                m_lock = 0;
                return;
            }
            m_lock = 0;
        }
        donate(m_holder);
    }
}

inline void CLock::unlock()
{
    for (;;) {
        if (CAS(&m_lock, 1, 0) == 0) {
            ASSERT(m_holder == gettid());
            ASSERT(m_level);
            --m_level;
            if (m_level) {
                memory_barrier();
                m_lock = 0;
                return;
            }
            m_holder = -1;
            memory_barrier();
            m_lock = 0;
            return;
        }
        donate(m_holder);
    }
}

#define LOCKER(lock) CLocker locker(lock)

template<typename T>
class CLockable {
public:
    CLockable() {}
    CLockable(T&& resource)
        : m_resource(move(resource))
    {
    }
    CLock& lock() { return m_lock; }
    T& resource() { return m_resource; }

    T lock_and_copy()
    {
        LOCKER(m_lock);
        return m_resource;
    }

private:
    T m_resource;
    CLock m_lock;
};

#else

class CLock {
public:
    CLock() { }
    ~CLock() { }
};

#define LOCKER(x)

#endif

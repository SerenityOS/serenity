#pragma once

#ifdef __serenity__

#include <AK/Assertions.h>
#include <AK/Types.h>
#include <unistd.h>

#define memory_barrier() asm volatile("" :: \
                                          : "memory")

static inline u32 CAS(volatile u32* mem, u32 newval, u32 oldval)
{
    u32 ret;
    asm volatile(
        "cmpxchgl %2, %1"
        : "=a"(ret), "+m"(*mem)
        : "r"(newval), "0"(oldval)
        : "cc", "memory");
    return ret;
}

namespace LibThread {

class Lock {
public:
    Lock() {}
    ~Lock() {}

    void lock();
    void unlock();

private:
    volatile u32 m_lock { 0 };
    u32 m_level { 0 };
    int m_holder { -1 };
};

class Locker {
public:
    [[gnu::always_inline]] inline explicit Locker(Lock& l)
        : m_lock(l)
    {
        lock();
    }
    [[gnu::always_inline]] inline ~Locker() { unlock(); }
    [[gnu::always_inline]] inline void unlock() { m_lock.unlock(); }
    [[gnu::always_inline]] inline void lock() { m_lock.lock(); }

private:
    Lock& m_lock;
};

[[gnu::always_inline]] inline void Lock::lock()
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

inline void Lock::unlock()
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

#define LOCKER(lock) LibThread::Locker locker(lock)

template<typename T>
class Lockable {
public:
    Lockable() {}
    Lockable(T&& resource)
        : m_resource(move(resource))
    {
    }
    Lock& lock() { return m_lock; }
    T& resource() { return m_resource; }

    T lock_and_copy()
    {
        LOCKER(m_lock);
        return m_resource;
    }

private:
    T m_resource;
    Lock m_lock;
};

}

#else

namespace LibThread {

class Lock {
public:
    Lock() { }
    ~Lock() { }
};

}

#define LOCKER(x)

#endif

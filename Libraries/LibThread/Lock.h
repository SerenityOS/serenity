#pragma once

#ifdef __serenity__

#include <AK/Assertions.h>
#include <AK/Types.h>
#include <AK/Atomic.h>
#include <unistd.h>

namespace LibThread {

class Lock {
public:
    Lock() {}
    ~Lock() {}

    void lock();
    void unlock();

private:
    AK::Atomic<bool> m_lock { false };
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
        bool expected = false;
        if (m_lock.compare_exchange_strong(expected, true, AK::memory_order_acq_rel)) {
            if (m_holder == -1 || m_holder == tid) {
                m_holder = tid;
                ++m_level;
                m_lock.store(false, AK::memory_order_release);
                return;
            }
            m_lock.store(false, AK::memory_order_release);
        }
        donate(m_holder);
    }
}

inline void Lock::unlock()
{
    for (;;) {
        bool expected = false;
        if (m_lock.compare_exchange_strong(expected, true, AK::memory_order_acq_rel)) {
            ASSERT(m_holder == gettid());
            ASSERT(m_level);
            --m_level;
            if (m_level) {
                m_lock.store(false, AK::memory_order_release);
                return;
            }
            m_holder = -1;
            m_lock.store(false, AK::memory_order_release);
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

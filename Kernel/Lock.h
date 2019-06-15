#pragma once

#include <AK/Assertions.h>
#include <AK/Types.h>
#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/KSyms.h>
#include <Kernel/Scheduler.h>

class Thread;
extern Thread* current;

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

class Lock {
public:
    Lock(const char* name = nullptr)
        : m_name(name)
    {
    }
    ~Lock() {}

    void lock();
    void unlock();
    bool unlock_if_locked();

    const char* name() const { return m_name; }

private:
    volatile dword m_lock { 0 };
    dword m_level { 0 };
    Thread* m_holder { nullptr };
    const char* m_name { nullptr };
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
    if (!are_interrupts_enabled()) {
        kprintf("Interrupts disabled when trying to take Lock{%s}\n", m_name);
        dump_backtrace();
        hang();
    }
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
        Scheduler::donate_to(m_holder, m_name);
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
        Scheduler::donate_to(m_holder, m_name);
    }
}

inline bool Lock::unlock_if_locked()
{
    for (;;) {
        if (CAS(&m_lock, 1, 0) == 0) {
            if (m_level == 0) {
                memory_barrier();
                m_lock = 0;
                return false;
            }
            ASSERT(m_holder == current);
            ASSERT(m_level);
            --m_level;
            if (m_level) {
                memory_barrier();
                m_lock = 0;
                return false;
            }
            m_holder = nullptr;
            memory_barrier();
            m_lock = 0;
            return true;
        }
    }
}

#define LOCKER(lock) Locker locker(lock)

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

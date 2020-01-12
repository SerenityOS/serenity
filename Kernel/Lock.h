#pragma once

#include <AK/Assertions.h>
#include <AK/Atomic.h>
#include <AK/Types.h>
#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/KSyms.h>
#include <Kernel/Scheduler.h>
#include <Kernel/WaitQueue.h>

class Thread;
extern Thread* current;

class Lock {
public:
    Lock(const char* name = nullptr)
        : m_name(name)
    {
    }
    ~Lock() {}

    void lock();
    void unlock();
    bool force_unlock_if_locked();
    bool is_locked() const { return m_holder; }

    const char* name() const { return m_name; }

private:
    Atomic<bool> m_lock { false };
    u32 m_level { 0 };
    Thread* m_holder { nullptr };
    const char* m_name { nullptr };
    WaitQueue m_queue;
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

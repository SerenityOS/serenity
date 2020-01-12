#include <Kernel/Lock.h>
#include <Kernel/Thread.h>

void Lock::lock()
{
    ASSERT(!Scheduler::is_active());
    if (!are_interrupts_enabled()) {
        kprintf("Interrupts disabled when trying to take Lock{%s}\n", m_name);
        dump_backtrace();
        hang();
    }
    for (;;) {
        bool expected = false;
        if (m_lock.compare_exchange_strong(expected, true, AK::memory_order_acq_rel)) {
            if (!m_holder || m_holder == current) {
                m_holder = current;
                ++m_level;
                m_lock.store(false, AK::memory_order_release);
                return;
            }
            current->wait_on(m_queue, &m_lock, m_holder, m_name);
        }
    }
}

void Lock::unlock()
{
    for (;;) {
        bool expected = false;
        if (m_lock.compare_exchange_strong(expected, true, AK::memory_order_acq_rel)) {
            ASSERT(m_holder == current);
            ASSERT(m_level);
            --m_level;
            if (m_level) {
                m_lock.store(false, AK::memory_order_release);
                return;
            }
            m_holder = nullptr;
            m_queue.wake_one(&m_lock);
            return;
        }
        // I don't know *who* is using "m_lock", so just yield.
        Scheduler::yield();
    }
}

bool Lock::unlock_if_locked()
{
    for (;;) {
        bool expected = false;
        if (m_lock.compare_exchange_strong(expected, true, AK::memory_order_acq_rel)) {
            if (m_level == 0) {
                m_lock.store(false, AK::memory_order_release);
                return false;
            }
            if (m_holder != current) {
                m_lock.store(false, AK::memory_order_release);
                return false;
            }
            ASSERT(m_level);
            --m_level;
            if (m_level) {
                m_lock.store(false, AK::memory_order_release);
                return false;
            }
            m_holder = nullptr;
            m_queue.wake_one(&m_lock);
            return true;
        }
        // I don't know *who* is using "m_lock", so just yield.
        Scheduler::yield();
    }
}

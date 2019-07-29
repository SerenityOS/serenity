#include <Kernel/Lock.h>

void Lock::lock()
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

void Lock::unlock()
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

bool Lock::unlock_if_locked()
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

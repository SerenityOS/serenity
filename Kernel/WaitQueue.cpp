#include <Kernel/Thread.h>
#include <Kernel/WaitQueue.h>

WaitQueue::WaitQueue()
{
}

WaitQueue::~WaitQueue()
{
}

void WaitQueue::enqueue(Thread& thread)
{
    InterruptDisabler disabler;
    m_threads.append(&thread);
}

void WaitQueue::wake_one()
{
    InterruptDisabler disabler;
    if (m_threads.is_empty())
        return;
    if (auto* thread = m_threads.take_first())
        thread->unblock();
}

void WaitQueue::wake_all()
{
    InterruptDisabler disabler;
    if (m_threads.is_empty())
        return;
    while (!m_threads.is_empty())
        m_threads.take_first()->unblock();
}

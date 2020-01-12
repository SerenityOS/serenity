#pragma once

#include <AK/Atomic.h>
#include <AK/SinglyLinkedList.h>
#include <Kernel/Thread.h>

class WaitQueue {
public:
    WaitQueue();
    ~WaitQueue();

    void enqueue(Thread&);
    void wake_one(Atomic<bool>* lock = nullptr);
    void wake_all();

private:
    typedef IntrusiveList<Thread, &Thread::m_wait_queue_node> ThreadList;
    ThreadList m_threads;
};

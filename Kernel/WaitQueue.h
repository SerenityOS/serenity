#pragma once

#include <AK/SinglyLinkedList.h>
#include <Kernel/Thread.h>

class WaitQueue {
public:
    WaitQueue();
    ~WaitQueue();

    void enqueue(Thread&);
    void wake_one();
    void wake_all();

private:
    typedef IntrusiveList<Thread, &Thread::m_wait_queue_node> ThreadList;
    ThreadList m_threads;
};

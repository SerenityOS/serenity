#pragma once

#include <AK/SinglyLinkedList.h>

class Thread;

class WaitQueue {
public:
    WaitQueue();
    ~WaitQueue();

    void enqueue(Thread&);
    void wake_one();
    void wake_all();

private:
    SinglyLinkedList<Thread*> m_threads;
};

#ifdef __serenity__

#include <AK/Assertions.h>
#include <LibCore/CEventLoop.h>
#include <LibCore/CThread.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

CThread& CThread::main_thread()
{
    static CThread* main_thread;
    if (!main_thread)
        main_thread = new CThread(MainThread);
    return *main_thread;
}

CThread::CThread(MainThreadTag)
    : m_thread_id(0)
{
}

CThread::CThread(int (*entry)(void*), void* user_data)
{
    ASSERT(entry);
    m_thread_id = create_thread(entry, user_data);
}

CThread::~CThread()
{
}

#endif

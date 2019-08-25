#include <LibThread/Thread.h>
#include <unistd.h>

LibThread::Thread::Thread(Function<int()> action)
    : CObject(nullptr)
    , m_action(move(action))
{
}

LibThread::Thread::~Thread()
{
    if (m_tid != -1) {
        dbg() << "trying to destroy a running thread!";
        ASSERT_NOT_REACHED();
    }
}

void LibThread::Thread::start()
{
    int rc = create_thread([](void* arg) {
        Thread* self = static_cast<Thread*>(arg);
        int exit_code = self->m_action();
        self->m_tid = -1;
        exit_thread(exit_code);
        return exit_code;
    }, static_cast<void*>(this));

    ASSERT(rc > 0);
    dbg() << "Started a thread, tid = " << rc;
    m_tid = rc;
}

void LibThread::Thread::quit(int code)
{
    ASSERT(m_tid == gettid());

    m_tid = -1;
    exit_thread(code);
}

#include <LibThread/Thread.h>
#include <pthread.h>
#include <unistd.h>

LibThread::Thread::Thread(Function<int()> action, StringView thread_name)
    : CObject(nullptr)
    , m_action(move(action))
    , m_thread_name(thread_name.is_null() ? "" : thread_name)
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
    int rc = pthread_create(
        &m_tid,
        nullptr,
        [](void* arg) -> void* {
            Thread* self = static_cast<Thread*>(arg);
            int exit_code = self->m_action();
            self->m_tid = -1;
            pthread_exit((void*)exit_code);
            return (void*)exit_code;
        },
        static_cast<void*>(this));

    ASSERT(rc == 0);
    if (!m_thread_name.is_empty()) {
        rc = pthread_setname_np(m_tid, m_thread_name.characters());
        ASSERT(rc == 0);
    }
    dbg() << "Started a thread, tid = " << m_tid;
}

void LibThread::Thread::quit(int code)
{
    ASSERT(m_tid == gettid());

    m_tid = -1;
    pthread_exit((void*)code);
}

#include <LibThread/BackgroundAction.h>
#include <LibThread/Thread.h>
#include <LibCore/CLock.h>
#include <AK/Queue.h>

static CLockable<Queue<Function<void()>>>* s_all_actions;
static LibThread::Thread* s_background_thread;

static int background_thread_func()
{
    while (true) {
        Function<void()> work_item;
        {
            LOCKER(s_all_actions->lock());

            if (!s_all_actions->resource().is_empty())
                work_item = s_all_actions->resource().dequeue();
        }
        if (work_item)
            work_item();
        else
            sleep(1);
    }

    ASSERT_NOT_REACHED();
}

static void init()
{
    s_all_actions = new CLockable<Queue<Function<void()>>>();
    s_background_thread = new LibThread::Thread(background_thread_func);
    s_background_thread->set_name("Background thread");
    s_background_thread->start();
}

CLockable<Queue<Function<void()>>>& LibThread::BackgroundActionBase::all_actions()
{
    if (s_all_actions == nullptr)
        init();
    return *s_all_actions;
}

LibThread::Thread& LibThread::BackgroundActionBase::background_thread()
{
    if (s_background_thread == nullptr)
        init();
    return *s_background_thread;
}

#include "CProcess.h"

#include <sched.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <signal.h>
#include <unistd.h>

void handle_sigchld(int)
{
    dbgprintf("CProcess(%d) Got SIGCHLD\n", getpid());
    int pid = waitpid(-1, nullptr, 0);
    dbgprintf("CProcess(%d) waitpid() returned %d\n", getpid(), pid);
    ASSERT(pid > 0);
}

CProcess::CProcess(int pid)
    : m_pid(pid)
{
    static bool signal_handled = false;
    if (!signal_handled) {
        signal_handled = true;
        signal(SIGCHLD, handle_sigchld);
    }
}

CProcess::~CProcess()
{
    dbgprintf("Destroying %d\n", m_pid);
    kill();
}

void CProcess::set_priority(Priority priority)
{
    int prio = -1;
    switch (priority) {
    case Priority::Highest:
        prio = sched_get_priority_max(SCHED_OTHER);
        break;
    case Priority::Normal:
        return;
        break;
    case Priority::Lowest:
        prio = sched_get_priority_min(SCHED_OTHER);
        break;
    }

    if (priority != Priority::Normal) {
        dbgprintf("Setting PID %d to prio %d\n", m_pid, prio);
        struct sched_param p;
        p.sched_priority = prio;
        int ret = sched_setparam(m_pid, &p);
        ASSERT(ret == 0);
    }
}

CProcess* CProcess::start_detached(const StringView& program, const Vector<StringView>& arguments, Priority priority)
{
    pid_t pid = 0;

    while (true) {
        dbgprintf("Forking for %s...\n", program.characters());
        int pid = fork();
        if (pid < 0) {
            dbgprintf("Fork %s failed! %s\n", program.characters(), strerror(errno));
            continue;
        } else if (pid > 0) {
            // parent...
            dbgprintf("Process %s hopefully started...\n", program.characters());
            auto p = new CProcess(pid);
            p->set_priority(priority);
            return p;
        }
        break;
    }

    while (true) {
        ASSERT(pid == 0);
        char* progv[256];
        ASSERT(arguments.size() < 254);
        progv[0] = const_cast<char*>(program.characters());

        for (int i = 0; i < arguments.size(); i++) {
            progv[i + 1] = const_cast<char*>(arguments[i].characters());
        }

        progv[arguments.size() + 1] = nullptr;

        int ret = execv(program.characters(), progv);
        if (ret < 0) {
            dbgprintf("Exec %s failed! %s\n", program.characters(), strerror(errno));
            continue;
        }

        ASSERT_NOT_REACHED();
    }
}

void CProcess::kill()
{
    dbgprintf("Killing PID %d\n", m_pid);
    ::kill(m_pid, SIGTERM);
}


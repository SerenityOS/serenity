#include <stdio.h>
#include <sys/types.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <sched.h>
#include <AK/Assertions.h>

void start_process(const char *prog, int prio)
{
    pid_t pid = 0;

    while (true) {
        dbgprintf("Forking for %s...\n", prog);
        int pid = fork();
        if (pid < 0) {
            dbgprintf("Fork %s failed! %s\n", prog, strerror(errno));
            continue;
        } else if (pid > 0) {
            // parent...
            dbgprintf("Process %s hopefully started with priority %d...\n", prog, prio);
            return;
        }
        break;
    }

    while (true) {
        if (pid == 0) {
            dbgprintf("Executing for %s... at prio %d\n", prog, prio);
            struct sched_param p;
            p.sched_priority = prio;
            int ret = sched_setparam(pid, &p);
            ASSERT(ret == 0);

            char* progv[256];
            progv[0] = (char*)prog;
            progv[1] = nullptr;
            ret = execv(prog, progv);
            if (ret < 0) {
                dbgprintf("Exec %s failed! %s", prog, strerror(errno));
                continue;
            }
            break;
        } else {
            break;
        }
    }
}

int main(int, char **)
{
    int lowest_prio = sched_get_priority_min(SCHED_OTHER);
    int highest_prio = sched_get_priority_max(SCHED_OTHER);
    start_process("/bin/LookupServer", lowest_prio);
    start_process("/bin/WindowServer", highest_prio);
    start_process("/bin/Taskbar", highest_prio);
    start_process("/bin/Terminal", highest_prio-1);
    start_process("/bin/Launcher", highest_prio);

    while (1) {
        sleep(1);
    }
}

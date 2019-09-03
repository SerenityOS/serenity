#include <AK/Assertions.h>
#include <LibCore/CFile.h>
#include <errno.h>
#include <fcntl.h>
#include <sched.h>
#include <stdio.h>
#include <string.h>
#include <sys/types.h>
#include <unistd.h>

void start_process(const String& program, const Vector<String>& arguments, int prio, const char* tty = nullptr)
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
            dbgprintf("Process %s hopefully started with priority %d...\n", program.characters(), prio);
            return;
        }
        break;
    }

    while (true) {
        dbgprintf("Executing for %s... at prio %d\n", program.characters(), prio);
        struct sched_param p;
        p.sched_priority = prio;
        int ret = sched_setparam(pid, &p);
        ASSERT(ret == 0);

        if (tty != nullptr) {
            close(0);
            ASSERT(open(tty, O_RDWR) == 0);
            dup2(0, 1);
            dup2(0, 2);
        }

        char* progv[256];
        progv[0] = const_cast<char*>(program.characters());
        for (int i = 0; i < arguments.size() && i < 254; i++)
            progv[i+1] = const_cast<char*>(arguments[i].characters());
        progv[arguments.size() + 1] = nullptr;
        ret = execv(progv[0], progv);
        if (ret < 0) {
            dbgprintf("Exec %s failed! %s", progv[0], strerror(errno));
            continue;
        }
        break;
    }
}

static void check_for_test_mode()
{
    CFile f("/proc/cmdline");
    if (!f.open(CIODevice::ReadOnly)) {
        dbgprintf("Failed to read command line: %s\n", f.error_string());
        ASSERT(false);
    }
    const String cmd = String::copy(f.read_all());
    dbgprintf("Read command line: %s\n", cmd.characters());
    if (cmd.matches("*testmode=1*")) {
        // Eventually, we should run a test binary and wait for it to finish
        // before shutting down. But this is good enough for now.
        dbgprintf("Waiting for testmode shutdown...\n");
        sleep(5);
        dbgprintf("Shutting down due to testmode...\n");
        if (fork() == 0) {
            execl("/bin/shutdown", "/bin/shutdown", "-n", nullptr);
            ASSERT_NOT_REACHED();
        }
    } else {
        dbgprintf("Continuing normally\n");
    }
}

int main(int, char**)
{
    int lowest_prio = sched_get_priority_min(SCHED_OTHER);
    int highest_prio = sched_get_priority_max(SCHED_OTHER);

    // Mount the filesystems.
    start_process("/bin/mount", { "-a" }, highest_prio);
    wait(nullptr);

    // NOTE: We don't start anything on tty0 since that's the "active" TTY while WindowServer is up.
    start_process("/bin/TTYServer", { "tty1" }, highest_prio, "/dev/tty1");
    start_process("/bin/TTYServer", { "tty2" }, highest_prio, "/dev/tty2");
    start_process("/bin/TTYServer", { "tty3" }, highest_prio, "/dev/tty3");

    // Drop privileges.
    setuid(100);
    setgid(100);

    start_process("/bin/LookupServer", {}, lowest_prio);
    start_process("/bin/WindowServer", {}, highest_prio);
    start_process("/bin/AudioServer", {}, highest_prio);
    start_process("/bin/Taskbar", {}, highest_prio);
    start_process("/bin/Terminal", {}, highest_prio - 1);
    start_process("/bin/Launcher", {}, highest_prio);

    // This won't return if we're in test mode.
    check_for_test_mode();

    while (1) {
        sleep(1);
    }
}

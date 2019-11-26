#include "Service.h"
#include <AK/Assertions.h>
#include <LibCore/CConfigFile.h>
#include <LibCore/CEventLoop.h>
#include <LibCore/CFile.h>
#include <errno.h>
#include <signal.h>
#include <stdio.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>

static void sigchld_handler(int)
{
    int status = 0;
    pid_t pid = waitpid(-1, &status, WNOHANG);
    if (!pid)
        return;

    dbg() << "Reaped child with pid " << pid;
    Service* service = Service::find_by_pid(pid);
    if (service == nullptr) {
        dbg() << "There was no service with this pid, what is going on?";
        return;
    }

    // Call service->did_exit(status) some time soon.
    // We wouldn't want to run the complex logic, such
    // as possibly spawning the service again, from the
    // signal handler, so defer it.
    CEventLoop::main().post_event(*service, make<CDeferredInvocationEvent>([=](CObject&) {
        service->did_exit(status);
    }));
    CEventLoop::wake();
}

static void check_for_test_mode()
{
    auto f = CFile::construct("/proc/cmdline");
    if (!f->open(CIODevice::ReadOnly)) {
        dbg() << "Failed to read command line: " << f->error_string();
        ASSERT(false);
    }
    const String cmd = String::copy(f->read_all());
    dbg() << "Read command line: " << cmd;
    if (cmd.matches("*testmode=1*")) {
        // Eventually, we should run a test binary and wait for it to finish
        // before shutting down. But this is good enough for now.
        dbg() << "Waiting for testmode shutdown...";
        sleep(5);
        dbg() << "Shutting down due to testmode...";
        if (fork() == 0) {
            execl("/bin/shutdown", "/bin/shutdown", "-n", nullptr);
            ASSERT_NOT_REACHED();
        }
    } else {
        dbg() << "Continuing normally";
    }
}

static void mount_all_filesystems()
{
    dbg() << "Spawning mount -a to mount all filesystems.";
    pid_t pid = fork();

    if (pid < 0) {
        perror("fork");
        ASSERT_NOT_REACHED();
    } else if (pid == 0) {
        execl("/bin/mount", "mount", "-a", nullptr);
        perror("exec");
        ASSERT_NOT_REACHED();
    } else {
        wait(nullptr);
    }
}

int main(int, char**)
{
    mount_all_filesystems();

    struct sigaction sa = {
        .sa_handler = sigchld_handler,
        .sa_mask = 0,
        .sa_flags = SA_RESTART
    };
    sigaction(SIGCHLD, &sa, nullptr);

    CEventLoop event_loop;

    // Read our config and instantiate services.
    // This takes care of setting up sockets.
    Vector<RefPtr<Service>> services;
    auto config = CConfigFile::get_for_system("SystemServer");
    for (auto name : config->groups())
        services.append(Service::construct(*config, name));

    // After we've set them all up, spawn them!
    for (auto& service : services)
        service->spawn();

    // This won't return if we're in test mode.
    check_for_test_mode();

    return event_loop.exec();
}

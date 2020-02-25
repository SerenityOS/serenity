/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "Service.h"
#include <AK/Assertions.h>
#include <AK/ByteBuffer.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/Event.h>
#include <LibCore/EventLoop.h>
#include <LibCore/File.h>
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
    Core::EventLoop::main().post_event(*service, make<Core::DeferredInvocationEvent>([=](auto&) {
        service->did_exit(status);
    }));
    Core::EventLoop::wake();
}

static void check_for_test_mode()
{
    auto f = Core::File::construct("/proc/cmdline");
    if (!f->open(Core::IODevice::ReadOnly)) {
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
    if (pledge("stdio proc exec tty accept unix rpath wpath cpath chown fattr id", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    mount_all_filesystems();

    struct sigaction sa = {
        .sa_handler = sigchld_handler,
        .sa_mask = 0,
        .sa_flags = SA_RESTART
    };
    sigaction(SIGCHLD, &sa, nullptr);

    Core::EventLoop event_loop;

    // Read our config and instantiate services.
    // This takes care of setting up sockets.
    Vector<RefPtr<Service>> services;
    auto config = Core::ConfigFile::get_for_system("SystemServer");
    for (auto name : config->groups())
        services.append(Service::construct(*config, name));

    // After we've set them all up, activate them!
    for (auto& service : services)
        service->activate();

    // This won't return if we're in test mode.
    check_for_test_mode();

    return event_loop.exec();
}

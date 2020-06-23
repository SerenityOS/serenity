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

String g_boot_mode = "graphical";

static void sigchld_handler(int)
{
    int status = 0;
    pid_t pid = waitpid(-1, &status, WNOHANG);
    if (!pid)
        return;

#ifdef SYSTEMSERVER_DEBUG
    dbg() << "Reaped child with pid " << pid << ", exit status " << status;
#endif

    Service* service = Service::find_by_pid(pid);
    if (service == nullptr) {
        // This can happen for multi-instance services.
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

static void parse_boot_mode()
{
    auto f = Core::File::construct("/proc/cmdline");
    if (!f->open(Core::IODevice::ReadOnly)) {
        dbg() << "Failed to read command line: " << f->error_string();
        return;
    }
    const String cmdline = String::copy(f->read_all(), Chomp);
    dbg() << "Read command line: " << cmdline;

    for (auto& part : cmdline.split_view(' ')) {
        auto pair = part.split_view('=', 2);
        if (pair.size() == 2 && pair[0] == "boot_mode")
            g_boot_mode = pair[1];
    }
    dbg() << "Booting in " << g_boot_mode << " mode";
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
    if (pledge("stdio proc exec tty accept unix rpath wpath cpath chown fattr id sigaction", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    mount_all_filesystems();
    parse_boot_mode();

    struct sigaction sa = {
        .sa_handler = sigchld_handler,
        .sa_mask = 0,
        .sa_flags = SA_RESTART
    };
    sigaction(SIGCHLD, &sa, nullptr);

    Core::EventLoop event_loop;

    // Read our config and instantiate services.
    // This takes care of setting up sockets.
    NonnullRefPtrVector<Service> services;
    auto config = Core::ConfigFile::get_for_system("SystemServer");
    for (auto name : config->groups()) {
        auto service = Service::construct(*config, name);
        if (service->is_enabled())
            services.append(service);
    }

    // After we've set them all up, activate them!
    dbg() << "Activating " << services.size() << " services...";
    for (auto& service : services)
        service.activate();

    return event_loop.exec();
}

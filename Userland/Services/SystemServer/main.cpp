/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
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
#include <AK/Debug.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/Event.h>
#include <LibCore/EventLoop.h>
#include <LibCore/File.h>
#include <errno.h>
#include <signal.h>
#include <stdio.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>

String g_boot_mode = "graphical";

static void sigchld_handler(int)
{
    for (;;) {
        int status = 0;
        pid_t pid = waitpid(-1, &status, WNOHANG);
        if (pid < 0) {
            perror("waitpid");
            break;
        }
        if (pid == 0)
            break;

        dbgln<debug_system_server>("Reaped child with pid {}, exit status {}", pid, status);

        Service* service = Service::find_by_pid(pid);
        if (service == nullptr) {
            // This can happen for multi-instance services.
            continue;
        }

        service->did_exit(status);
    }
}

static void parse_boot_mode()
{
    auto f = Core::File::construct("/proc/cmdline");
    if (!f->open(Core::IODevice::ReadOnly)) {
        dbgln("Failed to read command line: {}", f->error_string());
        return;
    }
    const String cmdline = String::copy(f->read_all(), Chomp);
    dbgln("Read command line: {}", cmdline);

    for (auto& part : cmdline.split_view(' ')) {
        auto pair = part.split_view('=', 2);
        if (pair.size() == 2 && pair[0] == "boot_mode")
            g_boot_mode = pair[1];
    }
    dbgln("Booting in {} mode", g_boot_mode);
}

static void prepare_devfs()
{
    // FIXME: Find a better way to all of this stuff, without hardcoding all of this!

    int rc = mount(-1, "/dev", "dev", 0);
    if (rc != 0) {
        ASSERT_NOT_REACHED();
    }

    rc = mkdir("/dev/pts", 0755);
    if (rc != 0) {
        ASSERT_NOT_REACHED();
    }

    rc = mount(-1, "/dev/pts", "devpts", 0);
    if (rc != 0) {
        ASSERT_NOT_REACHED();
    }

    rc = symlink("/dev/random", "/dev/urandom");
    if (rc < 0) {
        ASSERT_NOT_REACHED();
    }

    // FIXME: Find a better way to chown without hardcoding the gid!
    // This will fail with ENOENT in text mode.
    rc = chown("/dev/fb0", 0, 3);
    if (rc < 0 && errno != ENOENT) {
        ASSERT_NOT_REACHED();
    }

    // FIXME: Find a better way to chown without hardcoding the gid!
    rc = chown("/dev/keyboard", 0, 3);
    if (rc < 0) {
        ASSERT_NOT_REACHED();
    }

    // FIXME: Find a better way to chown without hardcoding the gid!
    rc = chown("/dev/mouse", 0, 3);
    if (rc < 0) {
        ASSERT_NOT_REACHED();
    }

    for (size_t index = 0; index < 4; index++) {
        // FIXME: Find a better way to chown without hardcoding the gid!
        rc = chown(String::formatted("/dev/tty{}", index).characters(), 0, 2);
        if (rc < 0) {
            ASSERT_NOT_REACHED();
        }
    }

    for (size_t index = 0; index < 4; index++) {
        // FIXME: Find a better way to chown without hardcoding the gid!
        rc = chown(String::formatted("/dev/ttyS{}", index).characters(), 0, 2);
        if (rc < 0) {
            ASSERT_NOT_REACHED();
        }
    }

    // FIXME: Find a better way to chown without hardcoding the gid!
    rc = chown("/dev/audio", 0, 4);
    if (rc < 0) {
        ASSERT_NOT_REACHED();
    }

    rc = symlink("/proc/self/fd/0", "/dev/stdin");
    if (rc < 0) {
        ASSERT_NOT_REACHED();
    }
    rc = symlink("/proc/self/fd/1", "/dev/stdout");
    if (rc < 0) {
        ASSERT_NOT_REACHED();
    }
    rc = symlink("/proc/self/fd/2", "/dev/stderr");
    if (rc < 0) {
        ASSERT_NOT_REACHED();
    }
}

static void mount_all_filesystems()
{
    dbgln("Spawning mount -a to mount all filesystems.");
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

static void create_tmp_rpc_directory()
{
    dbgln("Creating /tmp/rpc directory");
    auto old_umask = umask(0);
    auto rc = mkdir("/tmp/rpc", 01777);
    if (rc < 0) {
        perror("mkdir(/tmp/rpc)");
        ASSERT_NOT_REACHED();
    }
    umask(old_umask);
}

static void create_tmp_coredump_directory()
{
    dbgln("Creating /tmp/coredump directory");
    auto old_umask = umask(0);
    auto rc = mkdir("/tmp/coredump", 0755);
    if (rc < 0) {
        perror("mkdir(/tmp/coredump)");
        ASSERT_NOT_REACHED();
    }
    umask(old_umask);
}

int main(int, char**)
{
    prepare_devfs();

    if (pledge("stdio proc exec tty accept unix rpath wpath cpath chown fattr id sigaction", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    mount_all_filesystems();
    create_tmp_rpc_directory();
    create_tmp_coredump_directory();
    parse_boot_mode();

    Core::EventLoop event_loop;

    event_loop.register_signal(SIGCHLD, sigchld_handler);

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
    dbgln("Activating {} services...", services.size());
    for (auto& service : services)
        service.activate();

    return event_loop.exec();
}

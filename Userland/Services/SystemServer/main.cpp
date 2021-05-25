/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
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
#include <grp.h>
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

        dbgln_if(SYSTEMSERVER_DEBUG, "Reaped child with pid {}, exit status {}", pid, status);

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
    if (!f->open(Core::OpenMode::ReadOnly)) {
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

static void chown_wrapper(const char* path, uid_t uid, gid_t gid)
{
    int rc = chown(path, uid, gid);
    if (rc < 0 && errno != ENOENT) {
        VERIFY_NOT_REACHED();
    }
}

static void prepare_devfs()
{
    // FIXME: Find a better way to all of this stuff, without hardcoding all of this!

    int rc = mount(-1, "/dev", "dev", 0);
    if (rc != 0) {
        VERIFY_NOT_REACHED();
    }

    rc = mkdir("/dev/pts", 0755);
    if (rc != 0) {
        VERIFY_NOT_REACHED();
    }

    rc = mount(-1, "/dev/pts", "devpts", 0);
    if (rc != 0) {
        VERIFY_NOT_REACHED();
    }

    rc = symlink("/dev/random", "/dev/urandom");
    if (rc < 0) {
        VERIFY_NOT_REACHED();
    }

    auto phys_group = getgrnam("phys");
    VERIFY(phys_group);
    chown_wrapper("/dev/fb0", 0, phys_group->gr_gid);

    chown_wrapper("/dev/keyboard0", 0, phys_group->gr_gid);

    chown_wrapper("/dev/mouse0", 0, phys_group->gr_gid);

    auto tty_group = getgrnam("tty");
    VERIFY(tty_group);
    // FIXME: Count TTYs instead of using a hardcoded amount
    for (size_t index = 0; index < 6; index++) {
        chown_wrapper(String::formatted("/dev/tty{}", index).characters(), 0, tty_group->gr_gid);
    }

    // FIXME: Count serial TTYs instead of using a hardcoded amount
    for (size_t index = 0; index < 4; index++) {
        chown_wrapper(String::formatted("/dev/ttyS{}", index).characters(), 0, tty_group->gr_gid);
    }

    auto audio_group = getgrnam("audio");
    VERIFY(audio_group);
    chown_wrapper("/dev/audio", 0, audio_group->gr_gid);

    rc = symlink("/proc/self/fd/0", "/dev/stdin");
    if (rc < 0) {
        VERIFY_NOT_REACHED();
    }
    rc = symlink("/proc/self/fd/1", "/dev/stdout");
    if (rc < 0) {
        VERIFY_NOT_REACHED();
    }
    rc = symlink("/proc/self/fd/2", "/dev/stderr");
    if (rc < 0) {
        VERIFY_NOT_REACHED();
    }

    endgrent();
}

static void mount_all_filesystems()
{
    dbgln("Spawning mount -a to mount all filesystems.");
    pid_t pid = fork();

    if (pid < 0) {
        perror("fork");
        VERIFY_NOT_REACHED();
    } else if (pid == 0) {
        execl("/bin/mount", "mount", "-a", nullptr);
        perror("exec");
        VERIFY_NOT_REACHED();
    } else {
        wait(nullptr);
    }
}

static void create_tmp_coredump_directory()
{
    dbgln("Creating /tmp/coredump directory");
    auto old_umask = umask(0);
    // FIXME: the coredump directory should be made read-only once CrashDaemon is no longer responsible for compressing coredumps
    auto rc = mkdir("/tmp/coredump", 0777);
    if (rc < 0) {
        perror("mkdir(/tmp/coredump)");
        VERIFY_NOT_REACHED();
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

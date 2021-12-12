/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Peter Elliott <pelliott@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Service.h"
#include <AK/Assertions.h>
#include <AK/ByteBuffer.h>
#include <AK/Debug.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/DirIterator.h>
#include <LibCore/Event.h>
#include <LibCore/EventLoop.h>
#include <LibCore/File.h>
#include <errno.h>
#include <fcntl.h>
#include <grp.h>
#include <signal.h>
#include <stdio.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>

String g_system_mode = "graphical";

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

static void determine_system_mode()
{
    auto f = Core::File::construct("/proc/system_mode");
    if (!f->open(Core::OpenMode::ReadOnly)) {
        dbgln("Failed to read system_mode: {}", f->error_string());
        // Continue to assume "graphical".
        return;
    }
    const String system_mode = String::copy(f->read_all(), Chomp);
    if (f->error()) {
        dbgln("Failed to read system_mode: {}", f->error_string());
        // Continue to assume "graphical".
        return;
    }

    g_system_mode = system_mode;
    dbgln("Read system_mode: {}", g_system_mode);

    // FIXME: Support more than one framebuffer detection
    struct stat file_state;
    int rc = lstat("/dev/fb0", &file_state);
    if (rc < 0 && g_system_mode == "graphical") {
        g_system_mode = "text";
    } else if (rc == 0 && g_system_mode == "text") {
        dbgln("WARNING: Text mode with framebuffers won't work as expected! Consider using 'fbdev=off'.");
    }
    dbgln("System in {} mode", g_system_mode);
}

static void chown_wrapper(const char* path, uid_t uid, gid_t gid)
{
    int rc = chown(path, uid, gid);
    if (rc < 0 && errno != ENOENT) {
        VERIFY_NOT_REACHED();
    }
}
static void chmod_wrapper(const char* path, mode_t mode)
{
    int rc = chmod(path, mode);
    if (rc < 0 && errno != ENOENT) {
        VERIFY_NOT_REACHED();
    }
}

static void chown_all_matching_device_nodes(group* group, unsigned major_number)
{
    VERIFY(group);
    struct stat cur_file_stat;

    Core::DirIterator di("/dev/", Core::DirIterator::SkipParentAndBaseDir);
    if (di.has_error())
        VERIFY_NOT_REACHED();
    while (di.has_next()) {
        auto entry_name = di.next_full_path();
        auto rc = stat(entry_name.characters(), &cur_file_stat);
        if (rc < 0)
            continue;
        if (major(cur_file_stat.st_rdev) != major_number)
            continue;
        chown_wrapper(entry_name.characters(), 0, group->gr_gid);
    }
}

constexpr unsigned encoded_device(unsigned major, unsigned minor)
{
    return (minor & 0xff) | (major << 8) | ((minor & ~0xff) << 12);
}

inline char offset_character_with_number(char base_char, u8 offset)
{
    char offsetted_char = base_char;
    VERIFY(static_cast<size_t>(offsetted_char) + static_cast<size_t>(offset) < 256);
    offsetted_char += offset;
    return offsetted_char;
}

static void create_devfs_block_device(String name, mode_t mode, unsigned major, unsigned minor)
{
    if (auto rc = mknod(name.characters(), mode | S_IFBLK, encoded_device(major, minor)); rc < 0)
        VERIFY_NOT_REACHED();
}

static void populate_devfs_block_devices()
{
    Core::DirIterator di("/sys/dev/block/", Core::DirIterator::SkipParentAndBaseDir);
    if (di.has_error()) {
        warnln("Failed to open /sys/dev/block - {}", di.error());
        VERIFY_NOT_REACHED();
    }
    while (di.has_next()) {
        auto entry_name = di.next_path().split(':');
        VERIFY(entry_name.size() == 2);
        auto major_number = entry_name[0].to_uint<unsigned>().value();
        auto minor_number = entry_name[1].to_uint<unsigned>().value();
        switch (major_number) {
        case 29: {
            create_devfs_block_device(String::formatted("/dev/fb{}", minor_number), 0666, 29, minor_number);
            break;
        }
        case 30: {
            create_devfs_block_device(String::formatted("/dev/kcov{}", minor_number), 0666, 30, minor_number);
            break;
        }
        case 3: {
            create_devfs_block_device(String::formatted("/dev/hd{}", offset_character_with_number('a', minor_number)), 0600, 3, minor_number);
            break;
        }
        default:
            warnln("Unknown block device {}:{}", major_number, minor_number);
            break;
        }
    }
}

static void create_devfs_char_device(String name, mode_t mode, unsigned major, unsigned minor)
{
    if (auto rc = mknod(name.characters(), mode | S_IFCHR, encoded_device(major, minor)); rc < 0)
        VERIFY_NOT_REACHED();
}

static void populate_devfs_char_devices()
{
    Core::DirIterator di("/sys/dev/char/", Core::DirIterator::SkipParentAndBaseDir);
    if (di.has_error()) {
        warnln("Failed to open /sys/dev/char - {}", di.error());
        VERIFY_NOT_REACHED();
    }
    while (di.has_next()) {
        auto entry_name = di.next_path().split(':');
        VERIFY(entry_name.size() == 2);
        auto major_number = entry_name[0].to_uint<unsigned>().value();
        auto minor_number = entry_name[1].to_uint<unsigned>().value();
        switch (major_number) {
        case 42: {
            switch (minor_number) {
            case 42: {
                create_devfs_char_device("/dev/audio", 0220, 42, 42);
                break;
            }
            default:
                warnln("Unknown character device {}:{}", major_number, minor_number);
            }
            break;
        }
        case 29: {
            switch (minor_number) {
            case 0: {
                create_devfs_char_device("/dev/full", 0660, 29, 0);
                break;
            }
            default:
                warnln("Unknown character device {}:{}", major_number, minor_number);
            }
            break;
        }
        case 229: {
            create_devfs_char_device(String::formatted("/dev/hvc0p{}", minor_number), 0666, major_number, minor_number);
            break;
        }
        case 10: {
            switch (minor_number) {
            case 0: {
                create_devfs_char_device("/dev/mouse0", 0660, 10, 0);
                break;
            }
            case 183: {
                create_devfs_char_device("/dev/hwrng", 0660, 10, 183);
                break;
            }
            default:
                warnln("Unknown character device {}:{}", major_number, minor_number);
            }
            break;
        }
        case 85: {
            switch (minor_number) {
            case 0: {
                create_devfs_char_device("/dev/keyboard0", 0660, 85, 0);
                break;
            }
            default:
                warnln("Unknown character device {}:{}", major_number, minor_number);
            }
            break;
        }
        case 1: {
            switch (minor_number) {
            case 5: {
                create_devfs_char_device("/dev/zero", 0666, 1, 5);
                break;
            }
            case 1: {
                create_devfs_char_device("/dev/mem", 0660, 1, 1);
                break;
            }
            case 3: {
                create_devfs_char_device("/dev/null", 0666, 1, 3);
                break;
            }
            case 8: {
                create_devfs_char_device("/dev/random", 0666, 1, 8);
                break;
            }
            default:
                warnln("Unknown character device {}:{}", major_number, minor_number);
                break;
            }
            break;
        }
        case 5: {
            switch (minor_number) {
            case 1: {
                create_devfs_char_device("/dev/console", 0666, 5, 1);
                break;
            }
            case 2: {
                create_devfs_char_device("/dev/ptmx", 0666, 5, 2);
                break;
            }
            default:
                warnln("Unknown character device {}:{}", major_number, minor_number);
            }
            break;
        }
        case 4: {
            switch (minor_number) {
            case 0: {
                create_devfs_char_device("/dev/tty0", 0620, 4, 0);
                break;
            }
            case 1: {
                create_devfs_char_device("/dev/tty1", 0620, 4, 1);
                break;
            }
            case 2: {
                create_devfs_char_device("/dev/tty2", 0620, 4, 2);
                break;
            }
            case 3: {
                create_devfs_char_device("/dev/tty3", 0620, 4, 3);
                break;
            }
            case 64: {
                create_devfs_char_device("/dev/ttyS0", 0620, 4, 64);
                break;
            }
            case 65: {
                create_devfs_char_device("/dev/ttyS1", 0620, 4, 65);
                break;
            }
            case 66: {
                create_devfs_char_device("/dev/ttyS2", 0620, 4, 66);
                break;
            }
            case 67: {
                create_devfs_char_device("/dev/ttyS3", 0666, 4, 67);
                break;
            }
            default:
                warnln("Unknown character device {}:{}", major_number, minor_number);
            }
            break;
        }
        default:
            warnln("Unknown character device {}:{}", major_number, minor_number);
            break;
        }
    }
}

static void populate_devfs()
{
    mode_t old_mask = umask(0);
    printf("Changing umask %#o\n", old_mask);
    populate_devfs_char_devices();
    populate_devfs_block_devices();
    umask(old_mask);
}

static void prepare_synthetic_filesystems()
{
    // FIXME: Find a better way to all of this stuff, without hardcoding all of this!
    int rc = mount(-1, "/proc", "proc", MS_NOSUID);
    if (rc != 0) {
        VERIFY_NOT_REACHED();
    }

    rc = mount(-1, "/sys", "sys", 0);
    if (rc != 0) {
        VERIFY_NOT_REACHED();
    }

    rc = mount(-1, "/dev", "dev", 0);
    if (rc != 0) {
        VERIFY_NOT_REACHED();
    }

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

    rc = symlink("/proc/self/tty", "/dev/tty");
    if (rc < 0) {
        VERIFY_NOT_REACHED();
    }

    populate_devfs();

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
    chmod_wrapper("/dev/urandom", 0666);

    auto phys_group = getgrnam("phys");
    VERIFY(phys_group);
    // FIXME: Try to find a way to not hardcode the major number of framebuffer device nodes.
    chown_all_matching_device_nodes(phys_group, 29);

    chown_wrapper("/dev/keyboard0", 0, phys_group->gr_gid);

    chown_wrapper("/dev/mouse0", 0, phys_group->gr_gid);

    auto tty_group = getgrnam("tty");
    VERIFY(tty_group);
    // FIXME: Try to find a way to not hardcode the major number of tty nodes.
    chown_all_matching_device_nodes(tty_group, 4);

    auto audio_group = getgrnam("audio");
    VERIFY(audio_group);
    chown_wrapper("/dev/audio", 0, audio_group->gr_gid);

    // Note: We open the /dev/null device and set file descriptors 0, 1, 2 to it
    // because otherwise these file descriptors won't have a custody, making
    // the ProcFS file descriptor links (at /proc/PID/fd/{0,1,2}) to have an
    // absolute path of "device:1,3" instead of something like "/dev/null".
    // This affects also every other process that inherits the file descriptors
    // from SystemServer, so it is important for other things (also for ProcFS
    // tests that are running in CI mode).
    int stdin_new_fd = open("/dev/null", O_NONBLOCK);
    if (stdin_new_fd < 0) {
        VERIFY_NOT_REACHED();
    }
    rc = dup2(stdin_new_fd, 0);
    if (rc < 0) {
        VERIFY_NOT_REACHED();
    }

    rc = dup2(stdin_new_fd, 1);
    if (rc < 0) {
        VERIFY_NOT_REACHED();
    }
    rc = dup2(stdin_new_fd, 2);
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

int main(int argc, char** argv)
{
    bool user = false;
    Core::ArgsParser args_parser;
    args_parser.add_option(user, "Run in user-mode", "user", 'u');
    args_parser.parse(argc, argv);

    if (!user) {
        mount_all_filesystems();
        prepare_synthetic_filesystems();
    }

    if (pledge("stdio proc exec tty accept unix rpath wpath cpath chown fattr id sigaction", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (!user) {
        create_tmp_coredump_directory();
        determine_system_mode();
    }

    Core::EventLoop event_loop;

    event_loop.register_signal(SIGCHLD, sigchld_handler);

    // Read our config and instantiate services.
    // This takes care of setting up sockets.
    NonnullRefPtrVector<Service> services;
    auto config = (user)
        ? Core::ConfigFile::open_for_app("SystemServer")
        : Core::ConfigFile::open_for_system("SystemServer");
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

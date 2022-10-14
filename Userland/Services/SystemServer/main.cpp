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
#include <Kernel/API/DeviceEvent.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/DirIterator.h>
#include <LibCore/Event.h>
#include <LibCore/EventLoop.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <errno.h>
#include <fcntl.h>
#include <grp.h>
#include <signal.h>
#include <stdio.h>
#include <sys/stat.h>
#include <sys/sysmacros.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>

String g_system_mode = "graphical";
NonnullRefPtrVector<Service> g_services;

// NOTE: This handler ensures that the destructor of g_services is called.
static void sigterm_handler(int)
{
    exit(0);
}

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

static ErrorOr<void> determine_system_mode()
{
    ArmedScopeGuard declare_text_mode_on_failure([&] {
        // Note: Only if the mode is not set to self-test, degrade it to text mode.
        if (g_system_mode != "self-test")
            g_system_mode = "text";
    });

    auto f = Core::File::construct("/proc/system_mode");
    if (!f->open(Core::OpenMode::ReadOnly)) {
        dbgln("Failed to read system_mode: {}", f->error_string());
        // Continue and assume "text" mode.
        return {};
    }
    const String system_mode = String::copy(f->read_all(), Chomp);
    if (f->error()) {
        dbgln("Failed to read system_mode: {}", f->error_string());
        // Continue and assume "text" mode.
        return {};
    }

    g_system_mode = system_mode;
    declare_text_mode_on_failure.disarm();

    dbgln("Read system_mode: {}", g_system_mode);

    struct stat file_state;
    int rc = lstat("/dev/gpu/connector0", &file_state);
    if (rc != 0 && g_system_mode == "graphical") {
        dbgln("WARNING: No device nodes at /dev/gpu/ directory. This is probably a sign of disabled graphics functionality.");
        dbgln("To cope with this, I'll turn off graphical mode.");
        g_system_mode = "text";
    }
    return {};
}

static ErrorOr<void> chown_all_matching_device_nodes_under_specific_directory(StringView directory, group const& group)
{
    struct stat cur_file_stat;

    Core::DirIterator di(directory, Core::DirIterator::SkipParentAndBaseDir);
    if (di.has_error())
        VERIFY_NOT_REACHED();
    while (di.has_next()) {
        auto entry_name = di.next_full_path();
        auto rc = stat(entry_name.characters(), &cur_file_stat);
        if (rc < 0)
            continue;
        TRY(Core::System::chown(entry_name, 0, group.gr_gid));
    }
    return {};
}

static ErrorOr<void> chown_all_matching_device_nodes(group const& group, unsigned major_number)
{
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
        TRY(Core::System::chown(entry_name, 0, group.gr_gid));
    }
    return {};
}

inline char offset_character_with_number(char base_char, u8 offset)
{
    char offsetted_char = base_char;
    VERIFY(static_cast<size_t>(offsetted_char) + static_cast<size_t>(offset) < 256);
    offsetted_char += offset;
    return offsetted_char;
}

static void create_devtmpfs_block_device(String name, mode_t mode, unsigned major, unsigned minor)
{
    if (auto rc = mknod(name.characters(), mode | S_IFBLK, makedev(major, minor)); rc < 0)
        VERIFY_NOT_REACHED();
}

static void create_devtmpfs_char_device(String name, mode_t mode, unsigned major, unsigned minor)
{
    if (auto rc = mknod(name.characters(), mode | S_IFCHR, makedev(major, minor)); rc < 0)
        VERIFY_NOT_REACHED();
}

static void populate_devtmpfs_char_devices_based_on_sysfs()
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
        case 2: {
            switch (minor_number) {
            case 10: {
                create_devtmpfs_char_device("/dev/devctl", 0660, 2, 10);
                break;
            }
            default:
                warnln("Unknown character device {}:{}", major_number, minor_number);
            }
            break;
        }

        default:
            break;
        }
    }
}

static void populate_devtmpfs_devices_based_on_devctl()
{
    auto f = Core::File::construct("/dev/devctl");
    if (!f->open(Core::OpenMode::ReadOnly)) {
        warnln("Failed to open /dev/devctl - {}", f->error_string());
        VERIFY_NOT_REACHED();
    }

    DeviceEvent event;
    while (f->read((u8*)&event, sizeof(DeviceEvent)) > 0) {
        if (event.state != DeviceEvent::Inserted)
            continue;
        auto major_number = event.major_number;
        auto minor_number = event.minor_number;
        bool is_block_device = (event.is_block_device == 1);
        switch (major_number) {
        case 116: {
            if (!is_block_device) {
                create_devtmpfs_char_device(String::formatted("/dev/audio/{}", minor_number), 0220, 116, minor_number);
                break;
            }
            break;
        }
        case 28: {
            create_devtmpfs_block_device(String::formatted("/dev/gpu/render{}", minor_number), 0666, 28, minor_number);
            break;
        }
        case 226: {
            create_devtmpfs_char_device(String::formatted("/dev/gpu/connector{}", minor_number), 0666, 226, minor_number);
            break;
        }
        case 229: {
            if (!is_block_device) {
                create_devtmpfs_char_device(String::formatted("/dev/hvc0p{}", minor_number), 0666, major_number, minor_number);
            }
            break;
        }
        case 10: {
            if (!is_block_device) {
                switch (minor_number) {
                case 0: {
                    create_devtmpfs_char_device("/dev/input/mouse/0", 0660, 10, 0);
                    break;
                }
                case 183: {
                    create_devtmpfs_char_device("/dev/hwrng", 0660, 10, 183);
                    break;
                }
                default:
                    warnln("Unknown character device {}:{}", major_number, minor_number);
                }
            }
            break;
        }
        case 85: {
            if (!is_block_device) {
                switch (minor_number) {
                case 0: {
                    create_devtmpfs_char_device("/dev/input/keyboard/0", 0660, 85, 0);
                    break;
                }
                default:
                    warnln("Unknown character device {}:{}", major_number, minor_number);
                }
            }
            break;
        }
        case 1: {
            if (!is_block_device) {
                switch (minor_number) {
                case 5: {
                    create_devtmpfs_char_device("/dev/zero", 0666, 1, 5);
                    break;
                }
                case 1: {
                    create_devtmpfs_char_device("/dev/mem", 0660, 1, 1);
                    break;
                }
                case 3: {
                    create_devtmpfs_char_device("/dev/null", 0666, 1, 3);
                    break;
                }
                case 7: {
                    create_devtmpfs_char_device("/dev/full", 0666, 1, 7);
                    break;
                }
                case 8: {
                    create_devtmpfs_char_device("/dev/random", 0666, 1, 8);
                    break;
                }
                default:
                    warnln("Unknown character device {}:{}", major_number, minor_number);
                    break;
                }
            }
            break;
        }
        case 30: {
            if (!is_block_device) {
                create_devtmpfs_char_device(String::formatted("/dev/kcov{}", minor_number), 0666, 30, minor_number);
            }
            break;
        }
        case 3: {
            if (is_block_device) {
                create_devtmpfs_block_device(String::formatted("/dev/hd{}", offset_character_with_number('a', minor_number)), 0600, 3, minor_number);
            }
            break;
        }
        case 5: {
            if (!is_block_device) {
                switch (minor_number) {
                case 1: {
                    create_devtmpfs_char_device("/dev/console", 0666, 5, 1);
                    break;
                }
                case 2: {
                    create_devtmpfs_char_device("/dev/ptmx", 0666, 5, 2);
                    break;
                }
                case 0: {
                    create_devtmpfs_char_device("/dev/tty", 0666, 5, 0);
                    break;
                }
                default:
                    warnln("Unknown character device {}:{}", major_number, minor_number);
                }
            }
            break;
        }
        case 4: {
            if (!is_block_device) {
                switch (minor_number) {
                case 0: {
                    create_devtmpfs_char_device("/dev/tty0", 0620, 4, 0);
                    break;
                }
                case 1: {
                    create_devtmpfs_char_device("/dev/tty1", 0620, 4, 1);
                    break;
                }
                case 2: {
                    create_devtmpfs_char_device("/dev/tty2", 0620, 4, 2);
                    break;
                }
                case 3: {
                    create_devtmpfs_char_device("/dev/tty3", 0620, 4, 3);
                    break;
                }
                case 64: {
                    create_devtmpfs_char_device("/dev/ttyS0", 0620, 4, 64);
                    break;
                }
                case 65: {
                    create_devtmpfs_char_device("/dev/ttyS1", 0620, 4, 65);
                    break;
                }
                case 66: {
                    create_devtmpfs_char_device("/dev/ttyS2", 0620, 4, 66);
                    break;
                }
                case 67: {
                    create_devtmpfs_char_device("/dev/ttyS3", 0666, 4, 67);
                    break;
                }
                default:
                    warnln("Unknown character device {}:{}", major_number, minor_number);
                }
            }
            break;
        }
        default:
            if (!is_block_device)
                warnln("Unknown character device {}:{}", major_number, minor_number);
            else
                warnln("Unknown block device {}:{}", major_number, minor_number);
            break;
        }
    }
}

static void populate_devtmpfs()
{
    mode_t old_mask = umask(0);
    printf("Changing umask %#o\n", old_mask);
    populate_devtmpfs_char_devices_based_on_sysfs();
    populate_devtmpfs_devices_based_on_devctl();
    umask(old_mask);
}

static ErrorOr<void> prepare_synthetic_filesystems()
{
    // FIXME: Find a better way to all of this stuff, without hardcoding all of this!
    TRY(Core::System::mount(-1, "/proc"sv, "proc"sv, MS_NOSUID));
    TRY(Core::System::mount(-1, "/sys"sv, "sys"sv, 0));
    TRY(Core::System::mount(-1, "/dev"sv, "dev"sv, 0));

    TRY(Core::System::mkdir("/dev/audio"sv, 0755));
    TRY(Core::System::mkdir("/dev/input"sv, 0755));
    TRY(Core::System::mkdir("/dev/input/keyboard"sv, 0755));
    TRY(Core::System::mkdir("/dev/input/mouse"sv, 0755));

    TRY(Core::System::symlink("/proc/self/fd/0"sv, "/dev/stdin"sv));
    TRY(Core::System::symlink("/proc/self/fd/1"sv, "/dev/stdout"sv));
    TRY(Core::System::symlink("/proc/self/fd/2"sv, "/dev/stderr"sv));

    TRY(Core::System::mkdir("/dev/gpu"sv, 0755));

    populate_devtmpfs();

    TRY(Core::System::mkdir("/dev/pts"sv, 0755));

    TRY(Core::System::mount(-1, "/dev/pts"sv, "devpts"sv, 0));

    TRY(Core::System::symlink("/dev/random"sv, "/dev/urandom"sv));

    TRY(Core::System::chmod("/dev/urandom"sv, 0666));

    auto phys_group = TRY(Core::System::getgrnam("phys"sv));
    VERIFY(phys_group.has_value());
    // FIXME: Try to find a way to not hardcode the major number of display connector device nodes.
    TRY(chown_all_matching_device_nodes(phys_group.value(), 29));

    auto const filter_chown_ENOENT = [](ErrorOr<void> result) -> ErrorOr<void> {
        auto const chown_enoent = Error::from_syscall("chown"sv, -ENOENT);
        if (result.is_error() && result.error() == chown_enoent) {
            dbgln("{}", result.release_error());
            return {};
        }
        return result;
    };

    TRY(filter_chown_ENOENT(Core::System::chown("/dev/input/keyboard/0"sv, 0, phys_group.value().gr_gid)));
    TRY(filter_chown_ENOENT(Core::System::chown("/dev/input/mouse/0"sv, 0, phys_group.value().gr_gid)));

    auto tty_group = TRY(Core::System::getgrnam("tty"sv));
    VERIFY(tty_group.has_value());
    // FIXME: Try to find a way to not hardcode the major number of tty nodes.
    TRY(chown_all_matching_device_nodes(tty_group.release_value(), 4));

    auto audio_group = TRY(Core::System::getgrnam("audio"sv));
    VERIFY(audio_group.has_value());
    TRY(Core::System::chown("/dev/audio"sv, 0, audio_group->gr_gid));
    TRY(chown_all_matching_device_nodes_under_specific_directory("/dev/audio"sv, audio_group.release_value()));

    // Note: We open the /dev/null device and set file descriptors 0, 1, 2 to it
    // because otherwise these file descriptors won't have a custody, making
    // the ProcFS file descriptor links (at /proc/PID/fd/{0,1,2}) to have an
    // absolute path of "device:1,3" instead of something like "/dev/null".
    // This affects also every other process that inherits the file descriptors
    // from SystemServer, so it is important for other things (also for ProcFS
    // tests that are running in CI mode).
    int stdin_new_fd = TRY(Core::System::open("/dev/null"sv, O_NONBLOCK));

    TRY(Core::System::dup2(stdin_new_fd, 0));
    TRY(Core::System::dup2(stdin_new_fd, 1));
    TRY(Core::System::dup2(stdin_new_fd, 2));

    TRY(Core::System::endgrent());
    return {};
}

static ErrorOr<void> mount_all_filesystems()
{
    dbgln("Spawning mount -a to mount all filesystems.");
    pid_t pid = TRY(Core::System::fork());

    if (pid == 0)
        TRY(Core::System::exec("/bin/mount"sv, Vector { "mount"sv, "-a"sv }, Core::System::SearchInPath::No));

    wait(nullptr);
    return {};
}

static ErrorOr<void> create_tmp_coredump_directory()
{
    dbgln("Creating /tmp/coredump directory");
    auto old_umask = umask(0);
    // FIXME: the coredump directory should be made read-only once CrashDaemon is no longer responsible for compressing coredumps
    TRY(Core::System::mkdir("/tmp/coredump"sv, 0777));
    umask(old_umask);
    return {};
}

static ErrorOr<void> create_tmp_semaphore_directory()
{
    dbgln("Creating /tmp/semaphore directory");
    auto old_umask = umask(0);
    TRY(Core::System::mkdir("/tmp/semaphore"sv, 0777));
    umask(old_umask);
    return {};
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    bool user = false;
    Core::ArgsParser args_parser;
    args_parser.add_option(user, "Run in user-mode", "user", 'u');
    args_parser.parse(arguments);

    if (!user) {
        TRY(mount_all_filesystems());
        TRY(prepare_synthetic_filesystems());
    }

    TRY(Core::System::pledge("stdio proc exec tty accept unix rpath wpath cpath chown fattr id sigaction"));

    if (!user) {
        TRY(create_tmp_coredump_directory());
        TRY(create_tmp_semaphore_directory());
        TRY(determine_system_mode());
    }

    Core::EventLoop event_loop;

    event_loop.register_signal(SIGCHLD, sigchld_handler);
    event_loop.register_signal(SIGTERM, sigterm_handler);

    // Read our config and instantiate services.
    // This takes care of setting up sockets.
    auto config = (user)
        ? TRY(Core::ConfigFile::open_for_app("SystemServer"))
        : TRY(Core::ConfigFile::open_for_system("SystemServer"));
    for (auto const& name : config->groups()) {
        auto service = TRY(Service::try_create(*config, name));
        if (service->is_enabled())
            g_services.append(move(service));
    }

    // After we've set them all up, activate them!
    dbgln("Activating {} services...", g_services.size());
    for (auto& service : g_services)
        service.activate();

    return event_loop.exec();
}

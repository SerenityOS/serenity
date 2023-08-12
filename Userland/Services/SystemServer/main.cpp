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
#include <AK/String.h>
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

DeprecatedString g_system_mode = "graphical";
Vector<NonnullRefPtr<Service>> g_services;

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

        if (auto result = service->did_exit(status); result.is_error())
            dbgln("{}: {}", service->name(), result.release_error());
    }
}

static ErrorOr<void> determine_system_mode()
{
    ArmedScopeGuard declare_text_mode_on_failure([&] {
        // Note: Only if the mode is not set to self-test, degrade it to text mode.
        if (g_system_mode != "self-test")
            g_system_mode = "text";
    });

    auto file_or_error = Core::File::open("/sys/kernel/system_mode"sv, Core::File::OpenMode::Read);
    if (file_or_error.is_error()) {
        dbgln("Failed to read system_mode: {}", file_or_error.error());
        // Continue and assume "text" mode.
        return {};
    }
    auto const system_mode_buf_or_error = file_or_error.value()->read_until_eof();
    if (system_mode_buf_or_error.is_error()) {
        dbgln("Failed to read system_mode: {}", system_mode_buf_or_error.error());
        // Continue and assume "text" mode.
        return {};
    }
    DeprecatedString const system_mode = DeprecatedString::copy(system_mode_buf_or_error.value(), Chomp);

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

static ErrorOr<void> create_devtmpfs_block_device(StringView name, mode_t mode, unsigned major, unsigned minor)
{
    return Core::System::mknod(name, mode | S_IFBLK, makedev(major, minor));
}

static ErrorOr<void> create_devtmpfs_char_device(StringView name, mode_t mode, unsigned major, unsigned minor)
{
    return Core::System::mknod(name, mode | S_IFCHR, makedev(major, minor));
}

static ErrorOr<void> populate_devtmpfs_char_devices_based_on_sysfs()
{
    Core::DirIterator di("/sys/dev/char/", Core::DirIterator::SkipParentAndBaseDir);
    if (di.has_error()) {
        auto error = di.error();
        warnln("Failed to open /sys/dev/char - {}", error);
        return error;
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
                TRY(create_devtmpfs_char_device("/dev/devctl"sv, 0660, 2, 10));
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
    return {};
}

static ErrorOr<bool> read_one_or_eof(NonnullOwnPtr<Core::File>& file, DeviceEvent& event)
{
    auto const read_buf = TRY(file->read_some({ (u8*)&event, sizeof(DeviceEvent) }));
    if (read_buf.size() == sizeof(DeviceEvent)) {
        // Good! We could read another DeviceEvent.
        return true;
    }
    if (file->is_eof()) {
        // Good! We have reached the "natural" end of the file.
        return false;
    }
    // Bad! Kernel and SystemServer apparently disagree on the record size,
    // which means that previous data is likely to be invalid.
    return Error::from_string_view("File ended after incomplete record? /dev/devctl seems broken!"sv);
}

static ErrorOr<void> populate_devtmpfs_devices_based_on_devctl()
{
    auto file_or_error = Core::File::open("/dev/devctl"sv, Core::File::OpenMode::Read);
    if (file_or_error.is_error()) {
        warnln("Failed to open /dev/devctl - {}", file_or_error.error());
        VERIFY_NOT_REACHED();
    }
    auto file = file_or_error.release_value();

    DeviceEvent event;
    while (TRY(read_one_or_eof(file, event))) {
        if (event.state != DeviceEvent::Inserted)
            continue;
        auto major_number = event.major_number;
        auto minor_number = event.minor_number;
        bool is_block_device = (event.is_block_device == 1);
        switch (major_number) {
        case 116: {
            if (!is_block_device) {
                auto name = TRY(String::formatted("/dev/audio/{}", minor_number));
                TRY(create_devtmpfs_char_device(name.bytes_as_string_view(), 0220, 116, minor_number));
                break;
            }
            break;
        }
        case 28: {
            auto name = TRY(String::formatted("/dev/gpu/render{}", minor_number));
            TRY(create_devtmpfs_block_device(name.bytes_as_string_view(), 0666, 28, minor_number));
            break;
        }
        case 226: {
            auto name = TRY(String::formatted("/dev/gpu/connector{}", minor_number));
            TRY(create_devtmpfs_char_device(name.bytes_as_string_view(), 0666, 226, minor_number));
            break;
        }
        case 229: {
            if (!is_block_device) {
                auto name = TRY(String::formatted("/dev/hvc0p{}", minor_number));
                TRY(create_devtmpfs_char_device(name.bytes_as_string_view(), 0666, 229, minor_number));
            }
            break;
        }
        case 10: {
            if (!is_block_device) {
                switch (minor_number) {
                case 0: {
                    TRY(create_devtmpfs_char_device("/dev/input/mouse/0"sv, 0666, 10, 0));
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
                    TRY(create_devtmpfs_char_device("/dev/input/keyboard/0"sv, 0666, 85, 0));
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
                    TRY(create_devtmpfs_char_device("/dev/zero"sv, 0666, 1, 5));
                    break;
                }
                case 1: {
                    TRY(create_devtmpfs_char_device("/dev/mem"sv, 0666, 1, 1));
                    break;
                }
                case 3: {
                    TRY(create_devtmpfs_char_device("/dev/null"sv, 0666, 1, 3));
                    break;
                }
                case 7: {
                    TRY(create_devtmpfs_char_device("/dev/full"sv, 0666, 1, 7));
                    break;
                }
                case 8: {
                    TRY(create_devtmpfs_char_device("/dev/random"sv, 0666, 1, 8));
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
                auto name = TRY(String::formatted("/dev/kcov{}", minor_number));
                TRY(create_devtmpfs_char_device(name.bytes_as_string_view(), 0666, 30, minor_number));
            }
            break;
        }
        case 3: {
            if (is_block_device) {
                auto name = TRY(String::formatted("/dev/hd{}", offset_character_with_number('a', minor_number)));
                TRY(create_devtmpfs_block_device(name.bytes_as_string_view(), 0600, 3, minor_number));
            }
            break;
        }
        case 5: {
            if (!is_block_device) {
                switch (minor_number) {
                case 1: {
                    TRY(create_devtmpfs_char_device("/dev/console"sv, 0666, 5, 1));
                    break;
                }
                case 2: {
                    TRY(create_devtmpfs_char_device("/dev/ptmx"sv, 0666, 5, 2));
                    break;
                }
                case 0: {
                    TRY(create_devtmpfs_char_device("/dev/tty"sv, 0666, 5, 0));
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
                    TRY(create_devtmpfs_char_device("/dev/tty0"sv, 0620, 4, 0));
                    break;
                }
                case 1: {
                    TRY(create_devtmpfs_char_device("/dev/tty1"sv, 0620, 4, 1));
                    break;
                }
                case 2: {
                    TRY(create_devtmpfs_char_device("/dev/tty2"sv, 0620, 4, 2));
                    break;
                }
                case 3: {
                    TRY(create_devtmpfs_char_device("/dev/tty3"sv, 0620, 4, 3));
                    break;
                }
                case 64: {
                    TRY(create_devtmpfs_char_device("/dev/ttyS0"sv, 0620, 4, 64));
                    break;
                }
                case 65: {
                    TRY(create_devtmpfs_char_device("/dev/ttyS1"sv, 0620, 4, 65));
                    break;
                }
                case 66: {
                    TRY(create_devtmpfs_char_device("/dev/ttyS2"sv, 0620, 4, 66));
                    break;
                }
                case 67: {
                    TRY(create_devtmpfs_char_device("/dev/ttyS3"sv, 0666, 4, 67));
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
    return {};
}

static ErrorOr<void> populate_devtmpfs()
{
    mode_t old_mask = umask(0);
    printf("Changing umask %#o\n", old_mask);
    TRY(populate_devtmpfs_char_devices_based_on_sysfs());
    TRY(populate_devtmpfs_devices_based_on_devctl());
    umask(old_mask);
    return {};
}

static ErrorOr<void> prepare_synthetic_filesystems()
{
    TRY(Core::System::remount("/"sv, MS_NODEV | MS_NOSUID | MS_RDONLY));
    // FIXME: Find a better way to all of this stuff, without hardcoding all of this!
    TRY(Core::System::mount(-1, "/proc"sv, "proc"sv, MS_NOSUID));
    TRY(Core::System::mount(-1, "/sys"sv, "sys"sv, 0));
    TRY(Core::System::mount(-1, "/dev"sv, "ram"sv, MS_NOSUID | MS_NOEXEC | MS_NOREGULAR));

    TRY(Core::System::mount(-1, "/tmp"sv, "ram"sv, MS_NOSUID | MS_NODEV));
    // NOTE: Set /tmp to have a sticky bit with 0777 permissions.
    TRY(Core::System::chmod("/tmp"sv, 01777));

    TRY(Core::System::mkdir("/dev/audio"sv, 0755));
    TRY(Core::System::mkdir("/dev/input"sv, 0755));
    TRY(Core::System::mkdir("/dev/input/keyboard"sv, 0755));
    TRY(Core::System::mkdir("/dev/input/mouse"sv, 0755));

    TRY(Core::System::symlink("/proc/self/fd/0"sv, "/dev/stdin"sv));
    TRY(Core::System::symlink("/proc/self/fd/1"sv, "/dev/stdout"sv));
    TRY(Core::System::symlink("/proc/self/fd/2"sv, "/dev/stderr"sv));

    TRY(Core::System::mkdir("/dev/gpu"sv, 0755));

    TRY(populate_devtmpfs());

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

static ErrorOr<void> set_default_coredump_directory()
{
    dbgln("Setting /tmp/coredump as the coredump directory");
    auto sysfs_coredump_directory_variable_fd = TRY(Core::System::open("/sys/kernel/conf/coredump_directory"sv, O_RDWR));
    ScopeGuard close_on_exit([&] {
        close(sysfs_coredump_directory_variable_fd);
    });
    auto tmp_coredump_directory_path = "/tmp/coredump"sv;
    auto nwritten = TRY(Core::System::write(sysfs_coredump_directory_variable_fd, tmp_coredump_directory_path.bytes()));
    VERIFY(static_cast<size_t>(nwritten) == tmp_coredump_directory_path.length());
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
        TRY(set_default_coredump_directory());
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
    for (auto& service : g_services) {
        if (auto result = service->activate(); result.is_error())
            dbgln("{}: {}", service->name(), result.release_error());
    }

    return event_loop.exec();
}

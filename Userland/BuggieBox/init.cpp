/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

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
#include <Userland/BuggieBox/init.h>
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

decltype(serenity_main) rm_main;

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
    }
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
                    create_devtmpfs_char_device("/dev/mouse0", 0660, 10, 0);
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
                    create_devtmpfs_char_device("/dev/keyboard0", 0660, 85, 0);
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
    TRY(Core::System::mount(-1, "/dev"sv, "tmp"sv, MS_NOSUID | MS_NOEXEC | MS_NOREGULAR));
    TRY(Core::System::mount(-1, "/tmp"sv, "tmp"sv, 0));

    TRY(Core::System::mkdir("/dev/audio"sv, 0755));
    TRY(Core::System::mkdir("/dev/input"sv, 0755));
    TRY(Core::System::mkdir("/dev/input/keyboard"sv, 0755));
    TRY(Core::System::mkdir("/dev/input/mouse"sv, 0755));

    TRY(Core::System::symlink("/proc/self/fd/0"sv, "/dev/stdin"sv));
    TRY(Core::System::symlink("/proc/self/fd/1"sv, "/dev/stdout"sv));
    TRY(Core::System::symlink("/proc/self/fd/2"sv, "/dev/stderr"sv));

    TRY(Core::System::mkdir("/dev/gpu"sv, 0755));

    populate_devtmpfs();

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

static ErrorOr<unsigned> open_node(StringView name)
{
    auto fd_or_error = Core::System::open(name, O_RDWR);
    if (fd_or_error.is_error())
        fd_or_error = Core::System::open(name, O_RDONLY);
    if (fd_or_error.is_error()) {
        return fd_or_error.release_error();
    }
    return (unsigned)fd_or_error.release_value();
}

static ErrorOr<void> prepare_init_filesystem_environment()
{
    // Note: We first mount the /tmp, /proc, /sys and /dev directories before any
    // meaningful init environment could be used (i.e. emergenecy shell).
    auto result = Core::System::stat("/lock"sv);
    if (!result.is_error())
        return {};
    TRY(prepare_synthetic_filesystems());
    TRY(create_tmp_coredump_directory());
    TRY(create_tmp_semaphore_directory());

    int lock_fd = TRY(Core::System::open("/lock"sv, O_CREAT | O_EXCL));
    close(lock_fd);
    return {};
}

static ErrorOr<int> open_sysfs_indicated_root_device()
{
    auto root_device_node_or_error = Core::File::open("/sys/kernel/root_device", Core::OpenMode::ReadOnly, 0);
    if (root_device_node_or_error.is_error()) {
        dbgln("Error: Could not open /sys/kernel/root_device");
        return root_device_node_or_error.release_error();
    }
    auto root_device_node = root_device_node_or_error.release_value();
    auto root_device = root_device_node->read_all();
    auto root_device_stringview = StringView { root_device };

    // Note: If the user didn't specify root= in the kernel commandline, then it should contain
    // a simple \n char.
    if (root_device_stringview != "\n"sv)
        return TRY(open_node(root_device_stringview.trim("\n"sv)));
    return Error::from_errno(ENOENT);
}

static ErrorOr<void> continue_boot_sequence(Optional<StringView> boot_device_name)
{
    // Note: Depending on the /sys/kernel/root_device value, we either continue to use
    // the Init RAM filesystem we are currently working within, or do the following:
    // 1. Copy the contents of the init TmpFS instance to somewhere safe.
    // 1. Fail safely if the chosen boot device is not usable.
    // 2. Clean the entire TmpFS instance mounted on the root mountpoint.
    // 3. Mount on top of / the block device
    // 4. Try to launch the /bin/init binary on the chosen root device being mounted on /.
    // 5. In case of fork+exec failure, we will do our best to revert everything back into
    //    the original init TmpFS instance. It mainly depends on whether the actual binary was loaded
    //    and replaced the BuggieBox binary - if that's the case, there's nothing we can do to revert
    //    safely to the old environment.

    int boot_device_fd = -1;

    if (boot_device_name.has_value()) {
        if (auto result_or_error = open_node(boot_device_name.value()); result_or_error.is_error()) {
            boot_device_fd = (int)TRY(open_sysfs_indicated_root_device());
        } else {
            boot_device_fd = result_or_error.value();
        }
    } else {
        boot_device_fd = TRY(open_sysfs_indicated_root_device());
    }

    VERIFY(boot_device_fd != -1);
    char const* argv[] = { "rm", "-r", "-f", "--no-preserve-root", "/", nullptr };

    auto remove_all_args = Main::Arguments {
        5,
        const_cast<char**>(argv),
        Vector<StringView> { "rm"sv, "-r"sv, "-f"sv, "--no-preserve-root"sv, "/"sv },
    };

    TRY(Core::System::umount("/sys"sv));
    TRY(Core::System::umount("/tmp"sv));
    TRY(Core::System::umount("/dev"sv));
    TRY(Core::System::umount("/proc"sv));

    // Note: From this point on, we cannot simply revert back to the original init tmpfs instance.
    TRY(rm_main(remove_all_args));

    TRY(Core::System::mkdir("/new_root/"sv, 0777));
    // FIXME: Can we not hardcode the filesystem type?
    auto fs_type = "ext2"sv;
    dbgln("Mounting fd {} ({}) on /new_root", boot_device_fd, fs_type);
    TRY(Core::System::mount(boot_device_fd, "/new_root"sv, fs_type, 0));
    close(boot_device_fd);

    int new_root_fd = TRY(Core::System::open("/new_root"sv, O_DIRECTORY | O_RDONLY));
    TRY(Core::System::pivot_root(new_root_fd));

    TRY(Core::System::exec("/bin/SystemServer"sv, Vector { "/bin/SystemServer"sv }, Core::System::SearchInPath::No));
    return {};
}

ErrorOr<int> buggiebox_init_main(Main::Arguments arguments)
{
    String boot_device_name;
    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(boot_device_name, "Boot device", "device", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    TRY(prepare_init_filesystem_environment());
    [[maybe_unused]] auto result = continue_boot_sequence(boot_device_name.is_null() ? Optional<StringView> {} : boot_device_name);

    Core::EventLoop event_loop;
    event_loop.register_signal(SIGCHLD, sigchld_handler);

    pid_t pid = TRY(Core::System::fork());
    if (pid == 0)
        TRY(Core::System::exec("/bin/BuggieBox"sv, Vector { "emergency_shell"sv }, Core::System::SearchInPath::No));

    event_loop.exec();
    VERIFY_NOT_REACHED();
}

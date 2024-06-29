/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Error.h>
#include <AK/ScopeGuard.h>
#include <AK/Types.h>
#include <LibCore/Process.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <unistd.h>

static ErrorOr<void> prepare_bare_minimum_filesystem_mounts()
{
    TRY(Core::System::remount("/"sv, MS_NODEV | MS_NOSUID | MS_RDONLY));
    TRY(Core::System::mount(-1, "/proc"sv, "proc"sv, MS_NOSUID));
    TRY(Core::System::mount(-1, "/sys"sv, "sys"sv, 0));
    TRY(Core::System::mount(-1, "/dev"sv, "ram"sv, MS_NOSUID | MS_NOEXEC | MS_NOREGULAR));
    TRY(Core::System::mount(-1, "/tmp"sv, "ram"sv, MS_NOSUID | MS_NODEV));
    // NOTE: Set /tmp to have a sticky bit with 0777 permissions.
    TRY(Core::System::chmod("/tmp"sv, 01777));
    return {};
}

static ErrorOr<void> spawn_device_mapper_process()
{
    TRY(Core::Process::spawn("/bin/DeviceMapper"sv, ReadonlySpan<StringView> {}, {}, Core::Process::KeepAsChild::No));
    return {};
}

static ErrorOr<void> prepare_bare_minimum_devtmpfs_directory_structure()
{
    TRY(Core::System::mkdir("/dev/audio"sv, 0755));
    TRY(Core::System::mkdir("/dev/input"sv, 0755));
    TRY(Core::System::mkdir("/dev/input/keyboard"sv, 0755));
    TRY(Core::System::mkdir("/dev/input/mouse"sv, 0755));
    TRY(Core::System::symlink("/proc/self/fd/0"sv, "/dev/stdin"sv));
    TRY(Core::System::symlink("/proc/self/fd/1"sv, "/dev/stdout"sv));
    TRY(Core::System::symlink("/proc/self/fd/2"sv, "/dev/stderr"sv));
    TRY(Core::System::mkdir("/dev/gpu"sv, 0755));
    TRY(Core::System::mkdir("/dev/pts"sv, 0755));
    TRY(Core::System::mount(-1, "/dev/pts"sv, "devpts"sv, 0));
    TRY(Core::System::mkdir("/dev/loop"sv, 0755));
    TRY(Core::System::mount(-1, "/dev/loop"sv, "devloop"sv, 0));

    mode_t old_mask = umask(0);
    TRY(Core::System::create_char_device("/dev/devctl"sv, 0660, 2, 10));
    TRY(Core::System::create_char_device("/dev/zero"sv, 0666, 1, 5));
    TRY(Core::System::create_char_device("/dev/mem"sv, 0600, 1, 1));
    TRY(Core::System::create_char_device("/dev/null"sv, 0666, 1, 3));
    TRY(Core::System::create_char_device("/dev/full"sv, 0666, 1, 7));
    TRY(Core::System::create_char_device("/dev/random"sv, 0666, 1, 8));
    TRY(Core::System::create_char_device("/dev/input/mice"sv, 0666, 12, 0));
    TRY(Core::System::create_char_device("/dev/console"sv, 0666, 5, 1));
    TRY(Core::System::create_char_device("/dev/ptmx"sv, 0666, 5, 2));
    TRY(Core::System::create_char_device("/dev/tty"sv, 0666, 5, 0));
    TRY(Core::System::create_char_device("/dev/fuse"sv, 0666, 10, 229));
#ifdef ENABLE_KERNEL_COVERAGE_COLLECTION
    TRY(Core::System::create_block_device("/dev/kcov"sv, 0666, 30, 0));
#endif
    umask(old_mask);
    TRY(Core::System::symlink("/dev/random"sv, "/dev/urandom"sv));
    TRY(Core::System::chmod("/dev/urandom"sv, 0666));
    return {};
}

static ErrorOr<void> prepare_synthetic_filesystems()
{
    TRY(prepare_bare_minimum_filesystem_mounts());
    TRY(prepare_bare_minimum_devtmpfs_directory_structure());
    TRY(spawn_device_mapper_process());
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

static ErrorOr<void> mount_all_filesystems()
{
    dbgln("Spawning mount -a to mount all filesystems.");
    pid_t pid = TRY(Core::System::fork());

    if (pid == 0)
        TRY(Core::System::exec("/bin/mount"sv, Vector { "mount"sv, "-a"sv }, Core::System::SearchInPath::No));

    auto result = TRY(Core::System::waitpid(-1, 0));
    if (result.status == 0)
        return {};
    return Error::from_errno(-result.status);
}

static ErrorOr<int> acquire_new_stdin_fd(bool emergency)
{
    if (!emergency)
        return TRY(Core::System::open("/dev/null"sv, O_NONBLOCK));
    TRY(Core::System::create_char_device("/dev/tty_emergency"sv, 0660, 4, 0));
    return TRY(Core::System::open("/dev/tty_emergency"sv, O_RDWR));
}

static ErrorOr<void> reopen_base_file_descriptors(bool emergency)
{
    // NOTE: We open the /dev/null (or another) device and set file descriptors 0, 1, 2 to it
    // because otherwise these file descriptors won't have a custody, making
    // the ProcFS file descriptor links (at /proc/PID/fd/{0,1,2}) to have an
    // absolute path of "device:1,3" instead of something like "/dev/null".
    // This affects also every other process that inherits the file descriptors
    // from SystemServer, so it is important for other things (also for ProcFS
    // tests that are running in CI mode).
    int stdin_new_fd = TRY(acquire_new_stdin_fd(emergency));
    TRY(Core::System::dup2(stdin_new_fd, 0));
    TRY(Core::System::dup2(stdin_new_fd, 1));
    TRY(Core::System::dup2(stdin_new_fd, 2));
    return {};
}

static ErrorOr<void> execute_emergency_shell()
{
    outln("Emergency mode: Dropping to emergency shell mode");
    outln("You may use this shell as rescue environment now.");
    if (!Core::System::access("/bin/Shell"sv, X_OK, 0).is_error())
        TRY(Core::System::exec("/bin/Shell"sv, Vector { "/bin/Shell"sv }, Core::System::SearchInPath::No));
    if (!Core::System::access("/bin/BuggieBox"sv, X_OK, 0).is_error())
        TRY(Core::System::exec("/bin/BuggieBox"sv, Vector { "/bin/BuggieBox"sv, "/bin/Shell"sv }, Core::System::SearchInPath::No));
    outln("Failed to find a program to be used as rescue environment. Halting.");
    VERIFY_NOT_REACHED();
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    if (geteuid() != 0) {
        warnln("Not running as root :^(");
        return 1;
    }

    bool emergency = false;
    // NOTE: What determins this flag normally is the user running the OS
    // with a kernel commandline including "init_args=emergency".
    if (arguments.strings.size() > 1 && arguments.strings[1] == "emergency"sv)
        emergency = true;

    // NOTE: The reason we check for emergency state is because we should avoid trying to mount
    // anything if the user requested to use "emergency mode".
    if (!emergency) {
        // If we are not in emergency state, try to mount filesystems according
        // to the /etc/fstab file. If it fails, declare emergency state and drop to shell.
        if (auto result = mount_all_filesystems(); result.is_error())
            emergency = true;
    }

    // If we are not in emergency state, and the /bin/SystemServer program is not accessible
    // (or can't be run due to bad permissions) then declare emergency state and drop to shell.
    // The reason we check for emergency state is because we should avoid useless syscalls at this stage.
    if (!emergency && Core::System::access("/bin/SystemServer"sv, X_OK, 0).is_error())
        emergency = true;

    TRY(prepare_synthetic_filesystems());

    TRY(reopen_base_file_descriptors(emergency));

    TRY(create_tmp_coredump_directory());
    TRY(set_default_coredump_directory());
    TRY(create_tmp_semaphore_directory());

    if (!emergency)
        TRY(Core::System::exec("/bin/SystemServer"sv, Vector { "/bin/SystemServer"sv }, Core::System::SearchInPath::No));
    else {
        TRY(execute_emergency_shell());
    }
    VERIFY_NOT_REACHED();
}

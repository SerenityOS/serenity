/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Peter Elliott <pelliott@serenityos.org>
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
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
#include <LibCore/Process.h>
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

static constexpr StringView text_system_mode = "text"sv;
static constexpr StringView selftest_system_mode = "self-test"sv;
static constexpr StringView graphical_system_mode = "graphical"sv;
DeprecatedString g_system_mode = graphical_system_mode;
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

namespace SystemServer {

static ErrorOr<void> determine_system_mode()
{
    ArmedScopeGuard declare_text_mode_on_failure([&] {
        // Note: Only if the mode is not set to self-test, degrade it to text mode.
        if (g_system_mode != selftest_system_mode)
            g_system_mode = text_system_mode;
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
    return {};
}

static ErrorOr<void> prepare_bare_minimum_filesystem_mounts()
{
    TRY(Core::System::remount("/"sv, MS_NODEV | MS_NOSUID | MS_RDONLY));
    // FIXME: Find a better way to all of this stuff, without hardcoding all of this!
    TRY(Core::System::mount(-1, "/proc"sv, "proc"sv, MS_NOSUID));
    TRY(Core::System::mount(-1, "/sys"sv, "sys"sv, 0));
    TRY(Core::System::mount(-1, "/dev"sv, "ram"sv, MS_NOSUID | MS_NOEXEC | MS_NOREGULAR));
    TRY(Core::System::mount(-1, "/tmp"sv, "ram"sv, MS_NOSUID | MS_NODEV));
    // NOTE: Set /tmp to have a sticky bit with 0777 permissions.
    TRY(Core::System::chmod("/tmp"sv, 01777));
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

    mode_t old_mask = umask(0);
    TRY(Core::System::create_char_device("/dev/devctl"sv, 0660, 2, 10));
    TRY(Core::System::create_char_device("/dev/zero"sv, 0666, 1, 5));
    TRY(Core::System::create_char_device("/dev/mem"sv, 0600, 1, 1));
    TRY(Core::System::create_char_device("/dev/null"sv, 0666, 1, 3));
    TRY(Core::System::create_char_device("/dev/full"sv, 0666, 1, 7));
    TRY(Core::System::create_char_device("/dev/random"sv, 0666, 1, 8));
    TRY(Core::System::create_char_device("/dev/console"sv, 0666, 5, 1));
    TRY(Core::System::create_char_device("/dev/ptmx"sv, 0666, 5, 2));
    TRY(Core::System::create_char_device("/dev/tty"sv, 0666, 5, 0));
    umask(old_mask);
    TRY(Core::System::symlink("/dev/random"sv, "/dev/urandom"sv));
    TRY(Core::System::chmod("/dev/urandom"sv, 0666));
    return {};
}

static ErrorOr<void> spawn_device_mapper_process()
{
    TRY(Core::Process::spawn("/bin/DeviceMapper"sv, ReadonlySpan<StringView> {}, {}, Core::Process::KeepAsChild::No));
    return {};
}

static ErrorOr<void> prepare_synthetic_filesystems()
{
    TRY(prepare_bare_minimum_filesystem_mounts());
    TRY(prepare_bare_minimum_devtmpfs_directory_structure());
    TRY(spawn_device_mapper_process());
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

static ErrorOr<void> activate_services(Core::ConfigFile const& config)
{
    for (auto const& name : config.groups()) {
        auto service = TRY(Service::try_create(config, name));
        if (service->is_enabled_for_system_mode(g_system_mode)) {
            TRY(service->setup_sockets());
            g_services.append(move(service));
        }
    }
    // After we've set them all up, activate them!
    dbgln("Activating {} services...", g_services.size());
    for (auto& service : g_services) {
        dbgln_if(SYSTEMSERVER_DEBUG, "Activating {}", service->name());
        if (auto result = service->activate(); result.is_error())
            dbgln("{}: {}", service->name(), result.release_error());
    }

    return {};
}

static ErrorOr<void> reopen_base_file_descriptors()
{
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
    return {};
}

static ErrorOr<void> activate_base_services_based_on_system_mode()
{
    if (g_system_mode == graphical_system_mode) {
        bool found_gpu_device = false;
        for (int attempt = 0; attempt < 10; attempt++) {
            struct stat file_state;
            int rc = lstat("/dev/gpu/connector0", &file_state);
            if (rc == 0) {
                found_gpu_device = true;
                break;
            }
            sleep(1);
        }
        if (!found_gpu_device) {
            dbgln("WARNING: No device nodes at /dev/gpu/ directory after 10 seconds. This is probably a sign of disabled graphics functionality.");
            dbgln("To cope with this, graphical mode will not be enabled.");
            g_system_mode = text_system_mode;
        }
    }

    // Read our config and instantiate services.
    // This takes care of setting up sockets.
    auto config = TRY(Core::ConfigFile::open_for_system("SystemServer"));
    TRY(activate_services(*config));
    return {};
}

static ErrorOr<void> activate_user_services_based_on_system_mode()
{
    // Read our config and instantiate services.
    // This takes care of setting up sockets.
    auto config = TRY(Core::ConfigFile::open_for_app("SystemServer"));
    TRY(activate_services(*config));
    return {};
}

};

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    bool user = false;
    Core::ArgsParser args_parser;
    args_parser.add_option(user, "Run in user-mode", "user", 'u');
    args_parser.parse(arguments);

    if (!user) {
        TRY(SystemServer::mount_all_filesystems());
        TRY(SystemServer::prepare_synthetic_filesystems());
        TRY(SystemServer::reopen_base_file_descriptors());
    }

    TRY(Core::System::pledge("stdio proc exec tty accept unix rpath wpath cpath chown fattr id sigaction"));

    if (!user) {
        TRY(SystemServer::create_tmp_coredump_directory());
        TRY(SystemServer::set_default_coredump_directory());
        TRY(SystemServer::create_tmp_semaphore_directory());
        TRY(SystemServer::determine_system_mode());
    }

    Core::EventLoop event_loop;

    event_loop.register_signal(SIGCHLD, sigchld_handler);
    event_loop.register_signal(SIGTERM, sigterm_handler);

    if (!user) {
        TRY(SystemServer::activate_base_services_based_on_system_mode());
    } else {
        TRY(SystemServer::activate_user_services_based_on_system_mode());
    }

    return event_loop.exec();
}

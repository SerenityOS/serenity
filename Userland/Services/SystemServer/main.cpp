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
ByteString g_system_mode = graphical_system_mode;
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
    ByteString const system_mode = ByteString::copy(system_mode_buf_or_error.value(), Chomp);

    g_system_mode = system_mode;
    declare_text_mode_on_failure.disarm();

    dbgln("Read system_mode: {}", g_system_mode);
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

    TRY(Core::System::pledge("stdio proc exec tty accept unix rpath wpath cpath chown fattr id sigaction"));

    if (!user)
        TRY(SystemServer::determine_system_mode());

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

/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/RefPtr.h>
#include <LibCore/Account.h>
#include <LibCore/ElapsedTimer.h>
#include <LibCore/EventReceiver.h>
#include <LibCore/Notifier.h>

class Service final : public Core::EventReceiver {
    C_OBJECT_ABSTRACT(Service)

public:
    static ErrorOr<NonnullRefPtr<Service>> try_create(Core::ConfigFile const& config, StringView name);
    ~Service();

    bool is_enabled_for_system_mode(StringView) const;
    ErrorOr<void> activate();
    // Note: This is a `status` as in POSIX's wait syscall, not an exit-code.
    ErrorOr<void> did_exit(int status);

    ErrorOr<void> setup_sockets();

    static Service* find_by_pid(pid_t);

private:
    Service(Core::ConfigFile const&, StringView name);

    ErrorOr<void> spawn(int socket_fd = -1);

    ErrorOr<void> determine_account(int fd);

    ErrorOr<void> change_privileges();

    /// SocketDescriptor describes the details of a single socket that was
    /// requested by a service.
    struct SocketDescriptor {
        /// The path of the socket.
        ByteString path;
        /// File descriptor of the socket. -1 if the socket hasn't been opened.
        int fd { -1 };
        /// File permissions of the socket.
        mode_t permissions;
    };

    // Path to the executable. By default this is /bin/{m_name}.
    ByteString m_executable_path;
    // Extra arguments, starting from argv[1], to pass when exec'ing.
    ByteString m_extra_arguments;
    // File path to open as stdio fds.
    Optional<ByteString> m_stdio_file_path;
    int m_priority { 1 };
    // Whether we should re-launch it if it exits.
    bool m_keep_alive { false };
    // Whether we should accept connections on the socket and pass the accepted
    // (and not listening) socket to the service. This requires a multi-instance
    // service.
    bool m_accept_socket_connections { false };
    // Whether we should only spawn this service once somebody connects to the socket.
    bool m_lazy;
    // The name of the user we should run this service as.
    Optional<ByteString> m_user;
    // The working directory in which to spawn the service.
    Optional<ByteString> m_working_directory;
    // System modes in which to run this service. By default, this is the graphical mode.
    Vector<ByteString> m_system_modes;
    // Whether several instances of this service can run at once.
    bool m_multi_instance { false };
    // Environment variables to pass to the service.
    ByteString m_environment;
    // Socket descriptors for this service.
    Vector<SocketDescriptor> m_sockets;

    // The resolved user account to run this service as.
    Optional<Core::Account> m_account;
    bool m_must_login { false };

    // For single-instance services, PID of the running instance of this service.
    pid_t m_pid { -1 };
    RefPtr<Core::Notifier> m_socket_notifier;

    // Timer since we last spawned the service.
    Core::ElapsedTimer m_run_timer;
    // How many times we have tried to restart this service, only counting those
    // times where it has exited unsuccessfully and too quickly.
    int m_restart_attempts { 0 };

    ErrorOr<void> setup_socket(SocketDescriptor&);
    void setup_notifier();
    ErrorOr<void> handle_socket_connection();
};

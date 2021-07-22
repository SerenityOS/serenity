/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <AK/String.h>
#include <LibCore/Account.h>
#include <LibCore/ElapsedTimer.h>
#include <LibCore/Notifier.h>
#include <LibCore/Object.h>

class Service;

class ServiceManager {
public:
    virtual ~ServiceManager() = default;

    virtual bool is_running(StringView const&) = 0;
    virtual bool can_activate(Service&) = 0;
    virtual void did_spawn_service(Service&) = 0;
};

class Service final : public Core::Object {
    C_OBJECT(Service)

public:
    bool is_enabled() const;
    bool is_running() const { return m_pid >= 0; }
    bool activate();
    void delayed_activate(int);
    void did_exit(int exit_code);

    static Service* find_by_pid(pid_t);

    // FIXME: Port to Core::Property
    void save_to(JsonObject&);

    int after_delay() const { return m_after_delay; }
    auto const& after_services() const { return m_after_services; }

private:
    Service(ServiceManager&, const Core::ConfigFile&, StringView name);

    void spawn(int socket_fd = -1);

    /// SocketDescriptor describes the details of a single socket that was
    /// requested by a service.
    struct SocketDescriptor {
        /// The path of the socket.
        String path;
        /// File descriptor of the socket. -1 if the socket hasn't been opened.
        int fd { -1 };
        /// File permissions of the socket.
        mode_t permissions;
    };

    ServiceManager& m_manager;

    // Path to the executable. By default this is /bin/{m_name}.
    String m_executable_path;
    // Extra arguments, starting from argv[1], to pass when exec'ing.
    Vector<String> m_extra_arguments;
    // Other services that need to be running before this service can be activated
    Vector<String> m_after_services;
    int m_after_delay { 0 };
    // File path to open as stdio fds.
    String m_stdio_file_path;
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
    String m_user;
    // The working directory in which to spawn the service.
    String m_working_directory;
    // System modes in which to run this service. By default, this is the graphical mode.
    Vector<String> m_system_modes;
    // Whether several instances of this service can run at once.
    bool m_multi_instance { false };
    // Environment variables to pass to the service.
    Vector<String> m_environment;
    // Socket descriptors for this service.
    Vector<SocketDescriptor> m_sockets;

    // The resolved user account to run this service as.
    Optional<Core::Account> m_account;

    // For single-instance services, PID of the running instance of this service.
    pid_t m_pid { -1 };
    RefPtr<Core::Notifier> m_socket_notifier;

    RefPtr<Core::Timer> m_delayed_activate_timer;

    // Timer since we last spawned the service.
    Core::ElapsedTimer m_run_timer;
    // How many times we have tried to restart this service, only counting those
    // times where it has exited unsuccessfully and too quickly.
    int m_restart_attempts { 0 };

    void setup_socket(SocketDescriptor&);
    void setup_sockets();
    void setup_notifier();
    void handle_socket_connection();
};

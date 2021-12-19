/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Service.h"
#include <AK/Debug.h>
#include <AK/HashMap.h>
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/StringBuilder.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/File.h>
#include <LibCore/Socket.h>
#include <fcntl.h>
#include <sched.h>
#include <stdio.h>
#include <sys/ioctl.h>
#include <unistd.h>

static HashMap<pid_t, Service*> s_service_map;

Service* Service::find_by_pid(pid_t pid)
{
    auto it = s_service_map.find(pid);
    if (it == s_service_map.end())
        return nullptr;
    return (*it).value;
}

void Service::setup_socket(SocketDescriptor& socket)
{
    VERIFY(socket.fd == -1);

    auto ok = Core::File::ensure_parent_directories(socket.path);
    VERIFY(ok);

    // Note: we use SOCK_CLOEXEC here to make sure we don't leak every socket to
    // all the clients. We'll make the one we do need to pass down !CLOEXEC later
    // after forking off the process.
    int socket_fd = ::socket(AF_LOCAL, SOCK_STREAM | SOCK_NONBLOCK | SOCK_CLOEXEC, 0);
    if (socket_fd < 0) {
        perror("socket");
        VERIFY_NOT_REACHED();
    }
    socket.fd = socket_fd;

    if (m_account.has_value()) {
        auto& account = m_account.value();
        if (fchown(socket_fd, account.uid(), account.gid()) < 0) {
            perror("fchown");
            VERIFY_NOT_REACHED();
        }
    }

    if (fchmod(socket_fd, socket.permissions) < 0) {
        perror("fchmod");
        VERIFY_NOT_REACHED();
    }

    auto socket_address = Core::SocketAddress::local(socket.path);
    auto un_optional = socket_address.to_sockaddr_un();
    if (!un_optional.has_value()) {
        dbgln("Socket name {} is too long. BUG! This should have failed earlier!", socket.path);
        VERIFY_NOT_REACHED();
    }
    auto un = un_optional.value();
    int rc = bind(socket_fd, (const sockaddr*)&un, sizeof(un));
    if (rc < 0) {
        perror("bind");
        VERIFY_NOT_REACHED();
    }

    rc = listen(socket_fd, 16);
    if (rc < 0) {
        perror("listen");
        VERIFY_NOT_REACHED();
    }
}

void Service::setup_sockets()
{
    for (SocketDescriptor& socket : m_sockets)
        setup_socket(socket);
}

void Service::setup_notifier()
{
    VERIFY(m_lazy);
    VERIFY(m_sockets.size() == 1);
    VERIFY(!m_socket_notifier);

    m_socket_notifier = Core::Notifier::construct(m_sockets[0].fd, Core::Notifier::Event::Read, this);
    m_socket_notifier->on_ready_to_read = [this] {
        handle_socket_connection();
    };
}

void Service::handle_socket_connection()
{
    VERIFY(m_sockets.size() == 1);
    dbgln_if(SERVICE_DEBUG, "Ready to read on behalf of {}", name());

    int socket_fd = m_sockets[0].fd;

    if (m_accept_socket_connections) {
        int accepted_fd = accept(socket_fd, nullptr, nullptr);
        if (accepted_fd < 0) {
            perror("accept");
            return;
        }
        spawn(accepted_fd);
        close(accepted_fd);
    } else {
        remove_child(*m_socket_notifier);
        m_socket_notifier = nullptr;
        spawn(socket_fd);
    }
}

void Service::activate()
{
    VERIFY(m_pid < 0);

    if (m_lazy)
        setup_notifier();
    else
        spawn();
}

void Service::spawn(int socket_fd)
{
    if (!Core::File::exists(m_executable_path)) {
        dbgln("{}: binary \"{}\" does not exist, skipping service.", name(), m_executable_path);
        return;
    }

    dbgln_if(SERVICE_DEBUG, "Spawning {}", name());

    m_run_timer.start();
    pid_t pid = fork();

    if (pid < 0) {
        perror("fork");
        dbgln("Failed to spawn {}. Sucks, dude :(", name());
    } else if (pid == 0) {
        // We are the child.

        if (!m_working_directory.is_null()) {
            if (chdir(m_working_directory.characters()) < 0) {
                perror("chdir");
                VERIFY_NOT_REACHED();
            }
        }

        struct sched_param p;
        p.sched_priority = m_priority;
        int rc = sched_setparam(0, &p);
        if (rc < 0) {
            perror("sched_setparam");
            VERIFY_NOT_REACHED();
        }

        if (!m_stdio_file_path.is_null()) {
            close(STDIN_FILENO);
            int fd = open(m_stdio_file_path.characters(), O_RDWR, 0);
            VERIFY(fd <= 0);
            if (fd < 0) {
                perror("open");
                VERIFY_NOT_REACHED();
            }
            dup2(STDIN_FILENO, STDOUT_FILENO);
            dup2(STDIN_FILENO, STDERR_FILENO);

            if (isatty(STDIN_FILENO)) {
                ioctl(STDIN_FILENO, TIOCSCTTY);
            }
        } else {
            if (isatty(STDIN_FILENO)) {
                ioctl(STDIN_FILENO, TIOCNOTTY);
            }
            close(STDIN_FILENO);
            close(STDOUT_FILENO);
            close(STDERR_FILENO);

            int fd = open("/dev/null", O_RDWR);
            VERIFY(fd == STDIN_FILENO);
            dup2(STDIN_FILENO, STDOUT_FILENO);
            dup2(STDIN_FILENO, STDERR_FILENO);
        }

        StringBuilder builder;

        if (socket_fd >= 0) {
            // We were spawned by socket activation. We currently only support
            // single sockets for socket activation, so make sure that's the case.
            VERIFY(m_sockets.size() == 1);

            int fd = dup(socket_fd);
            builder.appendff("{}:{}", m_sockets[0].path, fd);
        } else {
            // We were spawned as a regular process, so dup every socket for this
            // service and let the service know via SOCKET_TAKEOVER.
            for (unsigned i = 0; i < m_sockets.size(); i++) {
                SocketDescriptor& socket = m_sockets.at(i);

                int new_fd = dup(socket.fd);
                if (i != 0)
                    builder.append(' ');
                builder.appendff("{}:{}", socket.path, new_fd);
            }
        }

        if (!m_sockets.is_empty()) {
            // The new descriptor is !CLOEXEC here.
            setenv("SOCKET_TAKEOVER", builder.to_string().characters(), true);
        }

        if (m_account.has_value()) {
            auto& account = m_account.value();
            if (setgid(account.gid()) < 0 || setgroups(account.extra_gids().size(), account.extra_gids().data()) < 0 || setuid(account.uid()) < 0) {
                dbgln("Failed to drop privileges (GID={}, UID={})\n", account.gid(), account.uid());
                exit(1);
            }
            setenv("HOME", account.home_directory().characters(), true);
        }

        for (String& env : m_environment)
            putenv(const_cast<char*>(env.characters()));

        char* argv[m_extra_arguments.size() + 2];
        argv[0] = const_cast<char*>(m_executable_path.characters());
        for (size_t i = 0; i < m_extra_arguments.size(); i++)
            argv[i + 1] = const_cast<char*>(m_extra_arguments[i].characters());
        argv[m_extra_arguments.size() + 1] = nullptr;

        rc = execv(argv[0], argv);
        warnln("Failed to execv({}, ...): {}", argv[0], strerror(errno));
        dbgln("Failed to execv({}, ...): {}", argv[0], strerror(errno));
        VERIFY_NOT_REACHED();
    } else if (!m_multi_instance) {
        // We are the parent.
        m_pid = pid;
        s_service_map.set(pid, this);
    }
}

void Service::did_exit(int exit_code)
{
    VERIFY(m_pid > 0);
    VERIFY(!m_multi_instance);

    dbgln("Service {} has exited with exit code {}", name(), exit_code);

    s_service_map.remove(m_pid);
    m_pid = -1;

    if (!m_keep_alive)
        return;

    int run_time_in_msec = m_run_timer.elapsed();
    bool exited_successfully = exit_code == 0;

    if (!exited_successfully && run_time_in_msec < 1000) {
        switch (m_restart_attempts) {
        case 0:
            dbgln("Trying again");
            break;
        case 1:
            dbgln("Third time's the charm?");
            break;
        default:
            dbgln("Giving up on {}. Good luck!", name());
            return;
        }
        m_restart_attempts++;
    }

    activate();
}

Service::Service(const Core::ConfigFile& config, StringView name)
    : Core::Object(nullptr)
{
    VERIFY(config.has_group(name));

    set_name(name);
    m_executable_path = config.read_entry(name, "Executable", String::formatted("/bin/{}", this->name()));
    m_extra_arguments = config.read_entry(name, "Arguments", "").split(' ');
    m_stdio_file_path = config.read_entry(name, "StdIO");

    String prio = config.read_entry(name, "Priority");
    if (prio == "low")
        m_priority = 10;
    else if (prio == "normal" || prio.is_null())
        m_priority = 30;
    else if (prio == "high")
        m_priority = 50;
    else
        VERIFY_NOT_REACHED();

    m_keep_alive = config.read_bool_entry(name, "KeepAlive");
    m_lazy = config.read_bool_entry(name, "Lazy");

    m_user = config.read_entry(name, "User");
    if (!m_user.is_null()) {
        auto result = Core::Account::from_name(m_user.characters());
        if (result.is_error())
            warnln("Failed to resolve user {}: {}", m_user, result.error());
        else
            m_account = result.value();
    }

    m_working_directory = config.read_entry(name, "WorkingDirectory");
    m_environment = config.read_entry(name, "Environment").split(' ');
    m_system_modes = config.read_entry(name, "SystemModes", "graphical").split(',');
    m_multi_instance = config.read_bool_entry(name, "MultiInstance");
    m_accept_socket_connections = config.read_bool_entry(name, "AcceptSocketConnections");

    String socket_entry = config.read_entry(name, "Socket");
    String socket_permissions_entry = config.read_entry(name, "SocketPermissions", "0600");

    if (!socket_entry.is_null()) {
        Vector<String> socket_paths = socket_entry.split(',');
        Vector<String> socket_perms = socket_permissions_entry.split(',');
        m_sockets.ensure_capacity(socket_paths.size());

        // Need i here to iterate along with all other vectors.
        for (unsigned i = 0; i < socket_paths.size(); i++) {
            String& path = socket_paths.at(i);

            // Socket path (plus NUL) must fit into the structs sent to the Kernel.
            VERIFY(path.length() < UNIX_PATH_MAX);

            // This is done so that the last permission repeats for every other
            // socket. So you can define a single permission, and have it
            // be applied for every socket.
            mode_t permissions = strtol(socket_perms.at(min(socket_perms.size() - 1, (long unsigned)i)).characters(), nullptr, 8) & 0777;

            m_sockets.empend(path, -1, permissions);
        }
    }

    // Lazy requires Socket, but only one.
    VERIFY(!m_lazy || m_sockets.size() == 1);
    // AcceptSocketConnections always requires Socket (single), Lazy, and MultiInstance.
    VERIFY(!m_accept_socket_connections || (m_sockets.size() == 1 && m_lazy && m_multi_instance));
    // MultiInstance doesn't work with KeepAlive.
    VERIFY(!m_multi_instance || !m_keep_alive);

    if (is_enabled())
        setup_sockets();
}

void Service::save_to(JsonObject& json)
{
    Core::Object::save_to(json);

    json.set("executable_path", m_executable_path);

    // FIXME: This crashes Inspector.
    /*
    JsonArray extra_args;
    for (String& arg : m_extra_arguments)
        extra_args.append(arg);
    json.set("extra_arguments", move(extra_args));

    JsonArray system_modes;
    for (String& mode : m_system_modes)
        system_modes.append(mode);
    json.set("system_modes", system_modes);

    JsonArray environment;
    for (String& env : m_environment)
        system_modes.append(env);
    json.set("environment", environment);

    JsonArray sockets;
    for (SocketDescriptor &socket : m_sockets) {
        JsonObject socket_obj;
        socket_obj.set("path", socket.path);
        socket_obj.set("permissions", socket.permissions);
        sockets.append(socket);
    }
    json.set("sockets", sockets);
    */

    json.set("stdio_file_path", m_stdio_file_path);
    json.set("priority", m_priority);
    json.set("keep_alive", m_keep_alive);
    json.set("lazy", m_lazy);
    json.set("user", m_user);
    json.set("multi_instance", m_multi_instance);
    json.set("accept_socket_connections", m_accept_socket_connections);

    if (m_pid > 0)
        json.set("pid", m_pid);
    else
        json.set("pid", nullptr);

    json.set("restart_attempts", m_restart_attempts);
    json.set("working_directory", m_working_directory);
}

bool Service::is_enabled() const
{
    extern String g_system_mode;
    return m_system_modes.contains_slow(g_system_mode);
}

/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/RefPtr.h>
#include <AK/String.h>
#include <LibCore/CNotifier.h>
#include <LibCore/CObject.h>

namespace AK {
class JsonObject;
}
namespace Core {
class ConfigFile;
}

class Service final : public Core::Object {
    C_OBJECT(Service)

public:
    void activate();
    void did_exit(int exit_code);

    static Service* find_by_pid(pid_t);

    void save_to(AK::JsonObject&) override;

private:
    Service(const Core::ConfigFile&, const StringView& name);

    void spawn();

    // Path to the executable. By default this is /bin/{m_name}.
    String m_executable_path;
    // Extra arguments, starting from argv[1], to pass when exec'ing.
    Vector<String> m_extra_arguments;
    // File path to open as stdio fds.
    String m_stdio_file_path;
    int m_priority { 1 };
    // Whether we should re-launch it if it exits.
    bool m_keep_alive { false };
    // Path to the socket to create and listen on on behalf of this service.
    String m_socket_path;
    // File system permissions for the socket.
    mode_t m_socket_permissions { 0 };
    // Whether we should only spawn this service once somebody connects to the socket.
    bool m_lazy;
    // The name of the user we should run this service as.
    String m_user;
    uid_t m_uid { 0 };
    gid_t m_gid { 0 };
    Vector<gid_t> m_extra_gids;

    // PID of the running instance of this service.
    pid_t m_pid { -1 };
    // An open fd to the socket.
    int m_socket_fd { -1 };
    RefPtr<Core::Notifier> m_socket_notifier;

    void resolve_user();
    void setup_socket();
    void setup_notifier();
};

/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/Socket.h>

namespace Core {

class LocalSocket final : public Socket {
    C_OBJECT(LocalSocket)
public:
    virtual ~LocalSocket() override;

    static ErrorOr<NonnullRefPtr<LocalSocket>> take_over_accepted_socket_from_system_server(String const& socket_path = String());
    pid_t peer_pid() const;

private:
    explicit LocalSocket(Object* parent = nullptr);
    LocalSocket(int fd, Object* parent = nullptr);

    // FIXME: better place to put this so both LocalSocket and LocalServer can
    // enjoy it?
    friend class LocalServer;

    static void parse_sockets_from_system_server();

    static HashMap<String, int> s_overtaken_sockets;
    static bool s_overtaken_sockets_parsed;
};

}

/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <LibCore/LocalServer.h>
#include <LibIPC/ConnectionFromClient.h>

namespace IPC {

template<typename ConnectionFromClientType>
class MultiServer {
public:
    static ErrorOr<NonnullOwnPtr<MultiServer>> try_create(Optional<String> socket_path = {})
    {
        auto server = TRY(Core::LocalServer::try_create());
        TRY(server->take_over_from_system_server(socket_path.value_or({})));
        return adopt_nonnull_own_or_enomem(new (nothrow) MultiServer(move(server)));
    }

private:
    explicit MultiServer(NonnullRefPtr<Core::LocalServer> server)
        : m_server(move(server))
    {
        m_server->on_accept = [&](auto client_socket) {
            auto client_id = ++m_next_client_id;
            (void)IPC::new_client_connection<ConnectionFromClientType>(move(client_socket), client_id);
        };
    }

    int m_next_client_id { 0 };
    RefPtr<Core::LocalServer> m_server;
};

}

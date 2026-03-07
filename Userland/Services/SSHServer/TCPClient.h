/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullOwnPtr.h>
#include <LibCore/EventReceiver.h>
#include <LibCore/Forward.h>
#include <LibCore/Socket.h>
#include <Services/SSHServer/SSHClient.h>

namespace SSH::Server {

class TCPClient : public Core::EventReceiver {
    C_OBJECT(TCPClient)
public:
    static NonnullRefPtr<TCPClient> create(NonnullOwnPtr<Core::TCPSocket>&& socket, NonnullRefPtr<Core::TCPServer> const& parent);
    virtual ~TCPClient() = default;

private:
    TCPClient(NonnullOwnPtr<Core::TCPSocket>&& socket, NonnullRefPtr<Core::TCPServer> const& parent);

    ErrorOr<void> on_ready_to_read();
    void die();

    NonnullOwnPtr<Core::TCPSocket> m_socket;
    ByteBuffer m_read_buffer;

    SSHClient m_ssh_client;
};

} // SSHServer

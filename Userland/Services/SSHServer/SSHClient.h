/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <LibCore/Forward.h>

namespace SSH::Server {

class TCPClient;

class SSHClient {
public:
    explicit SSHClient(Core::TCPSocket& tcp_socket)
        : m_tcp_socket { tcp_socket }
    {
    }

    ErrorOr<void> handle_data(ByteBuffer& data);

private:
    enum class State : u8 {
        Constructed,
        ProtocolVersionExchanged,
    };

    ErrorOr<void> handle_protocol_version(ByteBuffer& data);

    State m_state { State::Constructed };
    Core::TCPSocket& m_tcp_socket;
};

} // SSHServer

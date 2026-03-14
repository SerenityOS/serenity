/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <LibCore/Forward.h>
#include <LibSSH/KeyExchangeData.h>
#include <LibSSH/Peer.h>

namespace SSH::Server {

class TCPClient;

class SSHClient : public Peer {
public:
    explicit SSHClient(Core::TCPSocket& tcp_socket)
        : Peer(tcp_socket)
        , m_tcp_socket { tcp_socket }
    {
    }

    ErrorOr<void> handle_data(ByteBuffer& data);

private:
    enum class State : u8 {
        Constructed,
        WaitingForKeyProtocolExchange,
        WaitingForKeyExchange,
        WaitingForNewKeysMessage,
        KeyExchanged,
    };

    ErrorOr<void> handle_protocol_version(ByteBuffer& data);

    ErrorOr<void> handle_key_protocol_exchange(ByteBuffer& data);
    ErrorOr<void> send_key_protocol_message();

    ErrorOr<void> handle_key_exchange(ByteBuffer& data);
    ErrorOr<void> send_ecdh_reply(ByteBuffer&& client_public_key);

    State m_state { State::Constructed };
    Core::TCPSocket& m_tcp_socket;

    KeyExchangeData m_key_exchange_data {};
};

} // SSHServer

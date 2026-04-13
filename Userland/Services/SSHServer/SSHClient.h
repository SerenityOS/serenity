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
#include <LibSSH/Session.h>

namespace SSH::Server {

class TCPClient;

struct GenericMessage {
    AK_MAKE_NONCOPYABLE(GenericMessage);

public:
    explicit GenericMessage(ByteBuffer&& data)
        : data(move(data))
        , payload(this->data.bytes())
        , type(MUST(payload.read_value<MessageID>()))
    {
    }
    GenericMessage(GenericMessage&& other)
        : data(move(other.data))
        , payload(data.bytes())
        , type(other.type)
    {
        MUST(payload.discard(other.payload.offset()));
    }

    ByteBuffer data;
    FixedMemoryStream payload;
    MessageID type {};
};

class SSHClient : public Peer {
public:
    explicit SSHClient(Core::TCPSocket& tcp_socket)
        : Peer(tcp_socket)
        , m_tcp_socket { tcp_socket }
    {
    }

    enum class ShouldDisconnect : u8 {
        No,
        Yes,
    };

    ErrorOr<ShouldDisconnect> handle_data(ByteBuffer& data);

private:
    enum class State : u8 {
        Constructed,
        WaitingForKeyProtocolExchange,
        WaitingForKeyExchange,
        WaitingForNewKeysMessage,
        KeyExchanged,
        WaitingForUserAuthentication,
        Authentified,
    };

    ErrorOr<void> handle_protocol_version(ByteBuffer& data);

    ErrorOr<void> handle_key_protocol_exchange(ByteBuffer& data);
    ErrorOr<void> send_key_protocol_message();

    ErrorOr<void> handle_key_exchange(ByteBuffer& data);
    ErrorOr<void> send_ecdh_reply(ByteBuffer&& client_public_key);

    ErrorOr<GenericMessage> unpack_generic_message(ByteBuffer& data);

    ErrorOr<void> handle_service_request(GenericMessage data);
    ErrorOr<void> send_service_accept(StringView);

    ErrorOr<void> handle_user_authentication(GenericMessage data);
    ErrorOr<void> handle_publickey_message(GenericMessage&, ReadonlyBytes user_name, ReadonlyBytes service_name);
    ErrorOr<void> send_available_authentication_methods();
    ErrorOr<void> send_publickey_ok_message(ReadonlyBytes algorithm_name, ReadonlyBytes blob);
    ErrorOr<void> send_user_authentication_success();

    ErrorOr<ShouldDisconnect> handle_generic_packet(GenericMessage&&);

    ErrorOr<void> handle_channel_open_message(GenericMessage&);
    ErrorOr<void> send_channel_open_confirmation(Session const&);
    ErrorOr<void> handle_channel_request(GenericMessage&);
    ErrorOr<void> handle_channel_exec(Session&, GenericMessage&);
    ErrorOr<void> send_channel_success_message(Session const&);
    ErrorOr<void> send_channel_data(Session const&, ReadonlyBytes);
    ErrorOr<void> handle_channel_close(GenericMessage&);
    ErrorOr<void> send_channel_close(Session&);
    ErrorOr<Session*> find_session(u32 sender_channel_id);

    State m_state { State::Constructed };
    Core::TCPSocket& m_tcp_socket;

    KeyExchangeData m_key_exchange_data {};
    ByteBuffer m_cookie {};

    Vector<Session> m_sessions;
};

} // SSHServer

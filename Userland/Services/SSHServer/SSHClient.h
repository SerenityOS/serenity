/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Weakable.h>
#include <LibCore/Forward.h>
#include <LibSSH/BehaviorControl.h>
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

class SSHClient : public Peer, public Weakable<SSHClient> {
public:
    explicit SSHClient(Core::TCPSocket& tcp_socket, Function<void()> disconnect);
    ~SSHClient();

    ErrorOr<BehaviorControl> handle_data(ByteBuffer& data);
    void shutdown();

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
    ErrorOr<void> handle_publickey_message(GenericMessage&, Core::Account const& user, ReadonlyBytes service_name);
    ErrorOr<void> send_available_authentication_methods();
    ErrorOr<void> send_publickey_ok_message(ReadonlyBytes algorithm_name, ReadonlyBytes blob);
    ErrorOr<void> send_user_authentication_success();

    ErrorOr<BehaviorControl> handle_generic_packet(GenericMessage&&);

    ErrorOr<void> handle_channel_open_message(GenericMessage&);
    ErrorOr<void> send_channel_open_confirmation(Session const&);
    ErrorOr<void> handle_channel_request(GenericMessage&);
    ErrorOr<void> handle_channel_data(GenericMessage&);
    ErrorOr<void> handle_channel_subsystem(Session&, GenericMessage&);
    ErrorOr<void> handle_channel_exec(NonnullRefPtr<Session> const&, GenericMessage&);
    ErrorOr<void> send_channel_success_message(Session const&);
    ErrorOr<void> send_channel_data(Session&, ReadonlyBytes);
    ErrorOr<void> send_channel_extended_data(Session&, ReadonlyBytes);
    ErrorOr<void> queue_channel_data(Session&, ReadonlyBytes, bool is_extended_data);
    ErrorOr<void> flush_pending_channel_data(Session&);
    ErrorOr<void> send_channel_data_packet(Session const&, ReadonlyBytes, bool is_extended_data);
    ErrorOr<void> handle_channel_window_adjust_message(GenericMessage&);
    ErrorOr<void> adjust_channel_window_if_needed(Session&);
    ErrorOr<void> send_channel_window_adjust_message(Session&, u32 bytes_to_add);
    ErrorOr<void> handle_channel_eof(GenericMessage&);
    ErrorOr<void> send_exit_status(Session const&);
    ErrorOr<void> handle_channel_close(GenericMessage&);
    ErrorOr<void> send_channel_close(Session&);
    ErrorOr<NonnullRefPtr<Session>> find_session(u32 local_channel_id);

    template<typename F, typename F2>
    Coroutine<void> async_stream_std_data(NonnullRefPtr<Session>, F file_extractor, F2 sender);
    Coroutine<void> async_stream_data_to_subsystem(NonnullRefPtr<Session>);

    ErrorOr<void> manage_child_death();
    ErrorOr<void> close_session_if_needed(Session&);

    void disconnect(Error);

    State m_state { State::Constructed };
    u32 m_authentication_attempts {};
    bool m_is_shutdown { false };

    Function<void()> m_disconnect;

    KeyExchangeData m_key_exchange_data {};
    ByteBuffer m_cookie {};

    Vector<NonnullRefPtr<Session>> m_sessions;
    int m_sigchld_handler_id {};
};

} // SSHServer

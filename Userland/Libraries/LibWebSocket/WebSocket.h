/*
 * Copyright (c) 2021, Dex♪ <dexes.ttp@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Span.h>
#include <LibCore/Object.h>
#include <LibWebSocket/ConnectionInfo.h>
#include <LibWebSocket/Impl/WebSocketImpl.h>
#include <LibWebSocket/Message.h>

namespace WebSocket {

enum class ReadyState {
    Connecting = 0,
    Open = 1,
    Closing = 2,
    Closed = 3,
};

class WebSocket final : public Core::Object {
    C_OBJECT(WebSocket)
public:
    static NonnullRefPtr<WebSocket> create(ConnectionInfo);
    virtual ~WebSocket() override = default;

    URL const& url() const { return m_connection.url(); }

    ReadyState ready_state();

    // Call this to start the WebSocket connection.
    void start();

    // This can only be used if the `ready_state` is `ReadyState::Open`
    void send(Message const&);

    // This can only be used if the `ready_state` is `ReadyState::Open`
    void close(u16 code = 1005, String const& reason = {});

    Function<void()> on_open;
    Function<void(u16 code, String reason, bool was_clean)> on_close;
    Function<void(Message message)> on_message;

    enum class Error {
        CouldNotEstablishConnection,
        ConnectionUpgradeFailed,
        ServerClosedSocket,
    };

    Function<void(Error)> on_error;

private:
    explicit WebSocket(ConnectionInfo);

    // As defined in section 5.2
    enum class OpCode : u8 {
        Continuation = 0x0,
        Text = 0x1,
        Binary = 0x2,
        ConnectionClose = 0x8,
        Ping = 0x9,
        Pong = 0xA,
    };

    void drain_read();

    void send_client_handshake();
    void read_server_handshake();

    void read_frame();
    void send_frame(OpCode, ReadonlyBytes, bool is_final);

    void notify_open();
    void notify_close(u16 code, String reason, bool was_clean);
    void notify_error(Error);
    void notify_message(Message);

    void fatal_error(Error);
    void discard_connection();

    enum class InternalState {
        NotStarted,
        EstablishingProtocolConnection,
        SendingClientHandshake,
        WaitingForServerHandshake,
        Open,
        Closing,
        Closed,
        Errored,
    };

    InternalState m_state { InternalState::NotStarted };

    String m_websocket_key;
    bool m_has_read_server_handshake_first_line { false };
    bool m_has_read_server_handshake_upgrade { false };
    bool m_has_read_server_handshake_connection { false };
    bool m_has_read_server_handshake_accept { false };

    u16 m_last_close_code { 1005 };
    String m_last_close_message;

    ConnectionInfo m_connection;
    RefPtr<WebSocketImpl> m_impl;
};

}

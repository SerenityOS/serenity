/*
 * Copyright (c) 2021, Dex♪ <dexes.ttp@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/ByteBuffer.h>
#include <AK/Function.h>
#include <AK/RefCounted.h>
#include <AK/String.h>
#include <AK/WeakPtr.h>
#include <LibCore/Notifier.h>
#include <LibIPC/Forward.h>

namespace Protocol {

class WebSocketClient;

class WebSocket : public RefCounted<WebSocket> {
public:
    struct CertificateAndKey {
        String certificate;
        String key;
    };

    struct Message {
        ByteBuffer data;
        bool is_text { false };
    };

    enum class Error {
        CouldNotEstablishConnection,
        ConnectionUpgradeFailed,
        ServerClosedSocket,
    };

    enum class ReadyState {
        Connecting = 0,
        Open = 1,
        Closing = 2,
        Closed = 3,
    };

    static NonnullRefPtr<WebSocket> create_from_id(Badge<WebSocketClient>, WebSocketClient& client, i32 connection_id)
    {
        return adopt_ref(*new WebSocket(client, connection_id));
    }

    int id() const { return m_connection_id; }

    ReadyState ready_state();

    void send(ByteBuffer binary_or_text_message, bool is_text);
    void send(StringView text_message);
    void close(u16 code = 1005, String reason = {});

    Function<void()> on_open;
    Function<void(Message)> on_message;
    Function<void(Error)> on_error;
    Function<void(u16 code, String reason, bool was_clean)> on_close;
    Function<CertificateAndKey()> on_certificate_requested;

    void did_open(Badge<WebSocketClient>);
    void did_receive(Badge<WebSocketClient>, ByteBuffer, bool);
    void did_error(Badge<WebSocketClient>, i32);
    void did_close(Badge<WebSocketClient>, u16, String, bool);
    void did_request_certificates(Badge<WebSocketClient>);

private:
    explicit WebSocket(WebSocketClient&, i32 connection_id);
    WeakPtr<WebSocketClient> m_client;
    int m_connection_id { -1 };
};

}

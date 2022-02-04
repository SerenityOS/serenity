/*
 * Copyright (c) 2021, Dex♪ <dexes.ttp@gmail.com>
 * Copyright (c) 2022, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Span.h>
#include <AK/String.h>
#include <LibCore/Object.h>
#include <LibWebSocket/ConnectionInfo.h>

namespace WebSocket {

class WebSocketImpl : public Core::Object {
    C_OBJECT(WebSocketImpl);

public:
    virtual ~WebSocketImpl() override;
    explicit WebSocketImpl(Core::Object* parent = nullptr);

    void connect(ConnectionInfo const&);

    bool can_read_line() { return MUST(m_socket->can_read_line()); }
    ErrorOr<String> read_line(size_t size);

    bool can_read() { return MUST(m_socket->can_read_without_blocking()); }
    ErrorOr<ByteBuffer> read(int max_size);

    bool send(ReadonlyBytes bytes) { return m_socket->write_or_error(bytes); }

    bool eof() { return m_socket->is_eof(); }

    void discard_connection()
    {
        m_socket.clear();
    }

    Function<void()> on_connected;
    Function<void()> on_connection_error;
    Function<void()> on_ready_to_read;

private:
    OwnPtr<Core::Stream::BufferedSocketBase> m_socket;
};

}

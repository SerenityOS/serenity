/*
 * Copyright (c) 2021, Dex♪ <dexes.ttp@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Span.h>
#include <AK/String.h>
#include <LibCore/Object.h>
#include <LibTLS/TLSv12.h>
#include <LibWebSocket/ConnectionInfo.h>
#include <LibWebSocket/Impl/AbstractWebSocketImpl.h>

namespace WebSocket {

class TLSv12WebSocketConnectionImpl final : public AbstractWebSocketImpl {
    C_OBJECT(TLSv12WebSocketConnectionImpl);

public:
    virtual ~TLSv12WebSocketConnectionImpl() override;

    void connect(ConnectionInfo const& connection) override;

    virtual bool can_read_line() override;
    virtual String read_line(size_t size) override;

    virtual bool can_read() override;
    virtual ByteBuffer read(int max_size) override;

    virtual bool send(ReadonlyBytes data) override;

    virtual bool eof() override;

    virtual void discard_connection() override;

private:
    explicit TLSv12WebSocketConnectionImpl(Core::Object* parent = nullptr);

    OwnPtr<TLS::TLSv12> m_socket;
};

}

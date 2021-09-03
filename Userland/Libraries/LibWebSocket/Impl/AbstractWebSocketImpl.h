/*
 * Copyright (c) 2021, Dex♪ <dexes.ttp@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/ByteBuffer.h>
#include <YAK/Span.h>
#include <YAK/String.h>
#include <LibCore/Object.h>
#include <LibWebSocket/ConnectionInfo.h>

namespace WebSocket {

class AbstractWebSocketImpl : public Core::Object {
    C_OBJECT_ABSTRACT(AbstractWebSocketImpl);

public:
    virtual ~AbstractWebSocketImpl() override;
    explicit AbstractWebSocketImpl(Core::Object* parent = nullptr);

    virtual void connect(ConnectionInfo const&) = 0;

    virtual bool can_read_line() = 0;
    virtual String read_line(size_t size) = 0;

    virtual bool can_read() = 0;
    virtual ByteBuffer read(int max_size) = 0;

    virtual bool send(ReadonlyBytes) = 0;

    virtual bool eof() = 0;

    virtual void discard_connection() = 0;

    Function<void()> on_connected;
    Function<void()> on_connection_error;
    Function<void()> on_ready_to_read;
};

}

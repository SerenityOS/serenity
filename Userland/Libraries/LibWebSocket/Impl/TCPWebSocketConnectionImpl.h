/*
 * Copyright (c) 2021, The SerenityOS developers.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Span.h>
#include <AK/String.h>
#include <LibCore/Notifier.h>
#include <LibCore/Object.h>
#include <LibCore/TCPSocket.h>
#include <LibWebSocket/ConnectionInfo.h>
#include <LibWebSocket/Impl/AbstractWebSocketImpl.h>

namespace WebSocket {

class TCPWebSocketConnectionImpl final : public AbstractWebSocketImpl {
    C_OBJECT(TCPWebSocketConnectionImpl);

public:
    virtual ~TCPWebSocketConnectionImpl() override;
    explicit TCPWebSocketConnectionImpl(Core::Object* parent = nullptr);

    virtual void connect(ConnectionInfo const& connection) override;

    virtual bool can_read_line() override;
    virtual String read_line(size_t size) override;

    virtual bool can_read() override;
    virtual ByteBuffer read(int max_size) override;

    virtual bool send(ReadonlyBytes data) override;

    virtual bool eof() override;

    virtual void discard_connection() override;

private:
    RefPtr<Core::Notifier> m_notifier;
    RefPtr<Core::TCPSocket> m_socket;
};

}

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

#include <LibWebSocket/Impl/TLSv12WebSocketConnectionImpl.h>

namespace WebSocket {

TLSv12WebSocketConnectionImpl::TLSv12WebSocketConnectionImpl(Core::Object* parent)
    : AbstractWebSocketImpl(parent)
{
}

TLSv12WebSocketConnectionImpl::~TLSv12WebSocketConnectionImpl()
{
    discard_connection();
}

void TLSv12WebSocketConnectionImpl::connect(ConnectionInfo const& connection)
{
    VERIFY(!m_socket);
    VERIFY(on_connected);
    VERIFY(on_connection_error);
    VERIFY(on_ready_to_read);
    m_socket = TLS::TLSv12::construct(this);

    m_notifier = Core::Notifier::construct(m_socket->fd(), Core::Notifier::Read);
    m_notifier->on_ready_to_read = [this] {
        on_ready_to_read();
    };

    m_socket->set_root_certificates(DefaultRootCACertificates::the().certificates());
    m_socket->on_tls_error = [this](TLS::AlertDescription) {
        on_connection_error();
    };
    m_socket->on_tls_ready_to_write = [this] {
        on_connected();
    };
    m_socket->on_tls_finished = [this] {
        on_connection_error();
    };
    m_socket->on_tls_certificate_request = [this](auto&) {
        // FIXME : Once we handle TLS certificate requests, handle it here as well.
    };
    bool success = m_socket->connect(connection.url().host(), connection.url().port());
    if (!success) {
        deferred_invoke([this](auto&) {
            on_connection_error();
        });
    }
}

bool TLSv12WebSocketConnectionImpl::send(ReadonlyBytes data)
{
    return m_socket->write(data);
}

bool TLSv12WebSocketConnectionImpl::can_read_line()
{
    return m_socket->can_read_line();
}

String TLSv12WebSocketConnectionImpl::read_line(size_t size)
{
    return m_socket->read_line(size);
}

bool TLSv12WebSocketConnectionImpl::can_read()
{
    return m_socket->can_read();
}

ByteBuffer TLSv12WebSocketConnectionImpl::read(int max_size)
{
    return m_socket->read(max_size);
}

bool TLSv12WebSocketConnectionImpl::eof()
{
    return m_socket->eof();
}

void TLSv12WebSocketConnectionImpl::discard_connection()
{
    if (!m_socket)
        return;
    m_socket->on_tls_connected = nullptr;
    m_socket->on_tls_error = nullptr;
    m_socket->on_tls_finished = nullptr;
    m_socket->on_tls_certificate_request = nullptr;
    m_socket->on_ready_to_read = nullptr;
    remove_child(*m_socket);
    m_socket = nullptr;
}

}

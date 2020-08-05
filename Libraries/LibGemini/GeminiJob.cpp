/*
 * Copyright (c) 2020, The SerenityOS developers.
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

#include <LibCore/EventLoop.h>
#include <LibGemini/GeminiJob.h>
#include <LibGemini/GeminiResponse.h>
#include <LibTLS/TLSv12.h>
#include <stdio.h>
#include <unistd.h>

//#define GEMINIJOB_DEBUG

namespace Gemini {

void GeminiJob::start()
{
    ASSERT(!m_socket);
    m_socket = TLS::TLSv12::construct(this);
    m_socket->on_tls_connected = [this] {
#ifdef GEMINIJOB_DEBUG
        dbg() << "GeminiJob: on_connected callback";
#endif
        on_socket_connected();
    };
    m_socket->on_tls_error = [this](TLS::AlertDescription error) {
        if (error == TLS::AlertDescription::HandshakeFailure) {
            deferred_invoke([this](auto&) {
                return did_fail(Core::NetworkJob::Error::ProtocolFailed);
            });
        } else if (error == TLS::AlertDescription::DecryptError) {
            deferred_invoke([this](auto&) {
                return did_fail(Core::NetworkJob::Error::ConnectionFailed);
            });
        } else {
            deferred_invoke([this](auto&) {
                return did_fail(Core::NetworkJob::Error::TransmissionFailed);
            });
        }
    };
    m_socket->on_tls_finished = [this] {
        finish_up();
    };
    m_socket->on_tls_certificate_request = [this](auto&) {
        if (on_certificate_requested)
            on_certificate_requested(*this);
    };
    bool success = ((TLS::TLSv12&)*m_socket).connect(m_request.url().host(), m_request.url().port());
    if (!success) {
        deferred_invoke([this](auto&) {
            return did_fail(Core::NetworkJob::Error::ConnectionFailed);
        });
    }
}

void GeminiJob::shutdown()
{
    if (!m_socket)
        return;
    m_socket->on_tls_ready_to_read = nullptr;
    m_socket->on_tls_connected = nullptr;
    remove_child(*m_socket);
    m_socket = nullptr;
}

void GeminiJob::read_while_data_available(Function<IterationDecision()> read)
{
    while (m_socket->can_read()) {
        if (read() == IterationDecision::Break)
            break;
    }
}

void GeminiJob::set_certificate(String certificate, String private_key)
{
    if (!m_socket->add_client_key(
            ByteBuffer::wrap(const_cast<char*>(certificate.characters()), certificate.length()),
            ByteBuffer::wrap(const_cast<char*>(private_key.characters()), private_key.length()))) {
        dbg() << "LibGemini: Failed to set a client certificate";
        // FIXME: Do something about this failure
        ASSERT_NOT_REACHED();
    }
}

void GeminiJob::register_on_ready_to_read(Function<void()> callback)
{
    m_socket->on_tls_ready_to_read = [callback = move(callback)](auto&) {
        callback();
    };
}

void GeminiJob::register_on_ready_to_write(Function<void()> callback)
{
    m_socket->on_tls_ready_to_write = [callback = move(callback)](auto&) {
        callback();
    };
}

bool GeminiJob::can_read_line() const
{
    return m_socket->can_read_line();
}

ByteBuffer GeminiJob::read_line(size_t size)
{
    return m_socket->read_line(size);
}

ByteBuffer GeminiJob::receive(size_t size)
{
    return m_socket->read(size);
}

bool GeminiJob::can_read() const
{
    return m_socket->can_read();
}

bool GeminiJob::eof() const
{
    return m_socket->eof();
}

bool GeminiJob::write(const ByteBuffer& data)
{
    return m_socket->write(data);
}

}

/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <LibCore/EventLoop.h>
#include <LibHTTP/HttpResponse.h>
#include <LibHTTP/HttpsJob.h>
#include <LibTLS/TLSv12.h>
#include <stdio.h>
#include <unistd.h>

namespace HTTP {

void HttpsJob::start(NonnullRefPtr<Core::Socket> socket)
{
    VERIFY(!m_socket);
    VERIFY(is<TLS::TLSv12>(*socket));

    m_socket = static_ptr_cast<TLS::TLSv12>(socket);
    m_socket->on_tls_error = [&](TLS::AlertDescription error) {
        if (error == TLS::AlertDescription::HandshakeFailure) {
            deferred_invoke([this] {
                return did_fail(Core::NetworkJob::Error::ProtocolFailed);
            });
        } else if (error == TLS::AlertDescription::DecryptError) {
            deferred_invoke([this] {
                return did_fail(Core::NetworkJob::Error::ConnectionFailed);
            });
        } else {
            deferred_invoke([this] {
                return did_fail(Core::NetworkJob::Error::TransmissionFailed);
            });
        }
    };
    m_socket->on_tls_finished = [this] {
        if (!m_has_scheduled_finish)
            finish_up();
    };
    m_socket->on_tls_certificate_request = [this](auto&) {
        if (on_certificate_requested)
            on_certificate_requested(*this);
    };
    m_socket->set_idle(false);
    if (m_socket->is_established()) {
        dbgln("Reusing previous connection for {}", url());
        deferred_invoke([this] { on_socket_connected(); });
    } else {
        dbgln("Creating a new connection for {}", url());
        m_socket->set_root_certificates(m_override_ca_certificates ? *m_override_ca_certificates : DefaultRootCACertificates::the().certificates());
        m_socket->on_tls_connected = [this] {
            dbgln_if(HTTPSJOB_DEBUG, "HttpsJob: on_connected callback");
            on_socket_connected();
        };
        bool success = ((TLS::TLSv12&)*m_socket).connect(m_request.url().host(), m_request.url().port_or_default());
        if (!success) {
            deferred_invoke([this] {
                return did_fail(Core::NetworkJob::Error::ConnectionFailed);
            });
        }
    }
}

void HttpsJob::shutdown(ShutdownMode mode)
{
    if (!m_socket)
        return;
    if (mode == ShutdownMode::CloseSocket) {
        m_socket->close();
    } else {
        m_socket->on_tls_ready_to_read = nullptr;
        m_socket->on_tls_connected = nullptr;
        m_socket->set_on_tls_ready_to_write(nullptr);
        m_socket->set_idle(true);
        m_socket = nullptr;
    }
}

void HttpsJob::set_certificate(String certificate, String private_key)
{
    if (!m_socket->add_client_key(certificate.bytes(), private_key.bytes())) {
        dbgln("LibHTTP: Failed to set a client certificate");
        // FIXME: Do something about this failure
        VERIFY_NOT_REACHED();
    }
}

void HttpsJob::read_while_data_available(Function<IterationDecision()> read)
{
    while (m_socket->can_read()) {
        if (read() == IterationDecision::Break)
            break;
    }
}

void HttpsJob::register_on_ready_to_read(Function<void()> callback)
{
    m_socket->on_tls_ready_to_read = [callback = move(callback)](auto&) {
        callback();
    };
}

void HttpsJob::register_on_ready_to_write(Function<void()> callback)
{
    m_socket->set_on_tls_ready_to_write([callback = move(callback)](auto& tls) {
        Core::deferred_invoke([&tls] { tls.set_on_tls_ready_to_write(nullptr); });
        callback();
    });
}

bool HttpsJob::can_read_line() const
{
    return m_socket->can_read_line();
}

String HttpsJob::read_line(size_t size)
{
    return m_socket->read_line(size);
}

ByteBuffer HttpsJob::receive(size_t size)
{
    return m_socket->read(size);
}

bool HttpsJob::can_read() const
{
    return m_socket->can_read();
}

bool HttpsJob::eof() const
{
    return m_socket->eof();
}

bool HttpsJob::write(ReadonlyBytes data)
{
    return m_socket->write(data);
}

}

/*
 * Copyright (c) 2020, Ali Mohammad Pur <ali.mpfard@gmail.com>
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

#include <LibCore/DateTime.h>
#include <LibCore/Timer.h>
#include <LibCrypto/ASN1/DER.h>
#include <LibCrypto/PK/Code/EMSA_PSS.h>
#include <LibTLS/TLSv12.h>

namespace TLS {

Optional<ByteBuffer> TLSv12::read()
{
    if (m_context.application_buffer.size()) {
        auto buf = m_context.application_buffer.slice(0, m_context.application_buffer.size());
        m_context.application_buffer.clear();
        return buf;
    }
    return {};
}

ByteBuffer TLSv12::read(size_t max_size)
{
    if (m_context.application_buffer.size()) {
        auto length = min(m_context.application_buffer.size(), max_size);
        auto buf = m_context.application_buffer.slice(0, length);
        m_context.application_buffer = m_context.application_buffer.slice(length, m_context.application_buffer.size() - length);
        return buf;
    }
    return {};
}

ByteBuffer TLSv12::read_line(size_t max_size)
{
    if (!can_read_line())
        return {};

    auto* start = m_context.application_buffer.data();
    auto* newline = (u8*)memchr(m_context.application_buffer.data(), '\n', m_context.application_buffer.size());
    ASSERT(newline);

    size_t offset = newline - start;

    if (offset > max_size)
        return {};

    auto buffer = ByteBuffer::copy(start, offset);
    m_context.application_buffer = m_context.application_buffer.slice(offset + 1, m_context.application_buffer.size() - offset - 1);

    return buffer;
}

bool TLSv12::write(const ByteBuffer& buffer)
{
    if (m_context.connection_status != ConnectionStatus::Established) {
#ifdef TLS_DEBUG
        dbg() << "write request while not connected";
#endif
        return false;
    }

    PacketBuilder builder { MessageType::ApplicationData, m_context.version, buffer.size() };
    builder.append(buffer);
    auto packet = builder.build();

    update_packet(packet);
    write_packet(packet);

    return true;
}

bool TLSv12::connect(const String& hostname, int port)
{
    set_sni(hostname);
    return Core::Socket::connect(hostname, port);
}

bool TLSv12::common_connect(const struct sockaddr* saddr, socklen_t length)
{
    if (m_context.critical_error)
        return false;

    if (Core::Socket::is_connected()) {
        if (is_established()) {
            ASSERT_NOT_REACHED();
        } else {
            Core::Socket::close(); // reuse?
        }
    }

    Core::Socket::on_connected = [this] {
        Core::Socket::on_ready_to_read = [this] {
            read_from_socket();
        };

        auto packet = build_hello();
        write_packet(packet);

        deferred_invoke([&](auto&) {
            m_handshake_timeout_timer = Core::Timer::create_single_shot(
                m_max_wait_time_for_handshake_in_seconds * 1000, [&] {
                    auto timeout_diff = Core::DateTime::now().timestamp() - m_context.handshake_initiation_timestamp;
                    // If the timeout duration was actually within the max wait time (with a margin of error),
                    // we're not operating slow, so the server timed out.
                    // otherwise, it's our fault that the negotiation is taking too long, so extend the timer :P
                    if (timeout_diff < m_max_wait_time_for_handshake_in_seconds + 1) {
                        // The server did not respond fast enough,
                        // time the connection out.
                        alert(AlertLevel::Critical, AlertDescription::UserCanceled);
                        m_context.connection_finished = true;
                        m_context.tls_buffer.clear();
                        m_context.error_code = Error::TimedOut;
                        m_context.critical_error = (u8)Error::TimedOut;
                        check_connection_state(false); // Notify the client.
                    } else {
                        // Extend the timer, we are too slow.
                        m_handshake_timeout_timer->restart(m_max_wait_time_for_handshake_in_seconds * 1000);
                    }
                },
                this);
            write_into_socket();
            m_handshake_timeout_timer->start();
            m_context.handshake_initiation_timestamp = Core::DateTime::now().timestamp();
        });
        m_has_scheduled_write_flush = true;

        if (on_tls_connected)
            on_tls_connected();
    };
    bool success = Core::Socket::common_connect(saddr, length);
    if (!success)
        return false;

    return true;
}

void TLSv12::read_from_socket()
{
    if (m_context.application_buffer.size() > 0) {
        deferred_invoke([&](auto&) { read_from_socket(); });
        if (on_tls_ready_to_read)
            on_tls_ready_to_read(*this);
    }

    if (!check_connection_state(true))
        return;

    consume(Core::Socket::read(4096));
}

void TLSv12::write_into_socket()
{
#ifdef TLS_DEBUG
    dbg() << "Flushing cached records: " << m_context.tls_buffer.size() << " established? " << is_established();
#endif
    m_has_scheduled_write_flush = false;
    if (!check_connection_state(false))
        return;
    flush();

    if (!is_established())
        return;

    if (!m_context.application_buffer.size()) // hey client, you still have stuff to read...
        if (on_tls_ready_to_write)
            on_tls_ready_to_write(*this);
}

bool TLSv12::check_connection_state(bool read)
{
    if (!Core::Socket::is_open() || !Core::Socket::is_connected() || Core::Socket::eof()) {
        // an abrupt closure (the server is a jerk)
#ifdef TLS_DEBUG
        dbg() << "Socket not open, assuming abrupt closure";
#endif
        m_context.connection_finished = true;
    }
    if (m_context.critical_error) {
#ifdef TLS_DEBUG
        dbg() << "CRITICAL ERROR " << m_context.critical_error << " :(";
#endif
        if (on_tls_error)
            on_tls_error((AlertDescription)m_context.critical_error);
        return false;
    }
    if (((read && m_context.application_buffer.size() == 0) || !read) && m_context.connection_finished) {
        if (m_context.application_buffer.size() == 0) {
            if (on_tls_finished)
                on_tls_finished();
        }
        if (m_context.tls_buffer.size()) {
#ifdef TLS_DEBUG
            dbg() << "connection closed without finishing data transfer, " << m_context.tls_buffer.size() << " bytes still in buffer & " << m_context.application_buffer.size() << " bytes in application buffer";
#endif
        } else {
            m_context.connection_finished = false;
#ifdef TLS_DEBUG
            dbg() << "FINISHED";
#endif
        }
        if (!m_context.application_buffer.size()) {
            m_context.connection_status = ConnectionStatus::Disconnected;
            return false;
        }
    }
    return true;
}

bool TLSv12::flush()
{
    auto out_buffer = write_buffer().data();
    size_t out_buffer_index { 0 };
    size_t out_buffer_length = write_buffer().size();

    if (out_buffer_length == 0)
        return true;

#ifdef TLS_DEBUG
    dbg() << "SENDING...";
    print_buffer(out_buffer, out_buffer_length);
#endif
    if (Core::Socket::write(&out_buffer[out_buffer_index], out_buffer_length)) {
        write_buffer().clear();
        return true;
    }
    if (m_context.send_retries++ == 10) {
        // drop the records, we can't send
#ifdef TLS_DEBUG
        dbg() << "Dropping " << write_buffer().size() << " bytes worth of TLS records as max retries has been reached";
#endif
        write_buffer().clear();
        m_context.send_retries = 0;
    }
    return false;
}

}

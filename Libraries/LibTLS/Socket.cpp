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
        dbg() << "write request while not connected";
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

    auto packet = build_hello();
    write_packet(packet);

    Core::Socket::on_connected = [this] {
        Core::Socket::on_ready_to_read = [this] {
            if (!Core::Socket::is_open() || !Core::Socket::is_connected() || Core::Socket::eof()) {
                // an abrupt closure (the server is a jerk)
                dbg() << "Socket not open, assuming abrupt closure";
                m_context.connection_finished = true;
            }
            if (m_context.critical_error) {
                dbg() << "READ CRITICAL ERROR " << m_context.critical_error << " :(";
                if (on_tls_error)
                    on_tls_error((AlertDescription)m_context.critical_error);
                return;
            }
            if (m_context.application_buffer.size() == 0 && m_context.connection_finished) {
                if (on_tls_finished)
                    on_tls_finished();
                if (m_context.tls_buffer.size()) {
                    dbg() << "connection closed without finishing data transfer, " << m_context.tls_buffer.size() << " bytes still in buffer";
                } else {
                    m_context.connection_finished = false;
                }
                if (!m_context.application_buffer.size())
                    m_context.connection_status = ConnectionStatus::Disconnected;
                return;
            }
            flush();
            consume(Core::Socket::read(4096)); // FIXME: how much is proper?
            if (is_established() && m_context.application_buffer.size())
                if (on_tls_ready_to_read)
                    on_tls_ready_to_read(*this);
        };
        m_write_notifier = Core::Notifier::construct(fd(), Core::Notifier::Event::Write);
        m_write_notifier->on_ready_to_write = [this] {
            if (!Core::Socket::is_open() || !Core::Socket::is_connected() || Core::Socket::eof()) {
                // an abrupt closure (the server is a jerk)
                dbg() << "Socket not open, assuming abrupt closure";
                m_context.connection_finished = true;
            }
            if (m_context.critical_error) {
                dbg() << "WRITE CRITICAL ERROR " << m_context.critical_error << " :(";
                if (on_tls_error)
                    on_tls_error((AlertDescription)m_context.critical_error);
                return;
            }
            if (m_context.connection_finished) {
                if (on_tls_finished)
                    on_tls_finished();
                if (m_context.tls_buffer.size()) {
                    dbg() << "connection closed without finishing data transfer, " << m_context.tls_buffer.size() << " bytes still in buffer";
                } else {
                    m_context.connection_finished = false;
                    dbg() << "FINISHED";
                }
                if (!m_context.application_buffer.size()) {
                    m_context.connection_status = ConnectionStatus::Disconnected;
                    return;
                }
            }
            flush();
            if (is_established() && !m_context.application_buffer.size()) // hey client, you still have stuff to read...
                if (on_tls_ready_to_write)
                    on_tls_ready_to_write(*this);
        };
        if (on_tls_connected)
            on_tls_connected();
    };
    bool success = Core::Socket::common_connect(saddr, length);
    if (!success)
        return false;

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
        dbg() << "Dropping " << write_buffer().size() << " bytes worth of TLS records as max retries has been reached";
        write_buffer().clear();
        m_context.send_retries = 0;
    }
    return false;
}

}

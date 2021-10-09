/*
 * Copyright (c) 2020, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <LibCore/DateTime.h>
#include <LibCore/Timer.h>
#include <LibCrypto/PK/Code/EMSA_PSS.h>
#include <LibTLS/TLSv12.h>

// Each record can hold at most 18432 bytes, leaving some headroom and rounding down to
// a nice number gives us a maximum of 16 KiB for user-supplied application data,
// which will be sent as a single record containing a single ApplicationData message.
constexpr static size_t MaximumApplicationDataChunkSize = 16 * KiB;

namespace TLS {

Optional<ByteBuffer> TLSv12::read()
{
    if (m_context.application_buffer.size()) {
        auto buf = move(m_context.application_buffer);
        return { move(buf) };
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

String TLSv12::read_line(size_t max_size)
{
    if (!can_read_line())
        return {};

    auto* start = m_context.application_buffer.data();
    auto* newline = (u8*)memchr(m_context.application_buffer.data(), '\n', m_context.application_buffer.size());
    VERIFY(newline);

    size_t offset = newline - start;

    if (offset > max_size)
        return {};

    String line { bit_cast<char const*>(start), offset, Chomp };
    m_context.application_buffer = m_context.application_buffer.slice(offset + 1, m_context.application_buffer.size() - offset - 1);

    return line;
}

bool TLSv12::write(ReadonlyBytes buffer)
{
    if (m_context.connection_status != ConnectionStatus::Established) {
        dbgln_if(TLS_DEBUG, "write request while not connected");
        return false;
    }

    for (size_t offset = 0; offset < buffer.size(); offset += MaximumApplicationDataChunkSize) {
        PacketBuilder builder { MessageType::ApplicationData, m_context.options.version, buffer.size() - offset };
        builder.append(buffer.slice(offset, min(buffer.size() - offset, MaximumApplicationDataChunkSize)));
        auto packet = builder.build();

        update_packet(packet);
        write_packet(packet);
    }

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
            VERIFY_NOT_REACHED();
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

        deferred_invoke([&] {
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

void TLSv12::notify_client_for_app_data()
{
    if (m_context.application_buffer.size() > 0) {
        if (!m_has_scheduled_app_data_flush) {
            deferred_invoke([this] { notify_client_for_app_data(); });
            m_has_scheduled_app_data_flush = true;
        }
        if (on_tls_ready_to_read)
            on_tls_ready_to_read(*this);
    } else {
        if (m_context.connection_finished && !m_context.has_invoked_finish_or_error_callback) {
            m_context.has_invoked_finish_or_error_callback = true;
            if (on_tls_finished)
                on_tls_finished();
        }
    }
    m_has_scheduled_app_data_flush = false;
}

void TLSv12::read_from_socket()
{
    // If there's anything before we consume stuff, let the client know
    // since we won't be consuming things if the connection is terminated.
    notify_client_for_app_data();

    ScopeGuard notify_guard {
        [this] {
            // If anything new shows up, tell the client about the event.
            notify_client_for_app_data();
        }
    };

    if (!check_connection_state(true))
        return;

    consume(Core::Socket::read(4 * MiB));
}

void TLSv12::write_into_socket()
{
    dbgln_if(TLS_DEBUG, "Flushing cached records: {} established? {}", m_context.tls_buffer.size(), is_established());

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
    if (m_context.connection_finished)
        return false;

    if (!Core::Socket::is_open() || !Core::Socket::is_connected()) {
        // an abrupt closure (the server is a jerk)
        dbgln_if(TLS_DEBUG, "Socket not open, assuming abrupt closure");
        m_context.connection_finished = true;
        m_context.connection_status = ConnectionStatus::Disconnected;
        Core::Socket::close();
        return false;
    }

    if (read && Core::Socket::eof()) {
        if (m_context.application_buffer.size() == 0 && m_context.connection_status != ConnectionStatus::Disconnected) {
            m_context.has_invoked_finish_or_error_callback = true;
            if (on_tls_finished)
                on_tls_finished();
        }
        return false;
    }

    if (m_context.critical_error) {
        dbgln_if(TLS_DEBUG, "CRITICAL ERROR {} :(", m_context.critical_error);

        m_context.has_invoked_finish_or_error_callback = true;
        if (on_tls_error)
            on_tls_error((AlertDescription)m_context.critical_error);
        m_context.connection_finished = true;
        m_context.connection_status = ConnectionStatus::Disconnected;
        Core::Socket::close();
        return false;
    }
    if (((read && m_context.application_buffer.size() == 0) || !read) && m_context.connection_finished) {
        if (m_context.application_buffer.size() == 0 && m_context.connection_status != ConnectionStatus::Disconnected) {
            m_context.has_invoked_finish_or_error_callback = true;
            if (on_tls_finished)
                on_tls_finished();
        }
        if (m_context.tls_buffer.size()) {
            dbgln_if(TLS_DEBUG, "connection closed without finishing data transfer, {} bytes still in buffer and {} bytes in application buffer",
                m_context.tls_buffer.size(),
                m_context.application_buffer.size());
        }
        if (!m_context.application_buffer.size()) {
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

    if constexpr (TLS_DEBUG) {
        dbgln("SENDING...");
        print_buffer(out_buffer, out_buffer_length);
    }
    if (Core::Socket::write(&out_buffer[out_buffer_index], out_buffer_length)) {
        write_buffer().clear();
        return true;
    }
    if (m_context.send_retries++ == 10) {
        // drop the records, we can't send
        dbgln_if(TLS_DEBUG, "Dropping {} bytes worth of TLS records as max retries has been reached", write_buffer().size());
        write_buffer().clear();
        m_context.send_retries = 0;
    }
    return false;
}

}

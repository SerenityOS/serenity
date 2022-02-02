/*
 * Copyright (c) 2020, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <LibCore/DateTime.h>
#include <LibCore/EventLoop.h>
#include <LibCore/Timer.h>
#include <LibCrypto/PK/Code/EMSA_PSS.h>
#include <LibTLS/TLSv12.h>

// Each record can hold at most 18432 bytes, leaving some headroom and rounding down to
// a nice number gives us a maximum of 16 KiB for user-supplied application data,
// which will be sent as a single record containing a single ApplicationData message.
constexpr static size_t MaximumApplicationDataChunkSize = 16 * KiB;

namespace TLS {

ErrorOr<size_t> TLSv12::read(Bytes bytes)
{
    m_eof = false;
    auto size_to_read = min(bytes.size(), m_context.application_buffer.size());
    if (size_to_read == 0) {
        m_eof = true;
        return 0;
    }

    m_context.application_buffer.span().slice(0, size_to_read).copy_to(bytes);
    m_context.application_buffer = m_context.application_buffer.slice(size_to_read, m_context.application_buffer.size() - size_to_read);
    return size_to_read;
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

ErrorOr<size_t> TLSv12::write(ReadonlyBytes bytes)
{
    if (m_context.connection_status != ConnectionStatus::Established) {
        dbgln_if(TLS_DEBUG, "write request while not connected");
        return AK::Error::from_string_literal("TLS write request while not connected");
    }

    for (size_t offset = 0; offset < bytes.size(); offset += MaximumApplicationDataChunkSize) {
        PacketBuilder builder { MessageType::ApplicationData, m_context.options.version, bytes.size() - offset };
        builder.append(bytes.slice(offset, min(bytes.size() - offset, MaximumApplicationDataChunkSize)));
        auto packet = builder.build();

        update_packet(packet);
        write_packet(packet);
    }

    return bytes.size();
}

ErrorOr<NonnullOwnPtr<TLSv12>> TLSv12::connect(const String& host, u16 port, Options options)
{
    Core::EventLoop loop;
    OwnPtr<Core::Stream::Socket> tcp_socket = TRY(Core::Stream::TCPSocket::connect(host, port));
    TRY(tcp_socket->set_blocking(false));
    auto tls_socket = make<TLSv12>(move(tcp_socket), move(options));
    tls_socket->set_sni(host);
    tls_socket->on_connected = [&] {
        loop.quit(0);
    };
    tls_socket->on_tls_error = [&](auto alert) {
        loop.quit(256 - to_underlying(alert));
    };
    auto result = loop.exec();
    if (result == 0)
        return tls_socket;

    tls_socket->try_disambiguate_error();
    // FIXME: Should return richer information here.
    return AK::Error::from_string_literal(alert_name(static_cast<AlertDescription>(256 - result)));
}

ErrorOr<NonnullOwnPtr<TLSv12>> TLSv12::connect(const String& host, Core::Stream::Socket& underlying_stream, Options options)
{
    StreamVariantType socket { &underlying_stream };
    auto tls_socket = make<TLSv12>(&underlying_stream, move(options));
    tls_socket->set_sni(host);
    Core::EventLoop loop;
    tls_socket->on_connected = [&] {
        loop.quit(0);
    };
    tls_socket->on_tls_error = [&](auto alert) {
        loop.quit(256 - to_underlying(alert));
    };
    auto result = loop.exec();
    if (result == 0)
        return tls_socket;

    tls_socket->try_disambiguate_error();
    // FIXME: Should return richer information here.
    return AK::Error::from_string_literal(alert_name(static_cast<AlertDescription>(256 - result)));
}

void TLSv12::setup_connection()
{
    Core::deferred_invoke([this] {
        auto& stream = underlying_stream();
        stream.on_ready_to_read = [this] {
            auto result = read_from_socket();
            if (result.is_error())
                dbgln("Read error: {}", result.error());
        };

        m_handshake_timeout_timer = Core::Timer::create_single_shot(
            m_max_wait_time_for_handshake_in_seconds * 1000, [&] {
                dbgln("Handshake timeout :(");
                auto timeout_diff = Core::DateTime::now().timestamp() - m_context.handshake_initiation_timestamp;
                // If the timeout duration was actually within the max wait time (with a margin of error),
                // we're not operating slow, so the server timed out.
                // otherwise, it's our fault that the negotiation is taking too long, so extend the timer :P
                if (timeout_diff < m_max_wait_time_for_handshake_in_seconds + 1) {
                    // The server did not respond fast enough,
                    // time the connection out.
                    alert(AlertLevel::Critical, AlertDescription::UserCanceled);
                    m_context.tls_buffer.clear();
                    m_context.error_code = Error::TimedOut;
                    m_context.critical_error = (u8)Error::TimedOut;
                    check_connection_state(false); // Notify the client.
                } else {
                    // Extend the timer, we are too slow.
                    m_handshake_timeout_timer->restart(m_max_wait_time_for_handshake_in_seconds * 1000);
                }
            });
        auto packet = build_hello();
        write_packet(packet);
        write_into_socket();
        m_handshake_timeout_timer->start();
        m_context.handshake_initiation_timestamp = Core::DateTime::now().timestamp();
    });
    m_has_scheduled_write_flush = true;
}

void TLSv12::notify_client_for_app_data()
{
    if (m_context.application_buffer.size() > 0) {
        if (on_ready_to_read)
            on_ready_to_read();
    } else {
        if (m_context.connection_finished && !m_context.has_invoked_finish_or_error_callback) {
            m_context.has_invoked_finish_or_error_callback = true;
            if (on_tls_finished)
                on_tls_finished();
        }
    }
    m_has_scheduled_app_data_flush = false;
}

ErrorOr<void> TLSv12::read_from_socket()
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
        return {};

    u8 buffer[16 * KiB];
    Bytes bytes { buffer, array_size(buffer) };
    size_t nread = 0;
    auto& stream = underlying_stream();
    do {
        auto result = stream.read(bytes);
        if (result.is_error()) {
            if (result.error().is_errno() && result.error().code() != EINTR) {
                if (result.error().code() != EAGAIN)
                    dbgln("TLS Socket read failed, error: {}", result.error());
                break;
            }
            continue;
        }
        nread = result.release_value();
        consume(bytes.slice(0, nread));
    } while (nread > 0 && !m_context.critical_error);

    return {};
}

void TLSv12::write_into_socket()
{
    dbgln_if(TLS_DEBUG, "Flushing cached records: {} established? {}", m_context.tls_buffer.size(), is_established());

    m_has_scheduled_write_flush = false;
    if (!check_connection_state(false))
        return;

    MUST(flush());
}

bool TLSv12::check_connection_state(bool read)
{
    if (m_context.connection_finished)
        return false;

    if (m_context.close_notify)
        m_context.connection_finished = true;

    auto& stream = underlying_stream();

    if (!stream.is_open()) {
        // an abrupt closure (the server is a jerk)
        dbgln_if(TLS_DEBUG, "Socket not open, assuming abrupt closure");
        m_context.connection_finished = true;
        m_context.connection_status = ConnectionStatus::Disconnected;
        close();
        return false;
    }

    if (read && stream.is_eof()) {
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
        close();
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

ErrorOr<bool> TLSv12::flush()
{
    auto out_bytes = m_context.tls_buffer.bytes();

    if (out_bytes.is_empty())
        return true;

    if constexpr (TLS_DEBUG) {
        dbgln("SENDING...");
        print_buffer(out_bytes);
    }

    auto& stream = underlying_stream();
    Optional<AK::Error> error;
    size_t written;
    do {
        auto result = stream.write(out_bytes);
        if (result.is_error() && result.error().code() != EINTR && result.error().code() != EAGAIN) {
            error = result.release_error();
            dbgln("TLS Socket write error: {}", *error);
            break;
        }
        written = result.value();
        out_bytes = out_bytes.slice(written);
    } while (!out_bytes.is_empty());

    if (out_bytes.is_empty() && !error.has_value()) {
        m_context.tls_buffer.clear();
        return true;
    }

    if (m_context.send_retries++ == 10) {
        // drop the records, we can't send
        dbgln_if(TLS_DEBUG, "Dropping {} bytes worth of TLS records as max retries has been reached", m_context.tls_buffer.size());
        m_context.tls_buffer.clear();
        m_context.send_retries = 0;
    }
    return false;
}

void TLSv12::close()
{
    alert(AlertLevel::Critical, AlertDescription::CloseNotify);
    // bye bye.
    m_context.connection_status = ConnectionStatus::Disconnected;
}

}

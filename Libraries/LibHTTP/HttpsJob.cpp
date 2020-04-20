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
#include <LibCore/Gzip.h>
#include <LibHTTP/HttpResponse.h>
#include <LibHTTP/HttpsJob.h>
#include <LibTLS/TLSv12.h>
#include <stdio.h>
#include <unistd.h>

//#define HTTPJOB_DEBUG

namespace HTTP {

static ByteBuffer handle_content_encoding(const ByteBuffer& buf, const String& content_encoding)
{
#ifdef CHTTPJOB_DEBUG
    dbg() << "HttpsJob::handle_content_encoding: buf has content_encoding = " << content_encoding;
#endif

    if (content_encoding == "gzip") {
        if (!Core::Gzip::is_compressed(buf)) {
            dbg() << "HttpsJob::handle_content_encoding: buf is not gzip compressed!";
        }

#ifdef CHTTPJOB_DEBUG
        dbg() << "HttpsJob::handle_content_encoding: buf is gzip compressed!";
#endif

        auto uncompressed = Core::Gzip::decompress(buf);
        if (!uncompressed.has_value()) {
            dbg() << "HttpsJob::handle_content_encoding: Gzip::decompress() failed. Returning original buffer.";
            return buf;
        }

#ifdef CHTTPJOB_DEBUG
        dbg() << "HttpsJob::handle_content_encoding: Gzip::decompress() successful.\n"
              << "  Input size = " << buf.size() << "\n"
              << "  Output size = " << uncompressed.value().size();
#endif

        return uncompressed.value();
    }

    return buf;
}

HttpsJob::HttpsJob(const HttpRequest& request)
    : m_request(request)
{
}

HttpsJob::~HttpsJob()
{
    m_socket = nullptr;
}

void HttpsJob::on_socket_connected()
{

    m_socket->on_tls_ready_to_write = [&](TLS::TLSv12& tls) {
        if (m_sent_data)
            return;
        m_sent_data = true;
        auto raw_request = m_request.to_raw_request();
#if 0
        dbg() << "HttpsJob: raw_request:";
        dbg() << String::copy(raw_request).characters();
#endif
        bool success = tls.write(raw_request);
        if (!success)
            deferred_invoke([this](auto&) { did_fail(Core::NetworkJob::Error::TransmissionFailed); });
    };

    m_socket->on_tls_ready_to_read = [&](TLS::TLSv12& tls) {
        dbg() << " ON TLS READY TO READ: " << (u16)m_state;
        if (is_cancelled())
            return;
        if (m_state == State::InStatus) {
            if (!tls.can_read_line()) {
                dbg() << " cannot read line";
                return;
            }
            auto line = tls.read_line(PAGE_SIZE);
            if (line.is_null()) {
                fprintf(stderr, "HttpsJob: Expected HTTP status\n");
                return deferred_invoke([this](auto&) { did_fail(Core::NetworkJob::Error::TransmissionFailed); });
            }
            auto parts = String::copy(line, Chomp).split(' ');
            if (parts.size() < 3) {
                fprintf(stderr, "HttpsJob: Expected 3-part HTTP status, got '%s'\n", line.data());
                return deferred_invoke([this](auto&) { did_fail(Core::NetworkJob::Error::ProtocolFailed); });
            }
            bool ok;
            m_code = parts[1].to_uint(ok);
            if (!ok) {
                fprintf(stderr, "HttpsJob: Expected numeric HTTP status\n");
                return deferred_invoke([this](auto&) { did_fail(Core::NetworkJob::Error::ProtocolFailed); });
            }
            m_state = State::InHeaders;
            return;
        }
        if (m_state == State::InHeaders) {
            if (!tls.can_read_line())
                return;
            auto line = tls.read_line(PAGE_SIZE);
            if (line.is_null()) {
                fprintf(stderr, "HttpsJob: Expected HTTP header\n");
                return did_fail(Core::NetworkJob::Error::ProtocolFailed);
            }
            auto chomped_line = String::copy(line, Chomp);
            if (chomped_line.is_empty()) {
                m_state = State::InBody;
                return;
            }
            auto parts = chomped_line.split(':');
            if (parts.is_empty()) {
                fprintf(stderr, "HttpsJob: Expected HTTP header with key/value\n");
                return deferred_invoke([this](auto&) { did_fail(Core::NetworkJob::Error::ProtocolFailed); });
            }
            auto name = parts[0];
            if (chomped_line.length() < name.length() + 2) {
                fprintf(stderr, "HttpsJob: Malformed HTTP header: '%s' (%zu)\n", chomped_line.characters(), chomped_line.length());
                return deferred_invoke([this](auto&) { did_fail(Core::NetworkJob::Error::ProtocolFailed); });
            }
            auto value = chomped_line.substring(name.length() + 2, chomped_line.length() - name.length() - 2);
            m_headers.set(name, value);
#ifdef CHTTPJOB_DEBUG
            dbg() << "HttpsJob: [" << name << "] = '" << value << "'";
#endif
            return;
        }
        ASSERT(m_state == State::InBody);
        ASSERT(tls.can_read());
        auto payload = tls.read(64 * KB);
        if (!payload) {
            if (tls.eof())
                return finish_up();
            return deferred_invoke([this](auto&) { did_fail(Core::NetworkJob::Error::ProtocolFailed); });
        }
        dbg() << "Read payload, " << payload.size() << " bytes";
        m_received_buffers.append(payload);
        m_received_size += payload.size();

        auto content_length_header = m_headers.get("Content-Length");
        if (content_length_header.has_value()) {
            dbg() << "content length is " << content_length_header.value() << ", we have " << m_received_size;
            bool ok;
            if (m_received_size >= content_length_header.value().to_uint(ok) && ok)
                finish_up();
        } else {
            // no content-length, assume closed connection
            finish_up();
        }
    };
}

void HttpsJob::finish_up()
{
    m_state = State::Finished;
    auto flattened_buffer = ByteBuffer::create_uninitialized(m_received_size);
    u8* flat_ptr = flattened_buffer.data();
    for (auto& received_buffer : m_received_buffers) {
        memcpy(flat_ptr, received_buffer.data(), received_buffer.size());
        flat_ptr += received_buffer.size();
    }
    m_received_buffers.clear();

    auto content_encoding = m_headers.get("Content-Encoding");
    if (content_encoding.has_value()) {
        flattened_buffer = handle_content_encoding(flattened_buffer, content_encoding.value());
    }

    auto response = HttpResponse::create(m_code, move(m_headers), move(flattened_buffer));
    deferred_invoke([this, response](auto&) {
        did_finish(move(response));
    });
}

void HttpsJob::start()
{
    ASSERT(!m_socket);
    m_socket = TLS::TLSv12::construct(this);
    m_socket->on_tls_connected = [this] {
#ifdef CHTTPJOB_DEBUG
        dbg() << "HttpsJob: on_connected callback";
#endif
        on_socket_connected();
    };
    m_socket->on_tls_error = [&](TLS::AlertDescription error) {
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
    m_socket->on_tls_finished = [&] {
        finish_up();
    };
    bool success = ((TLS::TLSv12&)*m_socket).connect(m_request.url().host(), m_request.url().port());
    if (!success) {
        deferred_invoke([this](auto&) {
            return did_fail(Core::NetworkJob::Error::ConnectionFailed);
        });
    }
}

void HttpsJob::shutdown()
{
    if (!m_socket)
        return;
    m_socket->on_tls_ready_to_read = nullptr;
    m_socket->on_tls_connected = nullptr;
    remove_child(*m_socket);
    m_socket = nullptr;
}
}

/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <LibCore/Gzip.h>
#include <LibCore/TCPSocket.h>
#include <LibHTTP/HttpResponse.h>
#include <LibHTTP/Job.h>
#include <stdio.h>
#include <unistd.h>

//#define JOB_DEBUG

namespace HTTP {

static ByteBuffer handle_content_encoding(const ByteBuffer& buf, const String& content_encoding)
{
#ifdef JOB_DEBUG
    dbg() << "Job::handle_content_encoding: buf has content_encoding = " << content_encoding;
#endif

    if (content_encoding == "gzip") {
        if (!Core::Gzip::is_compressed(buf)) {
            dbg() << "Job::handle_content_encoding: buf is not gzip compressed!";
        }

#ifdef JOB_DEBUG
        dbg() << "Job::handle_content_encoding: buf is gzip compressed!";
#endif

        auto uncompressed = Core::Gzip::decompress(buf);
        if (!uncompressed.has_value()) {
            dbg() << "Job::handle_content_encoding: Gzip::decompress() failed. Returning original buffer.";
            return buf;
        }

#ifdef JOB_DEBUG
        dbg() << "Job::handle_content_encoding: Gzip::decompress() successful.\n"
              << "  Input size = " << buf.size() << "\n"
              << "  Output size = " << uncompressed.value().size();
#endif

        return uncompressed.value();
    }

    return buf;
}

Job::Job(const HttpRequest& request)
    : m_request(request)
{
}

Job::~Job()
{
}

void Job::on_socket_connected()
{
    register_on_ready_to_write([&] {
        if (m_sent_data)
            return;
        m_sent_data = true;
        auto raw_request = m_request.to_raw_request();
#ifdef JOB_DEBUG
        dbg() << "Job: raw_request:";
        dbg() << String::copy(raw_request).characters();
#endif
        bool success = write(raw_request);
        if (!success)
            deferred_invoke([this](auto&) { did_fail(Core::NetworkJob::Error::TransmissionFailed); });
    });
    register_on_ready_to_read([&] {
        if (is_cancelled())
            return;
        if (m_state == State::InStatus) {
            if (!can_read_line())
                return;
            auto line = read_line(PAGE_SIZE);
            if (line.is_null()) {
                fprintf(stderr, "Job: Expected HTTP status\n");
                return deferred_invoke([this](auto&) { did_fail(Core::NetworkJob::Error::TransmissionFailed); });
            }
            auto parts = String::copy(line, Chomp).split(' ');
            if (parts.size() < 3) {
                fprintf(stderr, "Job: Expected 3-part HTTP status, got '%s'\n", line.data());
                return deferred_invoke([this](auto&) { did_fail(Core::NetworkJob::Error::ProtocolFailed); });
            }
            bool ok;
            m_code = parts[1].to_uint(ok);
            if (!ok) {
                fprintf(stderr, "Job: Expected numeric HTTP status\n");
                return deferred_invoke([this](auto&) { did_fail(Core::NetworkJob::Error::ProtocolFailed); });
            }
            m_state = State::InHeaders;
            return;
        }
        if (m_state == State::InHeaders) {
            if (!can_read_line())
                return;
            auto line = read_line(PAGE_SIZE);
            if (line.is_null()) {
                fprintf(stderr, "Job: Expected HTTP header\n");
                return did_fail(Core::NetworkJob::Error::ProtocolFailed);
            }
            auto chomped_line = String::copy(line, Chomp);
            if (chomped_line.is_empty()) {
                m_state = State::InBody;
                return;
            }
            auto parts = chomped_line.split(':');
            if (parts.is_empty()) {
                fprintf(stderr, "Job: Expected HTTP header with key/value\n");
                return deferred_invoke([this](auto&) { did_fail(Core::NetworkJob::Error::ProtocolFailed); });
            }
            auto name = parts[0];
            if (chomped_line.length() < name.length() + 2) {
                fprintf(stderr, "Job: Malformed HTTP header: '%s' (%zu)\n", chomped_line.characters(), chomped_line.length());
                return deferred_invoke([this](auto&) { did_fail(Core::NetworkJob::Error::ProtocolFailed); });
            }
            auto value = chomped_line.substring(name.length() + 2, chomped_line.length() - name.length() - 2);
            m_headers.set(name, value);
#ifdef JOB_DEBUG
            dbg() << "Job: [" << name << "] = '" << value << "'";
#endif
            return;
        }
        ASSERT(m_state == State::InBody);
        ASSERT(can_read());

        read_while_data_available([&] {
            auto payload = receive(64 * KB);
            if (!payload) {
                if (eof()) {
                    finish_up();
                    return IterationDecision::Break;
                }

                if (should_fail_on_empty_payload()) {
                    deferred_invoke([this](auto&) { did_fail(Core::NetworkJob::Error::ProtocolFailed); });
                    return IterationDecision::Break;
                }
            }
            m_received_buffers.append(payload);
            m_received_size += payload.size();

            auto content_length_header = m_headers.get("Content-Length");
            Optional<u32> content_length {};

            if (content_length_header.has_value()) {
                bool ok;
                auto length = content_length_header.value().to_uint(ok);
                if (ok)
                    content_length = length;
            }

            did_progress(content_length, m_received_size);

            if (content_length.has_value()) {
                auto length = content_length.value();
                if (m_received_size >= length) {
                    m_received_size = length;
                    finish_up();
                    return IterationDecision::Break;
                }
            }
            return IterationDecision::Continue;
        });

        if (!is_established()) {
#ifdef JOB_DEBUG
            dbg() << "Connection appears to have closed, finishing up";
#endif
            finish_up();
        }
    });
}

void Job::finish_up()
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
}

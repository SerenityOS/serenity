/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <LibCompress/Gzip.h>
#include <LibCompress/Zlib.h>
#include <LibCore/Event.h>
#include <LibCore/TCPSocket.h>
#include <LibHTTP/HttpResponse.h>
#include <LibHTTP/Job.h>
#include <stdio.h>
#include <unistd.h>

namespace HTTP {

static ByteBuffer handle_content_encoding(const ByteBuffer& buf, const String& content_encoding)
{
    dbgln_if(JOB_DEBUG, "Job::handle_content_encoding: buf has content_encoding={}", content_encoding);

    if (content_encoding == "gzip") {
        if (!Compress::GzipDecompressor::is_likely_compressed(buf)) {
            dbgln("Job::handle_content_encoding: buf is not gzip compressed!");
        }

        dbgln_if(JOB_DEBUG, "Job::handle_content_encoding: buf is gzip compressed!");

        auto uncompressed = Compress::GzipDecompressor::decompress_all(buf);
        if (!uncompressed.has_value()) {
            dbgln("Job::handle_content_encoding: Gzip::decompress() failed. Returning original buffer.");
            return buf;
        }

        if constexpr (JOB_DEBUG) {
            dbgln("Job::handle_content_encoding: Gzip::decompress() successful.");
            dbgln("  Input size: {}", buf.size());
            dbgln("  Output size: {}", uncompressed.value().size());
        }

        return uncompressed.value();
    } else if (content_encoding == "deflate") {
        dbgln_if(JOB_DEBUG, "Job::handle_content_encoding: buf is deflate compressed!");

        // Even though the content encoding is "deflate", it's actually deflate with the zlib wrapper.
        // https://tools.ietf.org/html/rfc7230#section-4.2.2
        auto uncompressed = Compress::Zlib::decompress_all(buf);
        if (!uncompressed.has_value()) {
            // From the RFC:
            // "Note: Some non-conformant implementations send the "deflate"
            //        compressed data without the zlib wrapper."
            dbgln_if(JOB_DEBUG, "Job::handle_content_encoding: Zlib::decompress_all() failed. Trying DeflateDecompressor::decompress_all()");
            uncompressed = Compress::DeflateDecompressor::decompress_all(buf);

            if (!uncompressed.has_value()) {
                dbgln("Job::handle_content_encoding: DeflateDecompressor::decompress_all() failed, returning original buffer.");
                return buf;
            }
        }

        if constexpr (JOB_DEBUG) {
            dbgln("Job::handle_content_encoding: Deflate decompression successful.");
            dbgln("  Input size: {}", buf.size());
            dbgln("  Output size: {}", uncompressed.value().size());
        }

        return uncompressed.value();
    }

    return buf;
}

Job::Job(const HttpRequest& request, OutputStream& output_stream)
    : Core::NetworkJob(output_stream)
    , m_request(request)
{
}

Job::~Job()
{
}

void Job::flush_received_buffers()
{
    if (!m_can_stream_response || m_buffered_size == 0)
        return;
    dbgln_if(JOB_DEBUG, "Job: Flushing received buffers: have {} bytes in {} buffers", m_buffered_size, m_received_buffers.size());
    for (size_t i = 0; i < m_received_buffers.size(); ++i) {
        auto& payload = m_received_buffers[i];
        auto written = do_write(payload);
        m_buffered_size -= written;
        if (written == payload.size()) {
            // FIXME: Make this a take-first-friendly object?
            m_received_buffers.take_first();
            --i;
            continue;
        }
        VERIFY(written < payload.size());
        payload = payload.slice(written, payload.size() - written);
        break;
    }
    dbgln_if(JOB_DEBUG, "Job: Flushing received buffers done: have {} bytes in {} buffers", m_buffered_size, m_received_buffers.size());
}

void Job::on_socket_connected()
{
    register_on_ready_to_write([&] {
        if (m_sent_data)
            return;
        m_sent_data = true;
        auto raw_request = m_request.to_raw_request();

        if constexpr (JOB_DEBUG) {
            dbgln("Job: raw_request:");
            dbgln("{}", String::copy(raw_request));
        }

        bool success = write(raw_request);
        if (!success)
            deferred_invoke([this](auto&) { did_fail(Core::NetworkJob::Error::TransmissionFailed); });
    });
    register_on_ready_to_read([&] {
        if (is_cancelled())
            return;

        if (m_state == State::Finished) {
            // We have everything we want, at this point, we can either get an EOF, or a bunch of extra newlines
            // (unless "Connection: close" isn't specified)
            // So just ignore everything after this.
            return;
        }

        if (m_state == State::InStatus) {
            if (!can_read_line())
                return;
            auto line = read_line(PAGE_SIZE);
            if (line.is_null()) {
                warnln("Job: Expected HTTP status");
                return deferred_invoke([this](auto&) { did_fail(Core::NetworkJob::Error::TransmissionFailed); });
            }
            auto parts = line.split_view(' ');
            if (parts.size() < 3) {
                warnln("Job: Expected 3-part HTTP status, got '{}'", line);
                return deferred_invoke([this](auto&) { did_fail(Core::NetworkJob::Error::ProtocolFailed); });
            }
            auto code = parts[1].to_uint();
            if (!code.has_value()) {
                warnln("Job: Expected numeric HTTP status");
                return deferred_invoke([this](auto&) { did_fail(Core::NetworkJob::Error::ProtocolFailed); });
            }
            m_code = code.value();
            m_state = State::InHeaders;
            return;
        }
        if (m_state == State::InHeaders || m_state == State::Trailers) {
            if (!can_read_line())
                return;
            auto line = read_line(PAGE_SIZE);
            if (line.is_null()) {
                if (m_state == State::Trailers) {
                    // Some servers like to send two ending chunks
                    // use this fact as an excuse to ignore anything after the last chunk
                    // that is not a valid trailing header.
                    return finish_up();
                }
                warnln("Job: Expected HTTP header");
                return did_fail(Core::NetworkJob::Error::ProtocolFailed);
            }
            if (line.is_empty()) {
                if (m_state == State::Trailers) {
                    return finish_up();
                } else {
                    if (on_headers_received)
                        on_headers_received(m_headers, m_code > 0 ? m_code : Optional<u32> {});
                    m_state = State::InBody;
                }
                return;
            }
            auto parts = line.split_view(':');
            if (parts.is_empty()) {
                if (m_state == State::Trailers) {
                    // Some servers like to send two ending chunks
                    // use this fact as an excuse to ignore anything after the last chunk
                    // that is not a valid trailing header.
                    return finish_up();
                }
                warnln("Job: Expected HTTP header with key/value");
                return deferred_invoke([this](auto&) { did_fail(Core::NetworkJob::Error::ProtocolFailed); });
            }
            auto name = parts[0];
            if (line.length() < name.length() + 2) {
                if (m_state == State::Trailers) {
                    // Some servers like to send two ending chunks
                    // use this fact as an excuse to ignore anything after the last chunk
                    // that is not a valid trailing header.
                    return finish_up();
                }
                warnln("Job: Malformed HTTP header: '{}' ({})", line, line.length());
                return deferred_invoke([this](auto&) { did_fail(Core::NetworkJob::Error::ProtocolFailed); });
            }
            auto value = line.substring(name.length() + 2, line.length() - name.length() - 2);
            m_headers.set(name, value);
            if (name.equals_ignoring_case("Content-Encoding")) {
                // Assume that any content-encoding means that we can't decode it as a stream :(
                dbgln_if(JOB_DEBUG, "Content-Encoding {} detected, cannot stream output :(", value);
                m_can_stream_response = false;
            }
            dbgln_if(JOB_DEBUG, "Job: [{}] = '{}'", name, value);
            return;
        }
        VERIFY(m_state == State::InBody);
        VERIFY(can_read());

        read_while_data_available([&] {
            auto read_size = 64 * KiB;
            if (m_current_chunk_remaining_size.has_value()) {
            read_chunk_size:;
                auto remaining = m_current_chunk_remaining_size.value();
                if (remaining == -1) {
                    // read size
                    auto size_data = read_line(PAGE_SIZE);
                    if (m_should_read_chunk_ending_line) {
                        VERIFY(size_data.is_empty());
                        m_should_read_chunk_ending_line = false;
                        return IterationDecision::Continue;
                    }
                    auto size_lines = size_data.view().lines();
                    dbgln_if(JOB_DEBUG, "Job: Received a chunk with size '{}'", size_data);
                    if (size_lines.size() == 0) {
                        dbgln("Job: Reached end of stream");
                        finish_up();
                        return IterationDecision::Break;
                    } else {
                        auto chunk = size_lines[0].split_view(';', true);
                        String size_string = chunk[0];
                        char* endptr;
                        auto size = strtoul(size_string.characters(), &endptr, 16);
                        if (*endptr) {
                            // invalid number
                            deferred_invoke([this](auto&) { did_fail(Core::NetworkJob::Error::TransmissionFailed); });
                            return IterationDecision::Break;
                        }
                        if (size == 0) {
                            // This is the last chunk
                            // '0' *[; chunk-ext-name = chunk-ext-value]
                            // We're going to ignore _all_ chunk extensions
                            read_size = 0;
                            m_current_chunk_total_size = 0;
                            m_current_chunk_remaining_size = 0;

                            dbgln_if(JOB_DEBUG, "Job: Received the last chunk with extensions '{}'", size_string.substring_view(1, size_string.length() - 1));
                        } else {
                            m_current_chunk_total_size = size;
                            m_current_chunk_remaining_size = size;
                            read_size = size;

                            dbgln_if(JOB_DEBUG, "Job: Chunk of size '{}' started", size);
                        }
                    }
                } else {
                    read_size = remaining;

                    dbgln_if(JOB_DEBUG, "Job: Resuming chunk with '{}' bytes left over", remaining);
                }
            } else {
                auto transfer_encoding = m_headers.get("Transfer-Encoding");
                if (transfer_encoding.has_value()) {
                    // Note: Some servers add extra spaces around 'chunked', see #6302.
                    auto encoding = transfer_encoding.value().trim_whitespace();

                    dbgln_if(JOB_DEBUG, "Job: This content has transfer encoding '{}'", encoding);
                    if (encoding.equals_ignoring_case("chunked")) {
                        m_current_chunk_remaining_size = -1;
                        goto read_chunk_size;
                    } else {
                        dbgln("Job: Unknown transfer encoding '{}', the result will likely be wrong!", encoding);
                    }
                }
            }

            auto payload = receive(read_size);
            if (payload.is_empty()) {
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
            m_buffered_size += payload.size();
            m_received_size += payload.size();
            flush_received_buffers();

            if (m_current_chunk_remaining_size.has_value()) {
                auto size = m_current_chunk_remaining_size.value() - payload.size();

                dbgln_if(JOB_DEBUG, "Job: We have {} bytes left over in this chunk", size);
                if (size == 0) {
                    dbgln_if(JOB_DEBUG, "Job: Finished a chunk of {} bytes", m_current_chunk_total_size.value());

                    if (m_current_chunk_total_size.value() == 0) {
                        m_state = State::Trailers;
                        return IterationDecision::Break;
                    }

                    // we've read everything, now let's get the next chunk
                    size = -1;
                    if (can_read_line()) {
                        auto line = read_line(PAGE_SIZE);
                        VERIFY(line.is_empty());
                    } else {
                        m_should_read_chunk_ending_line = true;
                    }
                }
                m_current_chunk_remaining_size = size;
            }

            auto content_length_header = m_headers.get("Content-Length");
            Optional<u32> content_length {};

            if (content_length_header.has_value()) {
                auto length = content_length_header.value().to_uint();
                if (length.has_value())
                    content_length = length.value();
            }

            deferred_invoke([this, content_length](auto&) { did_progress(content_length, m_received_size); });

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
            dbgln_if(JOB_DEBUG, "Connection appears to have closed, finishing up");
            finish_up();
        }
    });
}

void Job::timer_event(Core::TimerEvent& event)
{
    event.accept();
    finish_up();
    if (m_buffered_size == 0)
        stop_timer();
}

void Job::finish_up()
{
    m_state = State::Finished;
    if (!m_can_stream_response) {
        auto flattened_buffer = ByteBuffer::create_uninitialized(m_received_size);
        u8* flat_ptr = flattened_buffer.data();
        for (auto& received_buffer : m_received_buffers) {
            memcpy(flat_ptr, received_buffer.data(), received_buffer.size());
            flat_ptr += received_buffer.size();
        }
        m_received_buffers.clear();

        // For the time being, we cannot stream stuff with content-encoding set to _anything_.
        // FIXME: LibCompress exposes a streaming interface, so this can be resolved
        auto content_encoding = m_headers.get("Content-Encoding");
        if (content_encoding.has_value()) {
            flattened_buffer = handle_content_encoding(flattened_buffer, content_encoding.value());
        }

        m_buffered_size = flattened_buffer.size();
        m_received_buffers.append(move(flattened_buffer));
        m_can_stream_response = true;
    }

    flush_received_buffers();
    if (m_buffered_size != 0) {
        // We have to wait for the client to consume all the downloaded data
        // before we can actually call `did_finish`. in a normal flow, this should
        // never be hit since the client is reading as we are writing, unless there
        // are too many concurrent downloads going on.
        dbgln_if(JOB_DEBUG, "Flush finished with {} bytes remaining, will try again later", m_buffered_size);
        if (!has_timer())
            start_timer(50);
        return;
    }

    auto response = HttpResponse::create(m_code, move(m_headers));
    deferred_invoke([this, response](auto&) {
        did_finish(move(response));
    });
}
}

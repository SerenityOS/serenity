/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/JsonArray.h>
#include <LibCompress/Gzip.h>
#include <LibCompress/Zlib.h>
#include <LibCore/Event.h>
#include <LibHTTP/HttpResponse.h>
#include <LibHTTP/Job.h>
#include <stdio.h>
#include <unistd.h>

namespace HTTP {

static Optional<ByteBuffer> handle_content_encoding(const ByteBuffer& buf, const String& content_encoding)
{
    dbgln_if(JOB_DEBUG, "Job::handle_content_encoding: buf has content_encoding={}", content_encoding);

    if (content_encoding == "gzip") {
        if (!Compress::GzipDecompressor::is_likely_compressed(buf)) {
            dbgln("Job::handle_content_encoding: buf is not gzip compressed!");
        }

        dbgln_if(JOB_DEBUG, "Job::handle_content_encoding: buf is gzip compressed!");

        auto uncompressed = Compress::GzipDecompressor::decompress_all(buf);
        if (!uncompressed.has_value()) {
            dbgln("Job::handle_content_encoding: Gzip::decompress() failed.");
            return {};
        }

        if constexpr (JOB_DEBUG) {
            dbgln("Job::handle_content_encoding: Gzip::decompress() successful.");
            dbgln("  Input size: {}", buf.size());
            dbgln("  Output size: {}", uncompressed.value().size());
        }

        return uncompressed.release_value();
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
                dbgln("Job::handle_content_encoding: DeflateDecompressor::decompress_all() failed.");
                return {};
            }
        }

        if constexpr (JOB_DEBUG) {
            dbgln("Job::handle_content_encoding: Deflate decompression successful.");
            dbgln("  Input size: {}", buf.size());
            dbgln("  Output size: {}", uncompressed.value().size());
        }

        return uncompressed.release_value();
    }

    return buf;
}

Job::Job(HttpRequest&& request, Core::Stream::Stream& output_stream)
    : Core::NetworkJob(output_stream)
    , m_request(move(request))
{
}

Job::~Job()
{
}

void Job::start(Core::Stream::Socket& socket)
{
    VERIFY(!m_socket);
    m_socket = static_cast<Core::Stream::BufferedSocketBase*>(&socket);
    dbgln_if(HTTPJOB_DEBUG, "Reusing previous connection for {}", url());
    deferred_invoke([this] {
        dbgln_if(HTTPJOB_DEBUG, "HttpJob: on_connected callback");
        on_socket_connected();
    });
}

void Job::shutdown(ShutdownMode mode)
{
    if (!m_socket)
        return;
    if (mode == ShutdownMode::CloseSocket) {
        m_socket->close();
    } else {
        m_socket->on_ready_to_read = nullptr;
        m_socket = nullptr;
    }
}

void Job::flush_received_buffers()
{
    if (!m_can_stream_response || m_buffered_size == 0)
        return;
    dbgln_if(JOB_DEBUG, "Job: Flushing received buffers: have {} bytes in {} buffers for {}", m_buffered_size, m_received_buffers.size(), m_request.url());
    for (size_t i = 0; i < m_received_buffers.size(); ++i) {
        auto& payload = m_received_buffers[i].pending_flush;
        auto result = do_write(payload);
        if (result.is_error()) {
            if (!result.error().is_errno()) {
                dbgln_if(JOB_DEBUG, "Job: Failed to flush received buffers: {}", result.error());
                continue;
            }
            if (result.error().code() == EINTR) {
                i--;
                continue;
            }
            break;
        }
        auto written = result.release_value();
        m_buffered_size -= written;
        if (written == payload.size()) {
            // FIXME: Make this a take-first-friendly object?
            (void)m_received_buffers.take_first();
            --i;
            continue;
        }
        VERIFY(written < payload.size());
        payload = payload.slice(written, payload.size() - written);
        break;
    }
    dbgln_if(JOB_DEBUG, "Job: Flushing received buffers done: have {} bytes in {} buffers for {}", m_buffered_size, m_received_buffers.size(), m_request.url());
}

void Job::register_on_ready_to_read(Function<void()> callback)
{
    m_socket->on_ready_to_read = [this, callback = move(callback)] {
        callback();

        // As `m_socket` is a buffered object, we might not get notifications for data in the buffer
        // so exhaust the buffer to ensure we don't end up waiting forever.
        auto can_read_without_blocking = m_socket->can_read_without_blocking();
        if (can_read_without_blocking.is_error())
            return deferred_invoke([this] { did_fail(Core::NetworkJob::Error::TransmissionFailed); });
        if (can_read_without_blocking.value() && m_state != State::Finished && !has_error()) {
            deferred_invoke([this] {
                if (m_socket && m_socket->on_ready_to_read)
                    m_socket->on_ready_to_read();
            });
        }
    };
}

ErrorOr<String> Job::read_line(size_t size)
{
    auto buffer = TRY(ByteBuffer::create_uninitialized(size));
    auto nread = TRY(m_socket->read_until(buffer, "\r\n"sv));
    return String::copy(buffer.span().slice(0, nread));
}

ErrorOr<ByteBuffer> Job::receive(size_t size)
{
    if (size == 0)
        return ByteBuffer {};

    auto buffer = TRY(ByteBuffer::create_uninitialized(size));
    size_t nread;
    do {
        auto result = m_socket->read(buffer);
        if (result.is_error() && result.error().is_errno() && result.error().code() == EINTR)
            continue;
        nread = TRY(result);
        break;
    } while (true);
    return buffer.slice(0, nread);
}

void Job::on_socket_connected()
{
    auto raw_request = m_request.to_raw_request();

    if constexpr (JOB_DEBUG) {
        dbgln("Job: raw_request:");
        dbgln("{}", String::copy(raw_request));
    }

    bool success = m_socket->write_or_error(raw_request);
    if (!success)
        deferred_invoke([this] { did_fail(Core::NetworkJob::Error::TransmissionFailed); });

    register_on_ready_to_read([&] {
        dbgln_if(JOB_DEBUG, "Ready to read for {}, state = {}, cancelled = {}", m_request.url(), to_underlying(m_state), is_cancelled());
        if (is_cancelled())
            return;

        if (m_state == State::Finished) {
            // We have everything we want, at this point, we can either get an EOF, or a bunch of extra newlines
            // (unless "Connection: close" isn't specified)
            // So just ignore everything after this.
            return;
        }

        if (m_socket->is_eof()) {
            dbgln_if(JOB_DEBUG, "Read failure: Actually EOF!");
            return deferred_invoke([this] { did_fail(Core::NetworkJob::Error::ProtocolFailed); });
        }

        while (m_state == State::InStatus) {
            auto can_read_line = m_socket->can_read_line();
            if (can_read_line.is_error()) {
                dbgln_if(JOB_DEBUG, "Job {} could not figure out whether we could read a line", m_request.url());
                return deferred_invoke([this] { did_fail(Core::NetworkJob::Error::TransmissionFailed); });
            }

            if (!can_read_line.value()) {
                dbgln_if(JOB_DEBUG, "Job {} cannot read line", m_request.url());
                auto maybe_buf = receive(64);
                if (maybe_buf.is_error()) {
                    dbgln_if(JOB_DEBUG, "Job {} cannot read any bytes!", m_request.url());
                    return deferred_invoke([this] { did_fail(Core::NetworkJob::Error::TransmissionFailed); });
                }

                dbgln_if(JOB_DEBUG, "{} bytes was read", maybe_buf.value().bytes().size());
                return;
            }
            auto maybe_line = read_line(PAGE_SIZE);
            if (maybe_line.is_error()) {
                dbgln_if(JOB_DEBUG, "Job {} could not read line: {}", m_request.url(), maybe_line.error());
                return deferred_invoke([this] { did_fail(Core::NetworkJob::Error::TransmissionFailed); });
            }

            auto line = maybe_line.release_value();

            dbgln_if(JOB_DEBUG, "Job {} read line of length {}", m_request.url(), line.length());
            if (line.is_null()) {
                dbgln("Job: Expected HTTP status");
                return deferred_invoke([this] { did_fail(Core::NetworkJob::Error::TransmissionFailed); });
            }
            auto parts = line.split_view(' ');
            if (parts.size() < 2) {
                dbgln("Job: Expected 2-part or 3-part HTTP status line, got '{}'", line);
                return deferred_invoke([this] { did_fail(Core::NetworkJob::Error::ProtocolFailed); });
            }
            auto code = parts[1].to_uint();
            if (!code.has_value()) {
                dbgln("Job: Expected numeric HTTP status");
                return deferred_invoke([this] { did_fail(Core::NetworkJob::Error::ProtocolFailed); });
            }
            m_code = code.value();
            m_state = State::InHeaders;

            auto can_read_without_blocking = m_socket->can_read_without_blocking();
            if (can_read_without_blocking.is_error())
                return deferred_invoke([this] { did_fail(Core::NetworkJob::Error::TransmissionFailed); });

            if (!can_read_without_blocking.value())
                return;
        }
        while (m_state == State::InHeaders || m_state == State::Trailers) {
            auto can_read_line = m_socket->can_read_line();
            if (can_read_line.is_error()) {
                dbgln_if(JOB_DEBUG, "Job {} could not figure out whether we could read a line", m_request.url());
                return deferred_invoke([this] { did_fail(Core::NetworkJob::Error::TransmissionFailed); });
            }

            if (!can_read_line.value()) {
                dbgln_if(JOB_DEBUG, "Can't read lines anymore :(");
                return;
            }

            // There's no max limit defined on headers, but for our sanity, let's limit it to 32K.
            auto maybe_line = read_line(32 * KiB);
            if (maybe_line.is_error()) {
                dbgln_if(JOB_DEBUG, "Job {} could not read a header line: {}", m_request.url(), maybe_line.error());
                return deferred_invoke([this] { did_fail(Core::NetworkJob::Error::TransmissionFailed); });
            }
            auto line = maybe_line.release_value();

            if (line.is_null()) {
                if (m_state == State::Trailers) {
                    // Some servers like to send two ending chunks
                    // use this fact as an excuse to ignore anything after the last chunk
                    // that is not a valid trailing header.
                    return finish_up();
                }
                dbgln("Job: Expected HTTP header");
                return did_fail(Core::NetworkJob::Error::ProtocolFailed);
            }
            if (line.is_empty()) {
                if (m_state == State::Trailers) {
                    return finish_up();
                }
                if (on_headers_received) {
                    if (!m_set_cookie_headers.is_empty())
                        m_headers.set("Set-Cookie", JsonArray { m_set_cookie_headers }.to_string());
                    on_headers_received(m_headers, m_code > 0 ? m_code : Optional<u32> {});
                }
                m_state = State::InBody;

                // We've reached the end of the headers, there's a possibility that the server
                // responds with nothing (content-length = 0 with normal encoding); if that's the case,
                // quit early as we won't be reading anything anyway.
                if (auto result = m_headers.get("Content-Length"sv).value_or(""sv).to_uint(); result.has_value()) {
                    if (result.value() == 0 && !m_headers.get("Transfer-Encoding"sv).value_or(""sv).view().trim_whitespace().equals_ignoring_case("chunked"sv))
                        return finish_up();
                }
                // There's also the possibility that the server responds with 204 (No Content),
                // and manages to set a Content-Length anyway, in such cases ignore Content-Length and quit early;
                // As the HTTP spec explicitly prohibits presence of Content-Length when the response code is 204.
                if (m_code == 204)
                    return finish_up();

                break;
            }
            auto parts = line.split_view(':');
            if (parts.is_empty()) {
                if (m_state == State::Trailers) {
                    // Some servers like to send two ending chunks
                    // use this fact as an excuse to ignore anything after the last chunk
                    // that is not a valid trailing header.
                    return finish_up();
                }
                dbgln("Job: Expected HTTP header with key/value");
                return deferred_invoke([this] { did_fail(Core::NetworkJob::Error::ProtocolFailed); });
            }
            auto name = parts[0];
            if (line.length() < name.length() + 2) {
                if (m_state == State::Trailers) {
                    // Some servers like to send two ending chunks
                    // use this fact as an excuse to ignore anything after the last chunk
                    // that is not a valid trailing header.
                    return finish_up();
                }
                dbgln("Job: Malformed HTTP header: '{}' ({})", line, line.length());
                return deferred_invoke([this] { did_fail(Core::NetworkJob::Error::ProtocolFailed); });
            }
            auto value = line.substring(name.length() + 2, line.length() - name.length() - 2);
            if (name.equals_ignoring_case("Set-Cookie")) {
                dbgln_if(JOB_DEBUG, "Job: Received Set-Cookie header: '{}'", value);
                m_set_cookie_headers.append(move(value));

                auto can_read_without_blocking = m_socket->can_read_without_blocking();
                if (can_read_without_blocking.is_error())
                    return deferred_invoke([this] { did_fail(Core::NetworkJob::Error::TransmissionFailed); });
                if (!can_read_without_blocking.value())
                    return;
            } else if (auto existing_value = m_headers.get(name); existing_value.has_value()) {
                StringBuilder builder;
                builder.append(existing_value.value());
                builder.append(',');
                builder.append(value);
                m_headers.set(name, builder.build());
            } else {
                m_headers.set(name, value);
            }
            if (name.equals_ignoring_case("Content-Encoding")) {
                // Assume that any content-encoding means that we can't decode it as a stream :(
                dbgln_if(JOB_DEBUG, "Content-Encoding {} detected, cannot stream output :(", value);
                m_can_stream_response = false;
            } else if (name.equals_ignoring_case("Content-Length")) {
                auto length = value.to_uint();
                if (length.has_value())
                    m_content_length = length.value();
            }
            dbgln_if(JOB_DEBUG, "Job: [{}] = '{}'", name, value);

            auto can_read_without_blocking = m_socket->can_read_without_blocking();
            if (can_read_without_blocking.is_error())
                return deferred_invoke([this] { did_fail(Core::NetworkJob::Error::TransmissionFailed); });
            if (!can_read_without_blocking.value()) {
                dbgln_if(JOB_DEBUG, "Can't read headers anymore, byebye :(");
                return;
            }
        }
        VERIFY(m_state == State::InBody);

        while (true) {
            auto can_read_without_blocking = m_socket->can_read_without_blocking();
            if (can_read_without_blocking.is_error())
                return deferred_invoke([this] { did_fail(Core::NetworkJob::Error::TransmissionFailed); });
            if (!can_read_without_blocking.value())
                break;

            auto read_size = 64 * KiB;
            if (m_current_chunk_remaining_size.has_value()) {
            read_chunk_size:;
                auto remaining = m_current_chunk_remaining_size.value();
                if (remaining == -1) {
                    // read size
                    auto maybe_size_data = read_line(PAGE_SIZE);
                    if (maybe_size_data.is_error()) {
                        dbgln_if(JOB_DEBUG, "Job: Could not receive chunk: {}", maybe_size_data.error());
                    }
                    auto size_data = maybe_size_data.release_value();

                    if (m_should_read_chunk_ending_line) {
                        VERIFY(size_data.is_empty());
                        m_should_read_chunk_ending_line = false;
                        continue;
                    }
                    auto size_lines = size_data.view().lines();
                    dbgln_if(JOB_DEBUG, "Job: Received a chunk with size '{}'", size_data);
                    if (size_lines.size() == 0) {
                        if (!m_socket->is_eof())
                            break;
                        dbgln("Job: Reached end of stream");
                        finish_up();
                        break;
                    } else {
                        auto chunk = size_lines[0].split_view(';', true);
                        String size_string = chunk[0];
                        char* endptr;
                        auto size = strtoul(size_string.characters(), &endptr, 16);
                        if (*endptr) {
                            // invalid number
                            deferred_invoke([this] { did_fail(Core::NetworkJob::Error::TransmissionFailed); });
                            break;
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
                    // HTTP/1.1 3.3.3.3:
                    // If a message is received with both a Transfer-Encoding and a Content-Length header field, the Transfer-Encoding overrides the Content-Length. [...]
                    // https://httpwg.org/specs/rfc7230.html#message.body.length
                    m_content_length = {};

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

            can_read_without_blocking = m_socket->can_read_without_blocking();
            if (can_read_without_blocking.is_error())
                return deferred_invoke([this] { did_fail(Core::NetworkJob::Error::TransmissionFailed); });
            if (!can_read_without_blocking.value())
                break;

            dbgln_if(JOB_DEBUG, "Waiting for payload for {}", m_request.url());
            auto maybe_payload = receive(read_size);
            if (maybe_payload.is_error()) {
                dbgln_if(JOB_DEBUG, "Could not read the payload: {}", maybe_payload.error());
                return deferred_invoke([this] { did_fail(Core::NetworkJob::Error::TransmissionFailed); });
            }

            auto payload = maybe_payload.release_value();

            if (payload.is_empty() && m_socket->is_eof()) {
                finish_up();
                break;
            }

            bool read_everything = false;
            if (m_content_length.has_value()) {
                auto length = m_content_length.value();
                if (m_received_size + payload.size() >= length) {
                    payload.resize(length - m_received_size);
                    read_everything = true;
                }
            }

            m_received_buffers.append(make<ReceivedBuffer>(payload));
            m_buffered_size += payload.size();
            m_received_size += payload.size();
            flush_received_buffers();

            deferred_invoke([this] { did_progress(m_content_length, m_received_size); });

            if (read_everything) {
                VERIFY(m_received_size <= m_content_length.value());
                finish_up();
                break;
            }

            if (m_current_chunk_remaining_size.has_value()) {
                auto size = m_current_chunk_remaining_size.value() - payload.size();

                dbgln_if(JOB_DEBUG, "Job: We have {} bytes left over in this chunk", size);
                if (size == 0) {
                    dbgln_if(JOB_DEBUG, "Job: Finished a chunk of {} bytes", m_current_chunk_total_size.value());

                    if (m_current_chunk_total_size.value() == 0) {
                        m_state = State::Trailers;
                        break;
                    }

                    // we've read everything, now let's get the next chunk
                    size = -1;

                    auto can_read_line = m_socket->can_read_line();
                    if (can_read_line.is_error())
                        return deferred_invoke([this] { did_fail(Core::NetworkJob::Error::TransmissionFailed); });
                    if (can_read_line.value()) {
                        auto maybe_line = read_line(PAGE_SIZE);
                        if (maybe_line.is_error()) {
                            return deferred_invoke([this] { did_fail(Core::NetworkJob::Error::TransmissionFailed); });
                        }

                        VERIFY(maybe_line.value().is_empty());
                    } else {
                        m_should_read_chunk_ending_line = true;
                    }
                }
                m_current_chunk_remaining_size = size;
            }
        }

        if (!m_socket->is_open()) {
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
    VERIFY(!m_has_scheduled_finish);
    m_state = State::Finished;
    if (!m_can_stream_response) {
        auto maybe_flattened_buffer = ByteBuffer::create_uninitialized(m_buffered_size);
        if (maybe_flattened_buffer.is_error())
            return did_fail(Core::NetworkJob::Error::TransmissionFailed);
        auto flattened_buffer = maybe_flattened_buffer.release_value();

        u8* flat_ptr = flattened_buffer.data();
        for (auto& received_buffer : m_received_buffers) {
            memcpy(flat_ptr, received_buffer.pending_flush.data(), received_buffer.pending_flush.size());
            flat_ptr += received_buffer.pending_flush.size();
        }
        m_received_buffers.clear();

        // For the time being, we cannot stream stuff with content-encoding set to _anything_.
        // FIXME: LibCompress exposes a streaming interface, so this can be resolved
        auto content_encoding = m_headers.get("Content-Encoding");
        if (content_encoding.has_value()) {
            if (auto result = handle_content_encoding(flattened_buffer, content_encoding.value()); result.has_value())
                flattened_buffer = result.release_value();
            else
                return did_fail(Core::NetworkJob::Error::TransmissionFailed);
        }

        m_buffered_size = flattened_buffer.size();
        m_received_buffers.append(make<ReceivedBuffer>(move(flattened_buffer)));
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

    m_has_scheduled_finish = true;
    auto response = HttpResponse::create(m_code, move(m_headers), m_received_size);
    deferred_invoke([this, response = move(response)] {
        // If the server responded with "Connection: close", close the connection
        // as the server may or may not want to close the socket.
        if (auto result = response->headers().get("Connection"sv); result.has_value() && result.value().equals_ignoring_case("close"sv))
            shutdown(ShutdownMode::CloseSocket);
        did_finish(response);
    });
}
}

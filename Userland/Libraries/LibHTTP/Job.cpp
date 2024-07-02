/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/AsyncStreamHelpers.h>
#include <AK/AsyncStreamTransform.h>
#include <AK/CharacterTypes.h>
#include <AK/Debug.h>
#include <AK/GenericAwaiter.h>
#include <AK/MemMem.h>
#include <AK/MemoryStream.h>
#include <AK/StreamBuffer.h>
#include <AK/Try.h>
#include <LibCompress/Brotli.h>
#include <LibCompress/Gzip.h>
#include <LibCompress/Zlib.h>
#include <LibCore/Event.h>
#include <LibCore/EventLoop.h>
#include <LibHTTP/HttpResponse.h>
#include <LibHTTP/Job.h>
#include <stdio.h>
#include <unistd.h>

namespace HTTP {

template<typename Stream>
struct SyncStreamAsyncWrapper final : public AsyncInputStream {
    explicit SyncStreamAsyncWrapper(MaybeOwned<Stream>&& stream)
        : m_stream(move(stream))
        , m_awaiter(GenericAwaiter { [&](auto cb) { m_stream->on_ready_to_read = move(cb); } })
    {
    }

    virtual void reset() override
    {
    }

    virtual Coroutine<ErrorOr<void>> close() override
    {
        if (m_stream->is_open())
            m_stream->close();
        co_return {};
    }

    virtual bool is_open() const override
    {
        return !m_stream->is_eof() || !m_buffer.is_empty();
    }

    virtual Coroutine<ErrorOr<bool>> enqueue_some(Badge<AsyncInputStream>) override
    {
        CO_TRY(co_await m_awaiter);
        auto buffer = AK::Detail::ByteBuffer<16 * KiB> {};
        auto bytes = CO_TRY(m_stream->read_some(buffer.must_get_bytes_for_writing(16 * KiB)));
        if (bytes.is_empty())
            co_return !m_stream->is_eof();
        m_buffer.append(bytes);
        co_return true;
    }

    virtual ReadonlyBytes buffered_data_unchecked(Badge<AsyncInputStream>) const override
    {
        return m_buffer.data();
    }

    virtual void dequeue(Badge<AsyncInputStream>, size_t bytes) override
    {
        m_buffer.dequeue(bytes);
    }

    Coroutine<ErrorOr<StringView>> read_line(size_t max_size)
    {
        co_return StringView { CO_TRY(co_await AsyncStreamHelpers::consume_until(*this, "\r\n"sv, max_size)) }.trim("\r\n"sv, TrimMode::Right);
    }

private:
    MaybeOwned<Stream> m_stream;
    GenericAwaiter m_awaiter;
    StreamBuffer m_buffer;
};

static ErrorOr<ByteBuffer> handle_content_encoding(ByteBuffer const& buf, ByteString const& content_encoding)
{
    dbgln_if(JOB_DEBUG, "Job::handle_content_encoding: buf has content_encoding={}", content_encoding);

    // FIXME: Actually do the decompression of the data using streams, instead of all at once when everything has been
    //        received. This will require that some of the decompression algorithms are implemented in a streaming way.
    //        Gzip and Deflate are implemented using Stream, while Brotli uses the newer Core::Stream. The Gzip and
    //        Deflate implementations will likely need to be changed to LibCore::Stream for this to work easily.

    if (content_encoding == "gzip") {
        if (!Compress::GzipDecompressor::is_likely_compressed(buf)) {
            dbgln("Job::handle_content_encoding: buf is not gzip compressed!");
        }

        dbgln_if(JOB_DEBUG, "Job::handle_content_encoding: buf is gzip compressed!");

        auto uncompressed = TRY(Compress::GzipDecompressor::decompress_all(buf));

        if constexpr (JOB_DEBUG) {
            dbgln("Job::handle_content_encoding: Gzip::decompress() successful.");
            dbgln("  Input size: {}", buf.size());
            dbgln("  Output size: {}", uncompressed.size());
        }

        return uncompressed;
    } else if (content_encoding == "deflate") {
        dbgln_if(JOB_DEBUG, "Job::handle_content_encoding: buf is deflate compressed!");

        // Even though the content encoding is "deflate", it's actually deflate with the zlib wrapper.
        // https://tools.ietf.org/html/rfc7230#section-4.2.2
        auto memory_stream = make<FixedMemoryStream>(buf);
        auto zlib_decompressor = Compress::ZlibDecompressor::create(move(memory_stream));
        Optional<ByteBuffer> uncompressed;
        if (zlib_decompressor.is_error()) {
            // From the RFC:
            // "Note: Some non-conformant implementations send the "deflate"
            //        compressed data without the zlib wrapper."
            dbgln_if(JOB_DEBUG, "Job::handle_content_encoding: ZlibDecompressor::decompress_all() failed. Trying DeflateDecompressor::decompress_all()");
            uncompressed = TRY(Compress::DeflateDecompressor::decompress_all(buf));
        } else {
            uncompressed = TRY(zlib_decompressor.value()->read_until_eof());
        }

        if constexpr (JOB_DEBUG) {
            dbgln("Job::handle_content_encoding: Deflate decompression successful.");
            dbgln("  Input size: {}", buf.size());
            dbgln("  Output size: {}", uncompressed.value().size());
        }

        return uncompressed.release_value();
    } else if (content_encoding == "br") {
        dbgln_if(JOB_DEBUG, "Job::handle_content_encoding: buf is brotli compressed!");

        FixedMemoryStream bufstream { buf };
        auto brotli_stream = Compress::BrotliDecompressionStream { MaybeOwned<Stream>(bufstream) };

        auto uncompressed = TRY(brotli_stream.read_until_eof());
        if constexpr (JOB_DEBUG) {
            dbgln("Job::handle_content_encoding: Brotli::decompress() successful.");
            dbgln("  Input size: {}", buf.size());
            dbgln("  Output size: {}", uncompressed.size());
        }

        return uncompressed;
    }

    return buf;
}

Job::Job(HttpRequest&& request, Core::File& output_stream)
    : Core::NetworkJob(output_stream)
    , m_request(move(request))
{
}

void Job::start(Core::BufferedSocketBase& socket)
{
    VERIFY(!m_socket);
    m_socket = &socket;
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
        m_socket->on_ready_to_read = nullptr;
    } else {
        m_socket->on_ready_to_read = nullptr;
        m_socket = nullptr;
    }
}

Coroutine<void> Job::flush_received_buffers()
{
    if (!m_can_stream_response || m_has_scheduled_flush)
        co_return;

    TemporaryChange change(m_has_scheduled_flush, true);

    while (m_buffered_size > 0) {
        dbgln_if(JOB_DEBUG, "Job: Flushing received buffers: have {} bytes in {} buffers for {}", m_buffered_size, m_received_buffers.size(), m_request.url());
        for (size_t i = 0; i < m_received_buffers.size(); ++i) {
            auto& payload = m_received_buffers[i]->pending_flush;
            auto result = co_await do_write(payload);
            if (result.is_error())
                break;
            auto written = result.release_value();
            m_buffered_size -= written;
            if (written == payload.size()) {
                // FIXME: Make this a take-first-friendly object?
                (void)m_received_buffers.take_first();
                break;
            }
            VERIFY(written < payload.size());
            payload = payload.slice(written, payload.size() - written);
            break;
        }
        dbgln_if(JOB_DEBUG, "Job: Flushing received buffers done: have {} bytes in {} buffers for {}", m_buffered_size, m_received_buffers.size(), m_request.url());
    }

    if (m_request_done) {
        m_has_scheduled_finish = true;
        auto response = HttpResponse::create(m_code, move(m_headers), m_received_size);
        // If the server responded with "Connection: close", close the connection
        // as the server may or may not want to close the socket. Also, if this is
        // a legacy HTTP server (1.0 or older), assume close is the default value.
        if (auto result = response->headers().get("Connection"sv); result.has_value() ? result->equals_ignoring_ascii_case("close"sv) : m_legacy_connection)
            shutdown(ShutdownMode::CloseSocket);
        did_finish(response);
    }
}

auto Job::parse_status(auto& stream) -> Coroutine<ErrorOr<void>>
{
    auto line = CO_TRY(co_await stream.read_line(PAGE_SIZE));

    dbgln_if(JOB_DEBUG, "Job {} read line of length {}", m_request.url(), line.length());
    auto parts = line.split_view(' ');
    if (parts.size() < 2) {
        dbgln("Job: Expected 2-part or 3-part HTTP status line, got '{}'", line);
        deferred_invoke([this] { did_fail(Core::NetworkJob::Error::ProtocolFailed); });
        co_return {};
    }

    if (!parts[0].matches("HTTP/?.?"sv, CaseSensitivity::CaseSensitive) || !is_ascii_digit(parts[0][5]) || !is_ascii_digit(parts[0][7])) {
        dbgln("Job: Expected HTTP-Version to be of the form 'HTTP/X.Y', got '{}'", parts[0]);
        deferred_invoke([this] { did_fail(Core::NetworkJob::Error::ProtocolFailed); });
        co_return {};
    }
    auto http_major_version = parse_ascii_digit(parts[0][5]);
    auto http_minor_version = parse_ascii_digit(parts[0][7]);
    m_legacy_connection = http_major_version < 1 || (http_major_version == 1 && http_minor_version == 0);

    auto code = parts[1].template to_number<unsigned>();
    if (!code.has_value()) {
        dbgln("Job: Expected numeric HTTP status");
        deferred_invoke([this] { did_fail(Core::NetworkJob::Error::ProtocolFailed); });
        co_return {};
    }
    m_code = code.value();
    co_return {};
}

auto Job::parse_headers(auto& stream, bool in_trailers) -> Coroutine<ErrorOr<void>>
{
    while (true) {
        // There's no max limit defined on headers, but for our sanity, let's limit it to 32K.
        auto line = StringView { CO_TRY(co_await stream.read_line(32 * KiB)) };

        if (line.is_empty()) {
            if (in_trailers) {
                co_await finish_up();
                co_return {};
            }
            if (on_headers_received)
                on_headers_received(m_headers, m_code > 0 ? m_code : Optional<u32> {});

            // We've reached the end of the headers, there's a possibility that the server
            // responds with nothing (content-length = 0 with normal encoding); if that's the case,
            // quit early as we won't be reading anything anyway.
            if (auto result = m_headers.get("Content-Length"sv).value_or(""sv).to_number<unsigned>(); result.has_value()) {
                if (result.value() == 0) {
                    auto transfer_encoding = m_headers.get("Transfer-Encoding"sv);
                    if (!transfer_encoding.has_value() || !transfer_encoding->view().trim_whitespace().equals_ignoring_ascii_case("chunked"sv)) {
                        co_await finish_up();
                        co_return {};
                    }
                }
            }
            // There's also the possibility that the server responds with 204 (No Content),
            // and manages to set a Content-Length anyway, in such cases ignore Content-Length and quit early;
            // As the HTTP spec explicitly prohibits presence of Content-Length when the response code is 204.
            if (m_code == 204) {
                co_await finish_up();
                co_return {};
            }

            break;
        }
        auto parts = line.split_view(':');
        if (parts.is_empty()) {
            if (in_trailers) {
                // Some servers like to send two ending chunks
                // use this fact as an excuse to ignore anything after the last chunk
                // that is not a valid trailing header.
                co_await finish_up();
                co_return {};
            }
            dbgln("Job: Expected HTTP header with key/value");
            deferred_invoke([this] { did_fail(Core::NetworkJob::Error::ProtocolFailed); });
            co_return {};
        }
        auto name = parts[0];
        if (line.length() < name.length() + 2) {
            if (in_trailers) {
                // Some servers like to send two ending chunks
                // use this fact as an excuse to ignore anything after the last chunk
                // that is not a valid trailing header.
                co_await finish_up();
                co_return {};
            }
            dbgln("Job: Malformed HTTP header: '{}' ({})", line, line.length());
            deferred_invoke([this] { did_fail(Core::NetworkJob::Error::ProtocolFailed); });
            co_return {};
        }
        auto value = line.substring_view(name.length() + 2, line.length() - name.length() - 2);
        m_headers.set(name, value);
        if (name.equals_ignoring_ascii_case("Content-Encoding"sv)) {
            // Assume that any content-encoding means that we can't decode it as a stream :(
            dbgln_if(JOB_DEBUG, "Content-Encoding {} detected, cannot stream output :(", value);
            m_can_stream_response = false;
        } else if (name.equals_ignoring_ascii_case("Content-Length"sv)) {
            auto length = value.to_number<u64>();
            if (length.has_value())
                m_content_length = length.value();
        }
        dbgln_if(JOB_DEBUG, "Job: [{}] = '{}'", name, value);
    }

    co_return {};
}

auto Job::parse_body(auto& stream) -> Coroutine<ErrorOr<bool>>
{
    auto should_read_trailers = false;
    while (true) {
        auto read_size = 64 * KiB;
        if (m_current_chunk_remaining_size.has_value()) {
        read_chunk_size:;
            auto remaining = m_current_chunk_remaining_size.value();
            if (remaining == -1) {
                // read size
                auto size_data = CO_TRY(co_await stream.read_line(PAGE_SIZE));
                if (m_should_read_chunk_ending_line) {
                    // NOTE: Some servers seem to send an extra \r\n here despite there being no size.
                    //       This makes us tolerate that.
                    size_data = size_data.trim("\r\n"sv, TrimMode::Right);
                    VERIFY(size_data.is_empty());
                    m_should_read_chunk_ending_line = false;
                    continue;
                }
                auto size_lines = size_data.trim_whitespace().lines();
                dbgln_if(JOB_DEBUG, "Job: Received a chunk with size '{}'", size_data);
                if (size_lines.size() == 0) {
                    if (!m_socket->is_eof())
                        break;
                    dbgln("Job: Reached end of stream");
                    co_await finish_up();
                    break;
                }
                auto chunk = size_lines[0].split_view(';', SplitBehavior::KeepEmpty);
                ByteString size_string = chunk[0];
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
                if (encoding.equals_ignoring_ascii_case("chunked"sv)) {
                    m_current_chunk_remaining_size = -1;
                    goto read_chunk_size;
                } else {
                    dbgln("Job: Unknown transfer encoding '{}', the result will likely be wrong!", encoding);
                }
            } else if (m_content_length.has_value()) {
                read_size = m_content_length.value() - m_received_size;
                if (read_size == 0) {
                    co_await finish_up();
                    break;
                }
            }
        }

        auto payload = CO_TRY(ByteBuffer::copy(CO_TRY(co_await stream.read(read_size))));

        if (payload.is_empty() && !stream.is_open()) {
            co_await finish_up();
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
        Core::EventLoop::current().adopt_coroutine(flush_received_buffers());

        deferred_invoke([this] { did_progress(m_content_length, m_received_size); });

        if (read_everything) {
            VERIFY(m_received_size <= m_content_length.value());
            co_await finish_up();
            break;
        }

        // Check after reading all the buffered data if we have reached the end of stream
        // for cases where the server didn't send a content length, chunked encoding but is
        // directly closing the connection.
        if (!m_content_length.has_value() && !m_current_chunk_remaining_size.has_value() && m_socket->is_eof()) {
            co_await finish_up();
            break;
        }

        if (m_current_chunk_remaining_size.has_value()) {
            auto size = m_current_chunk_remaining_size.value() - payload.size();

            dbgln_if(JOB_DEBUG, "Job: We have {} bytes left over in this chunk", size);
            if (size == 0) {
                dbgln_if(JOB_DEBUG, "Job: Finished a chunk of {} bytes", m_current_chunk_total_size.value());

                if (m_current_chunk_total_size.value() == 0) {
                    should_read_trailers = true;
                    break;
                }

                // we've read everything, now let's get the next chunk
                size = -1;

                auto line = CO_TRY(co_await stream.read_line(PAGE_SIZE));
                VERIFY(line.is_empty());
            }
            m_current_chunk_remaining_size = size;
        }
    }
    co_return should_read_trailers;
}

auto Job::read_response() -> Coroutine<ErrorOr<void>>
{
    auto stream = SyncStreamAsyncWrapper(MaybeOwned { *m_socket });

    if (is_cancelled())
        co_return {};

    if (m_socket->is_eof()) {
        // Some servers really like terminating connections by simply closing them (even TLS ones)
        // to signal end-of-data, if there's no:
        // - connection
        // - content-size
        // - transfer-encoding: chunked
        // header, simply treat EOF as a termination signal.
        if (m_headers.contains("connection"sv) || m_content_length.has_value() || m_current_chunk_total_size.has_value()) {
            dbgln_if(JOB_DEBUG, "Read failure: Actually EOF!");
            deferred_invoke([this] { did_fail(Core::NetworkJob::Error::ProtocolFailed); });
            co_return {};
        }

        co_await finish_up();
        co_return {};
    }

    CO_TRY(co_await parse_status(stream));
    CO_TRY(co_await parse_headers(stream, false));
    auto should_read_trailers = CO_TRY(co_await parse_body(stream));
    if (should_read_trailers)
        CO_TRY(co_await parse_headers(stream, true));

    co_await finish_up();

    co_return {};
}

void Job::on_socket_connected()
{
    auto raw_request = m_request.to_raw_request().release_value_but_fixme_should_propagate_errors();

    if constexpr (JOB_DEBUG) {
        dbgln("Job: raw_request:");
        dbgln("{}", ByteString::copy(raw_request));
    }

    if (m_socket->write_until_depleted(raw_request).is_error())
        return deferred_invoke([this] { did_fail(Core::NetworkJob::Error::TransmissionFailed); });

    Core::EventLoop::current().adopt_coroutine([this] mutable -> Coroutine<void> {
        auto result = co_await read_response();
        if (result.is_error()) {
            dbgln("Job: Failed to read response: {}", result.error());
            did_fail(Core::NetworkJob::Error::TransmissionFailed);
        }
    }());
}

Coroutine<void> Job::finish_up()
{
    if (m_has_scheduled_finish)
        co_return;

    if (!m_can_stream_response) {
        auto maybe_flattened_buffer = ByteBuffer::create_uninitialized(m_buffered_size);
        if (maybe_flattened_buffer.is_error())
            co_return did_fail(Core::NetworkJob::Error::TransmissionFailed);
        auto flattened_buffer = maybe_flattened_buffer.release_value();

        u8* flat_ptr = flattened_buffer.data();
        for (auto& received_buffer : m_received_buffers) {
            memcpy(flat_ptr, received_buffer->pending_flush.data(), received_buffer->pending_flush.size());
            flat_ptr += received_buffer->pending_flush.size();
        }
        m_received_buffers.clear();

        // For the time being, we cannot stream stuff with content-encoding set to _anything_.
        // FIXME: LibCompress exposes a streaming interface, so this can be resolved
        auto content_encoding = m_headers.get("Content-Encoding");
        if (content_encoding.has_value()) {
            if (auto result = handle_content_encoding(flattened_buffer, content_encoding.value()); !result.is_error())
                flattened_buffer = result.release_value();
            else
                co_return did_fail(Core::NetworkJob::Error::TransmissionFailed);
        }

        m_buffered_size = flattened_buffer.size();
        m_received_buffers.append(make<ReceivedBuffer>(move(flattened_buffer)));
        m_can_stream_response = true;
    }

    m_request_done = true;
    Core::EventLoop::current().adopt_coroutine(flush_received_buffers());
}
}

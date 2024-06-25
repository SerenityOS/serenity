/*
 * Copyright (c) 2024, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/AsyncStreamHelpers.h>
#include <AK/AsyncStreamTransform.h>
#include <AK/GenericLexer.h>
#include <AK/StreamBuffer.h>
#include <LibHTTP/Http11Connection.h>

namespace HTTP {

namespace {
StringView method_name(Method method)
{
    switch (method) {
#define CASE(x)     \
    case Method::x: \
        return #x##sv;
        ENUMERATE_METHODS(CASE)
#undef CASE
    default:
        VERIFY_NOT_REACHED();
    }
}

ByteBuffer format_request(RequestData const& data)
{
    StringBuilder builder;

    builder.append(method_name(data.method));
    builder.append(' ');
    builder.append(data.url);
    builder.append(" HTTP/1.1\r\n"sv);

    for (auto const& [name, value] : data.headers) {
        builder.append(name);
        builder.append(": "sv);
        builder.append(value);
        builder.append("\r\n"sv);
    }
    builder.append("\r\n"sv);

    return MUST(builder.to_byte_buffer());
}

struct StatusCodeAndHeaders {
    u16 status_code;
    Vector<Header> headers;
};

Coroutine<ErrorOr<StatusCodeAndHeaders>> receive_response_headers(AsyncStream& stream)
{
    auto status_line = CO_TRY(co_await AsyncStreamHelpers::consume_until(stream, "\r\n"sv));

    GenericLexer status_lexer { StringView { status_line } };
    if (!status_lexer.next_is("HTTP/1.1 ")) {
        stream.reset();
        co_return Error::from_string_literal("HTTP-version must be 'HTTP/1.1'");
    }
    status_lexer.consume(9);

    auto status_code = status_lexer.consume_decimal_integer<u16>();
    if (status_code.is_error()) {
        stream.reset();
        co_return Error::from_string_literal("Invalid HTTP status code");
    }

    Vector<Header> headers;
    while (true) {
        auto header = StringView { CO_TRY(co_await AsyncStreamHelpers::consume_until(stream, "\r\n"sv)) };
        if (header == "\r\n"sv)
            break;

        auto colon_position = header.find(':');
        if (!colon_position.has_value()) {
            stream.reset();
            co_return Error::from_string_literal("':' must be present in a header line");
        }

        headers.append({
            .header = header.substring_view(0, colon_position.value()),
            .value = header.substring_view(colon_position.value() + 1).trim_whitespace(),
        });
    }

    co_return StatusCodeAndHeaders {
        .status_code = status_code.value(),
        .headers = headers,
    };
}

class ChunkedBodyStream final : public AsyncStreamTransform<AsyncInputStream> {
public:
    ChunkedBodyStream(AsyncInputStream& stream)
        : AsyncStreamTransform(MaybeOwned { stream }, generate())
    {
    }

    ReadonlyBytes buffered_data_unchecked(Badge<AsyncInputStream>) const override
    {
        return m_buffer.data();
    }

    void dequeue(Badge<AsyncInputStream>, size_t bytes) override
    {
        m_buffer.dequeue(bytes);
    }

private:
    Generator generate()
    {
        while (true) {
            auto line = CO_TRY(co_await AsyncStreamHelpers::consume_until(*m_stream, "\r\n"sv));

            auto lexer = GenericLexer { line };
            auto length_or_error = lexer.consume_decimal_integer<size_t>();
            if (length_or_error.is_error()) {
                m_stream->reset();
                co_return Error::from_string_literal("Invalid chunk length");
            }
            if (!lexer.consume_specific("\r\n")) {
                m_stream->reset();
                co_return Error::from_string_literal("Expected \\r\\n after chunk length");
            }
            VERIFY(lexer.is_eof());
            size_t chunk_length = length_or_error.release_value();
            bool is_last_chunk = chunk_length == 0;

            while (chunk_length > 0) {
                auto data = CO_TRY(co_await m_stream->peek());
                size_t to_copy = min(data.size(), chunk_length);
                // FIXME: We can reuse the buffer of the underlying stream if our reading frame doesn't span
                //        multiple chunks.
                m_buffer.append(must_sync(m_stream->read(to_copy)));
                chunk_length -= to_copy;
                co_yield {};
            }

            if (CO_TRY(co_await m_stream->read(2)) != "\r\n"sv.bytes()) {
                m_stream->reset();
                co_return Error::from_string_literal("Expected \\r\\n after a chunk");
            }

            if (is_last_chunk)
                co_return {};
        }
    }

    StreamBuffer m_buffer;
};

}

Coroutine<ErrorOr<NonnullOwnPtr<Http11Response>>> Http11Response::create(Badge<Http11Connection>, RequestData&& data, AsyncStream& stream)
{
    auto header = format_request(data);

    if (data.body.has<Empty>()) {
        CO_TRY(co_await stream.write({ { header } }));
    } else if (data.body.has<RequestData::PlainBody>()) {
        auto& body = data.body.get<RequestData::PlainBody>().data;
        CO_TRY(co_await stream.write({ { header, body.bytes() } }));
    } else {
        VERIFY_NOT_REACHED();
    }

    auto [status_code, headers] = CO_TRY(co_await receive_response_headers(stream));

    Optional<size_t> content_length;
    Optional<StringView> transfer_encoding;
    for (auto const& header : headers) {
        if (header.header.equals_ignoring_ascii_case("Content-Length"sv)) {
            content_length = header.value.to_number<size_t>();
        } else if (header.header.equals_ignoring_ascii_case("Transfer-Encoding"sv)) {
            transfer_encoding = header.value;
        }
    }

    OwnPtr<AsyncInputStream> body;
    if (transfer_encoding.has_value()) {
        if (transfer_encoding.value() != "chunked"sv) {
            stream.reset();
            co_return Error::from_string_literal("Unsupported 'Transfer-Encoding'");
        }
        body = make<ChunkedBodyStream>(stream);
    } else {
        if (!content_length.has_value()) {
            stream.reset();
            co_return Error::from_string_literal("'Content-Length' must be provided");
        }
        body = make<AsyncInputStreamSlice>(stream, content_length.value());
    }

    co_return adopt_own(*new (nothrow) Http11Response(body.release_nonnull(), status_code, move(headers)));
}

}

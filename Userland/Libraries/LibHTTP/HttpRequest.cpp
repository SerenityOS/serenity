/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Base64.h>
#include <AK/StringBuilder.h>
#include <LibHTTP/HttpRequest.h>
#include <LibHTTP/Job.h>

namespace HTTP {

HttpRequest::HttpRequest()
{
}

HttpRequest::~HttpRequest()
{
}

String HttpRequest::method_name() const
{
    switch (m_method) {
    case Method::GET:
        return "GET";
    case Method::HEAD:
        return "HEAD";
    case Method::POST:
        return "POST";
    default:
        VERIFY_NOT_REACHED();
    }
}

ByteBuffer HttpRequest::to_raw_request() const
{
    StringBuilder builder;
    builder.append(method_name());
    builder.append(' ');
    // NOTE: The percent_encode is so that e.g. spaces are properly encoded.
    auto path = m_url.path();
    VERIFY(!path.is_empty());
    builder.append(URL::percent_encode(m_url.path(), URL::PercentEncodeSet::EncodeURI));
    if (!m_url.query().is_empty()) {
        builder.append('?');
        builder.append(URL::percent_encode(m_url.query(), URL::PercentEncodeSet::EncodeURI));
    }
    builder.append(" HTTP/1.1\r\nHost: ");
    builder.append(m_url.host());
    builder.append("\r\n");
    for (auto& header : m_headers) {
        builder.append(header.name);
        builder.append(": ");
        builder.append(header.value);
        builder.append("\r\n");
    }
    if (!m_body.is_empty()) {
        builder.appendff("Content-Length: {}\r\n\r\n", m_body.size());
        builder.append((char const*)m_body.data(), m_body.size());
    }
    builder.append("\r\n");
    return builder.to_byte_buffer();
}

Optional<HttpRequest> HttpRequest::from_raw_request(ReadonlyBytes raw_request)
{
    enum class State {
        InMethod,
        InResource,
        InProtocol,
        InHeaderName,
        InHeaderValue,
    };

    State state { State::InMethod };
    size_t index = 0;

    auto peek = [&](int offset = 0) -> u8 {
        if (index + offset >= raw_request.size())
            return 0;
        return raw_request[index + offset];
    };

    auto consume = [&]() -> u8 {
        VERIFY(index < raw_request.size());
        return raw_request[index++];
    };

    Vector<u8, 256> buffer;

    String method;
    String resource;
    String protocol;
    Vector<Header> headers;
    Header current_header;

    auto commit_and_advance_to = [&](auto& output, State new_state) {
        output = String::copy(buffer);
        buffer.clear();
        state = new_state;
    };

    while (index < raw_request.size()) {
        // FIXME: Figure out what the appropriate limitations should be.
        if (buffer.size() > 65536)
            return {};
        switch (state) {
        case State::InMethod:
            if (peek() == ' ') {
                consume();
                commit_and_advance_to(method, State::InResource);
                break;
            }
            buffer.append(consume());
            break;
        case State::InResource:
            if (peek() == ' ') {
                consume();
                commit_and_advance_to(resource, State::InProtocol);
                break;
            }
            buffer.append(consume());
            break;
        case State::InProtocol:
            if (peek(0) == '\r' && peek(1) == '\n') {
                consume();
                consume();
                commit_and_advance_to(protocol, State::InHeaderName);
                break;
            }
            buffer.append(consume());
            break;
        case State::InHeaderName:
            if (peek(0) == ':' && peek(1) == ' ') {
                consume();
                consume();
                commit_and_advance_to(current_header.name, State::InHeaderValue);
                break;
            }
            buffer.append(consume());
            break;
        case State::InHeaderValue:
            if (peek(0) == '\r' && peek(1) == '\n') {
                consume();
                consume();
                commit_and_advance_to(current_header.value, State::InHeaderName);
                headers.append(move(current_header));
                break;
            }
            buffer.append(consume());
            break;
        }
    }

    HttpRequest request;
    if (method == "GET")
        request.m_method = Method::GET;
    else if (method == "HEAD")
        request.m_method = Method::HEAD;
    else if (method == "POST")
        request.m_method = Method::POST;
    else
        return {};

    request.m_resource = URL::percent_decode(resource);
    request.m_headers = move(headers);

    return request;
}

void HttpRequest::set_headers(HashMap<String, String> const& headers)
{
    for (auto& it : headers)
        m_headers.append({ it.key, it.value });
}

Optional<HttpRequest::Header> HttpRequest::get_http_basic_authentication_header(URL const& url)
{
    if (!url.includes_credentials())
        return {};
    StringBuilder builder;
    builder.append(url.username());
    builder.append(':');
    builder.append(url.password());
    auto token = encode_base64(builder.to_string().bytes());
    builder.clear();
    builder.append("Basic ");
    builder.append(token);
    return Header { "Authorization", builder.to_string() };
}

Optional<HttpRequest::BasicAuthenticationCredentials> HttpRequest::parse_http_basic_authentication_header(String const& value)
{
    if (!value.starts_with("Basic ", AK::CaseSensitivity::CaseInsensitive))
        return {};
    auto token = value.substring_view(6);
    if (token.is_empty())
        return {};
    auto decoded_token_bb = decode_base64(token);
    if (decoded_token_bb.is_error())
        return {};
    auto decoded_token = String::copy(decoded_token_bb.value());
    auto colon_index = decoded_token.find(':');
    if (!colon_index.has_value())
        return {};
    auto username = decoded_token.substring_view(0, colon_index.value());
    auto password = decoded_token.substring_view(colon_index.value() + 1);
    return BasicAuthenticationCredentials { username, password };
}

}

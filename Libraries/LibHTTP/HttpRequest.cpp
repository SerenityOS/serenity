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

#include <AK/StringBuilder.h>
#include <LibHTTP/HttpJob.h>
#include <LibHTTP/HttpRequest.h>

namespace HTTP {

HttpRequest::HttpRequest()
{
}

HttpRequest::~HttpRequest()
{
}

RefPtr<Core::NetworkJob> HttpRequest::schedule()
{
    auto job = HttpJob::construct(*this);
    job->start();
    return job;
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
        ASSERT_NOT_REACHED();
    }
}

ByteBuffer HttpRequest::to_raw_request() const
{
    StringBuilder builder;
    builder.append(method_name());
    builder.append(' ');
    builder.append(m_url.path());
    if (!m_url.query().is_empty()) {
        builder.append('?');
        builder.append(m_url.query());
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
    builder.append("Connection: close\r\n\r\n");
    if (!m_body.is_empty()) {
        builder.append((const char*)m_body.data(), m_body.size());
        builder.append("\r\n");
    }
    return builder.to_byte_buffer();
}

Optional<HttpRequest> HttpRequest::from_raw_request(const ByteBuffer& raw_request)
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
        ASSERT(index < raw_request.size());
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

    request.m_resource = resource;
    request.m_headers = move(headers);

    return request;
}

void HttpRequest::set_headers(const HashMap<String, String>& headers)
{
    for (auto& it : headers)
        m_headers.append({ it.key, it.value });
}

}

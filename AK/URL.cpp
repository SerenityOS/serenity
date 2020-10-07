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

#include <AK/LexicalPath.h>
#include <AK/StringBuilder.h>
#include <AK/URL.h>
#include <AK/URLParser.h>

namespace AK {

static inline bool is_valid_protocol_character(char ch)
{
    return ch >= 'a' && ch <= 'z';
}

static inline bool is_valid_hostname_character(char ch)
{
    return ch && ch != '/' && ch != ':';
}

static inline bool is_digit(char ch)
{
    return ch >= '0' && ch <= '9';
}

bool URL::parse(const StringView& string)
{
    if (string.is_null())
        return false;

    enum class State {
        InProtocol,
        InHostname,
        InPort,
        InPath,
        InQuery,
        InFragment,
        InDataMimeType,
        InDataPayload,
    };

    Vector<char, 256> buffer;
    State state { State::InProtocol };

    size_t index = 0;

    auto peek = [&] {
        if (index >= string.length())
            return '\0';
        return string[index];
    };

    auto consume = [&] {
        if (index >= string.length())
            return '\0';
        return string[index++];
    };

    while (index < string.length()) {
        switch (state) {
        case State::InProtocol: {
            if (is_valid_protocol_character(peek())) {
                buffer.append(consume());
                continue;
            }
            if (consume() != ':')
                return false;

            m_protocol = String::copy(buffer);

            if (m_protocol == "data") {
                buffer.clear();
                state = State::InDataMimeType;
                continue;
            }

            if (m_protocol == "about") {
                buffer.clear();
                state = State::InPath;
                continue;
            }

            if (consume() != '/')
                return false;
            if (consume() != '/')
                return false;
            if (buffer.is_empty())
                return false;
            if (m_protocol == "http")
                m_port = 80;
            else if (m_protocol == "https")
                m_port = 443;
            else if (m_protocol == "gemini")
                m_port = 1965;
            state = State::InHostname;
            buffer.clear();
            continue;
        }
        case State::InHostname:
            if (is_valid_hostname_character(peek())) {
                buffer.append(consume());
                continue;
            }
            if (buffer.is_empty()) {
                if (m_protocol == "file") {
                    m_host = "";
                    state = State::InPath;
                    continue;
                }
                return false;
            }
            m_host = String::copy(buffer);
            buffer.clear();
            if (peek() == ':') {
                consume();
                state = State::InPort;
                continue;
            }
            if (peek() == '/') {
                state = State::InPath;
                continue;
            }
            return false;
        case State::InPort:
            if (is_digit(peek())) {
                buffer.append(consume());
                continue;
            }
            if (buffer.is_empty())
                return false;
            {
                auto port_opt = String::copy(buffer).to_uint();
                buffer.clear();
                if (!port_opt.has_value())
                    return false;
                m_port = port_opt.value();
            }
            if (peek() == '/') {
                state = State::InPath;
                continue;
            }
            return false;
        case State::InPath:
            if (peek() == '?' || peek() == '#') {
                m_path = String::copy(buffer);
                buffer.clear();
                state = peek() == '?' ? State::InQuery : State::InFragment;
                consume();
                continue;
            }
            buffer.append(consume());
            continue;
        case State::InQuery:
            if (peek() == '#') {
                m_query = String::copy(buffer);
                buffer.clear();
                consume();
                state = State::InFragment;
                continue;
            }
            buffer.append(consume());
            continue;
        case State::InFragment:
            buffer.append(consume());
            continue;
        case State::InDataMimeType: {
            if (peek() != ';' && peek() != ',') {
                buffer.append(consume());
                continue;
            }

            m_data_mime_type = String::copy(buffer);
            buffer.clear();

            if (peek() == ';') {
                consume();
                if (consume() != 'b')
                    return false;
                if (consume() != 'a')
                    return false;
                if (consume() != 's')
                    return false;
                if (consume() != 'e')
                    return false;
                if (consume() != '6')
                    return false;
                if (consume() != '4')
                    return false;
                m_data_payload_is_base64 = true;
            }

            if (consume() != ',')
                return false;

            state = State::InDataPayload;
            break;
        }
        case State::InDataPayload:
            buffer.append(consume());
            break;
        }
    }
    if (state == State::InHostname) {
        // We're still in the hostname, so e.g "http://serenityos.org"
        if (buffer.is_empty())
            return false;
        m_host = String::copy(buffer);
        m_path = "/";
    }
    if (state == State::InProtocol)
        return false;
    if (state == State::InPath)
        m_path = String::copy(buffer);
    if (state == State::InQuery)
        m_query = String::copy(buffer);
    if (state == State::InFragment)
        m_fragment = String::copy(buffer);
    if (state == State::InDataPayload)
        m_data_payload = urldecode(String::copy(buffer));
    if (m_query.is_null())
        m_query = "";
    if (m_fragment.is_null())
        m_fragment = "";
    return true;
}

URL::URL(const StringView& string)
{
    m_valid = parse(string);
}

String URL::to_string() const
{
    StringBuilder builder;
    builder.append(m_protocol);

    if (m_protocol == "about") {
        builder.append(':');
        builder.append(m_path);
        return builder.to_string();
    }

    if (m_protocol == "data") {
        builder.append(':');
        builder.append(m_data_mime_type);
        if (m_data_payload_is_base64)
            builder.append(";base64");
        builder.append(',');
        builder.append(m_data_payload);
        return builder.to_string();
    }

    builder.append("://");
    builder.append(m_host);
    if (protocol() != "file") {
        if (!(protocol() == "http" && port() == 80) && !(protocol() == "https" && port() == 443) && !(protocol() == "gemini" && port() == 1965)) {
            builder.append(':');
            builder.append(String::number(m_port));
        }
    }
    builder.append(m_path);
    if (!m_query.is_empty()) {
        builder.append('?');
        builder.append(m_query);
    }
    if (!m_fragment.is_empty()) {
        builder.append('#');
        builder.append(m_fragment);
    }
    return builder.to_string();
}

URL URL::complete_url(const String& string) const
{
    if (!is_valid())
        return {};

    URL url(string);
    if (url.is_valid())
        return url;

    if (protocol() == "data")
        return {};

    if (string.starts_with("//")) {
        URL url(String::formatted("{}:{}", m_protocol, string));
        if (url.is_valid())
            return url;
    }

    if (string.starts_with("/")) {
        url = *this;
        url.set_path(string);
        return url;
    }

    if (string.starts_with("#")) {
        url = *this;
        url.set_fragment(string.substring(1, string.length() - 1));
        return url;
    }

    StringBuilder builder;
    LexicalPath lexical_path(path());
    builder.append('/');

    bool document_url_ends_in_slash = path()[path().length() - 1] == '/';

    for (size_t i = 0; i < lexical_path.parts().size(); ++i) {
        if (i == lexical_path.parts().size() - 1 && !document_url_ends_in_slash)
            break;
        builder.append(lexical_path.parts()[i]);
        builder.append('/');
    }
    builder.append(string);
    auto built = builder.to_string();
    lexical_path = LexicalPath(built);

    built = lexical_path.string();
    if (string.ends_with('/') && !built.ends_with('/')) {
        builder.clear();
        builder.append(built);
        builder.append('/');
        built = builder.to_string();
    }

    url = *this;
    url.set_path(built);
    return url;
}

void URL::set_protocol(const String& protocol)
{
    m_protocol = protocol;
    m_valid = compute_validity();
}

void URL::set_host(const String& host)
{
    m_host = host;
    m_valid = compute_validity();
}

void URL::set_path(const String& path)
{
    m_path = path;
    m_valid = compute_validity();
}

void URL::set_query(const String& query)
{
    m_query = query;
}

void URL::set_fragment(const String& fragment)
{
    m_fragment = fragment;
}

bool URL::compute_validity() const
{
    // FIXME: This is by no means complete.
    if (m_protocol.is_empty())
        return false;
    if (m_protocol == "file") {
        if (m_path.is_empty())
            return false;
    } else {
        if (m_host.is_empty())
            return false;
    }
    return true;
}

URL URL::create_with_file_protocol(const String& path)
{
    URL url;
    url.set_protocol("file");
    url.set_path(path);
    return url;
}

URL URL::create_with_url_or_path(const String& url_or_path)
{
    URL url = url_or_path;
    if (url.is_valid())
        return url;

    String path = LexicalPath::canonicalized_path(url_or_path);
    return URL::create_with_file_protocol(path);
}

URL URL::create_with_data(const StringView& mime_type, const StringView& payload, bool is_base64)
{
    URL url;
    url.m_valid = true;
    url.set_protocol("data");
    url.m_data_payload = payload;
    url.m_data_mime_type = mime_type;
    url.m_data_payload_is_base64 = is_base64;

    return url;
}

String URL::basename() const
{
    if (!m_valid)
        return {};
    return LexicalPath(m_path).basename();
}

}

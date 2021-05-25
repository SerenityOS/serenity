/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Max Wipfli <mail@maxwipfli.ch>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <AK/StringBuilder.h>
#include <AK/URL.h>
#include <AK/URLParser.h>
#include <AK/Utf8View.h>

namespace AK {

constexpr bool is_ascii_alpha(u32 code_point)
{
    return ('a' <= code_point && code_point <= 'z') || ('A' <= code_point && code_point <= 'Z');
}

constexpr bool is_ascii_digit(u32 code_point)
{
    return '0' <= code_point && code_point <= '9';
}

constexpr bool is_ascii_alphanumeric(u32 code_point)
{
    return is_ascii_alpha(code_point) || is_ascii_digit(code_point);
}

constexpr bool is_ascii_hex_digit(u32 code_point)
{
    return is_ascii_digit(code_point) || (code_point >= 'a' && code_point <= 'f') || (code_point >= 'A' && code_point <= 'F');
}

static inline bool is_valid_scheme_character(char ch)
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
        InScheme,
        InHostname,
        InPort,
        InPath,
        InQuery,
        InFragment,
        InDataMimeType,
        InDataPayload,
    };

    Vector<char, 256> buffer;
    State state { State::InScheme };

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
        case State::InScheme: {
            if (is_valid_scheme_character(peek())) {
                buffer.append(consume());
                continue;
            }
            if (consume() != ':')
                return false;

            m_scheme = String::copy(buffer);

            if (m_scheme == "data") {
                buffer.clear();
                m_host = "";
                state = State::InDataMimeType;
                continue;
            }

            if (m_scheme == "about") {
                buffer.clear();
                m_host = "";
                state = State::InPath;
                continue;
            }

            if (consume() != '/')
                return false;
            if (consume() != '/')
                return false;
            if (buffer.is_empty())
                return false;
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
                if (m_scheme == "file") {
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
    if (state == State::InScheme)
        return false;
    if (state == State::InPath)
        m_path = String::copy(buffer);
    if (state == State::InQuery)
        m_query = String::copy(buffer);
    if (state == State::InFragment)
        m_fragment = String::copy(buffer);
    if (state == State::InDataPayload)
        m_data_payload = urldecode(String::copy(buffer));
    if (state == State::InPort) {
        auto port_opt = String::copy(buffer).to_uint();
        if (port_opt.has_value())
            m_port = port_opt.value();
    }

    if (m_query.is_null())
        m_query = "";
    if (m_fragment.is_null())
        m_fragment = "";

    if (!m_port && scheme_requires_port(m_scheme))
        set_port(default_port_for_scheme(m_scheme));

    return compute_validity();
}

URL::URL(const StringView& string)
{
    m_valid = parse(string);
}

String URL::to_string() const
{
    StringBuilder builder;
    builder.append(m_scheme);

    if (m_scheme == "about") {
        builder.append(':');
        builder.append(m_path);
        return builder.to_string();
    }

    if (m_scheme == "data") {
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
    if (default_port_for_scheme(scheme()) != port()) {
        builder.append(':');
        builder.append(String::number(m_port));
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

    if (scheme() == "data")
        return {};

    if (string.starts_with("//")) {
        URL url(String::formatted("{}:{}", m_scheme, string));
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

void URL::set_scheme(const String& scheme)
{
    m_scheme = scheme;
    m_valid = compute_validity();
}

void URL::set_host(const String& host)
{
    m_host = host;
    m_valid = compute_validity();
}

void URL::set_port(const u16 port)
{
    m_port = port;
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
    if (m_scheme.is_empty())
        return false;

    if (m_scheme == "about") {
        if (m_path.is_empty())
            return false;
        return true;
    }

    if (m_scheme == "file") {
        if (m_path.is_empty())
            return false;
        return true;
    }

    if (m_scheme == "data") {
        if (m_data_mime_type.is_empty())
            return false;
        return true;
    }

    if (m_host.is_empty())
        return false;

    if (!m_port && scheme_requires_port(m_scheme))
        return false;

    return true;
}

bool URL::scheme_requires_port(const StringView& scheme)
{
    return (default_port_for_scheme(scheme) != 0);
}

u16 URL::default_port_for_scheme(const StringView& scheme)
{
    if (scheme == "http")
        return 80;
    if (scheme == "https")
        return 443;
    if (scheme == "gemini")
        return 1965;
    if (scheme == "irc")
        return 6667;
    if (scheme == "ircs")
        return 6697;
    if (scheme == "ws")
        return 80;
    if (scheme == "wss")
        return 443;
    return 0;
}

URL URL::create_with_file_scheme(const String& path, const String& fragment)
{
    URL url;
    url.set_scheme("file");
    url.set_path(path);
    url.set_fragment(fragment);
    return url;
}

URL URL::create_with_url_or_path(const String& url_or_path)
{
    URL url = url_or_path;
    if (url.is_valid())
        return url;

    String path = LexicalPath::canonicalized_path(url_or_path);
    return URL::create_with_file_scheme(path);
}

URL URL::create_with_data(const StringView& mime_type, const StringView& payload, bool is_base64)
{
    URL url;
    url.set_scheme("data");
    url.m_valid = true;
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

void URL::append_percent_encoded(StringBuilder& builder, u32 code_point)
{
    if (code_point <= 0x7f)
        builder.appendff("%{:02X}", code_point);
    else if (code_point <= 0x07ff)
        builder.appendff("%{:02X}%{:02X}", ((code_point >> 6) & 0x1f) | 0xc0, (code_point & 0x3f) | 0x80);
    else if (code_point <= 0xffff)
        builder.appendff("%{:02X}%{:02X}%{:02X}", ((code_point >> 12) & 0x0f) | 0xe0, ((code_point >> 6) & 0x3f) | 0x80, (code_point & 0x3f) | 0x80);
    else if (code_point <= 0x10ffff)
        builder.appendff("%{:02X}%{:02X}%{:02X}%{:02X}", ((code_point >> 18) & 0x07) | 0xf0, ((code_point >> 12) & 0x3f) | 0x80, ((code_point >> 6) & 0x3f) | 0x80, (code_point & 0x3f) | 0x80);
    else
        VERIFY_NOT_REACHED();
}

// https://url.spec.whatwg.org/#c0-control-percent-encode-set
constexpr bool code_point_is_in_percent_encode_set(u32 code_point, URL::PercentEncodeSet set)
{
    switch (set) {
    case URL::PercentEncodeSet::C0Control:
        return code_point < 0x20 || code_point > 0x7E;
    case URL::PercentEncodeSet::Fragment:
        return code_point_is_in_percent_encode_set(code_point, URL::PercentEncodeSet::C0Control) || " \"<>`"sv.contains(code_point);
    case URL::PercentEncodeSet::Query:
        return code_point_is_in_percent_encode_set(code_point, URL::PercentEncodeSet::C0Control) || " \"#<>"sv.contains(code_point);
    case URL::PercentEncodeSet::SpecialQuery:
        return code_point_is_in_percent_encode_set(code_point, URL::PercentEncodeSet::Query) || code_point == '\'';
    case URL::PercentEncodeSet::Path:
        return code_point_is_in_percent_encode_set(code_point, URL::PercentEncodeSet::Query) || "?`{}"sv.contains(code_point);
    case URL::PercentEncodeSet::Userinfo:
        return code_point_is_in_percent_encode_set(code_point, URL::PercentEncodeSet::Path) || "/:;=@[\\]^|"sv.contains(code_point);
    case URL::PercentEncodeSet::Component:
        return code_point_is_in_percent_encode_set(code_point, URL::PercentEncodeSet::Userinfo) || "$%&+,"sv.contains(code_point);
    case URL::PercentEncodeSet::ApplicationXWWWFormUrlencoded:
        return code_point >= 0x7E || !(is_ascii_alphanumeric(code_point) || "!'()~"sv.contains(code_point));
    case URL::PercentEncodeSet::EncodeURI:
        // NOTE: This is the same percent encode set that JS encodeURI() uses.
        // https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/encodeURI
        return code_point >= 0x7E || (!is_ascii_alphanumeric(code_point) && !";,/?:@&=+$-_.!~*'()#"sv.contains(code_point));
    default:
        VERIFY_NOT_REACHED();
    }
}

void URL::append_percent_encoded_if_necessary(StringBuilder& builder, u32 code_point, URL::PercentEncodeSet set)
{
    if (code_point_is_in_percent_encode_set(code_point, set))
        append_percent_encoded(builder, code_point);
    else
        builder.append_code_point(code_point);
}

String URL::percent_encode(const StringView& input, URL::PercentEncodeSet set)
{
    StringBuilder builder;
    for (auto code_point : Utf8View(input)) {
        append_percent_encoded_if_necessary(builder, code_point, set);
    }
    return builder.to_string();
}

constexpr u8 parse_hex_digit(u8 digit)
{
    if (digit >= '0' && digit <= '9')
        return digit - '0';
    if (digit >= 'a' && digit <= 'f')
        return digit - 'a' + 10;
    if (digit >= 'A' && digit <= 'F')
        return digit - 'A' + 10;
    VERIFY_NOT_REACHED();
}

String URL::percent_decode(const StringView& input)
{
    if (!input.contains('%'))
        return input;
    StringBuilder builder;
    Utf8View utf8_view(input);
    for (auto it = utf8_view.begin(); !it.done(); ++it) {
        if (*it != '%') {
            builder.append_code_point(*it);
        } else if (!is_ascii_hex_digit(it.peek(1).value_or(0)) || !is_ascii_hex_digit(it.peek(2).value_or(0))) {
            builder.append_code_point(*it);
        } else {
            ++it;
            u8 byte = parse_hex_digit(*it) << 4;
            ++it;
            byte += parse_hex_digit(*it);
            builder.append(byte);
        }
    }
    return builder.to_string();
}

}

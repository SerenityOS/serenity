/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020, Benoit Lormeau <blormeau@outlook.com>
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

#include <AK/Base64.h>
#include <AK/GenericLexer.h>
#include <AK/LexicalPath.h>
#include <AK/StringBuilder.h>
#include <AK/URL.h>
#include <AK/URLHelper.h>

static bool is_scheme_char(char c)
{
    return is_alphanum(c) || c == '+' || c == '-' || c == '.';
}

static bool is_sub_delim(char c)
{
    return c == '!' || c == '$' || c == '&' || c == '\''
        || c == '(' || c == ')' || c == '*' || c == '+'
        || c == ',' || c == ';' || c == '=';
}

static bool is_unreserved(char c)
{
    return is_alphanum(c) || c == '-' || c == '.' || c == '_' || c == '~';
}

static bool is_percent_encoded(char c)
{
    return c == '%' || is_hex_digit(c);
}

static bool is_pchar(char c)
{
    return is_unreserved(c) || is_percent_encoded(c) || is_sub_delim(c) || c == ':' || c == '@';
}

static HashMap<String, u16, CaseInsensitiveStringTraits> s_protocols_default_port;

namespace AK {

URL::URL(const StringView& string)
{
    m_valid = parse(string) && compute_validity();
}

bool URL::parse(const StringView& input)
{
    GenericLexer lexer(input);

    // Scheme
    if (!lexer.next_is(is_alpha))
        return false;
    else {
        m_scheme = lexer.consume_while(is_scheme_char);
        m_scheme = m_scheme.to_lowercase();
        if (!lexer.consume_specific(':'))
            return false;

        m_port = get_protocol_default_port(m_scheme);
    }

    auto helper = URLHelper::from_scheme(m_scheme);

    if (helper->requires_special_handling())
        return helper->take_over_parsing({}, lexer, *this);

    // Authority
    if (helper->requires_authority_prefix() && !lexer.next_is("//"))
        return false;

    if (lexer.consume_specific("//")) {
        auto authority = lexer.consume_until([](char c) {
            return c == '/' || c == '?' || c == '#';
        });
        if (authority.is_empty() && !helper->can_authority_be_empty())
            return false;

        GenericLexer auth_lexer(authority);

        if (authority.contains('@')) { // There's user infos
            auto username = auth_lexer.consume_while([](char c) {
                return is_unreserved(c) || is_percent_encoded(c) || is_sub_delim(c);
            });
            m_username = decode(username);

            if (auth_lexer.consume_specific(':')) { // There's a password (can be empty)
                auto password = auth_lexer.consume_until('@');
                m_password = password.is_empty() ? "" : decode(password);
            }
            else {
                auth_lexer.ignore(); // Ignore the @
            }
        }

        if (auth_lexer.consume_specific('[')) { // IPv6 addresses are contained within brackets
            m_host = auth_lexer.consume_while([](char c) {
                return (is_hex_digit(c) || c == ':') && c != ']';
            });
            if (!auth_lexer.consume_specific(']'))
                return false;
        }
        else {
            m_host = auth_lexer.consume_while([](char c) {
                return is_unreserved(c) || is_percent_encoded(c) || is_sub_delim(c);
            });
        }
        if (auth_lexer.consume_specific(':')) { // There's a port
            auto port = StringUtils::convert_to_int(auth_lexer.consume_while(is_digit));
            if (port.has_value())
                m_port = port.value();
        }

        if (!auth_lexer.is_eof())
            return false;
    }

    // Path
    auto path = lexer.consume_until([](char c) {
        return c == '?' || c == '#';
    });
    m_path = path.is_empty() ? "/" : decode(path);

    // Query
    if (lexer.consume_specific('?'))
        m_query = lexer.consume_until('#');

    // Fragment
    lexer.consume_specific('#');
    m_fragment = lexer.consume_all();

    return lexer.is_eof();
}

// FIXME: Definitly use regex here if it ever gets into serenity
bool URL::compute_validity() const
{
    // Validate scheme
    if (m_scheme.is_empty() || !is_alpha(m_scheme[0]))
        return false;
    for (auto c : m_scheme) {
        if (!is_scheme_char(c))
            return false;
    }

    // Validate host
    Function<bool(char)> host_char_validator;
    if (m_host.contains(":")) { // Only IPv6 addresses can contain colons
        host_char_validator = [](char c) {
            return c == ':' || is_hex_digit(c);
        };
    }
    else {
        host_char_validator = [](char c) {
            return is_unreserved(c) || is_percent_encoded(c) || is_sub_delim(c);
        };
    }

    for (auto c : m_host) {
        if (!host_char_validator(c))
            return false;
    }

    // Validate query
    for (auto c : m_query) {
        if (!(is_pchar(c) || c == '/' || c == '?'))
            return false;
    }

    // Validate fragment
    for (auto c : m_fragment) {
        if (!(is_pchar(c) || c == '/' || c == '?'))
            return false;
    }

    return true;
}

String URL::to_string() const
{
    if (!m_valid)
        return {};

    StringBuilder builder;
    builder.append(m_scheme);
    builder.append(':');

    auto helper = URLHelper::from_scheme(m_scheme);
    if (helper->requires_special_handling())
        return helper->take_over_serializing({}, builder, *this);

    if (!m_host.is_empty() || helper->requires_authority_prefix()) {
        builder.append("//");
        if (!m_username.is_empty()) {
            builder.append(encode(m_username));
            if (!m_password.is_null()) {
                builder.append(':');
                builder.append(encode(m_password));
            }
            builder.append('@');
        }
        builder.append(m_host);
        if (m_port != get_protocol_default_port(m_scheme)) {
            builder.append(':');
            builder.appendf("%u", m_port);
        }
    }

    // Encode each parts of the path and keep the trailing slashes
    if (m_path.starts_with('/'))
        builder.append('/');
    auto path_parts = m_path.split_view('/');
    bool first = true;
    for (auto part : path_parts) {
        if (first)
            first = false;
        else
            builder.append('/');
        builder.append(encode(part));
    }
    if (m_path.length() > 1 && m_path.ends_with('/'))
        builder.append('/');

    if (!m_query.is_empty()) {
        builder.append('?');
        builder.append(m_query);
    }

    if (!m_fragment.is_empty()) {
        builder.append('#');
        builder.append(encode(m_fragment));
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
        URL url(String::format("%s:%s", m_scheme.characters(), string.characters()));
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
    LexicalPath lexical_path(m_path);
    builder.append('/');

    for (size_t i = 0; i < lexical_path.parts().size(); ++i) {
        if (i == lexical_path.parts().size() - 1 && !m_path.ends_with('/'))
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

String URL::basename() const
{
    if (!m_valid)
        return {};
    return LexicalPath(m_path).basename();
}

void URL::set_scheme(const String& scheme)
{
    m_scheme = scheme.to_lowercase();
    m_valid = compute_validity();
}

void URL::set_username(const String& username)
{
    m_username = username;
    m_valid = compute_validity();
}

void URL::set_password(const String& password)
{
    m_password = password;
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
    m_valid = compute_validity();
}

void URL::set_fragment(const String& fragment)
{
    m_fragment = fragment;
    m_valid = compute_validity();
}

HashMap<String, String> URL::parse_query_fields() const
{
    HashMap<String, String> fields;
    GenericLexer lexer(m_query);

    while (!lexer.is_eof()) {
        auto key = lexer.consume_until([&](char c) {
            return c == '=' || c == '&' || c == ';';
        });
        fields.ensure(key);

        if (lexer.consume_specific('=')) {
            auto value = lexer.consume_until([](char c) {
                return c == '&' || c == ';';
            });
            fields.set(key, value);
        }
        else {
            lexer.ignore();
        }
    }

    return fields;
}

void URL::set_query_fields(const HashMap<String, String>& fields)
{
    StringBuilder builder;
    bool first = true;

    for (auto [key, value] : fields) {
        if (first)
            first = false;
        else
            builder.append('&');

        builder.append(query_encode(key));
        if (!value.is_empty()) {
            builder.append('=');
            builder.append(query_encode(value));
        }
    }

    m_query = builder.to_string();
}

URL URL::create_with_file_protocol(const String& path)
{
    URL url;
    url.set_scheme("file");
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

u16 URL::get_protocol_default_port(const String& protocol)
{
    if (s_protocols_default_port.is_empty()) {
        s_protocols_default_port.set("gemini", 1965);
        s_protocols_default_port.set("http", 80);
        s_protocols_default_port.set("https", 443);
        s_protocols_default_port.set("ssh", 22);
        s_protocols_default_port.set("telnet", 23);
    }

    return s_protocols_default_port.get(protocol).value_or(0);
}

// Encode according to the RFC 3986. Use it to encode URL parts.
String URL::encode(const StringView& input)
{
    StringBuilder builder;
    for (char c : input) {
        if (is_alphanum(c) || c == '-' || c == '.'  || c == '_' || c == '~')
            builder.append(c);
        else {
            builder.append('%');
            builder.appendf("%02X", (u8)c);
        }
    }
    return builder.to_string();
}

String URL::decode(const StringView& input)
{
    if (input.is_empty())
        return {};

    GenericLexer lexer(input);

    StringBuilder builder;
    while (!lexer.is_eof()) {
        if (lexer.consume_specific('%') && is_hex_digit(lexer.peek(0)) && is_hex_digit(lexer.peek(1))) {
            auto hex = lexer.consume(2);
            auto byte_point = StringUtils::convert_to_uint_from_hex(hex);
            builder.append(byte_point.value());
        }
        else {
            builder.append(lexer.consume());
        }
    }
    return builder.to_string();
}

// Encode as a WWW form's data (application/x-www-form-urlencoded)
String URL::query_encode(const StringView& input)
{
    StringBuilder builder;
    for (char c : input) {
        if (c == ' ')
            builder.append('+');
        else if (is_alphanum(c) || c == '-' || c == '.'  || c == '_')
            builder.append(c);
        else {
            builder.append('%');
            builder.appendf("%02X", (u8)c);
        }
    }
    return builder.to_string();
}

String URL::query_decode(const StringView& input)
{
    if (input.is_empty())
        return {};

    GenericLexer lexer(input);

    StringBuilder builder;
    while (!lexer.is_eof()) {
        if (lexer.consume_specific('+')) {
            builder.append(' ');
        }
        else if (lexer.consume_specific('%') && is_hex_digit(lexer.peek(0)) && is_hex_digit(lexer.peek(1))) {
            auto hex = lexer.consume(2);
            auto byte_point = StringUtils::convert_to_uint_from_hex(hex);
            builder.append(byte_point.value());
        }
        else {
            builder.append(lexer.consume());
        }
    }
    return builder.to_string();
}

}

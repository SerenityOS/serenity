#include <AK/StringBuilder.h>
#include <AK/URL.h>

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
    enum class State {
        InProtocol,
        InHostname,
        InPort,
        InPath,
    };

    Vector<char, 256> buffer;
    State state { State::InProtocol };

    int index = 0;

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
        case State::InProtocol:
            if (is_valid_protocol_character(peek())) {
                buffer.append(consume());
                continue;
            }
            if (consume() != ':')
                return false;
            if (consume() != '/')
                return false;
            if (consume() != '/')
                return false;
            if (buffer.is_empty())
                return false;
            m_protocol = String::copy(buffer);
            buffer.clear();
            if (m_protocol == "file")
                state = State::InPath;
            else
                state = State::InHostname;
            continue;
        case State::InHostname:
            if (is_valid_hostname_character(peek())) {
                buffer.append(consume());
                continue;
            }
            if (buffer.is_empty())
                return false;
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
                bool ok;
                m_port = String::copy(buffer).to_uint(ok);
                buffer.clear();
                if (!ok)
                    return false;
            }
            if (peek() == '/') {
                state = State::InPath;
                continue;
            }
            return false;
        case State::InPath:
            buffer.append(consume());
            continue;
        }
    }
    m_path = String::copy(buffer);
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
    builder.append("://");
    if (protocol() != "file") {
        builder.append(m_host);
        builder.append(':');
        builder.append(String::number(m_port));
    }
    builder.append(m_path);
    return builder.to_string();
}

}

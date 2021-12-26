#pragma once

#include <AK/String.h>
#include <AK/StringView.h>

namespace AK {

class URL {
public:
    URL() {}
    URL(const StringView&);
    URL(const char* string)
        : URL(StringView(string))
    {
    }
    URL(const String& string)
        : URL(string.view())
    {
    }

    bool is_valid() const { return m_valid; }
    String protocol() const { return m_protocol; }
    String host() const { return m_host; }
    String path() const { return m_path; }
    u16 port() const { return m_port; }

    void set_protocol(const String& protocol) { m_protocol = protocol; }
    void set_host(const String& host) { m_host = host; }
    void set_path(const String& path) { m_path = path; }
    void set_port(u16 port) { m_port = port; }

    String to_string() const;

private:
    bool parse(const StringView&);

    bool m_valid { false };
    u16 m_port { 80 };
    String m_protocol;
    String m_host;
    String m_path;
};

}

using AK::URL;

inline const LogStream& operator<<(const LogStream& stream, const URL& value)
{
    return stream << value.to_string();
}

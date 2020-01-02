#pragma once

#include <AK/IPv4Address.h>
#include <arpa/inet.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <sys/un.h>

class CSocketAddress {
public:
    enum class Type {
        Invalid,
        IPv4,
        Local
    };

    CSocketAddress() {}
    CSocketAddress(const IPv4Address& address)
        : m_type(Type::IPv4)
        , m_ipv4_address(address)
    {
    }

    CSocketAddress(const IPv4Address& address, u16 port)
        : m_type(Type::IPv4)
        , m_ipv4_address(address)
        , m_port(port)
    {
    }

    static CSocketAddress local(const String& address)
    {
        CSocketAddress addr;
        addr.m_type = Type::Local;
        addr.m_local_address = address;
        return addr;
    }

    Type type() const { return m_type; }
    bool is_valid() const { return m_type != Type::Invalid; }
    IPv4Address ipv4_address() const { return m_ipv4_address; }
    u16 port() const { return m_port; }

    String to_string() const
    {
        switch (m_type) {
        case Type::IPv4:
            return String::format("%s:%d", m_ipv4_address.to_string().characters(), m_port);
        case Type::Local:
            return m_local_address;
        default:
            return "[CSocketAddress]";
        }
    }

    sockaddr_un to_sockaddr_un() const
    {
        ASSERT(type() == Type::Local);
        sockaddr_un address;
        address.sun_family = AF_LOCAL;
        RELEASE_ASSERT(m_local_address.length() < (int)sizeof(address.sun_path));
        strcpy(address.sun_path, m_local_address.characters());
        return address;
    }

    sockaddr_in to_sockaddr_in() const
    {
        ASSERT(type() == Type::IPv4);
        sockaddr_in address;
        address.sin_family = AF_INET;
        address.sin_addr.s_addr = m_ipv4_address.to_in_addr_t();
        address.sin_port = htons(m_port);
        return address;
    }

private:
    Type m_type { Type::Invalid };
    IPv4Address m_ipv4_address;
    u16 m_port { 0 };
    String m_local_address;
};

inline const LogStream& operator<<(const LogStream& stream, const CSocketAddress& value)
{
    return stream << value.to_string();
}

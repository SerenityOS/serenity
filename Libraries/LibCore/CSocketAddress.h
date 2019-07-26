#pragma once

#include <AK/IPv4Address.h>
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

    String to_string() const
    {
        switch (m_type) {
        case Type::IPv4:
            return m_ipv4_address.to_string();
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

private:
    Type m_type { Type::Invalid };
    IPv4Address m_ipv4_address;
    String m_local_address;
};

inline const LogStream& operator<<(const LogStream& stream, const CSocketAddress& value)
{
    return stream << value.to_string();
}

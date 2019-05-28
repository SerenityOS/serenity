#pragma once

#include <Kernel/Net/IPv4.h>

class CSocketAddress {
public:
    enum class Type
    {
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

    Type type() const { return m_type; }
    bool is_valid() const { return m_type != Type::Invalid; }
    IPv4Address ipv4_address() const { return m_ipv4_address; }

    String to_string() const
    {
        switch (m_type) {
        case Type::IPv4:
            return m_ipv4_address.to_string();
        default:
            return "[CSocketAddress]";
        }
    }

private:
    Type m_type { Type::Invalid };
    IPv4Address m_ipv4_address;
};

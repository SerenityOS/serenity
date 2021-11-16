/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/IPv4Address.h>
#include <arpa/inet.h>
#include <netinet/in.h>
#include <string.h>
#include <sys/socket.h>
#include <sys/un.h>

namespace Core {

class SocketAddress {
public:
    enum class Type {
        Invalid,
        IPv4,
        Local
    };

    SocketAddress() { }
    SocketAddress(const IPv4Address& address)
        : m_type(Type::IPv4)
        , m_ipv4_address(address)
    {
    }

    SocketAddress(const IPv4Address& address, u16 port)
        : m_type(Type::IPv4)
        , m_ipv4_address(address)
        , m_port(port)
    {
    }

    static SocketAddress local(const String& address)
    {
        SocketAddress addr;
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
            return String::formatted("{}:{}", m_ipv4_address, m_port);
        case Type::Local:
            return m_local_address;
        default:
            return "[SocketAddress]";
        }
    }

    Optional<sockaddr_un> to_sockaddr_un() const
    {
        VERIFY(type() == Type::Local);
        sockaddr_un address;
        address.sun_family = AF_LOCAL;
        bool fits = m_local_address.copy_characters_to_buffer(address.sun_path, sizeof(address.sun_path));
        if (!fits)
            return {};
        return address;
    }

    sockaddr_in to_sockaddr_in() const
    {
        VERIFY(type() == Type::IPv4);
        sockaddr_in address {};
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

}

template<>
struct AK::Formatter<Core::SocketAddress> : Formatter<String> {
    ErrorOr<void> format(FormatBuilder& builder, Core::SocketAddress const& value)
    {
        return Formatter<String>::format(builder, value.to_string());
    }
};

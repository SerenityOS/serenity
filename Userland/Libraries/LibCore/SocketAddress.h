/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
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

    SocketAddress() = default;
    SocketAddress(IPv4Address const& address)
        : m_type(Type::IPv4)
        , m_ipv4_address(address)
    {
    }

    SocketAddress(IPv4Address const& address, u16 port)
        : m_type(Type::IPv4)
        , m_ipv4_address(address)
        , m_port(port)
    {
    }

    static SocketAddress local(ByteString const& address)
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

    ByteString to_byte_string() const
    {
        switch (m_type) {
        case Type::IPv4:
            return ByteString::formatted("{}:{}", m_ipv4_address, m_port);
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

    bool operator==(SocketAddress const& other) const = default;
    bool operator!=(SocketAddress const& other) const = default;

private:
    Type m_type { Type::Invalid };
    IPv4Address m_ipv4_address;
    u16 m_port { 0 };
    ByteString m_local_address;
};

}

template<>
struct AK::Formatter<Core::SocketAddress> : Formatter<ByteString> {
    ErrorOr<void> format(FormatBuilder& builder, Core::SocketAddress const& value)
    {
        return Formatter<ByteString>::format(builder, value.to_byte_string());
    }
};

template<>
struct AK::Traits<Core::SocketAddress> : public DefaultTraits<Core::SocketAddress> {
    static unsigned hash(Core::SocketAddress const& socket_address)
    {
        return pair_int_hash(Traits<IPv4Address>::hash(socket_address.ipv4_address()), Traits<u16>::hash(socket_address.port()));
    }
};

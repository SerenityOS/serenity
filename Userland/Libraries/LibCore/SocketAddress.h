/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/IPv4Address.h>
#include <string.h>

#if defined(AK_OS_WINDOWS)
#    include <WS2tcpip.h>
#    include <WinSock2.h>
#    include <afunix.h>
#else
#    include <arpa/inet.h>
#    include <netinet/in.h>
#    include <sys/socket.h>
#    include <sys/un.h>
#endif

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

    static SocketAddress local(DeprecatedString const& address)
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

    DeprecatedString to_deprecated_string() const
    {
        switch (m_type) {
        case Type::IPv4:
            return DeprecatedString::formatted("{}:{}", m_ipv4_address, m_port);
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
        address.sun_family = AF_UNIX;
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
    DeprecatedString m_local_address;
};

}

template<>
struct AK::Formatter<Core::SocketAddress> : Formatter<DeprecatedString> {
    ErrorOr<void> format(FormatBuilder& builder, Core::SocketAddress const& value)
    {
        return Formatter<DeprecatedString>::format(builder, value.to_deprecated_string());
    }
};

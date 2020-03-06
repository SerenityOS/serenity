/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#pragma once

#include <AK/IPv4Address.h>
#include <arpa/inet.h>
#include <netinet/in.h>
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

    SocketAddress() {}
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
            return String::format("%s:%d", m_ipv4_address.to_string().characters(), m_port);
        case Type::Local:
            return m_local_address;
        default:
            return "[SocketAddress]";
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

const LogStream& operator<<(const LogStream&, const SocketAddress&);

}

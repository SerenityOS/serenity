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

#include <AK/HashMap.h>
#include <Kernel/DoubleBuffer.h>
#include <Kernel/KBuffer.h>
#include <Kernel/Lock.h>
#include <Kernel/Net/IPv4.h>
#include <Kernel/Net/Socket.h>

class IPv4SocketTuple {
public:
    IPv4SocketTuple(IPv4Address local_address, u16 local_port, IPv4Address peer_address, u16 peer_port)
        : m_local_address(local_address)
        , m_local_port(local_port)
        , m_peer_address(peer_address)
        , m_peer_port(peer_port) {};

    IPv4Address local_address() const { return m_local_address; };
    u16 local_port() const { return m_local_port; };
    IPv4Address peer_address() const { return m_peer_address; };
    u16 peer_port() const { return m_peer_port; };

    bool operator==(const IPv4SocketTuple other) const
    {
        return other.local_address() == m_local_address && other.local_port() == m_local_port && other.peer_address() == m_peer_address && other.peer_port() == m_peer_port;
    };

    String to_string() const
    {
        return String::format(
            "%s:%d -> %s:%d",
            m_local_address.to_string().characters(),
            m_local_port,
            m_peer_address.to_string().characters(),
            m_peer_port);
    }

private:
    IPv4Address m_local_address;
    u16 m_local_port { 0 };
    IPv4Address m_peer_address;
    u16 m_peer_port { 0 };
};

namespace AK {

template<>
struct Traits<IPv4SocketTuple> : public GenericTraits<IPv4SocketTuple> {
    static unsigned hash(const IPv4SocketTuple& tuple)
    {
        auto h1 = pair_int_hash(tuple.local_address().to_u32(), tuple.local_port());
        auto h2 = pair_int_hash(tuple.peer_address().to_u32(), tuple.peer_port());
        return pair_int_hash(h1, h2);
    }
};

}

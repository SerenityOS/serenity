/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <Kernel/Library/DoubleBuffer.h>
#include <Kernel/Library/KBuffer.h>
#include <Kernel/Library/KString.h>
#include <Kernel/Locking/Mutex.h>
#include <Kernel/Net/IP/IPv4.h>
#include <Kernel/Net/Socket.h>

namespace Kernel {

class IPv4SocketTuple {
public:
    IPv4SocketTuple(IPv4Address local_address, u16 local_port, IPv4Address peer_address, u16 peer_port)
        : m_local_address(local_address)
        , m_local_port(local_port)
        , m_peer_address(peer_address)
        , m_peer_port(peer_port) {};

    IPv4Address local_address() const { return m_local_address; }
    u16 local_port() const { return m_local_port; }
    IPv4Address peer_address() const { return m_peer_address; }
    u16 peer_port() const { return m_peer_port; }

    bool operator==(IPv4SocketTuple const& other) const
    {
        return other.local_address() == m_local_address && other.local_port() == m_local_port && other.peer_address() == m_peer_address && other.peer_port() == m_peer_port;
    }

    ErrorOr<NonnullOwnPtr<KString>> to_string() const
    {
        return KString::formatted(
            "{}:{} -> {}:{}",
            m_local_address,
            m_local_port,
            m_peer_address,
            m_peer_port);
    }

private:
    IPv4Address m_local_address;
    u16 m_local_port { 0 };
    IPv4Address m_peer_address;
    u16 m_peer_port { 0 };
};

}

namespace AK {

template<>
struct Traits<Kernel::IPv4SocketTuple> : public DefaultTraits<Kernel::IPv4SocketTuple> {
    static unsigned hash(Kernel::IPv4SocketTuple const& tuple)
    {
        auto h1 = pair_int_hash(tuple.local_address().to_u32(), tuple.local_port());
        auto h2 = pair_int_hash(tuple.peer_address().to_u32(), tuple.peer_port());
        return pair_int_hash(h1, h2);
    }
};

}

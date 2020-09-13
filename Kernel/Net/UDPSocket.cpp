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

#include <AK/Singleton.h>
#include <Kernel/Devices/RandomDevice.h>
#include <Kernel/Net/NetworkAdapter.h>
#include <Kernel/Net/Routing.h>
#include <Kernel/Net/UDP.h>
#include <Kernel/Net/UDPSocket.h>
#include <Kernel/Process.h>
#include <Kernel/Random.h>

namespace Kernel {

void UDPSocket::for_each(Function<void(const UDPSocket&)> callback)
{
    LOCKER(sockets_by_port().lock(), Lock::Mode::Shared);
    for (auto it : sockets_by_port().resource())
        callback(*it.value);
}

static AK::Singleton<Lockable<HashMap<u16, UDPSocket*>>> s_map;

Lockable<HashMap<u16, UDPSocket*>>& UDPSocket::sockets_by_port()
{
    return *s_map;
}

SocketHandle<UDPSocket> UDPSocket::from_port(u16 port)
{
    RefPtr<UDPSocket> socket;
    {
        LOCKER(sockets_by_port().lock(), Lock::Mode::Shared);
        auto it = sockets_by_port().resource().find(port);
        if (it == sockets_by_port().resource().end())
            return {};
        socket = (*it).value;
        ASSERT(socket);
    }
    return { *socket };
}

UDPSocket::UDPSocket(int protocol)
    : IPv4Socket(SOCK_DGRAM, protocol)
{
}

UDPSocket::~UDPSocket()
{
    LOCKER(sockets_by_port().lock());
    sockets_by_port().resource().remove(local_port());
}

NonnullRefPtr<UDPSocket> UDPSocket::create(int protocol)
{
    return adopt(*new UDPSocket(protocol));
}

KResultOr<size_t> UDPSocket::protocol_receive(const KBuffer& packet_buffer, UserOrKernelBuffer& buffer, size_t buffer_size, int flags)
{
    (void)flags;
    auto& ipv4_packet = *(const IPv4Packet*)(packet_buffer.data());
    auto& udp_packet = *static_cast<const UDPPacket*>(ipv4_packet.payload());
    ASSERT(udp_packet.length() >= sizeof(UDPPacket)); // FIXME: This should be rejected earlier.
    ASSERT(buffer_size >= (udp_packet.length() - sizeof(UDPPacket)));
    if (!buffer.write(udp_packet.payload(), udp_packet.length() - sizeof(UDPPacket)))
        return KResult(-EFAULT);
    return udp_packet.length() - sizeof(UDPPacket);
}

KResultOr<size_t> UDPSocket::protocol_send(const UserOrKernelBuffer& data, size_t data_length)
{
    auto routing_decision = route_to(peer_address(), local_address(), bound_interface());
    if (routing_decision.is_zero())
        return KResult(-EHOSTUNREACH);
    auto buffer = ByteBuffer::create_zeroed(sizeof(UDPPacket) + data_length);
    auto& udp_packet = *(UDPPacket*)(buffer.data());
    udp_packet.set_source_port(local_port());
    udp_packet.set_destination_port(peer_port());
    udp_packet.set_length(sizeof(UDPPacket) + data_length);
    if (!data.read(udp_packet.payload(), data_length))
        return KResult(-EFAULT);
    klog() << "sending as udp packet from " << routing_decision.adapter->ipv4_address().to_string().characters() << ":" << local_port() << " to " << peer_address().to_string().characters() << ":" << peer_port() << "!";
    auto udp_packet_buffer = UserOrKernelBuffer::for_kernel_buffer((u8*)&udp_packet);
    routing_decision.adapter->send_ipv4(routing_decision.next_hop, peer_address(), IPv4Protocol::UDP, udp_packet_buffer, buffer.size(), ttl());
    return data_length;
}

KResult UDPSocket::protocol_connect(FileDescription&, ShouldBlock)
{
    m_role = Role::Connected;
    set_connected(true);
    return KSuccess;
}

int UDPSocket::protocol_allocate_local_port()
{
    static const u16 first_ephemeral_port = 32768;
    static const u16 last_ephemeral_port = 60999;
    static const u16 ephemeral_port_range_size = last_ephemeral_port - first_ephemeral_port;
    u16 first_scan_port = first_ephemeral_port + get_good_random<u16>() % ephemeral_port_range_size;

    LOCKER(sockets_by_port().lock());
    for (u16 port = first_scan_port;;) {
        auto it = sockets_by_port().resource().find(port);
        if (it == sockets_by_port().resource().end()) {
            set_local_port(port);
            sockets_by_port().resource().set(port, this);
            return port;
        }
        ++port;
        if (port > last_ephemeral_port)
            port = first_ephemeral_port;
        if (port == first_scan_port)
            break;
    }
    return -EADDRINUSE;
}

KResult UDPSocket::protocol_bind()
{
    LOCKER(sockets_by_port().lock());
    if (sockets_by_port().resource().contains(local_port()))
        return KResult(-EADDRINUSE);
    sockets_by_port().resource().set(local_port(), this);
    return KSuccess;
}

}

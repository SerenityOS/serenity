#include <Kernel/Devices/RandomDevice.h>
#include <Kernel/Net/NetworkAdapter.h>
#include <Kernel/Net/Routing.h>
#include <Kernel/Net/TCP.h>
#include <Kernel/Net/TCPSocket.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/Process.h>

Lockable<HashMap<u16, TCPSocket*>>& TCPSocket::sockets_by_port()
{
    static Lockable<HashMap<u16, TCPSocket*>>* s_map;
    if (!s_map)
        s_map = new Lockable<HashMap<u16, TCPSocket*>>;
    return *s_map;
}

TCPSocketHandle TCPSocket::from_port(u16 port)
{
    RefPtr<TCPSocket> socket;
    {
        LOCKER(sockets_by_port().lock());
        auto it = sockets_by_port().resource().find(port);
        if (it == sockets_by_port().resource().end())
            return {};
        socket = (*it).value;
        ASSERT(socket);
    }
    return { move(socket) };
}

TCPSocket::TCPSocket(int protocol)
    : IPv4Socket(SOCK_STREAM, protocol)
{
}

TCPSocket::~TCPSocket()
{
    LOCKER(sockets_by_port().lock());
    sockets_by_port().resource().remove(local_port());
}

NonnullRefPtr<TCPSocket> TCPSocket::create(int protocol)
{
    return adopt(*new TCPSocket(protocol));
}

int TCPSocket::protocol_receive(const ByteBuffer& packet_buffer, void* buffer, size_t buffer_size, int flags)
{
    (void)flags;
    ASSERT(!packet_buffer.is_null());
    auto& ipv4_packet = *(const IPv4Packet*)(packet_buffer.pointer());
    auto& tcp_packet = *static_cast<const TCPPacket*>(ipv4_packet.payload());
    size_t payload_size = packet_buffer.size() - sizeof(IPv4Packet) - tcp_packet.header_size();
    kprintf("payload_size %u, will it fit in %u?\n", payload_size, buffer_size);
    ASSERT(buffer_size >= payload_size);
    memcpy(buffer, tcp_packet.payload(), payload_size);
    return payload_size;
}

int TCPSocket::protocol_send(const void* data, int data_length)
{
    auto* adapter = adapter_for_route_to(peer_address());
    if (!adapter)
        return -EHOSTUNREACH;
    send_tcp_packet(TCPFlags::PUSH | TCPFlags::ACK, data, data_length);
    return data_length;
}

void TCPSocket::send_tcp_packet(u16 flags, const void* payload, int payload_size)
{
    // FIXME: Maybe the socket should be bound to an adapter instead of looking it up every time?
    auto* adapter = adapter_for_route_to(peer_address());
    ASSERT(adapter);

    auto buffer = ByteBuffer::create_zeroed(sizeof(TCPPacket) + payload_size);
    auto& tcp_packet = *(TCPPacket*)(buffer.pointer());
    ASSERT(local_port());
    tcp_packet.set_source_port(local_port());
    tcp_packet.set_destination_port(peer_port());
    tcp_packet.set_window_size(1024);
    tcp_packet.set_sequence_number(m_sequence_number);
    tcp_packet.set_data_offset(sizeof(TCPPacket) / sizeof(u32));
    tcp_packet.set_flags(flags);

    if (flags & TCPFlags::ACK)
        tcp_packet.set_ack_number(m_ack_number);

    if (flags == TCPFlags::SYN) {
        ++m_sequence_number;
    } else {
        m_sequence_number += payload_size;
    }

    memcpy(tcp_packet.payload(), payload, payload_size);
    tcp_packet.set_checksum(compute_tcp_checksum(adapter->ipv4_address(), peer_address(), tcp_packet, payload_size));
    kprintf("sending tcp packet from %s:%u to %s:%u with (%s %s) seq_no=%u, ack_no=%u\n",
        adapter->ipv4_address().to_string().characters(),
        local_port(),
        peer_address().to_string().characters(),
        peer_port(),
        tcp_packet.has_syn() ? "SYN" : "",
        tcp_packet.has_ack() ? "ACK" : "",
        tcp_packet.sequence_number(),
        tcp_packet.ack_number());
    adapter->send_ipv4(MACAddress(), peer_address(), IPv4Protocol::TCP, move(buffer));
}

NetworkOrdered<u16> TCPSocket::compute_tcp_checksum(const IPv4Address& source, const IPv4Address& destination, const TCPPacket& packet, u16 payload_size)
{
    struct [[gnu::packed]] PseudoHeader
    {
        IPv4Address source;
        IPv4Address destination;
        u8 zero;
        u8 protocol;
        NetworkOrdered<u16> payload_size;
    };

    PseudoHeader pseudo_header { source, destination, 0, (u8)IPv4Protocol::TCP, sizeof(TCPPacket) + payload_size };

    u32 checksum = 0;
    auto* w = (const NetworkOrdered<u16>*)&pseudo_header;
    for (size_t i = 0; i < sizeof(pseudo_header) / sizeof(u16); ++i) {
        checksum += w[i];
        if (checksum > 0xffff)
            checksum = (checksum >> 16) + (checksum & 0xffff);
    }
    w = (const NetworkOrdered<u16>*)&packet;
    for (size_t i = 0; i < sizeof(packet) / sizeof(u16); ++i) {
        checksum += w[i];
        if (checksum > 0xffff)
            checksum = (checksum >> 16) + (checksum & 0xffff);
    }
    ASSERT(packet.data_offset() * 4 == sizeof(TCPPacket));
    w = (const NetworkOrdered<u16>*)packet.payload();
    for (size_t i = 0; i < payload_size / sizeof(u16); ++i) {
        checksum += w[i];
        if (checksum > 0xffff)
            checksum = (checksum >> 16) + (checksum & 0xffff);
    }
    if (payload_size & 1) {
        u16 expanded_byte = ((const u8*)packet.payload())[payload_size - 1] << 8;
        checksum += expanded_byte;
        if (checksum > 0xffff)
            checksum = (checksum >> 16) + (checksum & 0xffff);
    }
    return ~(checksum & 0xffff);
}

KResult TCPSocket::protocol_connect(FileDescription& description, ShouldBlock should_block)
{
    auto* adapter = adapter_for_route_to(peer_address());
    if (!adapter)
        return KResult(-EHOSTUNREACH);

    allocate_local_port_if_needed();

    m_sequence_number = 0;
    m_ack_number = 0;

    send_tcp_packet(TCPFlags::SYN);
    m_state = State::Connecting;

    if (should_block == ShouldBlock::Yes) {
        if (current->block<Thread::ConnectBlocker>(description) == Thread::BlockResult::InterruptedBySignal)
            return KResult(-EINTR);
        ASSERT(is_connected());
        return KSuccess;
    }

    return KResult(-EINPROGRESS);
}

int TCPSocket::protocol_allocate_local_port()
{
    static const u16 first_ephemeral_port = 32768;
    static const u16 last_ephemeral_port = 60999;
    static const u16 ephemeral_port_range_size = last_ephemeral_port - first_ephemeral_port;
    u16 first_scan_port = first_ephemeral_port + RandomDevice::random_value() % ephemeral_port_range_size;

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

bool TCPSocket::protocol_is_disconnected() const
{
    return m_state == State::Disconnecting || m_state == State::Disconnected;
}

KResult TCPSocket::protocol_bind()
{
    LOCKER(sockets_by_port().lock());
    if (sockets_by_port().resource().contains(local_port()))
        return KResult(-EADDRINUSE);
    sockets_by_port().resource().set(local_port(), this);
    return KSuccess;
}

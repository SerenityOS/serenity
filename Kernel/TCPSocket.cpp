#include <Kernel/TCPSocket.h>
#include <Kernel/TCP.h>
#include <Kernel/NetworkAdapter.h>
#include <Kernel/Process.h>
#include <Kernel/RandomDevice.h>

Lockable<HashMap<word, TCPSocket*>>& TCPSocket::sockets_by_port()
{
    static Lockable<HashMap<word, TCPSocket*>>* s_map;
    if (!s_map)
        s_map = new Lockable<HashMap<word, TCPSocket*>>;
    return *s_map;
}

TCPSocketHandle TCPSocket::from_port(word port)
{
    RetainPtr<TCPSocket> socket;
    {
        LOCKER(sockets_by_port().lock());
        auto it = sockets_by_port().resource().find(port);
        if (it == sockets_by_port().resource().end())
            return { };
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
    sockets_by_port().resource().remove(source_port());
}

Retained<TCPSocket> TCPSocket::create(int protocol)
{
    return adopt(*new TCPSocket(protocol));
}

int TCPSocket::protocol_receive(const ByteBuffer& packet_buffer, void* buffer, size_t buffer_size, int flags, sockaddr* addr, socklen_t* addr_length)
{
    (void)flags;
    (void)addr_length;
    ASSERT(!packet_buffer.is_null());
    auto& ipv4_packet = *(const IPv4Packet*)(packet_buffer.pointer());
    auto& tcp_packet = *static_cast<const TCPPacket*>(ipv4_packet.payload());
    size_t payload_size = packet_buffer.size() - sizeof(IPv4Packet) - tcp_packet.header_size();
    kprintf("payload_size %u, will it fit in %u?\n", payload_size, buffer_size);
    ASSERT(buffer_size >= payload_size);
    if (addr) {
        auto& ia = *(sockaddr_in*)addr;
        ia.sin_port = htons(tcp_packet.destination_port());
    }
    memcpy(buffer, tcp_packet.payload(), payload_size);
    return payload_size;
}

int TCPSocket::protocol_send(const void* data, int data_length)
{
    // FIXME: Figure out the adapter somehow differently.
    auto* adapter = NetworkAdapter::from_ipv4_address(IPv4Address(192, 168, 5, 2));
    if (!adapter)
        ASSERT_NOT_REACHED();
    send_tcp_packet(TCPFlags::PUSH | TCPFlags::ACK, data, data_length);
    return data_length;
}

void TCPSocket::send_tcp_packet(word flags, const void* payload, int payload_size)
{
    // FIXME: Figure out the adapter somehow differently.
    auto& adapter = *NetworkAdapter::from_ipv4_address(IPv4Address(192, 168, 5, 2));

    auto buffer = ByteBuffer::create_zeroed(sizeof(TCPPacket) + payload_size);
    auto& tcp_packet = *(TCPPacket*)(buffer.pointer());
    ASSERT(source_port());
    tcp_packet.set_source_port(source_port());
    tcp_packet.set_destination_port(destination_port());
    tcp_packet.set_window_size(1024);
    tcp_packet.set_sequence_number(m_sequence_number);
    tcp_packet.set_data_offset(sizeof(TCPPacket) / sizeof(dword));
    tcp_packet.set_flags(flags);

    if (flags & TCPFlags::ACK)
        tcp_packet.set_ack_number(m_ack_number);

    if (flags == TCPFlags::SYN) {
        ++m_sequence_number;
    } else {
        m_sequence_number += payload_size;
    }

    memcpy(tcp_packet.payload(), payload, payload_size);
    tcp_packet.set_checksum(compute_tcp_checksum(adapter.ipv4_address(), destination_address(), tcp_packet, payload_size));
    kprintf("sending tcp packet from %s:%u to %s:%u with (%s %s) seq_no=%u, ack_no=%u\n",
        adapter.ipv4_address().to_string().characters(),
        source_port(),
        destination_address().to_string().characters(),
        destination_port(),
        tcp_packet.has_syn() ? "SYN" : "",
        tcp_packet.has_ack() ? "ACK" : "",
        tcp_packet.sequence_number(),
        tcp_packet.ack_number()
    );
    adapter.send_ipv4(MACAddress(), destination_address(), IPv4Protocol::TCP, move(buffer));
}

NetworkOrdered<word> TCPSocket::compute_tcp_checksum(const IPv4Address& source, const IPv4Address& destination, const TCPPacket& packet, word payload_size)
{
    struct [[gnu::packed]] PseudoHeader {
        IPv4Address source;
        IPv4Address destination;
        byte zero;
        byte protocol;
        NetworkOrdered<word> payload_size;
    };

    PseudoHeader pseudo_header { source, destination, 0, (byte)IPv4Protocol::TCP, sizeof(TCPPacket) + payload_size };

    dword checksum = 0;
    auto* w = (const NetworkOrdered<word>*)&pseudo_header;
    for (size_t i = 0; i < sizeof(pseudo_header) / sizeof(word); ++i) {
        checksum += w[i];
        if (checksum > 0xffff)
            checksum = (checksum >> 16) + (checksum & 0xffff);
    }
    w = (const NetworkOrdered<word>*)&packet;
    for (size_t i = 0; i < sizeof(packet) / sizeof(word); ++i) {
        checksum += w[i];
        if (checksum > 0xffff)
            checksum = (checksum >> 16) + (checksum & 0xffff);
    }
    ASSERT(packet.data_offset() * 4 == sizeof(TCPPacket));
    w = (const NetworkOrdered<word>*)packet.payload();
    for (size_t i = 0; i < payload_size / sizeof(word); ++i) {
        checksum += w[i];
        if (checksum > 0xffff)
            checksum = (checksum >> 16) + (checksum & 0xffff);
    }
    if (payload_size & 1) {
        word expanded_byte = ((const byte*)packet.payload())[payload_size - 1] << 8;
        checksum += expanded_byte;
        if (checksum > 0xffff)
            checksum = (checksum >> 16) + (checksum & 0xffff);
    }
    return ~(checksum & 0xffff);
}

KResult TCPSocket::protocol_connect()
{
    // FIXME: Figure out the adapter somehow differently.
    auto* adapter = NetworkAdapter::from_ipv4_address(IPv4Address(192, 168, 5, 2));
    if (!adapter)
        ASSERT_NOT_REACHED();

    allocate_source_port_if_needed();

    m_sequence_number = 0;
    m_ack_number = 0;

    send_tcp_packet(TCPFlags::SYN);
    m_state = State::Connecting;

    current->set_blocked_socket(this);
    current->block(Thread::BlockedConnect);

    ASSERT(is_connected());
    return KSuccess;
}

int TCPSocket::protocol_allocate_source_port()
{
    static const word first_ephemeral_port = 32768;
    static const word last_ephemeral_port = 60999;
    static const word ephemeral_port_range_size = last_ephemeral_port - first_ephemeral_port;
    word first_scan_port = first_ephemeral_port + (word)(RandomDevice::random_percentage() * ephemeral_port_range_size);

    LOCKER(sockets_by_port().lock());
    for (word port = first_scan_port;;) {
        auto it = sockets_by_port().resource().find(port);
        if (it == sockets_by_port().resource().end()) {
            set_source_port(port);
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

#include <Kernel/Net/ICMPSocket.h>
#include <Kernel/Net/Routing.h>

ICMPSocket::ICMPSocket()
    : IPv4Socket(SOCK_DGRAM, IPPROTO_ICMP)
{
}

ICMPSocket::~ICMPSocket()
{
}

NonnullRefPtr<ICMPSocket> ICMPSocket::create()
{
    return adopt(*new ICMPSocket());
}

int ICMPSocket::protocol_receive(const KBuffer& packet_buffer, void* buffer, size_t buffer_size, int flags)
{
    (void)flags;
    auto& ipv4_packet = *(const IPv4Packet*)(packet_buffer.data());
    auto v4 = ByteBuffer::wrap(ipv4_packet.payload(), ipv4_packet.payload_size());

    size_t to_copy = min(buffer_size, (size_t)ipv4_packet.payload_size());
    memcpy(buffer, ipv4_packet.payload(), to_copy);
    return ipv4_packet.payload_size();
}

int ICMPSocket::protocol_send(const void* data, int data_length)
{
    if ((size_t)data_length < sizeof(ICMPHeader)) {
        // We must have at least the header to consider this a valid send
        return -EINVAL;
    }

    auto& header = *static_cast<const ICMPHeader*>(data);

    if (header.type() != ICMPType::EchoRequest || header.code() != 0) {
        return -EINVAL;
    }

    // We have to copy the request here because the data is const
    ByteBuffer buffer = ByteBuffer::create_zeroed(data_length);
    memcpy(buffer.data(), data, data_length);
    auto& icmp_request = *(ICMPEchoPacket*)buffer.data();
    // Clear out the checksum before we calculate it
    icmp_request.header.set_checksum(0);
    icmp_request.header.set_checksum(internet_checksum(buffer.data(), buffer.size()));

    auto routing_decision = route_to(peer_address(), local_address());
    if (routing_decision.is_zero())
        return -EHOSTUNREACH;

    routing_decision.adapter->send_ipv4(routing_decision.next_hop, peer_address(), IPv4Protocol::ICMP, (const u8*)buffer.data(), buffer.size(), ttl());
    return data_length;
}

KResult ICMPSocket::protocol_connect(FileDescription&, ShouldBlock)
{
    // connect isn't supported for ICMP sockets
    return KResult(-EINVAL);
}

int ICMPSocket::protocol_allocate_local_port()
{
    return 0;
}

KResult ICMPSocket::protocol_bind()
{
    // There aren't any ports here to worry about...
    return KSuccess;
}

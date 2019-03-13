#include <Kernel/IPv4Socket.h>
#include <Kernel/UnixTypes.h>
#include <Kernel/Process.h>
#include <Kernel/NetworkAdapter.h>
#include <Kernel/IPv4.h>
#include <Kernel/ICMP.h>
#include <Kernel/TCP.h>
#include <Kernel/UDP.h>
#include <Kernel/ARP.h>
#include <LibC/errno_numbers.h>

#define IPV4_SOCKET_DEBUG

Lockable<HashMap<word, IPv4Socket*>>& IPv4Socket::sockets_by_udp_port()
{
    static Lockable<HashMap<word, IPv4Socket*>>* s_map;
    if (!s_map)
        s_map = new Lockable<HashMap<word, IPv4Socket*>>;
    return *s_map;
}

Lockable<HashMap<word, IPv4Socket*>>& IPv4Socket::sockets_by_tcp_port()
{
    static Lockable<HashMap<word, IPv4Socket*>>* s_map;
    if (!s_map)
        s_map = new Lockable<HashMap<word, IPv4Socket*>>;
    return *s_map;
}

Lockable<HashTable<IPv4Socket*>>& IPv4Socket::all_sockets()
{
    static Lockable<HashTable<IPv4Socket*>>* s_table;
    if (!s_table)
        s_table = new Lockable<HashTable<IPv4Socket*>>;
    return *s_table;
}

Retained<IPv4Socket> IPv4Socket::create(int type, int protocol)
{
    return adopt(*new IPv4Socket(type, protocol));
}

IPv4Socket::IPv4Socket(int type, int protocol)
    : Socket(AF_INET, type, protocol)
{
    kprintf("%s(%u) IPv4Socket{%p} created with type=%u, protocol=%d\n", current->name().characters(), current->pid(), this, type, protocol);
    LOCKER(all_sockets().lock());
    all_sockets().resource().set(this);
}

IPv4Socket::~IPv4Socket()
{
    {
        LOCKER(all_sockets().lock());
        all_sockets().resource().remove(this);
    }
    if (type() == SOCK_DGRAM) {
        LOCKER(sockets_by_udp_port().lock());
        sockets_by_udp_port().resource().remove(m_source_port);
    }
    if (type() == SOCK_STREAM) {
        LOCKER(sockets_by_tcp_port().lock());
        sockets_by_tcp_port().resource().remove(m_source_port);
    }
}

bool IPv4Socket::get_address(sockaddr* address, socklen_t* address_size)
{
    // FIXME: Look into what fallback behavior we should have here.
    if (*address_size != sizeof(sockaddr_in))
        return false;
    memcpy(address, &m_destination_address, sizeof(sockaddr_in));
    *address_size = sizeof(sockaddr_in);
    return true;
}

KResult IPv4Socket::bind(const sockaddr* address, socklen_t address_size)
{
    ASSERT(!is_connected());
    if (address_size != sizeof(sockaddr_in))
        return KResult(-EINVAL);
    if (address->sa_family != AF_INET)
        return KResult(-EINVAL);

    ASSERT_NOT_REACHED();
}

KResult IPv4Socket::connect(const sockaddr* address, socklen_t address_size)
{
    ASSERT(!m_bound);
    if (address_size != sizeof(sockaddr_in))
        return KResult(-EINVAL);
    if (address->sa_family != AF_INET)
        return KResult(-EINVAL);

    auto& ia = *(const sockaddr_in*)address;
    m_destination_address = IPv4Address((const byte*)&ia.sin_addr.s_addr);
    m_destination_port = ntohs(ia.sin_port);

    if (type() == SOCK_STREAM) {
        // FIXME: Figure out the adapter somehow differently.
        auto* adapter = NetworkAdapter::from_ipv4_address(IPv4Address(192, 168, 5, 2));
        if (!adapter)
            ASSERT_NOT_REACHED();

        allocate_source_port_if_needed();

        send_tcp_packet(*adapter, TCPFlags::SYN);
        m_tcp_state = TCPState::Connecting1;

        current->set_blocked_socket(this);
        block(Process::BlockedConnect);
        Scheduler::yield();

        ASSERT(is_connected());
        return KSuccess;
    }

    return KSuccess;
}

void IPv4Socket::attach_fd(SocketRole)
{
    ++m_attached_fds;
}

void IPv4Socket::detach_fd(SocketRole)
{
    --m_attached_fds;
}

bool IPv4Socket::can_read(SocketRole) const
{
    return m_can_read;
}

ssize_t IPv4Socket::read(SocketRole, byte*, ssize_t)
{
    ASSERT_NOT_REACHED();
}

ssize_t IPv4Socket::write(SocketRole, const byte*, ssize_t)
{
    ASSERT_NOT_REACHED();
}

bool IPv4Socket::can_write(SocketRole) const
{
    ASSERT_NOT_REACHED();
}

void IPv4Socket::allocate_source_port_if_needed()
{
    if (m_source_port)
        return;
    if (type() == SOCK_DGRAM) {
        // This is not a very efficient allocation algorithm.
        // FIXME: Replace it with a bitmap or some other fast-paced looker-upper.
        LOCKER(sockets_by_udp_port().lock());
        for (word port = 2000; port < 60000; ++port) {
            auto it = sockets_by_udp_port().resource().find(port);
            if (it == sockets_by_udp_port().resource().end()) {
                m_source_port = port;
                sockets_by_udp_port().resource().set(port, this);
                return;
            }
        }
        ASSERT_NOT_REACHED();
    }
    if (type() == SOCK_STREAM) {
        // This is not a very efficient allocation algorithm.
        // FIXME: Replace it with a bitmap or some other fast-paced looker-upper.
        LOCKER(sockets_by_tcp_port().lock());
        for (word port = 2000; port < 60000; ++port) {
            auto it = sockets_by_tcp_port().resource().find(port);
            if (it == sockets_by_tcp_port().resource().end()) {
                m_source_port = port;
                sockets_by_tcp_port().resource().set(port, this);
                return;
            }
        }
        ASSERT_NOT_REACHED();
    }
}

struct [[gnu::packed]] TCPPseudoHeader {
    IPv4Address source;
    IPv4Address destination;
    byte zero;
    byte protocol;
    NetworkOrdered<word> payload_size;
};

NetworkOrdered<word> IPv4Socket::compute_tcp_checksum(const IPv4Address& source, const IPv4Address& destination, const TCPPacket& packet, word payload_size)
{
    TCPPseudoHeader pseudo_header { source, destination, 0, (byte)IPv4Protocol::TCP, sizeof(TCPPacket) + payload_size };

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
    if (payload_size & 1)
        ASSERT_NOT_REACHED();
    return ~(checksum & 0xffff);
}

void IPv4Socket::send_tcp_packet(NetworkAdapter& adapter, word flags, const void* payload, size_t payload_size)
{
    auto buffer = ByteBuffer::create_zeroed(sizeof(TCPPacket) + payload_size);
    auto& tcp_packet = *(TCPPacket*)(buffer.pointer());
    ASSERT(m_source_port);
    tcp_packet.set_source_port(m_source_port);
    tcp_packet.set_destination_port(m_destination_port);
    tcp_packet.set_window_size(1024);
    tcp_packet.set_sequence_number(m_tcp_sequence_number);
    tcp_packet.set_data_offset(sizeof(TCPPacket) / sizeof(dword));
    tcp_packet.set_flags(flags);

    if (flags & TCPFlags::ACK)
        tcp_packet.set_ack_number(m_tcp_ack_number);

    if (flags == TCPFlags::SYN) {
        ++m_tcp_sequence_number;
    } else {
        m_tcp_sequence_number += payload_size;
    }

    memcpy(tcp_packet.payload(), payload, payload_size);
    tcp_packet.set_checksum(compute_tcp_checksum(adapter.ipv4_address(), m_destination_address, tcp_packet, payload_size));
    kprintf("sending tcp packet from %s:%u to %s:%u!\n",
        adapter.ipv4_address().to_string().characters(),
        source_port(),
        m_destination_address.to_string().characters(),
        m_destination_port);
    adapter.send_ipv4(MACAddress(), m_destination_address, IPv4Protocol::TCP, move(buffer));
}

ssize_t IPv4Socket::sendto(const void* data, size_t data_length, int flags, const sockaddr* addr, socklen_t addr_length)
{
    (void)flags;
    if (addr && addr_length != sizeof(sockaddr_in))
        return -EINVAL;
    // FIXME: Find the adapter some better way!
    auto* adapter = NetworkAdapter::from_ipv4_address(IPv4Address(192, 168, 5, 2));
    if (!adapter) {
        // FIXME: Figure out which error code to return.
        ASSERT_NOT_REACHED();
    }

    if (addr) {
        if (addr->sa_family != AF_INET) {
            kprintf("sendto: Bad address family: %u is not AF_INET!\n", addr->sa_family);
            return -EAFNOSUPPORT;
        }

        auto& ia = *(const sockaddr_in*)addr;
        m_destination_address = IPv4Address((const byte*)&ia.sin_addr.s_addr);
        m_destination_port = ntohs(ia.sin_port);
    }

    allocate_source_port_if_needed();

    kprintf("sendto: destination=%s:%u\n", m_destination_address.to_string().characters(), m_destination_port);

    if (type() == SOCK_RAW) {
        adapter->send_ipv4(MACAddress(), m_destination_address, (IPv4Protocol)protocol(), ByteBuffer::copy((const byte*)data, data_length));
        return data_length;
    }

    if (type() == SOCK_DGRAM) {
        auto buffer = ByteBuffer::create_zeroed(sizeof(UDPPacket) + data_length);
        auto& udp_packet = *(UDPPacket*)(buffer.pointer());
        udp_packet.set_source_port(m_source_port);
        udp_packet.set_destination_port(m_destination_port);
        udp_packet.set_length(sizeof(UDPPacket) + data_length);
        memcpy(udp_packet.payload(), data, data_length);
        kprintf("sending as udp packet from %s:%u to %s:%u!\n",
            adapter->ipv4_address().to_string().characters(),
            source_port(),
            m_destination_address.to_string().characters(),
            m_destination_port);
        adapter->send_ipv4(MACAddress(), m_destination_address, IPv4Protocol::UDP, move(buffer));
        return data_length;
    }

    if (type() == SOCK_STREAM) {
        send_tcp_packet(*adapter, 0, data, data_length);
        return data_length;
    }

    ASSERT_NOT_REACHED();
}

ssize_t IPv4Socket::recvfrom(void* buffer, size_t buffer_length, int flags, sockaddr* addr, socklen_t* addr_length)
{
    (void)flags;
    if (addr_length && *addr_length < sizeof(sockaddr_in))
        return -EINVAL;

#ifdef IPV4_SOCKET_DEBUG
    kprintf("recvfrom: type=%d, source_port=%u\n", type(), source_port());
#endif

    ByteBuffer packet_buffer;
    {
        LOCKER(lock());
        if (!m_receive_queue.is_empty()) {
            packet_buffer = m_receive_queue.take_first();
            m_can_read = !m_receive_queue.is_empty();
        }
    }
    if (packet_buffer.is_null()) {
        current->set_blocked_socket(this);
        load_receive_deadline();
        block(Process::BlockedReceive);
        Scheduler::yield();

        LOCKER(lock());
        if (!m_can_read) {
            // Unblocked due to timeout.
            return -EAGAIN;
        }
        ASSERT(m_can_read);
        ASSERT(!m_receive_queue.is_empty());
        packet_buffer = m_receive_queue.take_first();
        m_can_read = !m_receive_queue.is_empty();
    }
    ASSERT(!packet_buffer.is_null());
    auto& ipv4_packet = *(const IPv4Packet*)(packet_buffer.pointer());

    if (addr) {
        auto& ia = *(sockaddr_in*)addr;
        memcpy(&ia.sin_addr, &m_destination_address, sizeof(IPv4Address));
        ia.sin_family = AF_INET;
        ASSERT(addr_length);
        *addr_length = sizeof(sockaddr_in);
    }

    if (type() == SOCK_RAW) {
        ASSERT(buffer_length >= ipv4_packet.payload_size());
        memcpy(buffer, ipv4_packet.payload(), ipv4_packet.payload_size());
        return ipv4_packet.payload_size();
    }

    if (type() == SOCK_DGRAM) {
        auto& udp_packet = *static_cast<const UDPPacket*>(ipv4_packet.payload());
        ASSERT(udp_packet.length() >= sizeof(UDPPacket)); // FIXME: This should be rejected earlier.
        ASSERT(buffer_length >= (udp_packet.length() - sizeof(UDPPacket)));
        if (addr) {
            auto& ia = *(sockaddr_in*)addr;
            ia.sin_port = htons(udp_packet.destination_port());
        }
        memcpy(buffer, udp_packet.payload(), udp_packet.length() - sizeof(UDPPacket));
        return udp_packet.length() - sizeof(UDPPacket);
    }

    if (type() == SOCK_STREAM) {
        auto& tcp_packet = *static_cast<const TCPPacket*>(ipv4_packet.payload());
        size_t payload_size = packet_buffer.size() - sizeof(TCPPacket);
        ASSERT(buffer_length >= payload_size);
        if (addr) {
            auto& ia = *(sockaddr_in*)addr;
            ia.sin_port = htons(tcp_packet.destination_port());
        }
        memcpy(buffer, tcp_packet.payload(), payload_size);
        return payload_size;
    }

    ASSERT_NOT_REACHED();
}

void IPv4Socket::did_receive(ByteBuffer&& packet)
{
    LOCKER(lock());
    m_receive_queue.append(move(packet));
    m_can_read = true;
#ifdef IPV4_SOCKET_DEBUG
    kprintf("IPv4Socket(%p): did_receive %d bytes, packets in queue: %d\n", this, packet.size(), m_receive_queue.size_slow());
#endif

}

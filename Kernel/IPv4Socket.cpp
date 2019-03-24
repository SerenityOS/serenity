#include <Kernel/IPv4Socket.h>
#include <Kernel/TCPSocket.h>
#include <Kernel/UDPSocket.h>
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

Lockable<HashTable<IPv4Socket*>>& IPv4Socket::all_sockets()
{
    static Lockable<HashTable<IPv4Socket*>>* s_table;
    if (!s_table)
        s_table = new Lockable<HashTable<IPv4Socket*>>;
    return *s_table;
}

Retained<IPv4Socket> IPv4Socket::create(int type, int protocol)
{
    if (type == SOCK_STREAM)
        return TCPSocket::create(protocol);
    if (type == SOCK_DGRAM)
        return UDPSocket::create(protocol);
    return adopt(*new IPv4Socket(type, protocol));
}

IPv4Socket::IPv4Socket(int type, int protocol)
    : Socket(AF_INET, type, protocol)
{
    kprintf("%s(%u) IPv4Socket{%p} created with type=%u, protocol=%d\n", current->process().name().characters(), current->pid(), this, type, protocol);
    LOCKER(all_sockets().lock());
    all_sockets().resource().set(this);
}

IPv4Socket::~IPv4Socket()
{
    LOCKER(all_sockets().lock());
    all_sockets().resource().remove(this);
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

    return protocol_connect();
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
    if (protocol_is_disconnected())
        return true;
    return m_can_read;
}

ssize_t IPv4Socket::read(SocketRole, byte* buffer, ssize_t size)
{
    return recvfrom(buffer, size, 0, nullptr, 0);
}

ssize_t IPv4Socket::write(SocketRole, const byte* data, ssize_t size)
{
    return sendto(data, size, 0, nullptr, 0);
}

bool IPv4Socket::can_write(SocketRole) const
{
    return true;
}

int IPv4Socket::allocate_source_port_if_needed()
{
    if (m_source_port)
        return m_source_port;
    int port = protocol_allocate_source_port();
    if (port < 0)
        return port;
    m_source_port = (word)port;
    return port;
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

    int rc = allocate_source_port_if_needed();
    if (rc < 0)
        return rc;

    kprintf("sendto: destination=%s:%u\n", m_destination_address.to_string().characters(), m_destination_port);

    if (type() == SOCK_RAW) {
        adapter->send_ipv4(MACAddress(), m_destination_address, (IPv4Protocol)protocol(), ByteBuffer::copy(data, data_length));
        return data_length;
    }

    return protocol_send(data, data_length);
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
#ifdef IPV4_SOCKET_DEBUG
            kprintf("IPv4Socket(%p): recvfrom without blocking %d bytes, packets in queue: %d\n", this, packet_buffer.size(), m_receive_queue.size_slow());
#endif
        }
    }
    if (packet_buffer.is_null()) {
        if (protocol_is_disconnected()) {
            kprintf("IPv4Socket{%p} is protocol-disconnected, returning 0 in recvfrom!\n", this);
            return 0;
        }

        current->set_blocked_socket(this);
        load_receive_deadline();
        current->block(Thread::BlockedReceive);

        LOCKER(lock());
        if (!m_can_read) {
            // Unblocked due to timeout.
            return -EAGAIN;
        }
        ASSERT(m_can_read);
        ASSERT(!m_receive_queue.is_empty());
        packet_buffer = m_receive_queue.take_first();
        m_can_read = !m_receive_queue.is_empty();
#ifdef IPV4_SOCKET_DEBUG
        kprintf("IPv4Socket(%p): recvfrom with blocking %d bytes, packets in queue: %d\n", this, packet_buffer.size(), m_receive_queue.size_slow());
#endif
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

    return protocol_receive(packet_buffer, buffer, buffer_length, flags, addr, addr_length);
}

void IPv4Socket::did_receive(ByteBuffer&& packet)
{
    LOCKER(lock());
    auto packet_size = packet.size();
    m_receive_queue.append(move(packet));
    m_can_read = true;
    m_bytes_received += packet_size;
#ifdef IPV4_SOCKET_DEBUG
    kprintf("IPv4Socket(%p): did_receive %d bytes, total_received=%u, packets in queue: %d\n", this, packet_size, m_bytes_received, m_receive_queue.size_slow());
#endif
}

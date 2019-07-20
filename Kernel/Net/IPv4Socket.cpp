#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/Net/ARP.h>
#include <Kernel/Net/ICMP.h>
#include <Kernel/Net/IPv4.h>
#include <Kernel/Net/IPv4Socket.h>
#include <Kernel/Net/NetworkAdapter.h>
#include <Kernel/Net/Routing.h>
#include <Kernel/Net/TCP.h>
#include <Kernel/Net/TCPSocket.h>
#include <Kernel/Net/UDP.h>
#include <Kernel/Net/UDPSocket.h>
#include <Kernel/Process.h>
#include <Kernel/UnixTypes.h>
#include <LibC/errno_numbers.h>

#define IPV4_SOCKET_DEBUG

Lockable<HashTable<IPv4Socket*>>& IPv4Socket::all_sockets()
{
    static Lockable<HashTable<IPv4Socket*>>* s_table;
    if (!s_table)
        s_table = new Lockable<HashTable<IPv4Socket*>>;
    return *s_table;
}

NonnullRefPtr<IPv4Socket> IPv4Socket::create(int type, int protocol)
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

bool IPv4Socket::get_local_address(sockaddr* address, socklen_t* address_size)
{
    // FIXME: Look into what fallback behavior we should have here.
    if (*address_size < sizeof(sockaddr_in))
        return false;
    auto& ia = (sockaddr_in&)*address;
    ia.sin_family = AF_INET;
    ia.sin_port = m_local_port;
    memcpy(&ia.sin_addr, &m_local_address, sizeof(IPv4Address));
    *address_size = sizeof(sockaddr_in);
    return true;
}

bool IPv4Socket::get_peer_address(sockaddr* address, socklen_t* address_size)
{
    // FIXME: Look into what fallback behavior we should have here.
    if (*address_size < sizeof(sockaddr_in))
        return false;
    auto& ia = (sockaddr_in&)*address;
    ia.sin_family = AF_INET;
    ia.sin_port = m_peer_port;
    memcpy(&ia.sin_addr, &m_peer_address, sizeof(IPv4Address));
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

    auto& ia = *(const sockaddr_in*)address;
    m_local_address = IPv4Address((const u8*)&ia.sin_addr.s_addr);
    m_local_port = ntohs(ia.sin_port);

    dbgprintf("IPv4Socket::bind %s{%p} to port %u\n", class_name(), this, m_local_port);

    return protocol_bind();
}

KResult IPv4Socket::connect(FileDescription& description, const sockaddr* address, socklen_t address_size, ShouldBlock should_block)
{
    ASSERT(!m_bound);
    if (address_size != sizeof(sockaddr_in))
        return KResult(-EINVAL);
    if (address->sa_family != AF_INET)
        return KResult(-EINVAL);

    auto& ia = *(const sockaddr_in*)address;
    m_peer_address = IPv4Address((const u8*)&ia.sin_addr.s_addr);
    m_peer_port = ntohs(ia.sin_port);

    return protocol_connect(description, should_block);
}

void IPv4Socket::attach(FileDescription&)
{
    ++m_attached_fds;
}

void IPv4Socket::detach(FileDescription&)
{
    --m_attached_fds;
}

bool IPv4Socket::can_read(FileDescription& description) const
{
    if (description.socket_role() == SocketRole::Listener)
        return can_accept();
    if (protocol_is_disconnected())
        return true;
    return m_can_read;
}

ssize_t IPv4Socket::read(FileDescription& description, u8* buffer, ssize_t size)
{
    return recvfrom(description, buffer, size, 0, nullptr, 0);
}

ssize_t IPv4Socket::write(FileDescription& description, const u8* data, ssize_t size)
{
    return sendto(description, data, size, 0, nullptr, 0);
}

bool IPv4Socket::can_write(FileDescription&) const
{
    return is_connected();
}

int IPv4Socket::allocate_local_port_if_needed()
{
    if (m_local_port)
        return m_local_port;
    int port = protocol_allocate_local_port();
    if (port < 0)
        return port;
    m_local_port = (u16)port;
    return port;
}

ssize_t IPv4Socket::sendto(FileDescription&, const void* data, size_t data_length, int flags, const sockaddr* addr, socklen_t addr_length)
{
    (void)flags;
    if (addr && addr_length != sizeof(sockaddr_in))
        return -EINVAL;

    if (addr) {
        if (addr->sa_family != AF_INET) {
            kprintf("sendto: Bad address family: %u is not AF_INET!\n", addr->sa_family);
            return -EAFNOSUPPORT;
        }

        auto& ia = *(const sockaddr_in*)addr;
        m_peer_address = IPv4Address((const u8*)&ia.sin_addr.s_addr);
        m_peer_port = ntohs(ia.sin_port);
    }

    auto* adapter = adapter_for_route_to(m_peer_address);
    if (!adapter)
        return -EHOSTUNREACH;

    int rc = allocate_local_port_if_needed();
    if (rc < 0)
        return rc;

    kprintf("sendto: destination=%s:%u\n", m_peer_address.to_string().characters(), m_peer_port);

    if (type() == SOCK_RAW) {
        adapter->send_ipv4(MACAddress(), m_peer_address, (IPv4Protocol)protocol(), ByteBuffer::copy(data, data_length));
        return data_length;
    }

    return protocol_send(data, data_length);
}

ssize_t IPv4Socket::recvfrom(FileDescription& description, void* buffer, size_t buffer_length, int flags, sockaddr* addr, socklen_t* addr_length)
{
    (void)flags;
    if (addr_length && *addr_length < sizeof(sockaddr_in))
        return -EINVAL;

#ifdef IPV4_SOCKET_DEBUG
    kprintf("recvfrom: type=%d, local_port=%u\n", type(), local_port());
#endif

    ReceivedPacket packet;
    {
        LOCKER(lock());
        if (!m_receive_queue.is_empty()) {
            packet = m_receive_queue.take_first();
            m_can_read = !m_receive_queue.is_empty();
#ifdef IPV4_SOCKET_DEBUG
            kprintf("IPv4Socket(%p): recvfrom without blocking %d bytes, packets in queue: %d\n", this, packet.data.size(), m_receive_queue.size_slow());
#endif
        }
    }
    if (packet.data.is_null()) {
        if (protocol_is_disconnected()) {
            kprintf("IPv4Socket{%p} is protocol-disconnected, returning 0 in recvfrom!\n", this);
            return 0;
        }

        load_receive_deadline();
        auto res = current->block<Thread::ReceiveBlocker>(description);

        LOCKER(lock());
        if (!m_can_read) {
            if (res == Thread::BlockResult::InterruptedBySignal)
                return -EINTR;

            // Unblocked due to timeout.
            return -EAGAIN;
        }
        ASSERT(m_can_read);
        ASSERT(!m_receive_queue.is_empty());
        packet = m_receive_queue.take_first();
        m_can_read = !m_receive_queue.is_empty();
#ifdef IPV4_SOCKET_DEBUG
        kprintf("IPv4Socket(%p): recvfrom with blocking %d bytes, packets in queue: %d\n", this, packet.data.size(), m_receive_queue.size_slow());
#endif
    }
    ASSERT(!packet.data.is_null());
    auto& ipv4_packet = *(const IPv4Packet*)(packet.data.pointer());

    if (addr) {
        dbgprintf("Incoming packet is from: %s:%u\n", packet.peer_address.to_string().characters(), packet.peer_port);
        auto& ia = *(sockaddr_in*)addr;
        memcpy(&ia.sin_addr, &packet.peer_address, sizeof(IPv4Address));
        ia.sin_port = htons(packet.peer_port);
        ia.sin_family = AF_INET;
        ASSERT(addr_length);
        *addr_length = sizeof(sockaddr_in);
    }

    if (type() == SOCK_RAW) {
        ASSERT(buffer_length >= ipv4_packet.payload_size());
        memcpy(buffer, ipv4_packet.payload(), ipv4_packet.payload_size());
        return ipv4_packet.payload_size();
    }

    return protocol_receive(packet.data, buffer, buffer_length, flags);
}

void IPv4Socket::did_receive(const IPv4Address& source_address, u16 source_port, ByteBuffer&& packet)
{
    LOCKER(lock());
    auto packet_size = packet.size();
    m_receive_queue.append({ source_address, source_port, move(packet) });
    m_can_read = true;
    m_bytes_received += packet_size;
#ifdef IPV4_SOCKET_DEBUG
    kprintf("IPv4Socket(%p): did_receive %d bytes, total_received=%u, packets in queue: %d\n", this, packet_size, m_bytes_received, m_receive_queue.size_slow());
#endif
}

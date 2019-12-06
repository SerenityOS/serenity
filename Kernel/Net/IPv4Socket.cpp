#include <AK/StringBuilder.h>
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
#include <LibC/sys/ioctl_numbers.h>

//#define IPV4_SOCKET_DEBUG

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
#ifdef IPV4_SOCKET_DEBUG
    kprintf("%s(%u) IPv4Socket{%p} created with type=%u, protocol=%d\n", current->process().name().characters(), current->pid(), this, type, protocol);
#endif
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
    ia.sin_port = htons(m_local_port);
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
    ia.sin_port = htons(m_peer_port);
    memcpy(&ia.sin_addr, &m_peer_address, sizeof(IPv4Address));
    *address_size = sizeof(sockaddr_in);
    return true;
}

KResult IPv4Socket::bind(const sockaddr* address, socklen_t address_size)
{
    ASSERT(setup_state() == SetupState::Unstarted);
    if (address_size != sizeof(sockaddr_in))
        return KResult(-EINVAL);
    if (address->sa_family != AF_INET)
        return KResult(-EINVAL);

    auto& ia = *(const sockaddr_in*)address;

    auto requested_local_port = ntohs(ia.sin_port);
    if (!current->process().is_superuser()) {
        if (requested_local_port < 1024) {
            dbg() << current->process() << " (uid " << current->process().uid() << ") attempted to bind " << class_name() << " to port " << requested_local_port;
            return KResult(-EACCES);
        }
    }

    m_local_address = IPv4Address((const u8*)&ia.sin_addr.s_addr);
    m_local_port = requested_local_port;

#ifdef IPV4_SOCKET_DEBUG
    dbgprintf("IPv4Socket::bind %s{%p} to %s:%u\n", class_name(), this, m_local_address.to_string().characters(), m_local_port);
#endif

    return protocol_bind();
}

KResult IPv4Socket::listen(int backlog)
{
    int rc = allocate_local_port_if_needed();
    if (rc < 0)
        return KResult(-EADDRINUSE);

    set_backlog(backlog);
    m_role = Role::Listener;

#ifdef IPV4_SOCKET_DEBUG
    kprintf("IPv4Socket{%p} listening with backlog=%d\n", this, backlog);
#endif

    return protocol_listen();
}

KResult IPv4Socket::connect(FileDescription& description, const sockaddr* address, socklen_t address_size, ShouldBlock should_block)
{
    if (address_size != sizeof(sockaddr_in))
        return KResult(-EINVAL);
    if (address->sa_family != AF_INET)
        return KResult(-EINVAL);
    if (m_role == Role::Connected)
        return KResult(-EISCONN);

    auto& ia = *(const sockaddr_in*)address;
    m_peer_address = IPv4Address((const u8*)&ia.sin_addr.s_addr);
    m_peer_port = ntohs(ia.sin_port);

    return protocol_connect(description, should_block);
}

void IPv4Socket::attach(FileDescription&)
{
}

void IPv4Socket::detach(FileDescription&)
{
}

bool IPv4Socket::can_read(const FileDescription&) const
{
    if (m_role == Role::Listener)
        return can_accept();
    if (protocol_is_disconnected())
        return true;
    return m_can_read;
}

bool IPv4Socket::can_write(const FileDescription&) const
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

    auto routing_decision = route_to(m_peer_address, m_local_address);
    if (routing_decision.is_zero())
        return -EHOSTUNREACH;

    if (m_local_address.to_u32() == 0)
        m_local_address = routing_decision.adapter->ipv4_address();

    int rc = allocate_local_port_if_needed();
    if (rc < 0)
        return rc;

#ifdef IPV4_SOCKET_DEBUG
    kprintf("sendto: destination=%s:%u\n", m_peer_address.to_string().characters(), m_peer_port);
#endif

    if (type() == SOCK_RAW) {
        routing_decision.adapter->send_ipv4(routing_decision.next_hop, m_peer_address, (IPv4Protocol)protocol(), (const u8*)data, data_length, m_ttl);
        return data_length;
    }

    int nsent = protocol_send(data, data_length);
    if (nsent > 0)
        current->did_ipv4_socket_write(nsent);
    return nsent;
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
        if (m_receive_queue.is_empty()) {
            // FIXME: Shouldn't this return -ENOTCONN instead of EOF?
            //        But if so, we still need to deliver at least one EOF read to userspace.. right?
            if (protocol_is_disconnected())
                return 0;
            if (!description.is_blocking())
                return -EAGAIN;
        }

        if (!m_receive_queue.is_empty()) {
            packet = m_receive_queue.take_first();
            m_can_read = !m_receive_queue.is_empty();
#ifdef IPV4_SOCKET_DEBUG
            kprintf("IPv4Socket(%p): recvfrom without blocking %d bytes, packets in queue: %d\n", this, packet.data.value().size(), m_receive_queue.size_slow());
#endif
        }
    }
    if (!packet.data.has_value()) {
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
        kprintf("IPv4Socket(%p): recvfrom with blocking %d bytes, packets in queue: %d\n", this, packet.data.value().size(), m_receive_queue.size_slow());
#endif
    }
    ASSERT(packet.data.has_value());
    auto& ipv4_packet = *(const IPv4Packet*)(packet.data.value().data());

    if (addr) {
#ifdef IPV4_SOCKET_DEBUG
        dbgprintf("Incoming packet is from: %s:%u\n", packet.peer_address.to_string().characters(), packet.peer_port);
#endif
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

    int nreceived = protocol_receive(packet.data.value(), buffer, buffer_length, flags);
    if (nreceived > 0)
        current->did_ipv4_socket_read(nreceived);
    return nreceived;
}

bool IPv4Socket::did_receive(const IPv4Address& source_address, u16 source_port, KBuffer&& packet)
{
    LOCKER(lock());
    if (m_receive_queue.size_slow() > 2000) {
        kprintf("IPv4Socket(%p): did_receive refusing packet since queue is full.\n", this);
        return false;
    }
    auto packet_size = packet.size();
    m_receive_queue.append({ source_address, source_port, move(packet) });
    m_can_read = true;
    m_bytes_received += packet_size;
#ifdef IPV4_SOCKET_DEBUG
    kprintf("IPv4Socket(%p): did_receive %d bytes, total_received=%u, packets in queue: %d\n", this, packet_size, m_bytes_received, m_receive_queue.size_slow());
#endif
    return true;
}

String IPv4Socket::absolute_path(const FileDescription&) const
{
    if (m_role == Role::None)
        return "socket";

    StringBuilder builder;
    builder.append("socket:");

    builder.appendf("%s:%d", m_local_address.to_string().characters(), m_local_port);
    if (m_role == Role::Accepted || m_role == Role::Connected)
        builder.appendf(" / %s:%d", m_peer_address.to_string().characters(), m_peer_port);

    switch (m_role) {
    case Role::Listener:
        builder.append(" (listening)");
        break;
    case Role::Accepted:
        builder.append(" (accepted)");
        break;
    case Role::Connected:
        builder.append(" (connected)");
        break;
    case Role::Connecting:
        builder.append(" (connecting)");
        break;
    default:
        ASSERT_NOT_REACHED();
    }

    return builder.to_string();
}

KResult IPv4Socket::setsockopt(int level, int option, const void* value, socklen_t value_size)
{
    if (level != IPPROTO_IP)
        return Socket::setsockopt(level, option, value, value_size);

    switch (option) {
    case IP_TTL:
        if (value_size < sizeof(int))
            return KResult(-EINVAL);
        if (*(const int*)value < 0 || *(const int*)value > 255)
            return KResult(-EINVAL);
        m_ttl = (u8)*(const int*)value;
        return KSuccess;
    default:
        return KResult(-ENOPROTOOPT);
    }
}

KResult IPv4Socket::getsockopt(FileDescription& description, int level, int option, void* value, socklen_t* value_size)
{
    if (level != IPPROTO_IP)
        return Socket::getsockopt(description, level, option, value, value_size);

    switch (option) {
    case IP_TTL:
        if (*value_size < sizeof(int))
            return KResult(-EINVAL);
        *(int*)value = m_ttl;
        return KSuccess;
    default:
        return KResult(-ENOPROTOOPT);
    }
}

int IPv4Socket::ioctl(FileDescription&, unsigned request, unsigned arg)
{
    auto* ifr = (ifreq*)arg;
    if (!current->process().validate_read_typed(ifr))
        return -EFAULT;

    char namebuf[IFNAMSIZ + 1];
    memcpy(namebuf, ifr->ifr_name, IFNAMSIZ);
    namebuf[sizeof(namebuf) - 1] = '\0';
    auto adapter = NetworkAdapter::lookup_by_name(namebuf);
    if (!adapter)
        return -ENODEV;

    switch (request) {
    case SIOCSIFADDR:
        if (!current->process().is_superuser())
            return -EPERM;
        if (ifr->ifr_addr.sa_family != AF_INET)
            return -EAFNOSUPPORT;
        adapter->set_ipv4_address(IPv4Address(((sockaddr_in&)ifr->ifr_addr).sin_addr.s_addr));
        return 0;

    case SIOCGIFADDR:
        if (!current->process().validate_write_typed(ifr))
            return -EFAULT;
        ifr->ifr_addr.sa_family = AF_INET;
        ((sockaddr_in&)ifr->ifr_addr).sin_addr.s_addr = adapter->ipv4_address().to_u32();
        return 0;

    case SIOCGIFHWADDR:
        if (!current->process().validate_write_typed(ifr))
            return -EFAULT;
        ifr->ifr_hwaddr.sa_family = AF_INET;
        {
            auto mac_address = adapter->mac_address();
            memcpy(ifr->ifr_hwaddr.sa_data, &mac_address, sizeof(MACAddress));
        }
        return 0;
    }

    return -EINVAL;
}

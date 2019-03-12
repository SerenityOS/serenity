#include <Kernel/IPv4Socket.h>
#include <Kernel/UnixTypes.h>
#include <Kernel/Process.h>
#include <Kernel/NetworkAdapter.h>
#include <LibC/errno_numbers.h>

Retained<IPv4Socket> IPv4Socket::create(int type, int protocol)
{
    return adopt(*new IPv4Socket(type, protocol));
}

IPv4Socket::IPv4Socket(int type, int protocol)
    : Socket(AF_INET, type, protocol)
{
    kprintf("%s(%u) IPv4Socket{%p} created with type=%u\n", current->name().characters(), current->pid(), this, type);
}

IPv4Socket::~IPv4Socket()
{
}

bool IPv4Socket::get_address(sockaddr* address, socklen_t* address_size)
{
    // FIXME: Look into what fallback behavior we should have here.
    if (*address_size != sizeof(sockaddr_in))
        return false;
    memcpy(address, &m_peer_address, sizeof(sockaddr_in));
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

    ASSERT_NOT_REACHED();
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
    ASSERT_NOT_REACHED();
}

ssize_t IPv4Socket::read(SocketRole role, byte* buffer, ssize_t size)
{
    ASSERT_NOT_REACHED();
}

ssize_t IPv4Socket::write(SocketRole role, const byte* data, ssize_t size)
{
    ASSERT_NOT_REACHED();
}

bool IPv4Socket::can_write(SocketRole role) const
{
    ASSERT_NOT_REACHED();
}

ssize_t IPv4Socket::sendto(const void* data, size_t data_length, int flags, const sockaddr* addr, socklen_t addr_length)
{
    (void)flags;
    ASSERT(data_length);
    if (addr_length != sizeof(sockaddr_in))
        return -EINVAL;
    // FIXME: Find the adapter some better way!
    auto* adapter = NetworkAdapter::from_ipv4_address(IPv4Address(192, 168, 5, 2));
    if (!adapter) {
        // FIXME: Figure out which error code to return.
        ASSERT_NOT_REACHED();
    }

    if (addr->sa_family != AF_INET) {
        kprintf("sendto: Bad address family: %u is not AF_INET!\n", addr->sa_family);
        return -EAFNOSUPPORT;
    }

    auto peer_address = IPv4Address((const byte*)&((const sockaddr_in*)addr)->sin_addr.s_addr);

    kprintf("sendto: peer_address=%s\n", peer_address.to_string().characters());

    // FIXME: If we can't find the right MAC address, block until it's available?
    //        I feel like this should happen in a layer below this code.
    MACAddress mac_address;
    adapter->send_ipv4(mac_address, peer_address, (IPv4Protocol)protocol(), ByteBuffer::copy((const byte*)data, data_length));
    return data_length;
}

#include <Kernel/LocalSocket.h>
#include <Kernel/UnixTypes.h>
#include <Kernel/Process.h>
#include <Kernel/VirtualFileSystem.h>
#include <LibC/errno_numbers.h>

//#define DEBUG_LOCAL_SOCKET

Retained<LocalSocket> LocalSocket::create(int type)
{
    return adopt(*new LocalSocket(type));
}

LocalSocket::LocalSocket(int type)
    : Socket(AF_LOCAL, type, 0)
{
#ifdef DEBUG_LOCAL_SOCKET
    kprintf("%s(%u) LocalSocket{%p} created with type=%u\n", current->name().characters(), current->pid(), this, type);
#endif
}

LocalSocket::~LocalSocket()
{
}

bool LocalSocket::get_address(sockaddr* address, socklen_t* address_size)
{
    // FIXME: Look into what fallback behavior we should have here.
    if (*address_size != sizeof(sockaddr_un))
        return false;
    memcpy(address, &m_address, sizeof(sockaddr_un));
    *address_size = sizeof(sockaddr_un);
    return true;
}

KResult LocalSocket::bind(const sockaddr* address, socklen_t address_size)
{
    ASSERT(!is_connected());
    if (address_size != sizeof(sockaddr_un))
        return KResult(-EINVAL);
    if (address->sa_family != AF_LOCAL)
        return KResult(-EINVAL);

    const sockaddr_un& local_address = *reinterpret_cast<const sockaddr_un*>(address);
    char safe_address[sizeof(local_address.sun_path) + 1];
    memcpy(safe_address, local_address.sun_path, sizeof(local_address.sun_path));

#ifdef DEBUG_LOCAL_SOCKET
    kprintf("%s(%u) LocalSocket{%p} bind(%s)\n", current->name().characters(), current->pid(), this, safe_address);
#endif

    auto result = VFS::the().open(safe_address, O_CREAT | O_EXCL, S_IFSOCK | 0666, current->cwd_inode());
    if (result.is_error()) {
        if (result.error() == -EEXIST)
            return KResult(-EADDRINUSE);
        return result.error();
    }
    m_file = move(result.value());

    ASSERT(m_file->inode());
    m_file->inode()->bind_socket(*this);

    m_address = local_address;
    m_bound = true;
    return KSuccess;
}

KResult LocalSocket::connect(const sockaddr* address, socklen_t address_size)
{
    ASSERT(!m_bound);
    if (address_size != sizeof(sockaddr_un))
        return KResult(-EINVAL);
    if (address->sa_family != AF_LOCAL)
        return KResult(-EINVAL);

    const sockaddr_un& local_address = *reinterpret_cast<const sockaddr_un*>(address);
    char safe_address[sizeof(local_address.sun_path) + 1];
    memcpy(safe_address, local_address.sun_path, sizeof(local_address.sun_path));

#ifdef DEBUG_LOCAL_SOCKET
    kprintf("%s(%u) LocalSocket{%p} connect(%s)\n", current->name().characters(), current->pid(), this, safe_address);
#endif

    auto descriptor_or_error = VFS::the().open(safe_address, 0, 0, current->cwd_inode());
    if (descriptor_or_error.is_error())
        return KResult(-ECONNREFUSED);
    m_file = move(descriptor_or_error.value());

    ASSERT(m_file->inode());
    if (!m_file->inode()->socket())
        return KResult(-ECONNREFUSED);

    m_address = local_address;

    auto peer = m_file->inode()->socket();
    auto result = peer->queue_connection_from(*this);
    if (result.is_error())
        return result;

    return current->wait_for_connect(*this);
}

void LocalSocket::attach_fd(SocketRole role)
{
    if (role == SocketRole::Accepted) {
        ++m_accepted_fds_open;
    } else if (role == SocketRole::Connected) {
        ++m_connected_fds_open;
    }
}

void LocalSocket::detach_fd(SocketRole role)
{
    if (role == SocketRole::Accepted) {
        ASSERT(m_accepted_fds_open);
        --m_accepted_fds_open;
    } else if (role == SocketRole::Connected) {
        ASSERT(m_connected_fds_open);
        --m_connected_fds_open;
    }
}

bool LocalSocket::can_read(SocketRole role) const
{
    if (role == SocketRole::Listener)
        return can_accept();
    if (role == SocketRole::Accepted)
        return !m_connected_fds_open || !m_for_server.is_empty();
    if (role == SocketRole::Connected)
        return !m_accepted_fds_open || !m_for_client.is_empty();
    ASSERT_NOT_REACHED();
}

ssize_t LocalSocket::read(SocketRole role, byte* buffer, ssize_t size)
{
    if (role == SocketRole::Accepted)
        return m_for_server.read(buffer, size);
    if (role == SocketRole::Connected)
        return m_for_client.read(buffer, size);
    ASSERT_NOT_REACHED();
}

ssize_t LocalSocket::write(SocketRole role, const byte* data, ssize_t size)
{
    if (role == SocketRole::Accepted) {
        if (!m_accepted_fds_open)
            return -EPIPE;
        return m_for_client.write(data, size);
    }
    if (role == SocketRole::Connected) {
        if (!m_connected_fds_open)
            return -EPIPE;
        return m_for_server.write(data, size);
    }
    ASSERT_NOT_REACHED();
}

bool LocalSocket::can_write(SocketRole role) const
{
    if (role == SocketRole::Accepted)
        return !m_connected_fds_open || m_for_client.bytes_in_write_buffer() < 4096;
    if (role == SocketRole::Connected)
        return !m_accepted_fds_open || m_for_server.bytes_in_write_buffer() < 4096;
    ASSERT_NOT_REACHED();
}

ssize_t LocalSocket::sendto(const void*, size_t, int, const sockaddr*, socklen_t)
{
    ASSERT_NOT_REACHED();
}

ssize_t LocalSocket::recvfrom(void*, size_t, int flags, sockaddr*, socklen_t*)
{
    ASSERT_NOT_REACHED();
}

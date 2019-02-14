#include <Kernel/LocalSocket.h>
#include <Kernel/UnixTypes.h>
#include <Kernel/Process.h>
#include <Kernel/VirtualFileSystem.h>
#include <LibC/errno_numbers.h>

RetainPtr<LocalSocket> LocalSocket::create(int type)
{
    return adopt(*new LocalSocket(type));
}

LocalSocket::LocalSocket(int type)
    : Socket(AF_LOCAL, type, 0)
{
    kprintf("%s(%u) LocalSocket{%p} created with type=%u\n", current->name().characters(), current->pid(), type);
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

bool LocalSocket::bind(const sockaddr* address, socklen_t address_size, int& error)
{
    ASSERT(!m_connected);
    if (address_size != sizeof(sockaddr_un)) {
        error = -EINVAL;
        return false;
    }
    if (address->sa_family != AF_LOCAL) {
        error = -EINVAL;
        return false;
    }

    const sockaddr_un& local_address = *reinterpret_cast<const sockaddr_un*>(address);
    char safe_address[sizeof(local_address.sun_path) + 1];
    memcpy(safe_address, local_address.sun_path, sizeof(local_address.sun_path));

    kprintf("%s(%u) LocalSocket{%p} bind(%s)\n", current->name().characters(), current->pid(), safe_address);

    m_file = VFS::the().open(safe_address, error, O_CREAT | O_EXCL, S_IFSOCK | 0666, *current->cwd_inode());
    if (!m_file) {
        if (error == -EEXIST)
            error = -EADDRINUSE;
        return false;
    }

    ASSERT(m_file->inode());
    m_file->inode()->bind_socket(*this);

    m_address = local_address;
    m_bound = true;
    return true;
}

RetainPtr<Socket> LocalSocket::connect(const sockaddr* address, socklen_t address_size, int& error)
{
    ASSERT(!m_bound);
    if (address_size != sizeof(sockaddr_un)) {
        error = -EINVAL;
        return nullptr;
    }
    if (address->sa_family != AF_LOCAL) {
        error = -EINVAL;
        return nullptr;
    }

    const sockaddr_un& local_address = *reinterpret_cast<const sockaddr_un*>(address);
    char safe_address[sizeof(local_address.sun_path) + 1];
    memcpy(safe_address, local_address.sun_path, sizeof(local_address.sun_path));

    kprintf("%s(%u) LocalSocket{%p} connect(%s)\n", current->name().characters(), current->pid(), safe_address);

    m_file = VFS::the().open(safe_address, error, 0, 0, *current->cwd_inode());
    if (!m_file) {
        error = -ECONNREFUSED;
        return nullptr;
    }

    ASSERT(m_file->inode());
    ASSERT(m_file->inode()->socket());

    m_peer = m_file->inode()->socket();
    m_address = local_address;
    m_connected = true;
    return m_peer;
}

bool LocalSocket::can_read(SocketRole role) const
{
    if (role == SocketRole::Accepted)
        return !m_for_server.is_empty();
    else
        return !m_for_client.is_empty();
}

ssize_t LocalSocket::read(SocketRole role, byte* buffer, size_t size)
{
    if (role == SocketRole::Accepted)
        return m_for_server.read(buffer, size);
    else
        return m_for_client.read(buffer, size);
}

ssize_t LocalSocket::write(SocketRole role, const byte* data, size_t size)
{
    if (role == SocketRole::Accepted)
        return m_for_client.write(data, size);
    else
        return m_for_server.write(data, size);
}

bool LocalSocket::can_write(SocketRole role) const
{
    if (role == SocketRole::Accepted)
        return !m_for_client.is_empty();
    else
        return !m_for_server.is_empty();
}

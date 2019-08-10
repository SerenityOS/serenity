#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Net/LocalSocket.h>
#include <Kernel/Process.h>
#include <Kernel/UnixTypes.h>
#include <LibC/errno_numbers.h>

//#define DEBUG_LOCAL_SOCKET

NonnullRefPtr<LocalSocket> LocalSocket::create(int type)
{
    return adopt(*new LocalSocket(type));
}

LocalSocket::LocalSocket(int type)
    : Socket(AF_LOCAL, type, 0)
{
#ifdef DEBUG_LOCAL_SOCKET
    kprintf("%s(%u) LocalSocket{%p} created with type=%u\n", current->process().name().characters(), current->pid(), this, type);
#endif
}

LocalSocket::~LocalSocket()
{
}

bool LocalSocket::get_local_address(sockaddr* address, socklen_t* address_size)
{
    // FIXME: Look into what fallback behavior we should have here.
    if (*address_size != sizeof(sockaddr_un))
        return false;
    memcpy(address, &m_address, sizeof(sockaddr_un));
    *address_size = sizeof(sockaddr_un);
    return true;
}

bool LocalSocket::get_peer_address(sockaddr* address, socklen_t* address_size)
{
    return get_local_address(address, address_size);
}

KResult LocalSocket::bind(const sockaddr* address, socklen_t address_size)
{
    ASSERT(setup_state() == SetupState::Unstarted);
    if (address_size != sizeof(sockaddr_un))
        return KResult(-EINVAL);
    if (address->sa_family != AF_LOCAL)
        return KResult(-EINVAL);

    const sockaddr_un& local_address = *reinterpret_cast<const sockaddr_un*>(address);
    char safe_address[sizeof(local_address.sun_path) + 1] = { 0 };
    memcpy(safe_address, local_address.sun_path, sizeof(local_address.sun_path));

#ifdef DEBUG_LOCAL_SOCKET
    kprintf("%s(%u) LocalSocket{%p} bind(%s)\n", current->process().name().characters(), current->pid(), this, safe_address);
#endif

    auto result = VFS::the().open(safe_address, O_CREAT | O_EXCL, S_IFSOCK | 0666, current->process().current_directory());
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
    set_setup_state(SetupState::Completed);
    return KSuccess;
}

KResult LocalSocket::connect(FileDescription& description, const sockaddr* address, socklen_t address_size, ShouldBlock)
{
    ASSERT(!m_bound);
    if (address_size != sizeof(sockaddr_un))
        return KResult(-EINVAL);
    if (address->sa_family != AF_LOCAL)
        return KResult(-EINVAL);

    const sockaddr_un& local_address = *reinterpret_cast<const sockaddr_un*>(address);
    char safe_address[sizeof(local_address.sun_path) + 1] = { 0 };
    memcpy(safe_address, local_address.sun_path, sizeof(local_address.sun_path));

#ifdef DEBUG_LOCAL_SOCKET
    kprintf("%s(%u) LocalSocket{%p} connect(%s)\n", current->process().name().characters(), current->pid(), this, safe_address);
#endif

    auto description_or_error = VFS::the().open(safe_address, 0, 0, current->process().current_directory());
    if (description_or_error.is_error())
        return KResult(-ECONNREFUSED);
    m_file = move(description_or_error.value());

    ASSERT(m_file->inode());
    if (!m_file->inode()->socket())
        return KResult(-ECONNREFUSED);

    m_address = local_address;

    ASSERT(m_connect_side_fd == &description);
    m_connect_side_role = Role::Connecting;

    auto peer = m_file->inode()->socket();
    auto result = peer->queue_connection_from(*this);
    if (result.is_error()) {
        m_connect_side_role = Role::None;
        return result;
    }

    if (is_connected()) {
        m_connect_side_role = Role::Connected;
        return KSuccess;
    }

    if (current->block<Thread::ConnectBlocker>(description) == Thread::BlockResult::InterruptedBySignal) {
        m_connect_side_role = Role::None;
        return KResult(-EINTR);
    }

#ifdef DEBUG_LOCAL_SOCKET
    kprintf("%s(%u) LocalSocket{%p} connect(%s) status is %s\n", current->process().name().characters(), current->pid(), this, safe_address, to_string(setup_state()));
#endif

    if (!is_connected()) {
        m_connect_side_role = Role::None;
        return KResult(-ECONNREFUSED);
    }
    m_connect_side_role = Role::Connected;
    return KSuccess;
}

KResult LocalSocket::listen(int backlog)
{
    LOCKER(lock());
    if (type() != SOCK_STREAM)
        return KResult(-EOPNOTSUPP);
    set_backlog(backlog);
    m_connect_side_role = m_role = Role::Listener;
    kprintf("LocalSocket{%p} listening with backlog=%d\n", this, backlog);
    return KSuccess;
}

void LocalSocket::attach(FileDescription& description)
{
    ASSERT(!m_accept_side_fd_open);
    if (m_connect_side_role == Role::None) {
        ASSERT(m_connect_side_fd == nullptr);
        m_connect_side_fd = &description;
    } else {
        ASSERT(m_connect_side_fd != &description);
        m_accept_side_fd_open = true;
    }
}

void LocalSocket::detach(FileDescription& description)
{
    if (m_connect_side_fd == &description) {
        m_connect_side_fd = nullptr;
    } else {
        ASSERT(m_accept_side_fd_open);
        m_accept_side_fd_open = false;
    }
}

bool LocalSocket::can_read(FileDescription& description) const
{
    auto role = this->role(description);
    if (role == Role::Listener)
        return can_accept();
    if (role == Role::Accepted)
        return !has_attached_peer(description) || !m_for_server.is_empty();
    if (role == Role::Connected)
        return !has_attached_peer(description) || !m_for_client.is_empty();
    ASSERT_NOT_REACHED();
}

bool LocalSocket::has_attached_peer(const FileDescription& description) const
{
    auto role = this->role(description);
    if (role == Role::Accepted)
        return m_connect_side_fd != nullptr;
    if (role == Role::Connected)
        return m_accept_side_fd_open;
    ASSERT_NOT_REACHED();
}

bool LocalSocket::can_write(FileDescription& description) const
{
    auto role = this->role(description);
    if (role == Role::Accepted)
        return !has_attached_peer(description) || m_for_client.bytes_in_write_buffer() < 16384;
    if (role == Role::Connected)
        return !has_attached_peer(description) || m_for_server.bytes_in_write_buffer() < 16384;
    ASSERT_NOT_REACHED();
}

ssize_t LocalSocket::sendto(FileDescription& description, const void* data, size_t data_size, int, const sockaddr*, socklen_t)
{
    if (!has_attached_peer(description))
        return -EPIPE;
    auto role = this->role(description);
    if (role == Role::Accepted)
        return m_for_client.write((const u8*)data, data_size);
    if (role == Role::Connected)
        return m_for_server.write((const u8*)data, data_size);
    ASSERT_NOT_REACHED();
}

ssize_t LocalSocket::recvfrom(FileDescription& description, void* buffer, size_t buffer_size, int, sockaddr*, socklen_t*)
{
    auto role = this->role(description);
    if (role == Role::Accepted) {
        if (!description.is_blocking()) {
            if (m_for_server.is_empty())
                return -EAGAIN;
        }
        return m_for_server.read((u8*)buffer, buffer_size);
    }
    if (role == Role::Connected) {
        if (!description.is_blocking()) {
            if (m_for_client.is_empty())
                return -EAGAIN;
        }
        return m_for_client.read((u8*)buffer, buffer_size);
    }
    ASSERT_NOT_REACHED();
}

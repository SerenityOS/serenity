/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <AK/StringBuilder.h>
#include <Kernel/API/Ioctl.h>
#include <Kernel/API/POSIX/errno.h>
#include <Kernel/Debug.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Library/StdLib.h>
#include <Kernel/Locking/Mutex.h>
#include <Kernel/Locking/MutexProtected.h>
#include <Kernel/Net/LocalSocket.h>
#include <Kernel/Tasks/Process.h>
#include <Kernel/UnixTypes.h>

namespace Kernel {

static Singleton<MutexProtected<LocalSocket::List>> s_list;

static MutexProtected<LocalSocket::List>& all_sockets()
{
    return *s_list;
}

void LocalSocket::for_each(Function<void(LocalSocket const&)> callback)
{
    all_sockets().for_each_shared([&](auto const& socket) {
        callback(socket);
    });
}

ErrorOr<void> LocalSocket::try_for_each(Function<ErrorOr<void>(LocalSocket const&)> callback)
{
    return all_sockets().with_shared([&](auto const& sockets) -> ErrorOr<void> {
        for (auto& socket : sockets)
            TRY(callback(socket));
        return {};
    });
}

ErrorOr<NonnullRefPtr<LocalSocket>> LocalSocket::try_create(int type)
{
    auto client_buffer = TRY(DoubleBuffer::try_create("LocalSocket: Client buffer"sv));
    auto server_buffer = TRY(DoubleBuffer::try_create("LocalSocket: Server buffer"sv));
    return adopt_nonnull_ref_or_enomem(new (nothrow) LocalSocket(type, move(client_buffer), move(server_buffer)));
}

ErrorOr<SocketPair> LocalSocket::try_create_connected_pair(int type)
{
    auto socket = TRY(LocalSocket::try_create(type));
    auto description1 = TRY(OpenFileDescription::try_create(*socket));

    TRY(socket->try_set_path("[socketpair]"sv));

    socket->set_acceptor(Process::current());
    socket->set_connected(true);
    socket->set_connect_side_role(Role::Connected);
    socket->set_role(Role::Accepted);

    auto description2 = TRY(OpenFileDescription::try_create(*socket));

    return SocketPair { move(description1), move(description2) };
}

LocalSocket::LocalSocket(int type, NonnullOwnPtr<DoubleBuffer> client_buffer, NonnullOwnPtr<DoubleBuffer> server_buffer)
    : Socket(AF_LOCAL, type, 0)
    , m_for_client(move(client_buffer))
    , m_for_server(move(server_buffer))
{
    auto& current_process = Process::current();
    auto current_process_credentials = current_process.credentials();
    m_prebind_uid = current_process_credentials->euid();
    m_prebind_gid = current_process_credentials->egid();
    m_prebind_mode = 0666;

    m_for_client->set_unblock_callback([this]() {
        evaluate_block_conditions();
    });
    m_for_server->set_unblock_callback([this]() {
        evaluate_block_conditions();
    });

    all_sockets().with_exclusive([&](auto& list) {
        list.append(*this);
    });

    dbgln_if(LOCAL_SOCKET_DEBUG, "LocalSocket({}) created with type={}", this, type);
}

LocalSocket::~LocalSocket()
{
    all_sockets().with_exclusive([&](auto& list) {
        list.remove(*this);
    });
}

void LocalSocket::get_local_address(sockaddr* address, socklen_t* address_size)
{
    auto& address_un = *reinterpret_cast<sockaddr_un*>(address);
    address_un = {
        .sun_family = AF_UNIX,
        .sun_path = {},
    };

    if (!m_path || m_path->is_empty()) {
        *address_size = sizeof(address_un.sun_family);
        return;
    }

    size_t bytes_to_copy = min(m_path->length() + 1, min(static_cast<size_t>(*address_size), sizeof(address_un.sun_path)));
    memcpy(address_un.sun_path, m_path->characters(), bytes_to_copy);
    *address_size = sizeof(address_un.sun_family) + bytes_to_copy;
}

void LocalSocket::get_peer_address(sockaddr* address, socklen_t* address_size)
{
    get_local_address(address, address_size);
}

ErrorOr<void> LocalSocket::bind(Credentials const& credentials, Userspace<sockaddr const*> user_address, socklen_t address_size)
{
    VERIFY(setup_state() == SetupState::Unstarted);
    if (address_size > sizeof(sockaddr_un))
        return set_so_error(EINVAL);

    sockaddr_un address = {};
    SOCKET_TRY(copy_from_user(&address, user_address, address_size));

    if (address.sun_family != AF_LOCAL)
        return set_so_error(EINVAL);

    auto path = SOCKET_TRY(KString::try_create(StringView { address.sun_path, strnlen(address.sun_path, sizeof(address.sun_path)) }));
    dbgln_if(LOCAL_SOCKET_DEBUG, "LocalSocket({}) bind({})", this, path);

    mode_t mode = S_IFSOCK | (m_prebind_mode & 0777);
    UidAndGid owner { m_prebind_uid, m_prebind_gid };
    auto result = VirtualFileSystem::open(Process::current().vfs_root_context(), credentials, path->view(), O_CREAT | O_EXCL | O_NOFOLLOW_NOERROR, mode, Process::current().current_directory(), owner);
    if (result.is_error()) {
        if (result.error().code() == EEXIST)
            return set_so_error(EADDRINUSE);
        return result.release_error();
    }

    auto file = move(result.value());
    auto inode = file->inode();

    VERIFY(inode);
    if (!inode->bind_socket(*this))
        return set_so_error(EADDRINUSE);

    m_inode = inode;

    m_path = move(path);
    m_bound.set();
    return {};
}

ErrorOr<void> LocalSocket::connect(Credentials const& credentials, OpenFileDescription& description, Userspace<sockaddr const*> user_address, socklen_t address_size)
{
    if (m_bound.was_set())
        return set_so_error(EISCONN);

    if (address_size > sizeof(sockaddr_un))
        return set_so_error(EINVAL);

    sockaddr_un address = {};
    SOCKET_TRY(copy_from_user(&address, user_address, address_size));

    if (address.sun_family != AF_LOCAL)
        return set_so_error(EINVAL);

    if (is_connected())
        return set_so_error(EISCONN);

    auto path = SOCKET_TRY(KString::try_create(StringView { address.sun_path, strnlen(address.sun_path, sizeof(address.sun_path)) }));
    dbgln_if(LOCAL_SOCKET_DEBUG, "LocalSocket({}) connect({})", this, *path);

    auto file = SOCKET_TRY(VirtualFileSystem::open(Process::current().vfs_root_context(), credentials, path->view(), O_RDWR, 0, Process::current().current_directory()));
    auto inode = file->inode();
    m_inode = inode;

    VERIFY(inode);

    auto peer = inode->bound_socket();
    if (!peer)
        return set_so_error(ECONNREFUSED);

    m_path = move(path);

    VERIFY(m_connect_side_fd == &description);
    set_connect_side_role(Role::Connecting);

    auto result = peer->queue_connection_from(*this);
    if (result.is_error()) {
        set_connect_side_role(Role::None);
        return result;
    }

    if (is_connected()) {
        set_connect_side_role(Role::Connected);
        return {};
    }

    auto unblock_flags = Thread::OpenFileDescriptionBlocker::BlockFlags::None;
    if (Thread::current()->block<Thread::ConnectBlocker>({}, description, unblock_flags).was_interrupted()) {
        set_connect_side_role(Role::None);
        return set_so_error(EINTR);
    }

    dbgln_if(LOCAL_SOCKET_DEBUG, "LocalSocket({}) connect({}) status is {}", this, *m_path, to_string(setup_state()));

    if (!has_flag(unblock_flags, Thread::OpenFileDescriptionBlocker::BlockFlags::Connect)) {
        set_connect_side_role(Role::None);
        return set_so_error(ECONNREFUSED);
    }
    set_connect_side_role(Role::Connected);
    return {};
}

ErrorOr<void> LocalSocket::listen(size_t backlog)
{
    MutexLocker locker(mutex());
    if (type() != SOCK_STREAM)
        return set_so_error(EOPNOTSUPP);
    set_backlog(backlog);
    auto previous_role = m_role;
    set_role(Role::Listener);
    set_connect_side_role(Role::Listener, previous_role != m_role);

    dbgln_if(LOCAL_SOCKET_DEBUG, "LocalSocket({}) listening with backlog={}", this, backlog);

    return {};
}

ErrorOr<void> LocalSocket::attach(OpenFileDescription& description)
{
    VERIFY(!m_accept_side_fd_open);
    if (m_connect_side_role == Role::None) {
        VERIFY(m_connect_side_fd == nullptr);
        m_connect_side_fd = &description;
    } else {
        VERIFY(m_connect_side_fd != &description);
        m_accept_side_fd_open = true;
    }

    evaluate_block_conditions();
    return {};
}

void LocalSocket::detach(OpenFileDescription& description)
{
    if (m_connect_side_fd == &description) {
        m_connect_side_fd = nullptr;
    } else {
        VERIFY(m_accept_side_fd_open);
        m_accept_side_fd_open = false;

        if (m_bound.was_set()) {
            if (m_inode)
                m_inode->unbind_socket();
        }
    }

    evaluate_block_conditions();
}

bool LocalSocket::can_read(OpenFileDescription const& description, u64) const
{
    auto role = this->role(description);
    if (role == Role::Listener)
        return can_accept();
    if (role == Role::Accepted)
        return !has_attached_peer(description) || !m_for_server->is_empty();
    if (role == Role::Connected)
        return !has_attached_peer(description) || !m_for_client->is_empty();
    return false;
}

bool LocalSocket::has_attached_peer(OpenFileDescription const& description) const
{
    auto role = this->role(description);
    if (role == Role::Accepted)
        return m_connect_side_fd != nullptr;
    if (role == Role::Connected)
        return m_accept_side_fd_open;
    return false;
}

bool LocalSocket::can_write(OpenFileDescription const& description, u64) const
{
    auto role = this->role(description);
    if (role == Role::Accepted)
        return !has_attached_peer(description) || m_for_client->space_for_writing();
    if (role == Role::Connected)
        return !has_attached_peer(description) || m_for_server->space_for_writing();
    return false;
}

ErrorOr<size_t> LocalSocket::sendto(OpenFileDescription& description, UserOrKernelBuffer const& data, size_t data_size, int, Userspace<sockaddr const*>, socklen_t)
{
    if (!has_attached_peer(description))
        return set_so_error(EPIPE);
    auto* socket_buffer = send_buffer_for(description);
    if (!socket_buffer)
        return set_so_error(EINVAL);
    auto nwritten_or_error = socket_buffer->write(data, data_size);
    if (!nwritten_or_error.is_error() && nwritten_or_error.value() > 0)
        Thread::current()->did_unix_socket_write(nwritten_or_error.value());
    return nwritten_or_error;
}

DoubleBuffer* LocalSocket::receive_buffer_for(OpenFileDescription& description)
{
    auto role = this->role(description);
    if (role == Role::Accepted)
        return m_for_server.ptr();
    if (role == Role::Connected)
        return m_for_client.ptr();
    return nullptr;
}

DoubleBuffer* LocalSocket::send_buffer_for(OpenFileDescription& description)
{
    auto role = this->role(description);
    if (role == Role::Connected)
        return m_for_server.ptr();
    if (role == Role::Accepted)
        return m_for_client.ptr();
    return nullptr;
}

ErrorOr<size_t> LocalSocket::recvfrom(OpenFileDescription& description, UserOrKernelBuffer& buffer, size_t buffer_size, int, Userspace<sockaddr*>, Userspace<socklen_t*>, UnixDateTime&, bool blocking)
{
    auto* socket_buffer = receive_buffer_for(description);
    if (!socket_buffer)
        return set_so_error(EINVAL);
    if (!blocking) {
        if (socket_buffer->is_empty()) {
            if (!has_attached_peer(description))
                return 0;
            return set_so_error(EAGAIN);
        }
    } else if (!can_read(description, 0)) {
        auto unblock_flags = Thread::OpenFileDescriptionBlocker::BlockFlags::None;
        if (Thread::current()->block<Thread::ReadBlocker>({}, description, unblock_flags).was_interrupted())
            return set_so_error(EINTR);
    }
    if (!has_attached_peer(description) && socket_buffer->is_empty())
        return 0;
    VERIFY(!socket_buffer->is_empty());
    auto nread_or_error = socket_buffer->read(buffer, buffer_size);
    if (!nread_or_error.is_error() && nread_or_error.value() > 0)
        Thread::current()->did_unix_socket_read(nread_or_error.value());
    return nread_or_error;
}

StringView LocalSocket::socket_path() const
{
    if (!m_path)
        return {};
    return m_path->view();
}

ErrorOr<NonnullOwnPtr<KString>> LocalSocket::pseudo_path(OpenFileDescription const& description) const
{
    StringBuilder builder;
    TRY(builder.try_append("socket:"sv));
    TRY(builder.try_append(socket_path()));

    switch (role(description)) {
    case Role::Listener:
        TRY(builder.try_append(" (listening)"sv));
        break;
    case Role::Accepted:
        TRY(builder.try_appendff(" (accepted from pid {})", origin_pid()));
        break;
    case Role::Connected:
        TRY(builder.try_appendff(" (connected to pid {})", acceptor_pid()));
        break;
    case Role::Connecting:
        TRY(builder.try_append(" (connecting)"sv));
        break;
    default:
        break;
    }

    return KString::try_create(builder.string_view());
}

ErrorOr<void> LocalSocket::getsockopt(OpenFileDescription& description, int level, int option, Userspace<void*> value, Userspace<socklen_t*> value_size)
{
    if (level != SOL_SOCKET)
        return Socket::getsockopt(description, level, option, value, value_size);

    MutexLocker locker(mutex());

    socklen_t size;
    TRY(copy_from_user(&size, value_size.unsafe_userspace_ptr()));

    switch (option) {
    case SO_SNDBUF: {
        if (size != sizeof(int))
            return EINVAL;
        int snd_buffer_size = static_cast<int>(m_for_server->space_for_writing() + m_for_server->immediately_readable());
        TRY(copy_to_user(static_ptr_cast<int*>(value), &snd_buffer_size));
        TRY(copy_to_user(value_size, &size));
        return {};
    }
    case SO_RCVBUF: {
        if (size != sizeof(int))
            return EINVAL;
        int rcv_buffer_size = static_cast<int>(m_for_client->space_for_writing() + m_for_client->immediately_readable());
        TRY(copy_to_user(static_ptr_cast<int*>(value), &rcv_buffer_size));
        TRY(copy_to_user(value_size, &size));
        return {};
    }
    case SO_PEERCRED: {
        if (size < sizeof(ucred))
            return EINVAL;
        switch (role(description)) {
        case Role::Accepted:
            TRY(copy_to_user(static_ptr_cast<ucred*>(value), &m_origin));
            size = sizeof(ucred);
            TRY(copy_to_user(value_size, &size));
            return {};
        case Role::Connected:
            TRY(copy_to_user(static_ptr_cast<ucred*>(value), &m_acceptor));
            size = sizeof(ucred);
            TRY(copy_to_user(value_size, &size));
            return {};
        case Role::Connecting:
            return ENOTCONN;
        default:
            return EINVAL;
        }
        VERIFY_NOT_REACHED();
    }
    default:
        return Socket::getsockopt(description, level, option, value, value_size);
    }
}

ErrorOr<void> LocalSocket::ioctl(OpenFileDescription& description, unsigned request, Userspace<void*> arg)
{
    switch (request) {
    case FIONREAD: {
        int readable = receive_buffer_for(description)->immediately_readable();
        return copy_to_user(static_ptr_cast<int*>(arg), &readable);
    }
    }

    return EINVAL;
}

ErrorOr<void> LocalSocket::chmod(Credentials const& credentials, OpenFileDescription& description, mode_t mode)
{
    if (m_inode) {
        if (auto custody = description.custody())
            return VirtualFileSystem::chmod(credentials, *custody, mode);
        VERIFY_NOT_REACHED();
    }

    m_prebind_mode = mode & 0777;
    return {};
}

ErrorOr<void> LocalSocket::chown(Credentials const& credentials, OpenFileDescription& description, UserID uid, GroupID gid)
{
    if (m_inode) {
        if (auto custody = description.custody())
            return VirtualFileSystem::chown(credentials, *custody, uid, gid);
        VERIFY_NOT_REACHED();
    }

    if (!credentials.is_superuser() && (credentials.euid() != uid || !credentials.in_group(gid)))
        return set_so_error(EPERM);

    m_prebind_uid = uid;
    m_prebind_gid = gid;
    return {};
}

Vector<NonnullRefPtr<OpenFileDescription>>& LocalSocket::recvfd_queue_for(OpenFileDescription const& description)
{
    VERIFY(mutex().is_exclusively_locked_by_current_thread());
    auto role = this->role(description);
    if (role == Role::Connected)
        return m_fds_for_client;
    if (role == Role::Accepted)
        return m_fds_for_server;
    VERIFY_NOT_REACHED();
}

Vector<NonnullRefPtr<OpenFileDescription>>& LocalSocket::sendfd_queue_for(OpenFileDescription const& description)
{
    VERIFY(mutex().is_exclusively_locked_by_current_thread());
    auto role = this->role(description);
    if (role == Role::Connected)
        return m_fds_for_server;
    if (role == Role::Accepted)
        return m_fds_for_client;
    VERIFY_NOT_REACHED();
}

ErrorOr<void> LocalSocket::sendfd(OpenFileDescription const& socket_description, NonnullRefPtr<OpenFileDescription> passing_description)
{
    MutexLocker locker(mutex());
    auto role = this->role(socket_description);
    if (role != Role::Connected && role != Role::Accepted)
        return set_so_error(EINVAL);
    auto& queue = sendfd_queue_for(socket_description);
    // FIXME: Figure out how we should limit this properly.
    if (queue.size() > 128)
        return set_so_error(EBUSY);
    SOCKET_TRY(queue.try_append(move(passing_description)));
    return {};
}

ErrorOr<NonnullRefPtr<OpenFileDescription>> LocalSocket::recvfd(OpenFileDescription const& socket_description)
{
    MutexLocker locker(mutex());
    auto role = this->role(socket_description);
    if (role != Role::Connected && role != Role::Accepted)
        return set_so_error(EINVAL);
    auto& queue = recvfd_queue_for(socket_description);
    if (queue.is_empty()) {
        // FIXME: Figure out the perfect error code for this.
        return set_so_error(EAGAIN);
    }
    return queue.take_first();
}

ErrorOr<Vector<NonnullRefPtr<OpenFileDescription>>> LocalSocket::recvfds(OpenFileDescription const& socket_description, int n)
{
    MutexLocker locker(mutex());
    Vector<NonnullRefPtr<OpenFileDescription>> fds;

    auto role = this->role(socket_description);
    if (role != Role::Connected && role != Role::Accepted)
        return set_so_error(EINVAL);
    auto& queue = recvfd_queue_for(socket_description);

    for (int i = 0; i < n; ++i) {
        if (queue.is_empty())
            break;

        fds.append(queue.take_first());
    }

    return fds;
}

ErrorOr<void> LocalSocket::try_set_path(StringView path)
{
    m_path = TRY(KString::try_create(path));
    return {};
}

}

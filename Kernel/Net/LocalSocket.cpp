/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/Singleton.h>
#include <AK/StringBuilder.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Net/LocalSocket.h>
#include <Kernel/Process.h>
#include <Kernel/StdLib.h>
#include <Kernel/UnixTypes.h>
#include <LibC/errno_numbers.h>

//#define DEBUG_LOCAL_SOCKET

namespace Kernel {

static AK::Singleton<Lockable<InlineLinkedList<LocalSocket>>> s_list;

Lockable<InlineLinkedList<LocalSocket>>& LocalSocket::all_sockets()
{
    return *s_list;
}

void LocalSocket::for_each(Function<void(const LocalSocket&)> callback)
{
    LOCKER(all_sockets().lock(), Lock::Mode::Shared);
    for (auto& socket : all_sockets().resource())
        callback(socket);
}

KResultOr<NonnullRefPtr<Socket>> LocalSocket::create(int type)
{
    return adopt(*new LocalSocket(type));
}

LocalSocket::LocalSocket(int type)
    : Socket(AF_LOCAL, type, 0)
{
    LOCKER(all_sockets().lock());
    all_sockets().resource().append(this);

    auto current_process = Process::current();
    m_prebind_uid = current_process->uid();
    m_prebind_gid = current_process->gid();
    m_prebind_mode = 0666;

#ifdef DEBUG_LOCAL_SOCKET
    dbg() << "LocalSocket{" << this << "} created with type=" << type;
#endif
}

LocalSocket::~LocalSocket()
{
    LOCKER(all_sockets().lock());
    all_sockets().resource().remove(this);
}

void LocalSocket::get_local_address(sockaddr* address, socklen_t* address_size)
{
    size_t bytes_to_copy = min(static_cast<size_t>(*address_size), sizeof(sockaddr_un));
    memcpy(address, &m_address, bytes_to_copy);
    *address_size = sizeof(sockaddr_un);
}

void LocalSocket::get_peer_address(sockaddr* address, socklen_t* address_size)
{
    get_local_address(address, address_size);
}

KResult LocalSocket::bind(Userspace<const sockaddr*> user_address, socklen_t address_size)
{
    ASSERT(setup_state() == SetupState::Unstarted);
    if (address_size != sizeof(sockaddr_un))
        return KResult(-EINVAL);

    sockaddr_un address;
    if (!copy_from_user(&address, user_address, sizeof(sockaddr_un)))
        return KResult(-EFAULT);

    if (address.sun_family != AF_LOCAL)
        return KResult(-EINVAL);

    auto path = String(address.sun_path, strnlen(address.sun_path, sizeof(address.sun_path)));

#ifdef DEBUG_LOCAL_SOCKET
    dbg() << "LocalSocket{" << this << "} bind(" << path << ")";
#endif

    mode_t mode = S_IFSOCK | (m_prebind_mode & 04777);
    UidAndGid owner { m_prebind_uid, m_prebind_gid };
    auto result = VFS::the().open(path, O_CREAT | O_EXCL | O_NOFOLLOW_NOERROR, mode, Process::current()->current_directory(), owner);
    if (result.is_error()) {
        if (result.error() == -EEXIST)
            return KResult(-EADDRINUSE);
        return result.error();
    }

    auto file = move(result.value());

    ASSERT(file->inode());
    if (!file->inode()->bind_socket(*this))
        return KResult(-EADDRINUSE);

    m_file = move(file);

    m_address = address;
    m_bound = true;
    return KSuccess;
}

KResult LocalSocket::connect(FileDescription& description, Userspace<const sockaddr*> address, socklen_t address_size, ShouldBlock)
{
    ASSERT(!m_bound);
    if (address_size != sizeof(sockaddr_un))
        return KResult(-EINVAL);
    u16 sa_family_copy;
    auto* user_address = reinterpret_cast<const sockaddr*>(address.unsafe_userspace_ptr());
    if (!copy_from_user(&sa_family_copy, &user_address->sa_family, sizeof(u16)))
        return KResult(-EFAULT);
    if (sa_family_copy != AF_LOCAL)
        return KResult(-EINVAL);
    if (is_connected())
        return KResult(-EISCONN);

    const auto& local_address = *reinterpret_cast<const sockaddr_un*>(user_address);
    char safe_address[sizeof(local_address.sun_path) + 1] = { 0 };
    if (!copy_from_user(&safe_address[0], &local_address.sun_path[0], sizeof(safe_address) - 1))
        return KResult(-EFAULT);
    safe_address[sizeof(safe_address) - 1] = '\0';

#ifdef DEBUG_LOCAL_SOCKET
    dbg() << "LocalSocket{" << this << "} connect(" << safe_address << ")";
#endif

    auto description_or_error = VFS::the().open(safe_address, O_RDWR, 0, Process::current()->current_directory());
    if (description_or_error.is_error())
        return KResult(-ECONNREFUSED);

    m_file = move(description_or_error.value());

    ASSERT(m_file->inode());
    if (!m_file->inode()->socket())
        return KResult(-ECONNREFUSED);

    m_address.sun_family = sa_family_copy;
    memcpy(m_address.sun_path, safe_address, sizeof(m_address.sun_path));

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

    if (Thread::current()->block<Thread::ConnectBlocker>(nullptr, description).was_interrupted()) {
        m_connect_side_role = Role::None;
        return KResult(-EINTR);
    }

#ifdef DEBUG_LOCAL_SOCKET
    dbg() << "LocalSocket{" << this << "} connect(" << safe_address << ") status is " << to_string(setup_state());
#endif

    if (!is_connected()) {
        m_connect_side_role = Role::None;
        return KResult(-ECONNREFUSED);
    }
    m_connect_side_role = Role::Connected;
    return KSuccess;
}

KResult LocalSocket::listen(size_t backlog)
{
    LOCKER(lock());
    if (type() != SOCK_STREAM)
        return KResult(-EOPNOTSUPP);
    set_backlog(backlog);
    m_connect_side_role = m_role = Role::Listener;
#ifdef DEBUG_LOCAL_SOCKET
    dbg() << "LocalSocket{" << this << "} listening with backlog=" << backlog;
#endif
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

bool LocalSocket::can_read(const FileDescription& description, size_t) const
{
    auto role = this->role(description);
    if (role == Role::Listener)
        return can_accept();
    if (role == Role::Accepted)
        return !has_attached_peer(description) || !m_for_server.is_empty();
    if (role == Role::Connected)
        return !has_attached_peer(description) || !m_for_client.is_empty();
    return false;
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

bool LocalSocket::can_write(const FileDescription& description, size_t) const
{
    auto role = this->role(description);
    if (role == Role::Accepted)
        return !has_attached_peer(description) || m_for_client.space_for_writing();
    if (role == Role::Connected)
        return !has_attached_peer(description) || m_for_server.space_for_writing();
    return false;
}

KResultOr<size_t> LocalSocket::sendto(FileDescription& description, const UserOrKernelBuffer& data, size_t data_size, int, Userspace<const sockaddr*>, socklen_t)
{
    if (!has_attached_peer(description))
        return KResult(-EPIPE);
    ssize_t nwritten = send_buffer_for(description).write(data, data_size);
    if (nwritten > 0)
        Thread::current()->did_unix_socket_write(nwritten);
    return nwritten;
}

DoubleBuffer& LocalSocket::receive_buffer_for(FileDescription& description)
{
    auto role = this->role(description);
    if (role == Role::Accepted)
        return m_for_server;
    if (role == Role::Connected)
        return m_for_client;
    ASSERT_NOT_REACHED();
}

DoubleBuffer& LocalSocket::send_buffer_for(FileDescription& description)
{
    auto role = this->role(description);
    if (role == Role::Connected)
        return m_for_server;
    if (role == Role::Accepted)
        return m_for_client;
    ASSERT_NOT_REACHED();
}

KResultOr<size_t> LocalSocket::recvfrom(FileDescription& description, UserOrKernelBuffer& buffer, size_t buffer_size, int, Userspace<sockaddr*>, Userspace<socklen_t*>, timeval&)
{
    auto& buffer_for_me = receive_buffer_for(description);
    if (!description.is_blocking()) {
        if (buffer_for_me.is_empty()) {
            if (!has_attached_peer(description))
                return 0;
            return KResult(-EAGAIN);
        }
    } else if (!can_read(description, 0)) {
        if (Thread::current()->block<Thread::ReadBlocker>(nullptr, description).was_interrupted())
            return KResult(-EINTR);
    }
    if (!has_attached_peer(description) && buffer_for_me.is_empty())
        return 0;
    ASSERT(!buffer_for_me.is_empty());
    int nread = buffer_for_me.read(buffer, buffer_size);
    if (nread > 0)
        Thread::current()->did_unix_socket_read(nread);
    return nread;
}

StringView LocalSocket::socket_path() const
{
    size_t len = strnlen(m_address.sun_path, sizeof(m_address.sun_path));
    return { m_address.sun_path, len };
}

String LocalSocket::absolute_path(const FileDescription& description) const
{
    StringBuilder builder;
    builder.append("socket:");
    builder.append(socket_path());

    switch (role(description)) {
    case Role::Listener:
        builder.append(" (listening)");
        break;
    case Role::Accepted:
        builder.appendf(" (accepted from pid %d)", origin_pid());
        break;
    case Role::Connected:
        builder.appendf(" (connected to pid %d)", acceptor_pid());
        break;
    case Role::Connecting:
        builder.append(" (connecting)");
        break;
    default:
        break;
    }

    return builder.to_string();
}

KResult LocalSocket::getsockopt(FileDescription& description, int level, int option, Userspace<void*> value, Userspace<socklen_t*> value_size)
{
    if (level != SOL_SOCKET)
        return Socket::getsockopt(description, level, option, value, value_size);

    socklen_t size;
    if (!copy_from_user(&size, value_size.unsafe_userspace_ptr()))
        return KResult(-EFAULT);

    switch (option) {
    case SO_PEERCRED: {
        if (size < sizeof(ucred))
            return KResult(-EINVAL);
        switch (role(description)) {
        case Role::Accepted:
            if (!copy_to_user(static_ptr_cast<ucred*>(value), &m_origin))
                return KResult(-EFAULT);
            size = sizeof(ucred);
            if (!copy_to_user(value_size, &size))
                return KResult(-EFAULT);
            return KSuccess;
        case Role::Connected:
            if (!copy_to_user(static_ptr_cast<ucred*>(value), &m_acceptor))
                return KResult(-EFAULT);
            size = sizeof(ucred);
            if (!copy_to_user(value_size, &size))
                return KResult(-EFAULT);
            return KSuccess;
        case Role::Connecting:
            return KResult(-ENOTCONN);
        default:
            return KResult(-EINVAL);
        }
        break;
    }
    default:
        return Socket::getsockopt(description, level, option, value, value_size);
    }
}

KResult LocalSocket::chmod(FileDescription&, mode_t mode)
{
    if (m_file)
        return m_file->chmod(mode);

    m_prebind_mode = mode & 04777;
    return KSuccess;
}

KResult LocalSocket::chown(FileDescription&, uid_t uid, gid_t gid)
{
    if (m_file)
        return m_file->chown(uid, gid);

    auto current_process = Process::current();
    if (!current_process->is_superuser() && (current_process->euid() != uid || !current_process->in_group(gid)))
        return KResult(-EPERM);

    m_prebind_uid = uid;
    m_prebind_gid = gid;
    return KSuccess;
}

NonnullRefPtrVector<FileDescription>& LocalSocket::recvfd_queue_for(const FileDescription& description)
{
    auto role = this->role(description);
    if (role == Role::Connected)
        return m_fds_for_client;
    if (role == Role::Accepted)
        return m_fds_for_server;
    ASSERT_NOT_REACHED();
}

NonnullRefPtrVector<FileDescription>& LocalSocket::sendfd_queue_for(const FileDescription& description)
{
    auto role = this->role(description);
    if (role == Role::Connected)
        return m_fds_for_server;
    if (role == Role::Accepted)
        return m_fds_for_client;
    ASSERT_NOT_REACHED();
}

KResult LocalSocket::sendfd(const FileDescription& socket_description, FileDescription& passing_description)
{
    LOCKER(lock());
    auto role = this->role(socket_description);
    if (role != Role::Connected && role != Role::Accepted)
        return KResult(-EINVAL);
    auto& queue = sendfd_queue_for(socket_description);
    // FIXME: Figure out how we should limit this properly.
    if (queue.size() > 16)
        return KResult(-EBUSY);
    queue.append(move(passing_description));
    return KSuccess;
}

KResultOr<NonnullRefPtr<FileDescription>> LocalSocket::recvfd(const FileDescription& socket_description)
{
    LOCKER(lock());
    auto role = this->role(socket_description);
    if (role != Role::Connected && role != Role::Accepted)
        return KResult(-EINVAL);
    auto& queue = recvfd_queue_for(socket_description);
    if (queue.is_empty()) {
        // FIXME: Figure out the perfect error code for this.
        return KResult(-EAGAIN);
    }
    return queue.take_first();
}

}

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

#include <AK/StringBuilder.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Net/LocalSocket.h>
#include <Kernel/Process.h>
#include <Kernel/UnixTypes.h>
#include <LibBareMetal/StdLib.h>
#include <LibC/errno_numbers.h>

//#define DEBUG_LOCAL_SOCKET

namespace Kernel {

Lockable<InlineLinkedList<LocalSocket>>& LocalSocket::all_sockets()
{
    static Lockable<InlineLinkedList<LocalSocket>>* s_list;
    if (!s_list)
        s_list = new Lockable<InlineLinkedList<LocalSocket>>();
    return *s_list;
}

void LocalSocket::for_each(Function<void(LocalSocket&)> callback)
{
    LOCKER(all_sockets().lock());
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

    m_prebind_uid = Process::current->uid();
    m_prebind_gid = Process::current->gid();
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

KResult LocalSocket::bind(const sockaddr* user_address, socklen_t address_size)
{
    ASSERT(setup_state() == SetupState::Unstarted);
    if (address_size != sizeof(sockaddr_un))
        return KResult(-EINVAL);

    sockaddr_un address;
    copy_from_user(&address, user_address, sizeof(sockaddr_un));

    if (address.sun_family != AF_LOCAL)
        return KResult(-EINVAL);

    auto path = String(address.sun_path, strnlen(address.sun_path, sizeof(address.sun_path)));

#ifdef DEBUG_LOCAL_SOCKET
    dbg() << "LocalSocket{" << this << "} bind(" << safe_address << ")";
#endif

    mode_t mode = S_IFSOCK | (m_prebind_mode & 04777);
    UidAndGid owner { m_prebind_uid, m_prebind_gid };
    auto result = VFS::the().open(path, O_CREAT | O_EXCL | O_NOFOLLOW_NOERROR, mode, Process::current->current_directory(), owner);
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

KResult LocalSocket::connect(FileDescription& description, const sockaddr* address, socklen_t address_size, ShouldBlock)
{
    ASSERT(!m_bound);
    if (address_size != sizeof(sockaddr_un))
        return KResult(-EINVAL);
    if (address->sa_family != AF_LOCAL)
        return KResult(-EINVAL);
    if (is_connected())
        return KResult(-EISCONN);

    const sockaddr_un& local_address = *reinterpret_cast<const sockaddr_un*>(address);
    char safe_address[sizeof(local_address.sun_path) + 1] = { 0 };
    memcpy(safe_address, local_address.sun_path, sizeof(local_address.sun_path));

#ifdef DEBUG_LOCAL_SOCKET
    dbg() << "LocalSocket{" << this << "} connect(" << safe_address << ")";
#endif

    auto description_or_error = VFS::the().open(safe_address, O_RDWR, 0, Process::current->current_directory());
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

    if (Thread::current->block<Thread::ConnectBlocker>(description) != Thread::BlockResult::WokeNormally) {
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

bool LocalSocket::can_read(const FileDescription& description) const
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

bool LocalSocket::can_write(const FileDescription& description) const
{
    auto role = this->role(description);
    if (role == Role::Accepted)
        return !has_attached_peer(description) || m_for_client.space_for_writing();
    if (role == Role::Connected)
        return !has_attached_peer(description) || m_for_server.space_for_writing();
    return false;
}

ssize_t LocalSocket::sendto(FileDescription& description, const void* data, size_t data_size, int, const sockaddr*, socklen_t)
{
    if (!has_attached_peer(description))
        return -EPIPE;
    ssize_t nwritten = send_buffer_for(description).write((const u8*)data, data_size);
    if (nwritten > 0)
        Thread::current->did_unix_socket_write(nwritten);
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

ssize_t LocalSocket::recvfrom(FileDescription& description, void* buffer, size_t buffer_size, int, sockaddr*, socklen_t*)
{
    auto& buffer_for_me = receive_buffer_for(description);
    if (!description.is_blocking()) {
        if (buffer_for_me.is_empty()) {
            if (!has_attached_peer(description))
                return 0;
            return -EAGAIN;
        }
    } else if (!can_read(description)) {
        auto result = Thread::current->block<Thread::ReadBlocker>(description);
        if (result != Thread::BlockResult::WokeNormally)
            return -EINTR;
    }
    if (!has_attached_peer(description) && buffer_for_me.is_empty())
        return 0;
    ASSERT(!buffer_for_me.is_empty());
    int nread = buffer_for_me.read((u8*)buffer, buffer_size);
    if (nread > 0)
        Thread::current->did_unix_socket_read(nread);
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

KResult LocalSocket::getsockopt(FileDescription& description, int level, int option, void* value, socklen_t* value_size)
{
    if (level != SOL_SOCKET)
        return Socket::getsockopt(description, level, option, value, value_size);

    switch (option) {
    case SO_PEERCRED: {
        if (*value_size < sizeof(ucred))
            return KResult(-EINVAL);
        auto& creds = *(ucred*)value;
        switch (role(description)) {
        case Role::Accepted:
            creds = m_origin;
            *value_size = sizeof(ucred);
            return KSuccess;
        case Role::Connected:
            creds = m_acceptor;
            *value_size = sizeof(ucred);
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

KResult LocalSocket::chmod(mode_t mode)
{
    if (m_file)
        return m_file->chmod(mode);

    m_prebind_mode = mode & 04777;
    return KSuccess;
}

KResult LocalSocket::chown(uid_t uid, gid_t gid)
{
    if (m_file)
        return m_file->chown(uid, gid);

    if (!Process::current->is_superuser() && (Process::current->euid() != uid || !Process::current->in_group(gid)))
        return KResult(-EPERM);

    m_prebind_uid = uid;
    m_prebind_gid = gid;
    return KSuccess;
}

}

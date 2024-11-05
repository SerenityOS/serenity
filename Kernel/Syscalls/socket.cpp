/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteBuffer.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/Net/LocalSocket.h>
#include <Kernel/Tasks/Process.h>
#include <Kernel/UnixTypes.h>

namespace Kernel {

#define REQUIRE_PROMISE_FOR_SOCKET_DOMAIN(domain) \
    do {                                          \
        if (domain == AF_INET)                    \
            TRY(require_promise(Pledge::inet));   \
        else if (domain == AF_INET6)              \
            TRY(require_promise(Pledge::inet));   \
        else if (domain == AF_LOCAL)              \
            TRY(require_promise(Pledge::unix));   \
    } while (0)

static void setup_socket_fd(Process::OpenFileDescriptions& fds, int fd, NonnullRefPtr<OpenFileDescription> description, int type)
{
    description->set_readable(true);
    description->set_writable(true);
    unsigned flags = 0;
    if (type & SOCK_CLOEXEC)
        flags |= FD_CLOEXEC;
    if (type & SOCK_NONBLOCK)
        description->set_blocking(false);
    fds[fd].set(*description, flags);
}

ErrorOr<FlatPtr> Process::sys$socket(int domain, int type, int protocol)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    REQUIRE_PROMISE_FOR_SOCKET_DOMAIN(domain);

    auto credentials = this->credentials();
    if ((type & SOCK_TYPE_MASK) == SOCK_RAW && !credentials->is_superuser())
        return EACCES;

    return m_fds.with_exclusive([&](auto& fds) -> ErrorOr<FlatPtr> {
        auto fd_allocation = TRY(fds.allocate());
        auto socket = TRY(Socket::create(domain, type, protocol));
        auto description = TRY(OpenFileDescription::try_create(socket));
        setup_socket_fd(fds, fd_allocation.fd, move(description), type);
        return fd_allocation.fd;
    });
}

ErrorOr<FlatPtr> Process::sys$bind(int sockfd, Userspace<sockaddr const*> address, socklen_t address_length)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    auto description = TRY(open_file_description(sockfd));
    if (!description->is_socket())
        return ENOTSOCK;
    auto& socket = *description->socket();
    REQUIRE_PROMISE_FOR_SOCKET_DOMAIN(socket.domain());
    TRY(socket.bind(credentials(), address, address_length));
    return 0;
}

ErrorOr<FlatPtr> Process::sys$listen(int sockfd, int backlog)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    // As per POSIX, the behavior of listen() with a backlog value of 0 is implementation defined:
    // "A backlog argument of 0 may allow the socket to accept connections, in which case the length of the listen queue may be set to an implementation-defined minimum value."
    // Since creating a socket that can't accept any connections seems relatively useless, and as other platforms (Linux, FreeBSD, etc) chose to support accepting connections
    // with this backlog value, support it as well by normalizing it to 1.
    // Also, as per POSIX, the behaviour of a negative backlog value is equivalent to a backlog value of 0:
    // "If listen() is called with a backlog argument value that is less than 0, the function behaves as if it had been called with a backlog argument value of 0."
    if (backlog <= 0)
        backlog = 1;
    auto description = TRY(open_file_description(sockfd));
    if (!description->is_socket())
        return ENOTSOCK;
    auto& socket = *description->socket();
    REQUIRE_PROMISE_FOR_SOCKET_DOMAIN(socket.domain());
    if (socket.is_connected())
        return EINVAL;
    TRY(socket.listen(backlog));
    return 0;
}

ErrorOr<FlatPtr> Process::sys$accept4(Userspace<Syscall::SC_accept4_params const*> user_params)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::accept));
    auto params = TRY(copy_typed_from_user(user_params));

    int accepting_socket_fd = params.sockfd;
    Userspace<sockaddr*> user_address((FlatPtr)params.addr);
    Userspace<socklen_t*> user_address_size((FlatPtr)params.addrlen);
    int flags = params.flags;

    socklen_t address_size = 0;
    if (user_address)
        TRY(copy_from_user(&address_size, static_ptr_cast<socklen_t const*>(user_address_size)));

    ScopedDescriptionAllocation fd_allocation;
    RefPtr<OpenFileDescription> accepting_socket_description;

    TRY(m_fds.with_exclusive([&](auto& fds) -> ErrorOr<void> {
        fd_allocation = TRY(fds.allocate());
        accepting_socket_description = TRY(fds.open_file_description(accepting_socket_fd));
        return {};
    }));
    if (!accepting_socket_description->is_socket())
        return ENOTSOCK;
    auto& socket = *accepting_socket_description->socket();

    LockRefPtr<Socket> accepted_socket;
    for (;;) {
        accepted_socket = socket.accept();
        if (accepted_socket)
            break;
        if (!accepting_socket_description->is_blocking())
            return EAGAIN;
        auto unblock_flags = Thread::FileBlocker::BlockFlags::None;
        if (Thread::current()->block<Thread::AcceptBlocker>({}, *accepting_socket_description, unblock_flags).was_interrupted())
            return EINTR;
    }

    if (user_address) {
        sockaddr_un address_buffer {};
        address_size = min(sizeof(sockaddr_un), static_cast<size_t>(address_size));
        accepted_socket->get_peer_address((sockaddr*)&address_buffer, &address_size);
        TRY(copy_to_user(user_address, &address_buffer, address_size));
        TRY(copy_to_user(user_address_size, &address_size));
    }

    auto accepted_socket_description = TRY(OpenFileDescription::try_create(*accepted_socket));

    accepted_socket_description->set_readable(true);
    accepted_socket_description->set_writable(true);
    if (flags & SOCK_NONBLOCK)
        accepted_socket_description->set_blocking(false);
    int fd_flags = 0;
    if (flags & SOCK_CLOEXEC)
        fd_flags |= FD_CLOEXEC;

    TRY(m_fds.with_exclusive([&](auto& fds) -> ErrorOr<void> {
        fds[fd_allocation.fd].set(move(accepted_socket_description), fd_flags);
        return {};
    }));

    // NOTE: Moving this state to Completed is what causes connect() to unblock on the client side.
    accepted_socket->set_setup_state(Socket::SetupState::Completed);
    return fd_allocation.fd;
}

ErrorOr<FlatPtr> Process::sys$connect(int sockfd, Userspace<sockaddr const*> user_address, socklen_t user_address_size)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);

    auto description = TRY(open_file_description(sockfd));
    if (!description->is_socket())
        return ENOTSOCK;
    auto& socket = *description->socket();
    REQUIRE_PROMISE_FOR_SOCKET_DOMAIN(socket.domain());
    TRY(socket.connect(credentials(), *description, user_address, user_address_size));
    return 0;
}

ErrorOr<FlatPtr> Process::sys$shutdown(int sockfd, int how)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::stdio));
    if (how != SHUT_RD && how != SHUT_WR && how != SHUT_RDWR)
        return EINVAL;
    auto description = TRY(open_file_description(sockfd));
    if (!description->is_socket())
        return ENOTSOCK;
    auto& socket = *description->socket();
    REQUIRE_PROMISE_FOR_SOCKET_DOMAIN(socket.domain());
    TRY(socket.shutdown(how));
    return 0;
}

ErrorOr<FlatPtr> Process::sys$sendmsg(int sockfd, Userspace<const struct msghdr*> user_msg, int flags)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);
    TRY(require_promise(Pledge::stdio));
    auto msg = TRY(copy_typed_from_user(user_msg));

    if (msg.msg_iovlen != 1)
        return ENOTSUP; // FIXME: Support this :)
    Vector<iovec, 1> iovs;
    TRY(iovs.try_resize(msg.msg_iovlen));
    TRY(copy_n_from_user(iovs.data(), msg.msg_iov, msg.msg_iovlen));
    if (iovs[0].iov_len > NumericLimits<ssize_t>::max())
        return EINVAL;

    Userspace<sockaddr const*> user_addr((FlatPtr)msg.msg_name);
    socklen_t addr_length = msg.msg_namelen;

    auto description = TRY(open_file_description(sockfd));
    if (!description->is_socket())
        return ENOTSOCK;

    auto& socket = *description->socket();
    if (socket.is_shut_down_for_writing()) {
        if ((flags & MSG_NOSIGNAL) == 0)
            Thread::current()->send_signal(SIGPIPE, &Process::current());
        return EPIPE;
    }

    if (msg.msg_controllen > 0) {
        // Handle command messages.
        auto cmsg_buffer = TRY(ByteBuffer::create_uninitialized(msg.msg_controllen));
        TRY(copy_from_user(cmsg_buffer.data(), msg.msg_control, msg.msg_controllen));
        msg.msg_control = cmsg_buffer.data();
        for (struct cmsghdr* cmsg = CMSG_FIRSTHDR(&msg); cmsg != nullptr; cmsg = CMSG_NXTHDR(&msg, cmsg)) {
            if (socket.is_local() && cmsg->cmsg_level == SOL_SOCKET && cmsg->cmsg_type == SCM_RIGHTS) {
                auto& local_socket = static_cast<LocalSocket&>(socket);
                int* fds = (int*)CMSG_DATA(cmsg);
                size_t nfds = (cmsg->cmsg_len - CMSG_ALIGN(sizeof(struct cmsghdr))) / sizeof(int);
                for (size_t i = 0; i < nfds; ++i) {
                    TRY(local_socket.sendfd(*description, TRY(open_file_description(fds[i]))));
                }
            }
        }
    }

    auto data_buffer = TRY(UserOrKernelBuffer::for_user_buffer((u8*)iovs[0].iov_base, iovs[0].iov_len));

    while (true) {
        while (!description->can_write()) {
            if (!description->is_blocking()) {
                return EAGAIN;
            }

            auto unblock_flags = Thread::FileBlocker::BlockFlags::None;
            if (Thread::current()->block<Thread::WriteBlocker>({}, *description, unblock_flags).was_interrupted()) {
                return EINTR;
            }
            // TODO: handle exceptions in unblock_flags
        }

        auto bytes_sent_or_error = socket.sendto(*description, data_buffer, iovs[0].iov_len, flags, user_addr, addr_length);
        if (bytes_sent_or_error.is_error()) {
            if ((flags & MSG_NOSIGNAL) == 0 && bytes_sent_or_error.error().code() == EPIPE)
                Thread::current()->send_signal(SIGPIPE, &Process::current());
            return bytes_sent_or_error.release_error();
        }

        auto bytes_sent = bytes_sent_or_error.release_value();
        if (bytes_sent > 0)
            return bytes_sent;
    }
}

ErrorOr<FlatPtr> Process::sys$recvmsg(int sockfd, Userspace<struct msghdr*> user_msg, int flags)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);
    TRY(require_promise(Pledge::stdio));

    struct msghdr msg;
    TRY(copy_from_user(&msg, user_msg));

    if (msg.msg_iovlen != 1)
        return ENOTSUP; // FIXME: Support this :)
    Vector<iovec, 1> iovs;
    TRY(iovs.try_resize(msg.msg_iovlen));
    TRY(copy_n_from_user(iovs.data(), msg.msg_iov, msg.msg_iovlen));

    Userspace<sockaddr*> user_addr((FlatPtr)msg.msg_name);
    Userspace<socklen_t*> user_addr_length(msg.msg_name ? (FlatPtr)&user_msg.unsafe_userspace_ptr()->msg_namelen : 0);

    auto description = TRY(open_file_description(sockfd));
    if (!description->is_socket())
        return ENOTSOCK;
    auto& socket = *description->socket();

    if (socket.is_shut_down_for_reading())
        return 0;

    auto data_buffer = TRY(UserOrKernelBuffer::for_user_buffer((u8*)iovs[0].iov_base, iovs[0].iov_len));
    UnixDateTime timestamp {};
    bool blocking = (flags & MSG_DONTWAIT) ? false : description->is_blocking();
    auto result = socket.recvfrom(*description, data_buffer, iovs[0].iov_len, flags, user_addr, user_addr_length, timestamp, blocking);

    if (result.is_error())
        return result.release_error();

    int msg_flags = 0;

    if (result.value() > iovs[0].iov_len) {
        VERIFY(socket.type() != SOCK_STREAM);
        msg_flags |= MSG_TRUNC;
    }

    socklen_t current_cmsg_len = 0;
    auto try_add_cmsg = [&](int level, int type, void const* data, socklen_t len) -> ErrorOr<bool> {
        if (current_cmsg_len + len > msg.msg_controllen) {
            msg_flags |= MSG_CTRUNC;
            return false;
        }

        cmsghdr cmsg = { (socklen_t)CMSG_LEN(len), level, type };
        cmsghdr* target = (cmsghdr*)(((char*)msg.msg_control) + current_cmsg_len);
        TRY(copy_to_user(target, &cmsg));
        TRY(copy_to_user(CMSG_DATA(target), data, len));
        current_cmsg_len += CMSG_ALIGN(cmsg.cmsg_len);
        return true;
    };

    if (socket.wants_timestamp()) {
        timeval time = timestamp.to_timeval();
        TRY(try_add_cmsg(SOL_SOCKET, SCM_TIMESTAMP, &time, sizeof(time)));
    }

    int space_for_fds = (msg.msg_controllen - current_cmsg_len - sizeof(struct cmsghdr)) / sizeof(int);
    if (space_for_fds > 0 && socket.is_local()) {
        auto& local_socket = static_cast<LocalSocket&>(socket);
        auto descriptions = TRY(local_socket.recvfds(description, space_for_fds));
        Vector<int> fdnums;
        for (auto& description : descriptions) {
            auto fd_allocation = TRY(m_fds.with_exclusive([](auto& fds) { return fds.allocate(); }));
            m_fds.with_exclusive([&](auto& fds) { fds[fd_allocation.fd].set(*description, 0); });
            fdnums.append(fd_allocation.fd);
        }
        if (!fdnums.is_empty())
            TRY(try_add_cmsg(SOL_SOCKET, SCM_RIGHTS, fdnums.data(), fdnums.size() * sizeof(int)));
    }

    TRY(copy_to_user(&user_msg.unsafe_userspace_ptr()->msg_controllen, &current_cmsg_len));

    TRY(copy_to_user(&user_msg.unsafe_userspace_ptr()->msg_flags, &msg_flags));
    return result.value();
}

template<Process::SockOrPeerName sock_or_peer_name, typename Params>
ErrorOr<void> Process::get_sock_or_peer_name(Params const& params)
{
    socklen_t addrlen_value;
    TRY(copy_from_user(&addrlen_value, params.addrlen, sizeof(socklen_t)));

    if (addrlen_value <= 0)
        return EINVAL;

    auto description = TRY(open_file_description(params.sockfd));
    if (!description->is_socket())
        return ENOTSOCK;

    auto& socket = *description->socket();
    REQUIRE_PROMISE_FOR_SOCKET_DOMAIN(socket.domain());

    sockaddr_un address_buffer {};
    addrlen_value = min(sizeof(sockaddr_un), static_cast<size_t>(addrlen_value));
    if constexpr (sock_or_peer_name == SockOrPeerName::SockName)
        socket.get_local_address((sockaddr*)&address_buffer, &addrlen_value);
    else
        socket.get_peer_address((sockaddr*)&address_buffer, &addrlen_value);
    TRY(copy_to_user(params.addr, &address_buffer, addrlen_value));
    return copy_to_user(params.addrlen, &addrlen_value);
}

ErrorOr<FlatPtr> Process::sys$getsockname(Userspace<Syscall::SC_getsockname_params const*> user_params)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    auto params = TRY(copy_typed_from_user(user_params));
    TRY(get_sock_or_peer_name<SockOrPeerName::SockName>(params));
    return 0;
}

ErrorOr<FlatPtr> Process::sys$getpeername(Userspace<Syscall::SC_getpeername_params const*> user_params)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    auto params = TRY(copy_typed_from_user(user_params));
    TRY(get_sock_or_peer_name<SockOrPeerName::PeerName>(params));
    return 0;
}

ErrorOr<FlatPtr> Process::sys$getsockopt(Userspace<Syscall::SC_getsockopt_params const*> user_params)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    auto params = TRY(copy_typed_from_user(user_params));

    int sockfd = params.sockfd;
    int level = params.level;
    int option = params.option;
    Userspace<void*> user_value((FlatPtr)params.value);
    Userspace<socklen_t*> user_value_size((FlatPtr)params.value_size);

    socklen_t value_size;
    TRY(copy_from_user(&value_size, params.value_size, sizeof(socklen_t)));

    auto description = TRY(open_file_description(sockfd));
    if (!description->is_socket())
        return ENOTSOCK;
    auto& socket = *description->socket();
    REQUIRE_PROMISE_FOR_SOCKET_DOMAIN(socket.domain());
    TRY(socket.getsockopt(*description, level, option, user_value, user_value_size));
    return 0;
}

ErrorOr<FlatPtr> Process::sys$setsockopt(Userspace<Syscall::SC_setsockopt_params const*> user_params)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    auto params = TRY(copy_typed_from_user(user_params));

    Userspace<void const*> user_value((FlatPtr)params.value);
    auto description = TRY(open_file_description(params.sockfd));
    if (!description->is_socket())
        return ENOTSOCK;
    auto& socket = *description->socket();
    REQUIRE_PROMISE_FOR_SOCKET_DOMAIN(socket.domain());
    TRY(socket.setsockopt(params.level, params.option, user_value, params.value_size));
    return 0;
}

ErrorOr<FlatPtr> Process::sys$socketpair(Userspace<Syscall::SC_socketpair_params const*> user_params)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    auto params = TRY(copy_typed_from_user(user_params));

    if (params.domain != AF_LOCAL)
        return EINVAL;

    if (params.protocol != 0 && params.protocol != PF_LOCAL)
        return EINVAL;

    auto pair = TRY(LocalSocket::try_create_connected_pair(params.type & SOCK_TYPE_MASK));

    return m_fds.with_exclusive([&](auto& fds) -> ErrorOr<FlatPtr> {
        auto fd_allocation0 = TRY(fds.allocate());
        auto fd_allocation1 = TRY(fds.allocate());

        int allocated_fds[2];
        allocated_fds[0] = fd_allocation0.fd;
        allocated_fds[1] = fd_allocation1.fd;
        setup_socket_fd(fds, allocated_fds[0], pair.description0, params.type);
        setup_socket_fd(fds, allocated_fds[1], pair.description1, params.type);

        if (copy_n_to_user(params.sv, allocated_fds, 2).is_error()) {
            // Avoid leaking both file descriptors on error.
            fds[allocated_fds[0]] = {};
            fds[allocated_fds[1]] = {};
            return EFAULT;
        }
        return 0;
    });
}

}

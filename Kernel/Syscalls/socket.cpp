/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/Net/LocalSocket.h>
#include <Kernel/Process.h>
#include <Kernel/UnixTypes.h>

namespace Kernel {

#define REQUIRE_PROMISE_FOR_SOCKET_DOMAIN(domain) \
    do {                                          \
        if (domain == AF_INET)                    \
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
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    REQUIRE_PROMISE_FOR_SOCKET_DOMAIN(domain);

    if ((type & SOCK_TYPE_MASK) == SOCK_RAW && !is_superuser())
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
    VERIFY_NO_PROCESS_BIG_LOCK(this)
    auto description = TRY(open_file_description(sockfd));
    if (!description->is_socket())
        return ENOTSOCK;
    auto& socket = *description->socket();
    REQUIRE_PROMISE_FOR_SOCKET_DOMAIN(socket.domain());
    TRY(socket.bind(address, address_length));
    return 0;
}

ErrorOr<FlatPtr> Process::sys$listen(int sockfd, int backlog)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this)
    if (backlog < 0)
        return EINVAL;
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
    VERIFY_NO_PROCESS_BIG_LOCK(this)
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

    RefPtr<Socket> accepted_socket;
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
    VERIFY_NO_PROCESS_BIG_LOCK(this)

    auto description = TRY(open_file_description(sockfd));
    if (!description->is_socket())
        return ENOTSOCK;
    auto& socket = *description->socket();
    REQUIRE_PROMISE_FOR_SOCKET_DOMAIN(socket.domain());
    TRY(socket.connect(*description, user_address, user_address_size, description->is_blocking() ? ShouldBlock::Yes : ShouldBlock::No));
    return 0;
}

ErrorOr<FlatPtr> Process::sys$shutdown(int sockfd, int how)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this)
    TRY(require_promise(Pledge::stdio));
    if (how & ~SHUT_RDWR)
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
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
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
    if (socket.is_shut_down_for_writing())
        return EPIPE;
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

        auto bytes_sent = TRY(socket.sendto(*description, data_buffer, iovs[0].iov_len, flags, user_addr, addr_length));
        if (bytes_sent > 0)
            return bytes_sent;
    }
}

ErrorOr<FlatPtr> Process::sys$recvmsg(int sockfd, Userspace<struct msghdr*> user_msg, int flags)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
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

    bool original_blocking = description->is_blocking();
    if (flags & MSG_DONTWAIT)
        description->set_blocking(false);

    auto data_buffer = TRY(UserOrKernelBuffer::for_user_buffer((u8*)iovs[0].iov_base, iovs[0].iov_len));
    Time timestamp {};
    auto result = socket.recvfrom(*description, data_buffer, iovs[0].iov_len, flags, user_addr, user_addr_length, timestamp);
    if (flags & MSG_DONTWAIT)
        description->set_blocking(original_blocking);

    if (result.is_error())
        return result.release_error();

    int msg_flags = 0;

    if (result.value() > iovs[0].iov_len) {
        VERIFY(socket.type() != SOCK_STREAM);
        msg_flags |= MSG_TRUNC;
    }

    if (socket.wants_timestamp()) {
        struct {
            cmsghdr cmsg;
            timeval timestamp;
        } cmsg_timestamp;
        socklen_t control_length = sizeof(cmsg_timestamp);
        if (msg.msg_controllen < control_length) {
            msg_flags |= MSG_CTRUNC;
        } else {
            cmsg_timestamp = { { control_length, SOL_SOCKET, SCM_TIMESTAMP }, timestamp.to_timeval() };
            TRY(copy_to_user(msg.msg_control, &cmsg_timestamp, control_length));
        }
        TRY(copy_to_user(&user_msg.unsafe_userspace_ptr()->msg_controllen, &control_length));
    }

    TRY(copy_to_user(&user_msg.unsafe_userspace_ptr()->msg_flags, &msg_flags));
    return result.value();
}

template<bool sockname, typename Params>
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
    if constexpr (sockname)
        socket.get_local_address((sockaddr*)&address_buffer, &addrlen_value);
    else
        socket.get_peer_address((sockaddr*)&address_buffer, &addrlen_value);
    TRY(copy_to_user(params.addr, &address_buffer, addrlen_value));
    return copy_to_user(params.addrlen, &addrlen_value);
}

ErrorOr<FlatPtr> Process::sys$getsockname(Userspace<Syscall::SC_getsockname_params const*> user_params)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    auto params = TRY(copy_typed_from_user(user_params));
    TRY(get_sock_or_peer_name<true>(params));
    return 0;
}

ErrorOr<FlatPtr> Process::sys$getpeername(Userspace<Syscall::SC_getpeername_params const*> user_params)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    auto params = TRY(copy_typed_from_user(user_params));
    TRY(get_sock_or_peer_name<false>(params));
    return 0;
}

ErrorOr<FlatPtr> Process::sys$getsockopt(Userspace<Syscall::SC_getsockopt_params const*> user_params)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this)
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
    VERIFY_NO_PROCESS_BIG_LOCK(this)
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
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
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

        if (copy_to_user(params.sv, allocated_fds, sizeof(allocated_fds)).is_error()) {
            // Avoid leaking both file descriptors on error.
            fds[allocated_fds[0]] = {};
            fds[allocated_fds[1]] = {};
            return EFAULT;
        }
        return 0;
    });
}

}

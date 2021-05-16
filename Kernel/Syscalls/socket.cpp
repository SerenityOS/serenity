/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/Net/IPv4Socket.h>
#include <Kernel/Net/LocalSocket.h>
#include <Kernel/Process.h>
#include <Kernel/UnixTypes.h>

namespace Kernel {

#define REQUIRE_PROMISE_FOR_SOCKET_DOMAIN(domain) \
    do {                                          \
        if (domain == AF_INET)                    \
            REQUIRE_PROMISE(inet);                \
        else if (domain == AF_LOCAL)              \
            REQUIRE_PROMISE(unix);                \
    } while (0)

void Process::setup_socket_fd(int fd, NonnullRefPtr<FileDescription> description, int type)
{
    description->set_readable(true);
    description->set_writable(true);
    unsigned flags = 0;
    if (type & SOCK_CLOEXEC)
        flags |= FD_CLOEXEC;
    if (type & SOCK_NONBLOCK)
        description->set_blocking(false);
    m_fds[fd].set(*description, flags);
}

KResultOr<int> Process::sys$socket(int domain, int type, int protocol)
{
    REQUIRE_PROMISE_FOR_SOCKET_DOMAIN(domain);

    if ((type & SOCK_TYPE_MASK) == SOCK_RAW && !is_superuser())
        return EACCES;
    int fd = alloc_fd();
    if (fd < 0)
        return fd;
    auto result = Socket::create(domain, type, protocol);
    if (result.is_error())
        return result.error();
    auto description_result = FileDescription::create(*result.value());
    if (description_result.is_error())
        return description_result.error();
    setup_socket_fd(fd, description_result.value(), type);
    return fd;
}

KResultOr<int> Process::sys$bind(int sockfd, Userspace<const sockaddr*> address, socklen_t address_length)
{
    auto description = file_description(sockfd);
    if (!description)
        return EBADF;
    if (!description->is_socket())
        return ENOTSOCK;
    auto& socket = *description->socket();
    REQUIRE_PROMISE_FOR_SOCKET_DOMAIN(socket.domain());
    return socket.bind(address, address_length);
}

KResultOr<int> Process::sys$listen(int sockfd, int backlog)
{
    if (backlog < 0)
        return EINVAL;
    auto description = file_description(sockfd);
    if (!description)
        return EBADF;
    if (!description->is_socket())
        return ENOTSOCK;
    auto& socket = *description->socket();
    REQUIRE_PROMISE_FOR_SOCKET_DOMAIN(socket.domain());
    if (socket.is_connected())
        return EINVAL;
    return socket.listen(backlog);
}

KResultOr<int> Process::sys$accept4(Userspace<const Syscall::SC_accept4_params*> user_params)
{
    REQUIRE_PROMISE(accept);

    Syscall::SC_accept4_params params;
    if (!copy_from_user(&params, user_params))
        return EFAULT;

    int accepting_socket_fd = params.sockfd;
    Userspace<sockaddr*> user_address((FlatPtr)params.addr);
    Userspace<socklen_t*> user_address_size((FlatPtr)params.addrlen);
    int flags = params.flags;

    socklen_t address_size = 0;
    if (user_address && !copy_from_user(&address_size, static_ptr_cast<const socklen_t*>(user_address_size)))
        return EFAULT;

    int accepted_socket_fd = alloc_fd();
    if (accepted_socket_fd < 0)
        return accepted_socket_fd;
    auto accepting_socket_description = file_description(accepting_socket_fd);
    if (!accepting_socket_description)
        return EBADF;
    if (!accepting_socket_description->is_socket())
        return ENOTSOCK;
    auto& socket = *accepting_socket_description->socket();

    if (!socket.can_accept()) {
        if (accepting_socket_description->is_blocking()) {
            auto unblock_flags = Thread::FileBlocker::BlockFlags::None;
            if (Thread::current()->block<Thread::AcceptBlocker>({}, *accepting_socket_description, unblock_flags).was_interrupted())
                return EINTR;
        } else {
            return EAGAIN;
        }
    }
    auto accepted_socket = socket.accept();
    VERIFY(accepted_socket);

    if (user_address) {
        u8 address_buffer[sizeof(sockaddr_un)];
        address_size = min(sizeof(sockaddr_un), static_cast<size_t>(address_size));
        accepted_socket->get_peer_address((sockaddr*)address_buffer, &address_size);
        if (!copy_to_user(user_address, address_buffer, address_size))
            return EFAULT;
        if (!copy_to_user(user_address_size, &address_size))
            return EFAULT;
    }

    auto accepted_socket_description_result = FileDescription::create(*accepted_socket);
    if (accepted_socket_description_result.is_error())
        return accepted_socket_description_result.error();

    accepted_socket_description_result.value()->set_readable(true);
    accepted_socket_description_result.value()->set_writable(true);
    if (flags & SOCK_NONBLOCK)
        accepted_socket_description_result.value()->set_blocking(false);
    int fd_flags = 0;
    if (flags & SOCK_CLOEXEC)
        fd_flags |= FD_CLOEXEC;
    m_fds[accepted_socket_fd].set(accepted_socket_description_result.release_value(), fd_flags);

    // NOTE: Moving this state to Completed is what causes connect() to unblock on the client side.
    accepted_socket->set_setup_state(Socket::SetupState::Completed);
    return accepted_socket_fd;
}

KResultOr<int> Process::sys$connect(int sockfd, Userspace<const sockaddr*> user_address, socklen_t user_address_size)
{
    int fd = alloc_fd();
    if (fd < 0)
        return fd;
    auto description = file_description(sockfd);
    if (!description)
        return EBADF;
    if (!description->is_socket())
        return ENOTSOCK;

    auto& socket = *description->socket();
    REQUIRE_PROMISE_FOR_SOCKET_DOMAIN(socket.domain());

    return socket.connect(*description, user_address, user_address_size, description->is_blocking() ? ShouldBlock::Yes : ShouldBlock::No);
}

KResultOr<int> Process::sys$shutdown(int sockfd, int how)
{
    REQUIRE_PROMISE(stdio);
    if (how & ~SHUT_RDWR)
        return EINVAL;
    auto description = file_description(sockfd);
    if (!description)
        return EBADF;
    if (!description->is_socket())
        return ENOTSOCK;

    auto& socket = *description->socket();
    REQUIRE_PROMISE_FOR_SOCKET_DOMAIN(socket.domain());
    return socket.shutdown(how);
}

KResultOr<ssize_t> Process::sys$sendmsg(int sockfd, Userspace<const struct msghdr*> user_msg, int flags)
{
    REQUIRE_PROMISE(stdio);
    struct msghdr msg;
    if (!copy_from_user(&msg, user_msg))
        return EFAULT;

    if (msg.msg_iovlen != 1)
        return ENOTSUP; // FIXME: Support this :)
    Vector<iovec, 1> iovs;
    if (!iovs.try_resize(msg.msg_iovlen))
        return ENOMEM;
    if (!copy_n_from_user(iovs.data(), msg.msg_iov, msg.msg_iovlen))
        return EFAULT;

    Userspace<const sockaddr*> user_addr((FlatPtr)msg.msg_name);
    socklen_t addr_length = msg.msg_namelen;

    auto description = file_description(sockfd);
    if (!description)
        return EBADF;
    if (!description->is_socket())
        return ENOTSOCK;
    auto& socket = *description->socket();
    if (socket.is_shut_down_for_writing())
        return EPIPE;
    auto data_buffer = UserOrKernelBuffer::for_user_buffer((u8*)iovs[0].iov_base, iovs[0].iov_len);
    if (!data_buffer.has_value())
        return EFAULT;
    auto result = socket.sendto(*description, data_buffer.value(), iovs[0].iov_len, flags, user_addr, addr_length);
    if (result.is_error())
        return result.error();
    return result.value();
}

KResultOr<ssize_t> Process::sys$recvmsg(int sockfd, Userspace<struct msghdr*> user_msg, int flags)
{
    REQUIRE_PROMISE(stdio);

    struct msghdr msg;
    if (!copy_from_user(&msg, user_msg))
        return EFAULT;

    if (msg.msg_iovlen != 1)
        return ENOTSUP; // FIXME: Support this :)
    Vector<iovec, 1> iovs;
    if (!iovs.try_resize(msg.msg_iovlen))
        return ENOMEM;
    if (!copy_n_from_user(iovs.data(), msg.msg_iov, msg.msg_iovlen))
        return EFAULT;

    Userspace<sockaddr*> user_addr((FlatPtr)msg.msg_name);
    Userspace<socklen_t*> user_addr_length(msg.msg_name ? (FlatPtr)&user_msg.unsafe_userspace_ptr()->msg_namelen : 0);

    auto description = file_description(sockfd);
    if (!description)
        return EBADF;
    if (!description->is_socket())
        return ENOTSOCK;
    auto& socket = *description->socket();

    if (socket.is_shut_down_for_reading())
        return 0;

    bool original_blocking = description->is_blocking();
    if (flags & MSG_DONTWAIT)
        description->set_blocking(false);

    auto data_buffer = UserOrKernelBuffer::for_user_buffer((u8*)iovs[0].iov_base, iovs[0].iov_len);
    if (!data_buffer.has_value())
        return EFAULT;
    Time timestamp {};
    auto result = socket.recvfrom(*description, data_buffer.value(), iovs[0].iov_len, flags, user_addr, user_addr_length, timestamp);
    if (flags & MSG_DONTWAIT)
        description->set_blocking(original_blocking);

    if (result.is_error())
        return result.error();

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
            if (!copy_to_user(msg.msg_control, &cmsg_timestamp, control_length))
                return EFAULT;
        }
        if (!copy_to_user(&user_msg.unsafe_userspace_ptr()->msg_controllen, &control_length))
            return EFAULT;
    }

    if (!copy_to_user(&user_msg.unsafe_userspace_ptr()->msg_flags, &msg_flags))
        return EFAULT;

    return result.value();
}

template<bool sockname, typename Params>
int Process::get_sock_or_peer_name(const Params& params)
{
    socklen_t addrlen_value;
    if (!copy_from_user(&addrlen_value, params.addrlen, sizeof(socklen_t)))
        return EFAULT;

    if (addrlen_value <= 0)
        return EINVAL;

    auto description = file_description(params.sockfd);
    if (!description)
        return EBADF;

    if (!description->is_socket())
        return ENOTSOCK;

    auto& socket = *description->socket();
    REQUIRE_PROMISE_FOR_SOCKET_DOMAIN(socket.domain());

    u8 address_buffer[sizeof(sockaddr_un)];
    addrlen_value = min(sizeof(sockaddr_un), static_cast<size_t>(addrlen_value));
    if constexpr (sockname)
        socket.get_local_address((sockaddr*)address_buffer, &addrlen_value);
    else
        socket.get_peer_address((sockaddr*)address_buffer, &addrlen_value);
    if (!copy_to_user(params.addr, address_buffer, addrlen_value))
        return EFAULT;
    if (!copy_to_user(params.addrlen, &addrlen_value))
        return EFAULT;
    return 0;
}

KResultOr<int> Process::sys$getsockname(Userspace<const Syscall::SC_getsockname_params*> user_params)
{
    Syscall::SC_getsockname_params params;
    if (!copy_from_user(&params, user_params))
        return EFAULT;
    return get_sock_or_peer_name<true>(params);
}

KResultOr<int> Process::sys$getpeername(Userspace<const Syscall::SC_getpeername_params*> user_params)
{
    Syscall::SC_getpeername_params params;
    if (!copy_from_user(&params, user_params))
        return EFAULT;
    return get_sock_or_peer_name<false>(params);
}

KResultOr<int> Process::sys$getsockopt(Userspace<const Syscall::SC_getsockopt_params*> user_params)
{
    Syscall::SC_getsockopt_params params;
    if (!copy_from_user(&params, user_params))
        return EFAULT;

    int sockfd = params.sockfd;
    int level = params.level;
    int option = params.option;
    Userspace<void*> user_value((FlatPtr)params.value);
    Userspace<socklen_t*> user_value_size((FlatPtr)params.value_size);

    socklen_t value_size;
    if (!copy_from_user(&value_size, params.value_size, sizeof(socklen_t)))
        return EFAULT;

    auto description = file_description(sockfd);
    if (!description)
        return EBADF;
    if (!description->is_socket())
        return ENOTSOCK;
    auto& socket = *description->socket();

    REQUIRE_PROMISE_FOR_SOCKET_DOMAIN(socket.domain());
    return socket.getsockopt(*description, level, option, user_value, user_value_size);
}

KResultOr<int> Process::sys$setsockopt(Userspace<const Syscall::SC_setsockopt_params*> user_params)
{
    Syscall::SC_setsockopt_params params;
    if (!copy_from_user(&params, user_params))
        return EFAULT;
    Userspace<const void*> user_value((FlatPtr)params.value);
    auto description = file_description(params.sockfd);
    if (!description)
        return EBADF;
    if (!description->is_socket())
        return ENOTSOCK;
    auto& socket = *description->socket();
    REQUIRE_PROMISE_FOR_SOCKET_DOMAIN(socket.domain());
    return socket.setsockopt(params.level, params.option, user_value, params.value_size);
}

KResultOr<int> Process::sys$socketpair(Userspace<const Syscall::SC_socketpair_params*> user_params)
{
    Syscall::SC_socketpair_params params;
    if (!copy_from_user(&params, user_params))
        return EFAULT;

    if (params.domain != AF_LOCAL)
        return EINVAL;

    if (params.protocol != 0 && params.protocol != PF_LOCAL)
        return EINVAL;

    auto result = LocalSocket::create_connected_pair(params.type & SOCK_TYPE_MASK);
    if (result.is_error())
        return result.error();
    auto pair = result.value();

    int fds[2];
    fds[0] = alloc_fd();
    if (fds[0] < 0)
        return ENFILE;
    setup_socket_fd(fds[0], pair.description1, params.type);

    fds[1] = alloc_fd();
    if (fds[1] < 0) {
        // FIXME: This leaks fds[0]
        return ENFILE;
    }
    setup_socket_fd(fds[1], pair.description2, params.type);

    if (!copy_to_user(params.sv, fds, sizeof(fds))) {
        // FIXME: This leaks both file descriptors
        return EFAULT;
    }

    return KSuccess;
}
}

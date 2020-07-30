#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/Net/IPv4Socket.h>
#include <Kernel/Net/LocalSocket.h>
#include <Kernel/Process.h>

namespace Kernel {

#define REQUIRE_PROMISE_FOR_SOCKET_DOMAIN(domain) \
    do {                                          \
        if (domain == AF_INET)                    \
            REQUIRE_PROMISE(inet);                \
        else if (domain == AF_LOCAL)              \
            REQUIRE_PROMISE(unix);                \
    } while (0)

int Process::sys$socket(int domain, int type, int protocol)
{
    REQUIRE_PROMISE_FOR_SOCKET_DOMAIN(domain);

    if ((type & SOCK_TYPE_MASK) == SOCK_RAW && !is_superuser())
        return -EACCES;
    int fd = alloc_fd();
    if (fd < 0)
        return fd;
    auto result = Socket::create(domain, type, protocol);
    if (result.is_error())
        return result.error();
    auto description = FileDescription::create(*result.value());
    description->set_readable(true);
    description->set_writable(true);
    unsigned flags = 0;
    if (type & SOCK_CLOEXEC)
        flags |= FD_CLOEXEC;
    if (type & SOCK_NONBLOCK)
        description->set_blocking(false);
    m_fds[fd].set(move(description), flags);
    return fd;
}

int Process::sys$bind(int sockfd, const sockaddr* address, socklen_t address_length)
{
    if (!validate_read(address, address_length))
        return -EFAULT;
    auto description = file_description(sockfd);
    if (!description)
        return -EBADF;
    if (!description->is_socket())
        return -ENOTSOCK;
    auto& socket = *description->socket();
    REQUIRE_PROMISE_FOR_SOCKET_DOMAIN(socket.domain());
    return socket.bind(address, address_length);
}

int Process::sys$listen(int sockfd, int backlog)
{
    if (backlog < 0)
        return -EINVAL;
    auto description = file_description(sockfd);
    if (!description)
        return -EBADF;
    if (!description->is_socket())
        return -ENOTSOCK;
    auto& socket = *description->socket();
    REQUIRE_PROMISE_FOR_SOCKET_DOMAIN(socket.domain());
    if (socket.is_connected())
        return -EINVAL;
    return socket.listen(backlog);
}

int Process::sys$accept(int accepting_socket_fd, sockaddr* user_address, socklen_t* user_address_size)
{
    REQUIRE_PROMISE(accept);

    socklen_t address_size = 0;
    if (user_address) {
        if (!validate_write_typed(user_address_size))
            return -EFAULT;
        copy_from_user(&address_size, user_address_size);
        if (!validate_write(user_address, address_size))
            return -EFAULT;
    }

    int accepted_socket_fd = alloc_fd();
    if (accepted_socket_fd < 0)
        return accepted_socket_fd;
    auto accepting_socket_description = file_description(accepting_socket_fd);
    if (!accepting_socket_description)
        return -EBADF;
    if (!accepting_socket_description->is_socket())
        return -ENOTSOCK;
    auto& socket = *accepting_socket_description->socket();

    if (!socket.can_accept()) {
        if (accepting_socket_description->is_blocking()) {
            if (Thread::current()->block<Thread::AcceptBlocker>(*accepting_socket_description).was_interrupted())
                return -EINTR;
        } else {
            return -EAGAIN;
        }
    }
    auto accepted_socket = socket.accept();
    ASSERT(accepted_socket);

    if (user_address) {
        u8 address_buffer[sizeof(sockaddr_un)];
        address_size = min(sizeof(sockaddr_un), static_cast<size_t>(address_size));
        accepted_socket->get_peer_address((sockaddr*)address_buffer, &address_size);
        copy_to_user(user_address, address_buffer, address_size);
        copy_to_user(user_address_size, &address_size);
    }

    auto accepted_socket_description = FileDescription::create(*accepted_socket);
    accepted_socket_description->set_readable(true);
    accepted_socket_description->set_writable(true);
    // NOTE: The accepted socket inherits fd flags from the accepting socket.
    //       I'm not sure if this matches other systems but it makes sense to me.
    accepted_socket_description->set_blocking(accepting_socket_description->is_blocking());
    m_fds[accepted_socket_fd].set(move(accepted_socket_description), m_fds[accepting_socket_fd].flags());

    // NOTE: Moving this state to Completed is what causes connect() to unblock on the client side.
    accepted_socket->set_setup_state(Socket::SetupState::Completed);
    return accepted_socket_fd;
}

int Process::sys$connect(int sockfd, const sockaddr* user_address, socklen_t user_address_size)
{
    if (!validate_read(user_address, user_address_size))
        return -EFAULT;
    int fd = alloc_fd();
    if (fd < 0)
        return fd;
    auto description = file_description(sockfd);
    if (!description)
        return -EBADF;
    if (!description->is_socket())
        return -ENOTSOCK;

    auto& socket = *description->socket();
    REQUIRE_PROMISE_FOR_SOCKET_DOMAIN(socket.domain());

    u8 address[sizeof(sockaddr_un)];
    size_t address_size = min(sizeof(address), static_cast<size_t>(user_address_size));
    copy_from_user(address, user_address, address_size);

    return socket.connect(*description, (const sockaddr*)address, address_size, description->is_blocking() ? ShouldBlock::Yes : ShouldBlock::No);
}

int Process::sys$shutdown(int sockfd, int how)
{
    REQUIRE_PROMISE(stdio);
    if (how & ~SHUT_RDWR)
        return -EINVAL;
    auto description = file_description(sockfd);
    if (!description)
        return -EBADF;
    if (!description->is_socket())
        return -ENOTSOCK;

    auto& socket = *description->socket();
    REQUIRE_PROMISE_FOR_SOCKET_DOMAIN(socket.domain());
    return socket.shutdown(how);
}

ssize_t Process::sys$sendto(const Syscall::SC_sendto_params* user_params)
{
    REQUIRE_PROMISE(stdio);
    Syscall::SC_sendto_params params;
    if (!validate_read_and_copy_typed(&params, user_params))
        return -EFAULT;

    int flags = params.flags;
    const sockaddr* addr = params.addr;
    socklen_t addr_length = params.addr_length;

    if (!validate(params.data))
        return -EFAULT;
    if (addr && !validate_read(addr, addr_length))
        return -EFAULT;
    auto description = file_description(params.sockfd);
    if (!description)
        return -EBADF;
    if (!description->is_socket())
        return -ENOTSOCK;
    auto& socket = *description->socket();
    if (socket.is_shut_down_for_writing())
        return -EPIPE;
    SmapDisabler disabler;
    return socket.sendto(*description, params.data.data, params.data.size, flags, addr, addr_length);
}

ssize_t Process::sys$recvfrom(const Syscall::SC_recvfrom_params* user_params)
{
    REQUIRE_PROMISE(stdio);

    Syscall::SC_recvfrom_params params;
    if (!validate_read_and_copy_typed(&params, user_params))
        return -EFAULT;

    int flags = params.flags;
    sockaddr* addr = params.addr;
    socklen_t* addr_length = params.addr_length;

    SmapDisabler disabler;
    if (!validate(params.buffer))
        return -EFAULT;
    if (addr_length) {
        if (!validate_write_typed(addr_length))
            return -EFAULT;
        if (!validate_write(addr, *addr_length))
            return -EFAULT;
    } else if (addr) {
        return -EINVAL;
    }
    auto description = file_description(params.sockfd);
    if (!description)
        return -EBADF;
    if (!description->is_socket())
        return -ENOTSOCK;
    auto& socket = *description->socket();

    if (socket.is_shut_down_for_reading())
        return 0;

    bool original_blocking = description->is_blocking();
    if (flags & MSG_DONTWAIT)
        description->set_blocking(false);

    auto nrecv = socket.recvfrom(*description, params.buffer.data, params.buffer.size, flags, addr, addr_length);
    if (flags & MSG_DONTWAIT)
        description->set_blocking(original_blocking);

    return nrecv;
}

template<bool sockname, typename Params>
int Process::get_sock_or_peer_name(const Params& params)
{
    socklen_t addrlen_value;
    if (!validate_read_and_copy_typed(&addrlen_value, params.addrlen))
        return -EFAULT;

    if (addrlen_value <= 0)
        return -EINVAL;

    if (!validate_write(params.addr, addrlen_value))
        return -EFAULT;

    if (!validate_write_typed(params.addrlen))
        return -EFAULT;

    auto description = file_description(params.sockfd);
    if (!description)
        return -EBADF;

    if (!description->is_socket())
        return -ENOTSOCK;

    auto& socket = *description->socket();
    REQUIRE_PROMISE_FOR_SOCKET_DOMAIN(socket.domain());

    u8 address_buffer[sizeof(sockaddr_un)];
    addrlen_value = min(sizeof(sockaddr_un), static_cast<size_t>(addrlen_value));
    if constexpr (sockname)
        socket.get_local_address((sockaddr*)address_buffer, &addrlen_value);
    else
        socket.get_peer_address((sockaddr*)address_buffer, &addrlen_value);
    copy_to_user(params.addr, address_buffer, addrlen_value);
    copy_to_user(params.addrlen, &addrlen_value);
    return 0;
}

int Process::sys$getsockname(const Syscall::SC_getsockname_params* user_params)
{
    Syscall::SC_getsockname_params params;
    if (!validate_read_and_copy_typed(&params, user_params))
        return -EFAULT;
    return get_sock_or_peer_name<true>(params);
}

int Process::sys$getpeername(const Syscall::SC_getpeername_params* user_params)
{
    Syscall::SC_getpeername_params params;
    if (!validate_read_and_copy_typed(&params, user_params))
        return -EFAULT;
    return get_sock_or_peer_name<false>(params);
}

int Process::sys$getsockopt(const Syscall::SC_getsockopt_params* params)
{
    if (!validate_read_typed(params))
        return -EFAULT;

    SmapDisabler disabler;

    int sockfd = params->sockfd;
    int level = params->level;
    int option = params->option;
    void* value = params->value;
    socklen_t* value_size = params->value_size;

    if (!validate_write_typed(value_size))
        return -EFAULT;
    if (!validate_write(value, *value_size))
        return -EFAULT;
    auto description = file_description(sockfd);
    if (!description)
        return -EBADF;
    if (!description->is_socket())
        return -ENOTSOCK;
    auto& socket = *description->socket();

    if (has_promised(Pledge::accept) && socket.is_local() && level == SOL_SOCKET && option == SO_PEERCRED) {
        // We make an exception for SOL_SOCKET::SO_PEERCRED on local sockets if you've pledged "accept"
    } else {
        REQUIRE_PROMISE_FOR_SOCKET_DOMAIN(socket.domain());
    }
    return socket.getsockopt(*description, level, option, value, value_size);
}

int Process::sys$setsockopt(const Syscall::SC_setsockopt_params* user_params)
{
    Syscall::SC_setsockopt_params params;
    if (!validate_read_and_copy_typed(&params, user_params))
        return -EFAULT;
    if (!validate_read(params.value, params.value_size))
        return -EFAULT;
    auto description = file_description(params.sockfd);
    if (!description)
        return -EBADF;
    if (!description->is_socket())
        return -ENOTSOCK;
    auto& socket = *description->socket();
    REQUIRE_PROMISE_FOR_SOCKET_DOMAIN(socket.domain());
    return socket.setsockopt(params.level, params.option, params.value, params.value_size);
}

}

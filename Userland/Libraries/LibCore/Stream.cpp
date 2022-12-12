/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Stream.h"
#include <LibCore/System.h>
#include <fcntl.h>
#include <netdb.h>
#include <poll.h>
#include <sys/ioctl.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <unistd.h>
#ifdef AK_OS_SERENITY
#    include <serenity.h>
#endif
#ifdef AK_OS_FREEBSD
#    include <sys/ucred.h>
#endif

namespace Core::Stream {

bool Stream::read_or_error(Bytes buffer)
{
    VERIFY(buffer.size());

    size_t nread = 0;
    do {
        if (is_eof())
            return false;

        auto result = read(buffer.slice(nread));
        if (result.is_error()) {
            if (result.error().is_errno() && result.error().code() == EINTR) {
                continue;
            }

            return false;
        }

        nread += result.value().size();
    } while (nread < buffer.size());

    return true;
}

ErrorOr<ByteBuffer> Stream::read_all(size_t block_size)
{
    return read_all_impl(block_size);
}

ErrorOr<ByteBuffer> Stream::read_all_impl(size_t block_size, size_t expected_file_size)
{
    ByteBuffer data;
    data.ensure_capacity(expected_file_size);

    size_t total_read = 0;
    Bytes buffer;
    while (!is_eof()) {
        if (buffer.is_empty()) {
            buffer = TRY(data.get_bytes_for_writing(block_size));
        }

        auto nread = TRY(read(buffer)).size();
        total_read += nread;
        buffer = buffer.slice(nread);
    }

    data.resize(total_read);
    return data;
}

ErrorOr<void> Stream::discard(size_t discarded_bytes)
{
    // Note: This was chosen arbitrarily.
    // Note: This can't be PAGE_SIZE because it is defined to sysconf() on Lagom.
    constexpr size_t continuous_read_size = 4096;

    Array<u8, continuous_read_size> buffer;

    while (discarded_bytes > 0) {
        auto slice = TRY(read(buffer.span().slice(0, min(discarded_bytes, continuous_read_size))));
        discarded_bytes -= slice.size();
    }

    return {};
}

bool Stream::write_or_error(ReadonlyBytes buffer)
{
    VERIFY(buffer.size());

    size_t nwritten = 0;
    do {
        auto result = write(buffer.slice(nwritten));
        if (result.is_error()) {
            if (result.error().is_errno() && result.error().code() == EINTR) {
                continue;
            }

            return false;
        }

        nwritten += result.value();
    } while (nwritten < buffer.size());

    return true;
}

ErrorOr<off_t> SeekableStream::tell() const
{
    // Seek with 0 and SEEK_CUR does not modify anything despite the const_cast,
    // so it's safe to do this.
    return const_cast<SeekableStream*>(this)->seek(0, SeekMode::FromCurrentPosition);
}

ErrorOr<off_t> SeekableStream::size()
{
    auto original_position = TRY(tell());

    auto seek_result = seek(0, SeekMode::FromEndPosition);
    if (seek_result.is_error()) {
        // Let's try to restore the original position, just in case.
        auto restore_result = seek(original_position, SeekMode::SetPosition);
        if (restore_result.is_error()) {
            dbgln("Core::SeekableStream::size: Couldn't restore initial position, stream might have incorrect position now!");
        }

        return seek_result.release_error();
    }

    TRY(seek(original_position, SeekMode::SetPosition));
    return seek_result.value();
}

ErrorOr<void> SeekableStream::discard(size_t discarded_bytes)
{
    TRY(seek(discarded_bytes, SeekMode::FromCurrentPosition));
    return {};
}

ErrorOr<NonnullOwnPtr<File>> File::open(StringView filename, OpenMode mode, mode_t permissions)
{
    auto file = TRY(adopt_nonnull_own_or_enomem(new (nothrow) File(mode)));
    TRY(file->open_path(filename, permissions));
    return file;
}

ErrorOr<NonnullOwnPtr<File>> File::adopt_fd(int fd, OpenMode mode, ShouldCloseFileDescriptor should_close_file_descriptor)
{
    if (fd < 0) {
        return Error::from_errno(EBADF);
    }

    if (!has_any_flag(mode, OpenMode::ReadWrite)) {
        dbgln("Core::File::adopt_fd: Attempting to adopt a file with neither Read nor Write specified in mode");
        return Error::from_errno(EINVAL);
    }

    auto file = TRY(adopt_nonnull_own_or_enomem(new (nothrow) File(mode, should_close_file_descriptor)));
    file->m_fd = fd;
    return file;
}

ErrorOr<NonnullOwnPtr<File>> File::standard_input()
{
    return File::adopt_fd(STDIN_FILENO, OpenMode::Read, ShouldCloseFileDescriptor::No);
}
ErrorOr<NonnullOwnPtr<File>> File::standard_output()
{
    return File::adopt_fd(STDOUT_FILENO, OpenMode::Write, ShouldCloseFileDescriptor::No);
}
ErrorOr<NonnullOwnPtr<File>> File::standard_error()
{
    return File::adopt_fd(STDERR_FILENO, OpenMode::Write, ShouldCloseFileDescriptor::No);
}

ErrorOr<NonnullOwnPtr<File>> File::open_file_or_standard_stream(StringView filename, OpenMode mode)
{
    if (!filename.is_empty() && filename != "-"sv)
        return File::open(filename, mode);

    switch (mode) {
    case OpenMode::Read:
        return standard_input();
    case OpenMode::Write:
        return standard_output();
    default:
        VERIFY_NOT_REACHED();
    }
}

int File::open_mode_to_options(OpenMode mode)
{
    int flags = 0;
    if (has_flag(mode, OpenMode::ReadWrite)) {
        flags |= O_RDWR | O_CREAT;
    } else if (has_flag(mode, OpenMode::Read)) {
        flags |= O_RDONLY;
    } else if (has_flag(mode, OpenMode::Write)) {
        flags |= O_WRONLY | O_CREAT;
        bool should_truncate = !has_any_flag(mode, OpenMode::Append | OpenMode::MustBeNew);
        if (should_truncate)
            flags |= O_TRUNC;
    }

    if (has_flag(mode, OpenMode::Append))
        flags |= O_APPEND;
    if (has_flag(mode, OpenMode::Truncate))
        flags |= O_TRUNC;
    if (has_flag(mode, OpenMode::MustBeNew))
        flags |= O_EXCL;
    if (!has_flag(mode, OpenMode::KeepOnExec))
        flags |= O_CLOEXEC;
    if (!has_flag(mode, OpenMode::Nonblocking))
        flags |= O_NONBLOCK;
    return flags;
}

ErrorOr<void> File::open_path(StringView filename, mode_t permissions)
{
    VERIFY(m_fd == -1);
    auto flags = open_mode_to_options(m_mode);

    m_fd = TRY(System::open(filename, flags, permissions));
    return {};
}

ErrorOr<Bytes> File::read(Bytes buffer)
{
    if (!has_flag(m_mode, OpenMode::Read)) {
        // NOTE: POSIX says that if the fd is not open for reading, the call
        //       will return EBADF. Since we already know whether we can or
        //       can't read the file, let's avoid a syscall.
        return Error::from_errno(EBADF);
    }

    ssize_t nread = TRY(System::read(m_fd, buffer));
    m_last_read_was_eof = nread == 0;
    return buffer.trim(nread);
}

ErrorOr<ByteBuffer> File::read_all(size_t block_size)
{
    // Note: This is used as a heuristic, it's not valid for devices or virtual files.
    auto const potential_file_size = TRY(System::fstat(m_fd)).st_size;

    return read_all_impl(block_size, potential_file_size);
}

ErrorOr<size_t> File::write(ReadonlyBytes buffer)
{
    if (!has_flag(m_mode, OpenMode::Write)) {
        // NOTE: Same deal as Read.
        return Error::from_errno(EBADF);
    }

    return TRY(System::write(m_fd, buffer));
}

bool File::is_eof() const { return m_last_read_was_eof; }
bool File::is_open() const { return m_fd >= 0; }

void File::close()
{
    if (!is_open()) {
        return;
    }

    // NOTE: The closing of the file can be interrupted by a signal, in which
    // case EINTR will be returned by the close syscall. So let's try closing
    // the file until we aren't interrupted by rude signals. :^)
    ErrorOr<void> result;
    do {
        result = System::close(m_fd);
    } while (result.is_error() && result.error().code() == EINTR);

    VERIFY(!result.is_error());
    m_fd = -1;
}

ErrorOr<off_t> File::seek(i64 offset, SeekMode mode)
{
    int syscall_mode;
    switch (mode) {
    case SeekMode::SetPosition:
        syscall_mode = SEEK_SET;
        break;
    case SeekMode::FromCurrentPosition:
        syscall_mode = SEEK_CUR;
        break;
    case SeekMode::FromEndPosition:
        syscall_mode = SEEK_END;
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    off_t seek_result = TRY(System::lseek(m_fd, offset, syscall_mode));
    m_last_read_was_eof = false;
    return seek_result;
}

ErrorOr<void> File::truncate(off_t length)
{
    return System::ftruncate(m_fd, length);
}

ErrorOr<int> Socket::create_fd(SocketDomain domain, SocketType type)
{
    int socket_domain;
    switch (domain) {
    case SocketDomain::Inet:
        socket_domain = AF_INET;
        break;
    case SocketDomain::Local:
        socket_domain = AF_LOCAL;
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    int socket_type;
    switch (type) {
    case SocketType::Stream:
        socket_type = SOCK_STREAM;
        break;
    case SocketType::Datagram:
        socket_type = SOCK_DGRAM;
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    // Let's have a safe default of CLOEXEC. :^)
#ifdef SOCK_CLOEXEC
    return System::socket(socket_domain, socket_type | SOCK_CLOEXEC, 0);
#else
    auto fd = TRY(System::socket(socket_domain, socket_type, 0));
    TRY(System::fcntl(fd, F_SETFD, FD_CLOEXEC));
    return fd;
#endif
}

ErrorOr<IPv4Address> Socket::resolve_host(DeprecatedString const& host, SocketType type)
{
    int socket_type;
    switch (type) {
    case SocketType::Stream:
        socket_type = SOCK_STREAM;
        break;
    case SocketType::Datagram:
        socket_type = SOCK_DGRAM;
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    struct addrinfo hints = {};
    hints.ai_family = AF_UNSPEC;
    hints.ai_socktype = socket_type;
    hints.ai_flags = 0;
    hints.ai_protocol = 0;

    // FIXME: Convert this to Core::System
    struct addrinfo* results = nullptr;
    int rc = getaddrinfo(host.characters(), nullptr, &hints, &results);
    if (rc != 0) {
        if (rc == EAI_SYSTEM) {
            return Error::from_syscall("getaddrinfo"sv, -errno);
        }

        auto const* error_string = gai_strerror(rc);
        return Error::from_string_view({ error_string, strlen(error_string) });
    }

    ScopeGuard free_results = [results] { freeaddrinfo(results); };

    for (auto* result = results; result != nullptr; result = result->ai_next) {
        if (result->ai_family == AF_INET) {
            auto* socket_address = bit_cast<struct sockaddr_in*>(result->ai_addr);
            NetworkOrdered<u32> network_ordered_address { socket_address->sin_addr.s_addr };
            return IPv4Address { network_ordered_address };
        }
    }

    return Error::from_string_literal("Could not resolve to IPv4 address");
}

ErrorOr<void> Socket::connect_local(int fd, DeprecatedString const& path)
{
    auto address = SocketAddress::local(path);
    auto maybe_sockaddr = address.to_sockaddr_un();
    if (!maybe_sockaddr.has_value()) {
        dbgln("Core::Stream::Socket::connect_local: Could not obtain a sockaddr_un");
        return Error::from_errno(EINVAL);
    }

    auto addr = maybe_sockaddr.release_value();
    return System::connect(fd, bit_cast<struct sockaddr*>(&addr), sizeof(addr));
}

ErrorOr<void> Socket::connect_inet(int fd, SocketAddress const& address)
{
    auto addr = address.to_sockaddr_in();
    return System::connect(fd, bit_cast<struct sockaddr*>(&addr), sizeof(addr));
}

ErrorOr<Bytes> PosixSocketHelper::read(Bytes buffer, int flags)
{
    if (!is_open()) {
        return Error::from_errno(ENOTCONN);
    }

    ssize_t nread = TRY(System::recv(m_fd, buffer.data(), buffer.size(), flags));
    m_last_read_was_eof = nread == 0;

    // If a socket read is EOF, then no more data can be read from it because
    // the protocol has disconnected. In this case, we can just disable the
    // notifier if we have one.
    if (m_last_read_was_eof && m_notifier)
        m_notifier->set_enabled(false);

    return buffer.trim(nread);
}

ErrorOr<size_t> PosixSocketHelper::write(ReadonlyBytes buffer, int flags)
{
    if (!is_open()) {
        return Error::from_errno(ENOTCONN);
    }

    return TRY(System::send(m_fd, buffer.data(), buffer.size(), flags));
}

void PosixSocketHelper::close()
{
    if (!is_open()) {
        return;
    }

    if (m_notifier)
        m_notifier->set_enabled(false);

    ErrorOr<void> result;
    do {
        result = System::close(m_fd);
    } while (result.is_error() && result.error().code() == EINTR);

    VERIFY(!result.is_error());
    m_fd = -1;
}

ErrorOr<bool> PosixSocketHelper::can_read_without_blocking(int timeout) const
{
    struct pollfd the_fd = { .fd = m_fd, .events = POLLIN, .revents = 0 };

    // FIXME: Convert this to Core::System
    int rc;
    do {
        rc = ::poll(&the_fd, 1, timeout);
    } while (rc < 0 && errno == EINTR);

    if (rc < 0) {
        return Error::from_syscall("poll"sv, -errno);
    }

    return (the_fd.revents & POLLIN) > 0;
}

ErrorOr<void> PosixSocketHelper::set_blocking(bool enabled)
{
    int value = enabled ? 0 : 1;
    return System::ioctl(m_fd, FIONBIO, &value);
}

ErrorOr<void> PosixSocketHelper::set_close_on_exec(bool enabled)
{
    int flags = TRY(System::fcntl(m_fd, F_GETFD));

    if (enabled)
        flags |= FD_CLOEXEC;
    else
        flags &= ~FD_CLOEXEC;

    TRY(System::fcntl(m_fd, F_SETFD, flags));
    return {};
}

ErrorOr<void> PosixSocketHelper::set_receive_timeout(Time timeout)
{
    auto timeout_spec = timeout.to_timespec();
    return System::setsockopt(m_fd, SOL_SOCKET, SO_RCVTIMEO, &timeout_spec, sizeof(timeout_spec));
}

void PosixSocketHelper::setup_notifier()
{
    if (!m_notifier)
        m_notifier = Core::Notifier::construct(m_fd, Core::Notifier::Read);
}

ErrorOr<NonnullOwnPtr<TCPSocket>> TCPSocket::connect(DeprecatedString const& host, u16 port)
{
    auto ip_address = TRY(resolve_host(host, SocketType::Stream));
    return connect(SocketAddress { ip_address, port });
}

ErrorOr<NonnullOwnPtr<TCPSocket>> TCPSocket::connect(SocketAddress const& address)
{
    auto socket = TRY(adopt_nonnull_own_or_enomem(new (nothrow) TCPSocket()));

    auto fd = TRY(create_fd(SocketDomain::Inet, SocketType::Stream));
    socket->m_helper.set_fd(fd);

    TRY(connect_inet(fd, address));

    socket->setup_notifier();
    return socket;
}

ErrorOr<NonnullOwnPtr<TCPSocket>> TCPSocket::adopt_fd(int fd)
{
    if (fd < 0) {
        return Error::from_errno(EBADF);
    }

    auto socket = TRY(adopt_nonnull_own_or_enomem(new (nothrow) TCPSocket()));
    socket->m_helper.set_fd(fd);
    socket->setup_notifier();
    return socket;
}

ErrorOr<size_t> PosixSocketHelper::pending_bytes() const
{
    if (!is_open()) {
        return Error::from_errno(ENOTCONN);
    }

    int value;
    TRY(System::ioctl(m_fd, FIONREAD, &value));
    return static_cast<size_t>(value);
}

ErrorOr<NonnullOwnPtr<UDPSocket>> UDPSocket::connect(DeprecatedString const& host, u16 port, Optional<Time> timeout)
{
    auto ip_address = TRY(resolve_host(host, SocketType::Datagram));
    return connect(SocketAddress { ip_address, port }, timeout);
}

ErrorOr<NonnullOwnPtr<UDPSocket>> UDPSocket::connect(SocketAddress const& address, Optional<Time> timeout)
{
    auto socket = TRY(adopt_nonnull_own_or_enomem(new (nothrow) UDPSocket()));

    auto fd = TRY(create_fd(SocketDomain::Inet, SocketType::Datagram));
    socket->m_helper.set_fd(fd);
    if (timeout.has_value()) {
        TRY(socket->m_helper.set_receive_timeout(timeout.value()));
    }

    TRY(connect_inet(fd, address));

    socket->setup_notifier();
    return socket;
}

ErrorOr<NonnullOwnPtr<LocalSocket>> LocalSocket::connect(DeprecatedString const& path, PreventSIGPIPE prevent_sigpipe)
{
    auto socket = TRY(adopt_nonnull_own_or_enomem(new (nothrow) LocalSocket(prevent_sigpipe)));

    auto fd = TRY(create_fd(SocketDomain::Local, SocketType::Stream));
    socket->m_helper.set_fd(fd);

    TRY(connect_local(fd, path));

    socket->setup_notifier();
    return socket;
}

ErrorOr<NonnullOwnPtr<LocalSocket>> LocalSocket::adopt_fd(int fd, PreventSIGPIPE prevent_sigpipe)
{
    if (fd < 0) {
        return Error::from_errno(EBADF);
    }

    auto socket = TRY(adopt_nonnull_own_or_enomem(new (nothrow) LocalSocket(prevent_sigpipe)));
    socket->m_helper.set_fd(fd);
    socket->setup_notifier();
    return socket;
}

ErrorOr<int> LocalSocket::receive_fd(int flags)
{
#if defined(AK_OS_SERENITY)
    return Core::System::recvfd(m_helper.fd(), flags);
#elif defined(AK_OS_LINUX) || defined(AK_OS_MACOS)
    union {
        struct cmsghdr cmsghdr;
        char control[CMSG_SPACE(sizeof(int))];
    } cmsgu {};
    char c = 0;
    struct iovec iov {
        .iov_base = &c,
        .iov_len = 1,
    };
    struct msghdr msg {
        .msg_name = NULL,
        .msg_namelen = 0,
        .msg_iov = &iov,
        .msg_iovlen = 1,
        .msg_control = cmsgu.control,
        .msg_controllen = sizeof(cmsgu.control),
        .msg_flags = 0,
    };
    TRY(Core::System::recvmsg(m_helper.fd(), &msg, 0));

    struct cmsghdr* cmsg = CMSG_FIRSTHDR(&msg);
    if (!cmsg || cmsg->cmsg_len != CMSG_LEN(sizeof(int)))
        return Error::from_string_literal("Malformed message when receiving file descriptor");

    VERIFY(cmsg->cmsg_level == SOL_SOCKET);
    VERIFY(cmsg->cmsg_type == SCM_RIGHTS);
    int fd = *((int*)CMSG_DATA(cmsg));

    if (flags & O_CLOEXEC) {
        auto fd_flags = TRY(Core::System::fcntl(fd, F_GETFD));
        TRY(Core::System::fcntl(fd, F_SETFD, fd_flags | FD_CLOEXEC));
    }

    return fd;
#else
    (void)flags;
    return Error::from_string_literal("File descriptor passing not supported on this platform");
#endif
}

ErrorOr<void> LocalSocket::send_fd(int fd)
{
#if defined(AK_OS_SERENITY)
    return Core::System::sendfd(m_helper.fd(), fd);
#elif defined(AK_OS_LINUX) || defined(AK_OS_MACOS)
    char c = 'F';
    struct iovec iov {
        .iov_base = &c,
        .iov_len = sizeof(c)
    };

    union {
        struct cmsghdr cmsghdr;
        char control[CMSG_SPACE(sizeof(int))];
    } cmsgu {};

    struct msghdr msg {
        .msg_name = NULL,
        .msg_namelen = 0,
        .msg_iov = &iov,
        .msg_iovlen = 1,
        .msg_control = cmsgu.control,
        .msg_controllen = sizeof(cmsgu.control),
        .msg_flags = 0,
    };

    struct cmsghdr* cmsg = CMSG_FIRSTHDR(&msg);
    cmsg->cmsg_len = CMSG_LEN(sizeof(int));
    cmsg->cmsg_level = SOL_SOCKET;
    cmsg->cmsg_type = SCM_RIGHTS;

    *((int*)CMSG_DATA(cmsg)) = fd;

    TRY(Core::System::sendmsg(m_helper.fd(), &msg, 0));
    return {};
#else
    (void)fd;
    return Error::from_string_literal("File descriptor passing not supported on this platform");
#endif
}

ErrorOr<pid_t> LocalSocket::peer_pid() const
{
#ifdef AK_OS_MACOS
    pid_t pid;
    socklen_t pid_size = sizeof(pid);
#elif defined(AK_OS_FREEBSD)
    struct xucred creds = {};
    socklen_t creds_size = sizeof(creds);
#elif defined(AK_OS_OPENBSD)
    struct sockpeercred creds = {};
    socklen_t creds_size = sizeof(creds);
#else
    struct ucred creds = {};
    socklen_t creds_size = sizeof(creds);
#endif

#ifdef AK_OS_MACOS
    TRY(System::getsockopt(m_helper.fd(), SOL_LOCAL, LOCAL_PEERPID, &pid, &pid_size));
    return pid;
#elif defined(AK_OS_FREEBSD)
    TRY(System::getsockopt(m_helper.fd(), SOL_LOCAL, LOCAL_PEERCRED, &creds, &creds_size));
    return creds.cr_pid;
#else
    TRY(System::getsockopt(m_helper.fd(), SOL_SOCKET, SO_PEERCRED, &creds, &creds_size));
    return creds.pid;
#endif
}

ErrorOr<Bytes> LocalSocket::read_without_waiting(Bytes buffer)
{
    return m_helper.read(buffer, MSG_DONTWAIT);
}

Optional<int> LocalSocket::fd() const
{
    if (!is_open())
        return {};
    return m_helper.fd();
}

ErrorOr<int> LocalSocket::release_fd()
{
    if (!is_open()) {
        return Error::from_errno(ENOTCONN);
    }

    auto fd = m_helper.fd();
    m_helper.set_fd(-1);
    return fd;
}

WrappedAKInputStream::WrappedAKInputStream(NonnullOwnPtr<InputStream> stream)
    : m_stream(move(stream))
{
}

ErrorOr<Bytes> WrappedAKInputStream::read(Bytes bytes)
{
    auto bytes_read = m_stream->read(bytes);

    if (m_stream->has_any_error())
        return Error::from_string_literal("Underlying InputStream indicated an error");

    return bytes.slice(0, bytes_read);
}

ErrorOr<void> WrappedAKInputStream::discard(size_t discarded_bytes)
{
    if (!m_stream->discard_or_error(discarded_bytes))
        return Error::from_string_literal("Underlying InputStream indicated an error");

    return {};
}

ErrorOr<size_t> WrappedAKInputStream::write(ReadonlyBytes)
{
    VERIFY_NOT_REACHED();
}

bool WrappedAKInputStream::is_eof() const
{
    return m_stream->unreliable_eof();
}

bool WrappedAKInputStream::is_open() const
{
    return true;
}

void WrappedAKInputStream::close()
{
}

WrappedAKOutputStream::WrappedAKOutputStream(NonnullOwnPtr<OutputStream> stream)
    : m_stream(move(stream))
{
}

ErrorOr<Bytes> WrappedAKOutputStream::read(Bytes)
{
    VERIFY_NOT_REACHED();
}

ErrorOr<size_t> WrappedAKOutputStream::write(ReadonlyBytes bytes)
{
    auto bytes_written = m_stream->write(bytes);

    if (m_stream->has_any_error())
        return Error::from_string_literal("Underlying OutputStream indicated an error");

    return bytes_written;
}

bool WrappedAKOutputStream::is_eof() const
{
    VERIFY_NOT_REACHED();
}

bool WrappedAKOutputStream::is_open() const
{
    return true;
}

void WrappedAKOutputStream::close()
{
}

WrapInAKInputStream::WrapInAKInputStream(Core::Stream::Stream& stream)
    : m_stream(stream)
{
}

size_t WrapInAKInputStream::read(Bytes bytes)
{
    if (has_any_error())
        return 0;

    auto data_or_error = m_stream.read(bytes);
    if (data_or_error.is_error()) {
        set_fatal_error();
        return 0;
    }

    return data_or_error.value().size();
}

bool WrapInAKInputStream::unreliable_eof() const
{
    return m_stream.is_eof();
}

bool WrapInAKInputStream::read_or_error(Bytes bytes)
{
    if (read(bytes) < bytes.size()) {
        set_fatal_error();
        return false;
    }

    return true;
}

bool WrapInAKInputStream::discard_or_error(size_t count)
{
    auto maybe_error = m_stream.discard(count);

    if (maybe_error.is_error()) {
        set_fatal_error();
        return false;
    }

    return true;
}

}

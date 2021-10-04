/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteBuffer.h>
#include <AK/ByteReader.h>
#include <AK/Debug.h>
#include <LibCore/Notifier.h>
#include <LibCore/Socket.h>
#include <arpa/inet.h>
#include <errno.h>
#include <fcntl.h>
#include <netdb.h>
#include <netinet/in.h>
#include <sys/socket.h>

namespace Core {

Socket::Socket(Type type, Object* parent)
    : IODevice(parent)
    , m_type(type)
{
    register_property(
        "source_address", [this] { return m_source_address.to_string(); },
        [](auto&) { return false; });

    register_property(
        "destination_address", [this] { return m_destination_address.to_string(); },
        [](auto&) { return false; });

    register_property(
        "source_port", [this] { return m_source_port; },
        [](auto&) { return false; });

    register_property(
        "destination_port", [this] { return m_destination_port; },
        [](auto&) { return false; });

    register_property(
        "connected", [this] { return m_connected; },
        [](auto&) { return false; });
}

Socket::~Socket()
{
    close();
}

bool Socket::connect(const String& hostname, int port)
{
    auto* hostent = gethostbyname(hostname.characters());
    if (!hostent) {
        dbgln("Socket::connect: Unable to resolve '{}'", hostname);
        return false;
    }

    // On macOS, the pointer in the hostent structure is misaligned. Load it using ByteReader to avoid UB
    auto* host_addr = AK::ByteReader::load_pointer<u8 const>(reinterpret_cast<u8 const*>(&hostent->h_addr_list[0]));
    IPv4Address host_address(host_addr);
    dbgln_if(CSOCKET_DEBUG, "Socket::connect: Resolved '{}' to {}", hostname, host_address);
    return connect(host_address, port);
}

void Socket::set_blocking(bool blocking)
{
    int flags = fcntl(fd(), F_GETFL, 0);
    VERIFY(flags >= 0);
    if (blocking)
        flags = fcntl(fd(), F_SETFL, flags & ~O_NONBLOCK);
    else
        flags = fcntl(fd(), F_SETFL, flags | O_NONBLOCK);
    VERIFY(flags == 0);
}

bool Socket::connect(const SocketAddress& address, int port)
{
    VERIFY(!is_connected());
    VERIFY(address.type() == SocketAddress::Type::IPv4);
    dbgln_if(CSOCKET_DEBUG, "{} connecting to {}...", *this, address);

    VERIFY(port > 0 && port <= 65535);

    struct sockaddr_in addr;
    memset(&addr, 0, sizeof(addr));
    auto ipv4_address = address.ipv4_address();
    memcpy(&addr.sin_addr.s_addr, &ipv4_address, sizeof(IPv4Address));
    addr.sin_family = AF_INET;
    addr.sin_port = htons(port);

    m_destination_address = address;
    m_destination_port = port;

    return common_connect((struct sockaddr*)&addr, sizeof(addr));
}

bool Socket::connect(const SocketAddress& address)
{
    VERIFY(!is_connected());
    VERIFY(address.type() == SocketAddress::Type::Local);
    dbgln_if(CSOCKET_DEBUG, "{} connecting to {}...", *this, address);

    sockaddr_un saddr;
    saddr.sun_family = AF_LOCAL;
    auto dest_address = address.to_string();
    bool fits = dest_address.copy_characters_to_buffer(saddr.sun_path, sizeof(saddr.sun_path));
    if (!fits) {
        warnln("Core::Socket: Failed to connect() to {}: Path is too long!", dest_address);
        errno = EINVAL;
        return false;
    }
    m_destination_address = address;

    return common_connect((const sockaddr*)&saddr, sizeof(saddr));
}

bool Socket::common_connect(const struct sockaddr* addr, socklen_t addrlen)
{
    auto connected = [this] {
        int so_error;
        socklen_t so_error_len = sizeof(so_error);

        int rc = getsockopt(fd(), SOL_SOCKET, SO_ERROR, &so_error, &so_error_len);
        if (rc < 0) {
            dbgln_if(CSOCKET_DEBUG, "Failed to check the status of SO_ERROR");
            m_connected = false;
            if (on_error)
                on_error();
        }

        if (so_error == 0) {
            dbgln_if(CSOCKET_DEBUG, "{} connected!", *this);
            m_connected = true;
            ensure_read_notifier();
            if (on_connected)
                on_connected();
        } else {
            dbgln_if(CSOCKET_DEBUG, "Failed to connect to {}", *this);
            m_connected = false;
            if (on_error)
                on_error();
        }

        if (m_notifier) {
            m_notifier->remove_from_parent();
            m_notifier = nullptr;
        }
    };
    int rc = ::connect(fd(), addr, addrlen);
    if (rc < 0) {
        if (errno == EINPROGRESS) {
            dbgln_if(CSOCKET_DEBUG, "{} connection in progress (EINPROGRESS)", *this);
            m_notifier = Notifier::construct(fd(), Notifier::Event::Write, this);
            m_notifier->on_ready_to_write = move(connected);
            return true;
        }
        int saved_errno = errno;
        warnln("Core::Socket: Failed to connect() to {}: {}", destination_address().to_string(), strerror(saved_errno));
        errno = saved_errno;
        return false;
    }
    dbgln_if(CSOCKET_DEBUG, "{} connected ok!", *this);
    connected();
    return true;
}

ByteBuffer Socket::receive(int max_size)
{
    auto buffer = read(max_size);
    if (eof())
        m_connected = false;
    return buffer;
}

bool Socket::send(ReadonlyBytes data)
{
    auto remaining_bytes = data.size();
    while (remaining_bytes > 0) {
        ssize_t nsent = ::send(fd(), data.data() + (data.size() - remaining_bytes), remaining_bytes, 0);
        if (nsent < 0) {
            set_error(errno);
            return false;
        }
        remaining_bytes -= nsent;
    }
    return true;
}

void Socket::did_update_fd(int fd)
{
    if (fd < 0) {
        if (m_read_notifier) {
            m_read_notifier->remove_from_parent();
            m_read_notifier = nullptr;
        }
        if (m_notifier) {
            m_notifier->remove_from_parent();
            m_notifier = nullptr;
        }
        return;
    }
    if (m_connected) {
        ensure_read_notifier();
    } else {
        // I don't think it would be right if we updated the fd while not connected *but* while having a notifier..
        VERIFY(!m_read_notifier);
    }
}

bool Socket::close()
{
    m_connected = false;
    if (m_notifier)
        m_notifier->close();
    if (m_read_notifier)
        m_read_notifier->close();
    return IODevice::close();
}

void Socket::set_idle(bool idle)
{
    if (m_read_notifier)
        m_read_notifier->set_enabled(!idle);
    if (m_notifier)
        m_notifier->set_enabled(!idle);
}

void Socket::ensure_read_notifier()
{
    VERIFY(m_connected);
    m_read_notifier = Notifier::construct(fd(), Notifier::Event::Read, this);
    m_read_notifier->on_ready_to_read = [this] {
        if (on_ready_to_read)
            on_ready_to_read();
    };
}

}

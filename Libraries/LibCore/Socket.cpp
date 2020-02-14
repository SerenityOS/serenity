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

#include <AK/ByteBuffer.h>
#include <LibCore/Notifier.h>
#include <LibCore/Socket.h>
#include <arpa/inet.h>
#include <errno.h>
#include <fcntl.h>
#include <netdb.h>
#include <netinet/in.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/socket.h>
#include <unistd.h>

//#define CSOCKET_DEBUG

namespace Core {

Socket::Socket(Type type, Object* parent)
    : IODevice(parent)
    , m_type(type)
{
}

Socket::~Socket()
{
    close();
}

bool Socket::connect(const String& hostname, int port)
{
    auto* hostent = gethostbyname(hostname.characters());
    if (!hostent) {
        dbg() << "Socket::connect: Unable to resolve '" << hostname << "'";
        return false;
    }

    IPv4Address host_address((const u8*)hostent->h_addr_list[0]);
#ifdef CSOCKET_DEBUG
    dbg() << "Socket::connect: Resolved '" << hostname << "' to " << host_address;
#endif
    return connect(host_address, port);
}

void Socket::set_blocking(bool blocking)
{
    int flags = fcntl(fd(), F_GETFL, 0);
    ASSERT(flags >= 0);
    if (blocking)
        flags = fcntl(fd(), F_SETFL, flags & ~O_NONBLOCK);
    else
        flags = fcntl(fd(), F_SETFL, flags | O_NONBLOCK);
    ASSERT(flags == 0);
}

bool Socket::connect(const SocketAddress& address, int port)
{
    ASSERT(!is_connected());
    ASSERT(address.type() == SocketAddress::Type::IPv4);
#ifdef CSOCKET_DEBUG
    dbg() << *this << " connecting to " << address << "...";
#endif

    ASSERT(port > 0 && port <= 65535);

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
    ASSERT(!is_connected());
    ASSERT(address.type() == SocketAddress::Type::Local);
#ifdef CSOCKET_DEBUG
    dbg() << *this << " connecting to " << address << "...";
#endif

    sockaddr_un saddr;
    saddr.sun_family = AF_LOCAL;
    strcpy(saddr.sun_path, address.to_string().characters());

    return common_connect((const sockaddr*)&saddr, sizeof(saddr));
}

bool Socket::common_connect(const struct sockaddr* addr, socklen_t addrlen)
{
    int rc = ::connect(fd(), addr, addrlen);
    if (rc < 0) {
        if (errno == EINPROGRESS) {
#ifdef CSOCKET_DEBUG
            dbg() << *this << " connection in progress (EINPROGRESS)";
#endif
            m_notifier = Notifier::construct(fd(), Notifier::Event::Write, this);
            m_notifier->on_ready_to_write = [this] {
#ifdef CSOCKET_DEBUG
                dbg() << *this << " connected!";
#endif
                m_connected = true;
                ensure_read_notifier();
                m_notifier->set_event_mask(Notifier::Event::None);
                if (on_connected)
                    on_connected();
            };
            return true;
        }
        perror("Socket::common_connect: connect");
        return false;
    }
#ifdef CSOCKET_DEBUG
    dbg() << *this << " connected ok!";
#endif
    m_connected = true;
    ensure_read_notifier();
    if (on_connected)
        on_connected();
    return true;
}

ByteBuffer Socket::receive(int max_size)
{
    auto buffer = read(max_size);
    if (eof()) {
        dbg() << *this << " connection appears to have closed in receive().";
        m_connected = false;
    }
    return buffer;
}

bool Socket::send(const ByteBuffer& data)
{
    int nsent = ::send(fd(), data.data(), data.size(), 0);
    if (nsent < 0) {
        set_error(errno);
        return false;
    }
    ASSERT(nsent == data.size());
    return true;
}

void Socket::did_update_fd(int fd)
{
    if (fd < 0) {
        m_read_notifier = nullptr;
        return;
    }
    if (m_connected) {
        ensure_read_notifier();
    } else {
        // I don't think it would be right if we updated the fd while not connected *but* while having a notifier..
        ASSERT(!m_read_notifier);
    }
}

void Socket::ensure_read_notifier()
{
    ASSERT(m_connected);
    m_read_notifier = Notifier::construct(fd(), Notifier::Event::Read, this);
    m_read_notifier->on_ready_to_read = [this] {
        if (on_ready_to_read)
            on_ready_to_read();
    };
}

}

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
#include <Kernel/Net/IPv4Socket.h>
#include <Kernel/Net/LocalSocket.h>
#include <Kernel/Net/Socket.h>
#include <Kernel/Process.h>
#include <Kernel/UnixTypes.h>
#include <LibC/errno_numbers.h>

//#define SOCKET_DEBUG

KResultOr<NonnullRefPtr<Socket>> Socket::create(int domain, int type, int protocol)
{
    switch (domain) {
    case AF_LOCAL:
        return LocalSocket::create(type & SOCK_TYPE_MASK);
    case AF_INET:
        return IPv4Socket::create(type & SOCK_TYPE_MASK, protocol);
    default:
        return KResult(-EAFNOSUPPORT);
    }
}

Socket::Socket(int domain, int type, int protocol)
    : m_domain(domain)
    , m_type(type)
    , m_protocol(protocol)
{
    auto& process = current->process();
    m_origin = { process.pid(), process.uid(), process.gid() };
}

Socket::~Socket()
{
}

void Socket::set_setup_state(SetupState new_setup_state)
{
#ifdef SOCKET_DEBUG
    kprintf("%s(%u) Socket{%p} setup state moving from %s to %s\n", current->process().name().characters(), current->pid(), this, to_string(m_setup_state), to_string(new_setup_state));
#endif

    m_setup_state = new_setup_state;
}

RefPtr<Socket> Socket::accept()
{
    LOCKER(m_lock);
    if (m_pending.is_empty())
        return nullptr;
#ifdef SOCKET_DEBUG
    kprintf("%s(%u) Socket{%p} de-queueing connection\n", current->process().name().characters(), current->pid(), this);
#endif
    auto client = m_pending.take_first();
    ASSERT(!client->is_connected());
    auto& process = current->process();
    client->m_acceptor = { process.pid(), process.uid(), process.gid() };
    client->m_connected = true;
    client->m_role = Role::Accepted;
    return client;
}

KResult Socket::queue_connection_from(NonnullRefPtr<Socket> peer)
{
#ifdef SOCKET_DEBUG
    kprintf("%s(%u) Socket{%p} queueing connection\n", current->process().name().characters(), current->pid(), this);
#endif
    LOCKER(m_lock);
    if (m_pending.size() >= m_backlog)
        return KResult(-ECONNREFUSED);
    m_pending.append(peer);
    return KSuccess;
}

KResult Socket::setsockopt(int level, int option, const void* value, socklen_t value_size)
{
    ASSERT(level == SOL_SOCKET);
    switch (option) {
    case SO_SNDTIMEO:
        if (value_size != sizeof(timeval))
            return KResult(-EINVAL);
        m_send_timeout = *(const timeval*)value;
        return KSuccess;
    case SO_RCVTIMEO:
        if (value_size != sizeof(timeval))
            return KResult(-EINVAL);
        m_receive_timeout = *(const timeval*)value;
        return KSuccess;
    default:
        kprintf("%s(%u): setsockopt() at SOL_SOCKET with unimplemented option %d\n", current->process().name().characters(), current->process().pid(), option);
        return KResult(-ENOPROTOOPT);
    }
}

KResult Socket::getsockopt(FileDescription&, int level, int option, void* value, socklen_t* value_size)
{
    ASSERT(level == SOL_SOCKET);
    switch (option) {
    case SO_SNDTIMEO:
        if (*value_size < sizeof(timeval))
            return KResult(-EINVAL);
        *(timeval*)value = m_send_timeout;
        *value_size = sizeof(timeval);
        return KSuccess;
    case SO_RCVTIMEO:
        if (*value_size < sizeof(timeval))
            return KResult(-EINVAL);
        *(timeval*)value = m_receive_timeout;
        *value_size = sizeof(timeval);
        return KSuccess;
    case SO_ERROR:
        if (*value_size < sizeof(int))
            return KResult(-EINVAL);
        kprintf("%s(%u): getsockopt() SO_ERROR: WARNING! I have no idea what the real error is, so I'll just stick my fingers in my ears and pretend there is none! %d\n", current->process().name().characters(), option);
        *(int*)value = 0;
        *value_size = sizeof(int);
        return KSuccess;
    default:
        kprintf("%s(%u): getsockopt() at SOL_SOCKET with unimplemented option %d\n", current->process().name().characters(), option);
        return KResult(-ENOPROTOOPT);
    }
}

void Socket::load_receive_deadline()
{
    kgettimeofday(m_receive_deadline);
    m_receive_deadline.tv_sec += m_receive_timeout.tv_sec;
    m_receive_deadline.tv_usec += m_receive_timeout.tv_usec;
    m_receive_deadline.tv_sec += (m_send_timeout.tv_usec / 1000000) * 1;
    m_receive_deadline.tv_usec %= 1000000;
}

void Socket::load_send_deadline()
{
    kgettimeofday(m_send_deadline);
    m_send_deadline.tv_sec += m_send_timeout.tv_sec;
    m_send_deadline.tv_usec += m_send_timeout.tv_usec;
    m_send_deadline.tv_sec += (m_send_timeout.tv_usec / 1000000) * 1;
    m_send_deadline.tv_usec %= 1000000;
}

ssize_t Socket::read(FileDescription& description, u8* buffer, ssize_t size)
{
    return recvfrom(description, buffer, size, 0, nullptr, 0);
}

ssize_t Socket::write(FileDescription& description, const u8* data, ssize_t size)
{
    return sendto(description, data, size, 0, nullptr, 0);
}

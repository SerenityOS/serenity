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

#pragma once

#include <AK/HashTable.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <Kernel/FileSystem/File.h>
#include <Kernel/KResult.h>
#include <Kernel/Lock.h>
#include <Kernel/UnixTypes.h>

namespace Kernel {

enum class ShouldBlock {
    No = 0,
    Yes = 1
};

class FileDescription;

class Socket : public File {
public:
    static KResultOr<NonnullRefPtr<Socket>> create(int domain, int type, int protocol);
    virtual ~Socket() override;

    int domain() const { return m_domain; }
    int type() const { return m_type; }
    int protocol() const { return m_protocol; }

    bool is_shut_down_for_writing() const { return m_shut_down_for_writing; }
    bool is_shut_down_for_reading() const { return m_shut_down_for_reading; }

    enum class SetupState {
        Unstarted,  // we haven't tried to set the socket up yet
        InProgress, // we're in the process of setting things up - for TCP maybe we've sent a SYN packet
        Completed,  // the setup process is complete, but not necessarily successful
    };

    enum class Role : u8 {
        None,
        Listener,
        Accepted,
        Connected,
        Connecting
    };

    static const char* to_string(SetupState setup_state)
    {
        switch (setup_state) {
        case SetupState::Unstarted:
            return "Unstarted";
        case SetupState::InProgress:
            return "InProgress";
        case SetupState::Completed:
            return "Completed";
        default:
            return "None";
        }
    }

    SetupState setup_state() const { return m_setup_state; }
    void set_setup_state(SetupState setup_state);

    virtual Role role(const FileDescription&) const { return m_role; }

    bool is_connected() const { return m_connected; }
    void set_connected(bool connected) { m_connected = connected; }

    bool can_accept() const { return !m_pending.is_empty(); }
    RefPtr<Socket> accept();

    KResult shutdown(int how);

    virtual KResult bind(const sockaddr*, socklen_t) = 0;
    virtual KResult connect(FileDescription&, const sockaddr*, socklen_t, ShouldBlock) = 0;
    virtual KResult listen(size_t) = 0;
    virtual void get_local_address(sockaddr*, socklen_t*) = 0;
    virtual void get_peer_address(sockaddr*, socklen_t*) = 0;
    virtual bool is_local() const { return false; }
    virtual bool is_ipv4() const { return false; }
    virtual void attach(FileDescription&) = 0;
    virtual void detach(FileDescription&) = 0;
    virtual ssize_t sendto(FileDescription&, const void*, size_t, int flags, const sockaddr*, socklen_t) = 0;
    virtual ssize_t recvfrom(FileDescription&, void*, size_t, int flags, sockaddr*, socklen_t*) = 0;

    virtual KResult setsockopt(int level, int option, const void*, socklen_t);
    virtual KResult getsockopt(FileDescription&, int level, int option, void*, socklen_t*);

    pid_t origin_pid() const { return m_origin.pid; }
    uid_t origin_uid() const { return m_origin.uid; }
    gid_t origin_gid() const { return m_origin.gid; }
    pid_t acceptor_pid() const { return m_acceptor.pid; }
    uid_t acceptor_uid() const { return m_acceptor.uid; }
    gid_t acceptor_gid() const { return m_acceptor.gid; }

    Lock& lock() { return m_lock; }

    // ^File
    virtual ssize_t read(FileDescription&, u8*, ssize_t) override final;
    virtual ssize_t write(FileDescription&, const u8*, ssize_t) override final;
    virtual String absolute_path(const FileDescription&) const override = 0;


    bool has_receive_timeout() const { return m_receive_timeout.tv_sec || m_receive_timeout.tv_usec; }
    const timeval& receive_timeout() const { return m_receive_timeout; }

    bool has_send_timeout() const { return m_send_timeout.tv_sec || m_send_timeout.tv_usec; }
    const timeval& send_timeout() const { return m_send_timeout; }

protected:
    Socket(int domain, int type, int protocol);

    KResult queue_connection_from(NonnullRefPtr<Socket>);

    size_t backlog() const { return m_backlog; }
    void set_backlog(size_t backlog) { m_backlog = backlog; }

    virtual const char* class_name() const override { return "Socket"; }

    virtual void shut_down_for_reading() {}
    virtual void shut_down_for_writing() {}

    Role m_role { Role::None };

protected:
    ucred m_origin { 0, 0, 0 };
    ucred m_acceptor { 0, 0, 0 };

private:
    virtual bool is_socket() const final { return true; }

    Lock m_lock { "Socket" };

    int m_domain { 0 };
    int m_type { 0 };
    int m_protocol { 0 };
    size_t m_backlog { 0 };
    SetupState m_setup_state { SetupState::Unstarted };
    bool m_connected { false };
    bool m_shut_down_for_reading { false };
    bool m_shut_down_for_writing { false };

    timeval m_receive_timeout { 0, 0 };
    timeval m_send_timeout { 0, 0 };

    NonnullRefPtrVector<Socket> m_pending;
};

template<typename SocketType>
class SocketHandle {
public:
    SocketHandle() {}

    SocketHandle(NonnullRefPtr<SocketType>&& socket)
        : m_socket(move(socket))
    {
        if (m_socket)
            m_socket->lock().lock();
    }

    SocketHandle(SocketHandle&& other)
        : m_socket(move(other.m_socket))
    {
    }

    ~SocketHandle()
    {
        if (m_socket)
            m_socket->lock().unlock();
    }

    SocketHandle(const SocketHandle&) = delete;
    SocketHandle& operator=(const SocketHandle&) = delete;

    operator bool() const { return m_socket; }

    SocketType* operator->() { return &socket(); }
    const SocketType* operator->() const { return &socket(); }

    SocketType& socket() { return *m_socket; }
    const SocketType& socket() const { return *m_socket; }

private:
    RefPtr<SocketType> m_socket;
};

}

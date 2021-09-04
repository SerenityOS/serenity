/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtrVector.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/Time.h>
#include <Kernel/FileSystem/File.h>
#include <Kernel/KResult.h>
#include <Kernel/Locking/Mutex.h>
#include <Kernel/Net/NetworkAdapter.h>
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

    static StringView to_string(SetupState setup_state)
    {
        switch (setup_state) {
        case SetupState::Unstarted:
            return "Unstarted"sv;
        case SetupState::InProgress:
            return "InProgress"sv;
        case SetupState::Completed:
            return "Completed"sv;
        default:
            return "None"sv;
        }
    }

    SetupState setup_state() const { return m_setup_state; }
    void set_setup_state(SetupState setup_state);

    virtual Role role(FileDescription const&) const { return m_role; }

    bool is_connected() const { return m_connected; }
    void set_connected(bool);

    bool can_accept() const { return !m_pending.is_empty(); }
    RefPtr<Socket> accept();

    KResult shutdown(int how);

    virtual KResult bind(Userspace<sockaddr const*>, socklen_t) = 0;
    virtual KResult connect(FileDescription&, Userspace<sockaddr const*>, socklen_t, ShouldBlock) = 0;
    virtual KResult listen(size_t) = 0;
    virtual void get_local_address(sockaddr*, socklen_t*) = 0;
    virtual void get_peer_address(sockaddr*, socklen_t*) = 0;
    virtual bool is_local() const { return false; }
    virtual bool is_ipv4() const { return false; }
    virtual KResultOr<size_t> sendto(FileDescription&, UserOrKernelBuffer const&, size_t, int flags, Userspace<sockaddr const*>, socklen_t) = 0;
    virtual KResultOr<size_t> recvfrom(FileDescription&, UserOrKernelBuffer&, size_t, int flags, Userspace<sockaddr*>, Userspace<socklen_t*>, Time&) = 0;

    virtual KResult setsockopt(int level, int option, Userspace<void const*>, socklen_t);
    virtual KResult getsockopt(FileDescription&, int level, int option, Userspace<void*>, Userspace<socklen_t*>);

    ProcessID origin_pid() const { return m_origin.pid; }
    UserID origin_uid() const { return m_origin.uid; }
    GroupID origin_gid() const { return m_origin.gid; }
    ProcessID acceptor_pid() const { return m_acceptor.pid; }
    UserID acceptor_uid() const { return m_acceptor.uid; }
    GroupID acceptor_gid() const { return m_acceptor.gid; }
    const RefPtr<NetworkAdapter> bound_interface() const { return m_bound_interface; }

    Mutex& mutex() { return m_mutex; }

    // ^File
    virtual KResultOr<size_t> read(FileDescription&, u64, UserOrKernelBuffer&, size_t) override final;
    virtual KResultOr<size_t> write(FileDescription&, u64, UserOrKernelBuffer const&, size_t) override final;
    virtual KResult stat(::stat&) const override;
    virtual String absolute_path(FileDescription const&) const override = 0;

    bool has_receive_timeout() const { return m_receive_timeout != Time::zero(); }
    Time const& receive_timeout() const { return m_receive_timeout; }

    bool has_send_timeout() const { return m_send_timeout != Time::zero(); }
    Time const& send_timeout() const { return m_send_timeout; }

    bool wants_timestamp() const { return m_timestamp; }

protected:
    Socket(int domain, int type, int protocol);

    KResult queue_connection_from(NonnullRefPtr<Socket>);

    size_t backlog() const { return m_backlog; }
    void set_backlog(size_t backlog) { m_backlog = backlog; }

    virtual StringView class_name() const override { return "Socket"; }

    virtual void shut_down_for_reading() { }
    virtual void shut_down_for_writing() { }

    Role m_role { Role::None };

    KResult so_error() const { return m_so_error; }
    KResult set_so_error(KResult error)
    {
        m_so_error = error;
        return error;
    }

    void set_origin(Process const&);
    void set_acceptor(Process const&);

    void set_role(Role role) { m_role = role; }

protected:
    ucred m_origin { 0, 0, 0 };
    ucred m_acceptor { 0, 0, 0 };

private:
    virtual bool is_socket() const final { return true; }

    Mutex m_mutex { "Socket"sv };

    int m_domain { 0 };
    int m_type { 0 };
    int m_protocol { 0 };
    size_t m_backlog { 0 };
    SetupState m_setup_state { SetupState::Unstarted };
    bool m_connected { false };
    bool m_shut_down_for_reading { false };
    bool m_shut_down_for_writing { false };

    RefPtr<NetworkAdapter> m_bound_interface { nullptr };

    Time m_receive_timeout {};
    Time m_send_timeout {};
    int m_timestamp { 0 };

    KResult m_so_error { KSuccess };

    NonnullRefPtrVector<Socket> m_pending;
};

template<typename SocketType>
class SocketHandle {
public:
    SocketHandle() = default;

    SocketHandle(NonnullRefPtr<SocketType>&& socket)
        : m_socket(move(socket))
    {
        if (m_socket)
            m_socket->mutex().lock();
    }

    SocketHandle(SocketHandle&& other)
        : m_socket(move(other.m_socket))
    {
    }

    ~SocketHandle()
    {
        if (m_socket)
            m_socket->mutex().unlock();
    }

    SocketHandle(SocketHandle const&) = delete;
    SocketHandle& operator=(SocketHandle const&) = delete;

    operator bool() const { return m_socket; }

    SocketType* operator->() { return &socket(); }
    SocketType const* operator->() const { return &socket(); }

    SocketType& socket() { return *m_socket; }
    SocketType const& socket() const { return *m_socket; }

private:
    RefPtr<SocketType> m_socket;
};

}

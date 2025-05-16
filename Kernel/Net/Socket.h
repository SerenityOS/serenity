/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/RefPtr.h>
#include <AK/Time.h>
#include <Kernel/FileSystem/File.h>
#include <Kernel/Locking/Mutex.h>
#include <Kernel/Net/NetworkAdapter.h>
#include <Kernel/UnixTypes.h>

namespace Kernel {

class OpenFileDescription;

class Socket : public File {
public:
    static ErrorOr<NonnullRefPtr<Socket>> create(int domain, int type, int protocol);
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

    virtual Role role(OpenFileDescription const&) const { return m_role; }

    bool is_connected() const { return m_connected; }
    void set_connected(bool);

    bool can_accept() const { return !m_pending.is_empty(); }
    RefPtr<Socket> accept();

    ErrorOr<void> shutdown(int how);

    virtual ErrorOr<void> bind(Credentials const&, Userspace<sockaddr const*>, socklen_t) = 0;
    virtual ErrorOr<void> connect(Credentials const&, OpenFileDescription&, Userspace<sockaddr const*>, socklen_t) = 0;
    virtual ErrorOr<void> listen(size_t) = 0;
    virtual void get_local_address(sockaddr*, socklen_t*) = 0;
    virtual void get_peer_address(sockaddr*, socklen_t*) = 0;
    virtual bool is_local() const { return false; }
    virtual bool is_ipv4() const { return false; }
    virtual ErrorOr<size_t> sendto(OpenFileDescription&, UserOrKernelBuffer const&, size_t, int flags, Userspace<sockaddr const*>, socklen_t) = 0;
    virtual ErrorOr<size_t> recvfrom(OpenFileDescription&, UserOrKernelBuffer&, size_t, int flags, Userspace<sockaddr*>, Userspace<socklen_t*>, UnixDateTime&, bool blocking) = 0;

    virtual ErrorOr<void> setsockopt(int level, int option, Userspace<void const*>, socklen_t);
    virtual ErrorOr<void> getsockopt(OpenFileDescription&, int level, int option, Userspace<void*>, Userspace<socklen_t*>);

    ProcessID origin_pid() const { return m_origin.pid; }
    UserID origin_uid() const { return m_origin.uid; }
    GroupID origin_gid() const { return m_origin.gid; }
    ProcessID acceptor_pid() const { return m_acceptor.pid; }
    UserID acceptor_uid() const { return m_acceptor.uid; }
    GroupID acceptor_gid() const { return m_acceptor.gid; }
    SpinlockProtected<RefPtr<NetworkAdapter>, LockRank::None> const& bound_interface() const { return m_bound_interface; }

    Mutex& mutex() { return m_mutex; }

    // ^File
    virtual ErrorOr<size_t> read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t) override final;
    virtual ErrorOr<size_t> write(OpenFileDescription&, u64, UserOrKernelBuffer const&, size_t) override final;
    virtual ErrorOr<struct stat> stat() const override;
    virtual ErrorOr<NonnullOwnPtr<KString>> pseudo_path(OpenFileDescription const&) const override = 0;

    bool has_receive_timeout() const { return m_receive_timeout != Duration::zero(); }
    Duration const& receive_timeout() const { return m_receive_timeout; }

    bool has_send_timeout() const { return m_send_timeout != Duration::zero(); }
    Duration const& send_timeout() const { return m_send_timeout; }

    bool wants_timestamp() const { return m_timestamp; }

protected:
    Socket(int domain, int type, int protocol);

    ErrorOr<void> queue_connection_from(NonnullRefPtr<Socket>);

    size_t backlog() const { return m_backlog; }
    void set_backlog(size_t backlog) { m_backlog = backlog; }

    virtual StringView class_name() const override { return "Socket"sv; }

    virtual void shut_down_for_reading() { }
    virtual void shut_down_for_writing() { }

    Role m_role { Role::None };

    SpinlockProtected<Optional<ErrnoCode>, LockRank::None>& so_error() { return m_so_error; }

    Error set_so_error(ErrnoCode error_code)
    {
        m_so_error.with([&error_code](auto& so_error) {
            so_error = error_code;
        });
        return Error::from_errno(error_code);
    }

    Error set_so_error(Error error)
    {
        m_so_error.with([&error](auto& so_error) {
            so_error = static_cast<ErrnoCode>(error.code());
        });
        return error;
    }

    void clear_so_error()
    {
        m_so_error.with([](auto& so_error) {
            so_error = {};
        });
    }

    void set_origin(Process const&);
    void set_acceptor(Process const&);

    void set_role(Role role) { m_role = role; }

    ucred m_origin { 0, 0, 0 };
    ucred m_acceptor { 0, 0, 0 };
    bool m_routing_disabled { false };
    bool m_broadcast_allowed { false };

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

    SpinlockProtected<RefPtr<NetworkAdapter>, LockRank::None> m_bound_interface;

    Duration m_receive_timeout {};
    Duration m_send_timeout {};
    int m_timestamp { 0 };

    SpinlockProtected<Optional<ErrnoCode>, LockRank::None> m_so_error;

    Vector<NonnullRefPtr<Socket>> m_pending;
};

// This is a special variant of TRY() that also updates the socket's SO_ERROR field on error.
#define SOCKET_TRY(expression)                                                            \
    ({                                                                                    \
        auto&& result = (expression);                                                     \
        if (result.is_error())                                                            \
            return set_so_error(result.release_error());                                  \
        static_assert(!::AK::Detail::IsLvalueReference<decltype(result.release_value())>, \
            "Do not return a reference from a fallible expression");                      \
        result.release_value();                                                           \
    })

}

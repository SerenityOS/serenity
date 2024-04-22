/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/IntrusiveList.h>
#include <AK/SetOnce.h>
#include <Kernel/Library/DoubleBuffer.h>
#include <Kernel/Net/Socket.h>

namespace Kernel {

class OpenFileDescription;

struct SocketPair {
    NonnullRefPtr<OpenFileDescription> description0;
    NonnullRefPtr<OpenFileDescription> description1;
};

class LocalSocket final : public Socket {

public:
    static ErrorOr<NonnullRefPtr<LocalSocket>> try_create(int type);
    static ErrorOr<SocketPair> try_create_connected_pair(int type);
    virtual ~LocalSocket() override;

    ErrorOr<void> sendfd(OpenFileDescription const& socket_description, NonnullRefPtr<OpenFileDescription> passing_description);
    ErrorOr<NonnullRefPtr<OpenFileDescription>> recvfd(OpenFileDescription const& socket_description);
    ErrorOr<Vector<NonnullRefPtr<OpenFileDescription>>> recvfds(OpenFileDescription const& socket_description, int n);

    static void for_each(Function<void(LocalSocket const&)>);
    static ErrorOr<void> try_for_each(Function<ErrorOr<void>(LocalSocket const&)>);

    StringView socket_path() const;
    ErrorOr<NonnullOwnPtr<KString>> pseudo_path(OpenFileDescription const& description) const override;

    // ^Socket
    virtual ErrorOr<void> bind(Credentials const&, Userspace<sockaddr const*>, socklen_t) override;
    virtual ErrorOr<void> connect(Credentials const&, OpenFileDescription&, Userspace<sockaddr const*>, socklen_t) override;
    virtual ErrorOr<void> listen(size_t) override;
    virtual void get_local_address(sockaddr*, socklen_t*) override;
    virtual void get_peer_address(sockaddr*, socklen_t*) override;
    virtual ErrorOr<void> attach(OpenFileDescription&) override;
    virtual void detach(OpenFileDescription&) override;
    virtual bool can_read(OpenFileDescription const&, u64) const override;
    virtual bool can_write(OpenFileDescription const&, u64) const override;
    virtual ErrorOr<size_t> sendto(OpenFileDescription&, UserOrKernelBuffer const&, size_t, int, Userspace<sockaddr const*>, socklen_t) override;
    virtual ErrorOr<size_t> recvfrom(OpenFileDescription&, UserOrKernelBuffer&, size_t, int flags, Userspace<sockaddr*>, Userspace<socklen_t*>, UnixDateTime&, bool blocking) override;
    virtual ErrorOr<void> getsockopt(OpenFileDescription&, int level, int option, Userspace<void*>, Userspace<socklen_t*>) override;
    virtual ErrorOr<void> ioctl(OpenFileDescription&, unsigned request, Userspace<void*> arg) override;
    virtual ErrorOr<void> chown(Credentials const&, OpenFileDescription&, UserID, GroupID) override;
    virtual ErrorOr<void> chmod(Credentials const&, OpenFileDescription&, mode_t) override;

private:
    explicit LocalSocket(int type, NonnullOwnPtr<DoubleBuffer> client_buffer, NonnullOwnPtr<DoubleBuffer> server_buffer);
    virtual StringView class_name() const override { return "LocalSocket"sv; }
    virtual bool is_local() const override { return true; }
    bool has_attached_peer(OpenFileDescription const&) const;
    DoubleBuffer* receive_buffer_for(OpenFileDescription&);
    DoubleBuffer* send_buffer_for(OpenFileDescription&);
    Vector<NonnullRefPtr<OpenFileDescription>>& sendfd_queue_for(OpenFileDescription const&);
    Vector<NonnullRefPtr<OpenFileDescription>>& recvfd_queue_for(OpenFileDescription const&);

    void set_connect_side_role(Role connect_side_role, bool force_evaluate_block_conditions = false)
    {
        auto previous = m_connect_side_role;
        m_connect_side_role = connect_side_role;
        if (previous != m_connect_side_role || force_evaluate_block_conditions)
            evaluate_block_conditions();
    }

    ErrorOr<void> try_set_path(StringView);

    // The inode this socket is bound to.
    RefPtr<Inode> m_inode;

    UserID m_prebind_uid { 0 };
    GroupID m_prebind_gid { 0 };
    mode_t m_prebind_mode { 0 };

    // A single LocalSocket is shared between two file descriptions
    // on the connect side and the accept side; so we need to store
    // an additional role for the connect side and differentiate
    // between them.
    Role m_connect_side_role { Role::None };
    OpenFileDescription* m_connect_side_fd { nullptr };

    virtual Role role(OpenFileDescription const& description) const override
    {
        if (m_connect_side_fd == &description)
            return m_connect_side_role;
        return m_role;
    }

    SetOnce m_bound;
    bool m_accept_side_fd_open { false };
    OwnPtr<KString> m_path;

    NonnullOwnPtr<DoubleBuffer> m_for_client;
    NonnullOwnPtr<DoubleBuffer> m_for_server;

    Vector<NonnullRefPtr<OpenFileDescription>> m_fds_for_client;
    Vector<NonnullRefPtr<OpenFileDescription>> m_fds_for_server;

    IntrusiveListNode<LocalSocket> m_list_node;

public:
    using List = IntrusiveList<&LocalSocket::m_list_node>;
};

}

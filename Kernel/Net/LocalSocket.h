/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/IntrusiveList.h>
#include <Kernel/DoubleBuffer.h>
#include <Kernel/Net/Socket.h>

namespace Kernel {

class OpenFileDescription;

struct SocketPair {
    NonnullRefPtr<OpenFileDescription> description0;
    NonnullRefPtr<OpenFileDescription> description1;
};

class LocalSocket final : public Socket {

public:
    static KResultOr<NonnullRefPtr<LocalSocket>> try_create(int type);
    static KResultOr<SocketPair> try_create_connected_pair(int type);
    virtual ~LocalSocket() override;

    KResult sendfd(const OpenFileDescription& socket_description, OpenFileDescription& passing_description);
    KResultOr<NonnullRefPtr<OpenFileDescription>> recvfd(const OpenFileDescription& socket_description);

    static void for_each(Function<void(const LocalSocket&)>);

    StringView socket_path() const;
    String absolute_path(const OpenFileDescription& description) const override;

    // ^Socket
    virtual KResult bind(Userspace<const sockaddr*>, socklen_t) override;
    virtual KResult connect(OpenFileDescription&, Userspace<const sockaddr*>, socklen_t, ShouldBlock = ShouldBlock::Yes) override;
    virtual KResult listen(size_t) override;
    virtual void get_local_address(sockaddr*, socklen_t*) override;
    virtual void get_peer_address(sockaddr*, socklen_t*) override;
    virtual KResult attach(OpenFileDescription&) override;
    virtual void detach(OpenFileDescription&) override;
    virtual bool can_read(const OpenFileDescription&, size_t) const override;
    virtual bool can_write(const OpenFileDescription&, size_t) const override;
    virtual KResultOr<size_t> sendto(OpenFileDescription&, const UserOrKernelBuffer&, size_t, int, Userspace<const sockaddr*>, socklen_t) override;
    virtual KResultOr<size_t> recvfrom(OpenFileDescription&, UserOrKernelBuffer&, size_t, int flags, Userspace<sockaddr*>, Userspace<socklen_t*>, Time&) override;
    virtual KResult getsockopt(OpenFileDescription&, int level, int option, Userspace<void*>, Userspace<socklen_t*>) override;
    virtual KResult ioctl(OpenFileDescription&, unsigned request, Userspace<void*> arg) override;
    virtual KResult chown(OpenFileDescription&, UserID, GroupID) override;
    virtual KResult chmod(OpenFileDescription&, mode_t) override;

private:
    explicit LocalSocket(int type, NonnullOwnPtr<DoubleBuffer> client_buffer, NonnullOwnPtr<DoubleBuffer> server_buffer);
    virtual StringView class_name() const override { return "LocalSocket"; }
    virtual bool is_local() const override { return true; }
    bool has_attached_peer(const OpenFileDescription&) const;
    DoubleBuffer* receive_buffer_for(OpenFileDescription&);
    DoubleBuffer* send_buffer_for(OpenFileDescription&);
    NonnullRefPtrVector<OpenFileDescription>& sendfd_queue_for(const OpenFileDescription&);
    NonnullRefPtrVector<OpenFileDescription>& recvfd_queue_for(const OpenFileDescription&);

    void set_connect_side_role(Role connect_side_role, bool force_evaluate_block_conditions = false)
    {
        auto previous = m_connect_side_role;
        m_connect_side_role = connect_side_role;
        if (previous != m_connect_side_role || force_evaluate_block_conditions)
            evaluate_block_conditions();
    }

    KResult try_set_path(StringView);

    // An open socket file on the filesystem.
    RefPtr<OpenFileDescription> m_file;

    UserID m_prebind_uid { 0 };
    GroupID m_prebind_gid { 0 };
    mode_t m_prebind_mode { 0 };

    // A single LocalSocket is shared between two file descriptions
    // on the connect side and the accept side; so we need to store
    // an additional role for the connect side and differentiate
    // between them.
    Role m_connect_side_role { Role::None };
    OpenFileDescription* m_connect_side_fd { nullptr };

    virtual Role role(const OpenFileDescription& description) const override
    {
        if (m_connect_side_fd == &description)
            return m_connect_side_role;
        return m_role;
    }

    bool m_bound { false };
    bool m_accept_side_fd_open { false };
    OwnPtr<KString> m_path;

    NonnullOwnPtr<DoubleBuffer> m_for_client;
    NonnullOwnPtr<DoubleBuffer> m_for_server;

    NonnullRefPtrVector<OpenFileDescription> m_fds_for_client;
    NonnullRefPtrVector<OpenFileDescription> m_fds_for_server;

    IntrusiveListNode<LocalSocket> m_list_node;

public:
    using List = IntrusiveList<LocalSocket, RawPtr<LocalSocket>, &LocalSocket::m_list_node>;
};

}

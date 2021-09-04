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

class FileDescription;

struct SocketPair {
    NonnullRefPtr<FileDescription> description0;
    NonnullRefPtr<FileDescription> description1;
};

class LocalSocket final : public Socket {

public:
    static KResultOr<NonnullRefPtr<LocalSocket>> try_create(int type);
    static KResultOr<SocketPair> try_create_connected_pair(int type);
    virtual ~LocalSocket() override;

    KResult sendfd(FileDescription const& socket_description, FileDescription& passing_description);
    KResultOr<NonnullRefPtr<FileDescription>> recvfd(FileDescription const& socket_description);

    static void for_each(Function<void(LocalSocket const&)>);

    StringView socket_path() const;
    String absolute_path(FileDescription const& description) const override;

    // ^Socket
    virtual KResult bind(Userspace<sockaddr const*>, socklen_t) override;
    virtual KResult connect(FileDescription&, Userspace<sockaddr const*>, socklen_t, ShouldBlock = ShouldBlock::Yes) override;
    virtual KResult listen(size_t) override;
    virtual void get_local_address(sockaddr*, socklen_t*) override;
    virtual void get_peer_address(sockaddr*, socklen_t*) override;
    virtual KResult attach(FileDescription&) override;
    virtual void detach(FileDescription&) override;
    virtual bool can_read(FileDescription const&, size_t) const override;
    virtual bool can_write(FileDescription const&, size_t) const override;
    virtual KResultOr<size_t> sendto(FileDescription&, UserOrKernelBuffer const&, size_t, int, Userspace<sockaddr const*>, socklen_t) override;
    virtual KResultOr<size_t> recvfrom(FileDescription&, UserOrKernelBuffer&, size_t, int flags, Userspace<sockaddr*>, Userspace<socklen_t*>, Time&) override;
    virtual KResult getsockopt(FileDescription&, int level, int option, Userspace<void*>, Userspace<socklen_t*>) override;
    virtual KResult ioctl(FileDescription&, unsigned request, Userspace<void*> arg) override;
    virtual KResult chown(FileDescription&, UserID, GroupID) override;
    virtual KResult chmod(FileDescription&, mode_t) override;

private:
    explicit LocalSocket(int type, NonnullOwnPtr<DoubleBuffer> client_buffer, NonnullOwnPtr<DoubleBuffer> server_buffer);
    virtual StringView class_name() const override { return "LocalSocket"; }
    virtual bool is_local() const override { return true; }
    bool has_attached_peer(FileDescription const&) const;
    DoubleBuffer* receive_buffer_for(FileDescription&);
    DoubleBuffer* send_buffer_for(FileDescription&);
    NonnullRefPtrVector<FileDescription>& sendfd_queue_for(FileDescription const&);
    NonnullRefPtrVector<FileDescription>& recvfd_queue_for(FileDescription const&);

    void set_connect_side_role(Role connect_side_role, bool force_evaluate_block_conditions = false)
    {
        auto previous = m_connect_side_role;
        m_connect_side_role = connect_side_role;
        if (previous != m_connect_side_role || force_evaluate_block_conditions)
            evaluate_block_conditions();
    }

    KResult try_set_path(StringView);

    // An open socket file on the filesystem.
    RefPtr<FileDescription> m_file;

    UserID m_prebind_uid { 0 };
    GroupID m_prebind_gid { 0 };
    mode_t m_prebind_mode { 0 };

    // A single LocalSocket is shared between two file descriptions
    // on the connect side and the accept side; so we need to store
    // an additional role for the connect side and differentiate
    // between them.
    Role m_connect_side_role { Role::None };
    FileDescription* m_connect_side_fd { nullptr };

    virtual Role role(FileDescription const& description) const override
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

    NonnullRefPtrVector<FileDescription> m_fds_for_client;
    NonnullRefPtrVector<FileDescription> m_fds_for_server;

    IntrusiveListNode<LocalSocket> m_list_node;

public:
    using List = IntrusiveList<LocalSocket, RawPtr<LocalSocket>, &LocalSocket::m_list_node>;
};

}

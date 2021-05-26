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
    NonnullRefPtr<FileDescription> description1;
    NonnullRefPtr<FileDescription> description2;
};

class LocalSocket final : public Socket {

public:
    static KResultOr<NonnullRefPtr<Socket>> create(int type);
    static KResultOr<SocketPair> create_connected_pair(int type);
    virtual ~LocalSocket() override;

    KResult sendfd(const FileDescription& socket_description, FileDescription& passing_description);
    KResultOr<NonnullRefPtr<FileDescription>> recvfd(const FileDescription& socket_description);

    static void for_each(Function<void(const LocalSocket&)>);

    StringView socket_path() const;
    String absolute_path(const FileDescription& description) const override;

    // ^Socket
    virtual KResult bind(Userspace<const sockaddr*>, socklen_t) override;
    virtual KResult connect(FileDescription&, Userspace<const sockaddr*>, socklen_t, ShouldBlock = ShouldBlock::Yes) override;
    virtual KResult listen(size_t) override;
    virtual void get_local_address(sockaddr*, socklen_t*) override;
    virtual void get_peer_address(sockaddr*, socklen_t*) override;
    virtual KResult attach(FileDescription&) override;
    virtual void detach(FileDescription&) override;
    virtual bool can_read(const FileDescription&, size_t) const override;
    virtual bool can_write(const FileDescription&, size_t) const override;
    virtual KResultOr<size_t> sendto(FileDescription&, const UserOrKernelBuffer&, size_t, int, Userspace<const sockaddr*>, socklen_t) override;
    virtual KResultOr<size_t> recvfrom(FileDescription&, UserOrKernelBuffer&, size_t, int flags, Userspace<sockaddr*>, Userspace<socklen_t*>, Time&) override;
    virtual KResult getsockopt(FileDescription&, int level, int option, Userspace<void*>, Userspace<socklen_t*>) override;
    virtual KResult chown(FileDescription&, uid_t, gid_t) override;
    virtual KResult chmod(FileDescription&, mode_t) override;

private:
    explicit LocalSocket(int type);
    virtual const char* class_name() const override { return "LocalSocket"; }
    virtual bool is_local() const override { return true; }
    bool has_attached_peer(const FileDescription&) const;
    DoubleBuffer* receive_buffer_for(FileDescription&);
    DoubleBuffer* send_buffer_for(FileDescription&);
    NonnullRefPtrVector<FileDescription>& sendfd_queue_for(const FileDescription&);
    NonnullRefPtrVector<FileDescription>& recvfd_queue_for(const FileDescription&);

    void set_connect_side_role(Role connect_side_role, bool force_evaluate_block_conditions = false)
    {
        auto previous = m_connect_side_role;
        m_connect_side_role = connect_side_role;
        if (previous != m_connect_side_role || force_evaluate_block_conditions)
            evaluate_block_conditions();
    }

    // An open socket file on the filesystem.
    RefPtr<FileDescription> m_file;

    uid_t m_prebind_uid { 0 };
    gid_t m_prebind_gid { 0 };
    mode_t m_prebind_mode { 0 };

    // A single LocalSocket is shared between two file descriptions
    // on the connect side and the accept side; so we need to store
    // an additional role for the connect side and differentiate
    // between them.
    Role m_connect_side_role { Role::None };
    FileDescription* m_connect_side_fd { nullptr };

    virtual Role role(const FileDescription& description) const override
    {
        if (m_connect_side_fd == &description)
            return m_connect_side_role;
        return m_role;
    }

    bool m_bound { false };
    bool m_accept_side_fd_open { false };
    sockaddr_un m_address { 0, { 0 } };

    DoubleBuffer m_for_client;
    DoubleBuffer m_for_server;

    NonnullRefPtrVector<FileDescription> m_fds_for_client;
    NonnullRefPtrVector<FileDescription> m_fds_for_server;

    IntrusiveListNode<LocalSocket> m_list_node;

public:
    using List = IntrusiveList<LocalSocket, RawPtr<LocalSocket>, &LocalSocket::m_list_node>;
};

}

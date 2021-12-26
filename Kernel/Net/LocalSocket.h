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

#include <AK/InlineLinkedList.h>
#include <Kernel/DoubleBuffer.h>
#include <Kernel/Net/Socket.h>

namespace Kernel {

class FileDescription;

class LocalSocket final : public Socket
    , public InlineLinkedListNode<LocalSocket> {
    friend class InlineLinkedListNode<LocalSocket>;

public:
    static KResultOr<NonnullRefPtr<Socket>> create(int type);
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
    virtual void attach(FileDescription&) override;
    virtual void detach(FileDescription&) override;
    virtual bool can_read(const FileDescription&, size_t) const override;
    virtual bool can_write(const FileDescription&, size_t) const override;
    virtual KResultOr<size_t> sendto(FileDescription&, const UserOrKernelBuffer&, size_t, int, Userspace<const sockaddr*>, socklen_t) override;
    virtual KResultOr<size_t> recvfrom(FileDescription&, UserOrKernelBuffer&, size_t, int flags, Userspace<sockaddr*>, Userspace<socklen_t*>, timeval&) override;
    virtual KResult getsockopt(FileDescription&, int level, int option, Userspace<void*>, Userspace<socklen_t*>) override;
    virtual KResult chown(FileDescription&, uid_t, gid_t) override;
    virtual KResult chmod(FileDescription&, mode_t) override;

private:
    explicit LocalSocket(int type);
    virtual const char* class_name() const override { return "LocalSocket"; }
    virtual bool is_local() const override { return true; }
    bool has_attached_peer(const FileDescription&) const;
    static Lockable<InlineLinkedList<LocalSocket>>& all_sockets();
    DoubleBuffer& receive_buffer_for(FileDescription&);
    DoubleBuffer& send_buffer_for(FileDescription&);
    NonnullRefPtrVector<FileDescription>& sendfd_queue_for(const FileDescription&);
    NonnullRefPtrVector<FileDescription>& recvfd_queue_for(const FileDescription&);

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

    // for InlineLinkedList
    LocalSocket* m_prev { nullptr };
    LocalSocket* m_next { nullptr };
};

}

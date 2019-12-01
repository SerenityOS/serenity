#pragma once

#include <AK/InlineLinkedList.h>
#include <Kernel/DoubleBuffer.h>
#include <Kernel/Net/Socket.h>

class FileDescription;

class LocalSocket final : public Socket, public InlineLinkedListNode<LocalSocket> {
    friend class InlineLinkedListNode<LocalSocket>;
public:
    static NonnullRefPtr<LocalSocket> create(int type);
    virtual ~LocalSocket() override;

    static void for_each(Function<void(LocalSocket&)>);

    StringView socket_path() const;
    String absolute_path(const FileDescription& description) const override;

    // ^Socket
    virtual KResult bind(const sockaddr*, socklen_t) override;
    virtual KResult connect(FileDescription&, const sockaddr*, socklen_t, ShouldBlock = ShouldBlock::Yes) override;
    virtual KResult listen(int) override;
    virtual bool get_local_address(sockaddr*, socklen_t*) override;
    virtual bool get_peer_address(sockaddr*, socklen_t*) override;
    virtual void attach(FileDescription&) override;
    virtual void detach(FileDescription&) override;
    virtual bool can_read(const FileDescription&) const override;
    virtual bool can_write(const FileDescription&) const override;
    virtual ssize_t sendto(FileDescription&, const void*, size_t, int, const sockaddr*, socklen_t) override;
    virtual ssize_t recvfrom(FileDescription&, void*, size_t, int flags, sockaddr*, socklen_t*) override;

private:
    explicit LocalSocket(int type);
    virtual const char* class_name() const override { return "LocalSocket"; }
    virtual bool is_local() const override { return true; }
    bool has_attached_peer(const FileDescription&) const;
    static Lockable<InlineLinkedList<LocalSocket>>& all_sockets();
    DoubleBuffer& receive_buffer_for(FileDescription&);
    DoubleBuffer& send_buffer_for(FileDescription&);

    // An open socket file on the filesystem.
    RefPtr<FileDescription> m_file;

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
    sockaddr_un m_address;

    DoubleBuffer m_for_client;
    DoubleBuffer m_for_server;

    // for InlineLinkedList
    LocalSocket* m_prev { nullptr };
    LocalSocket* m_next { nullptr };
};

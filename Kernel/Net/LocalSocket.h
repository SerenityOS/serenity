#pragma once

#include <Kernel/DoubleBuffer.h>
#include <Kernel/Net/Socket.h>

class FileDescription;

class LocalSocket final : public Socket {
public:
    static NonnullRefPtr<LocalSocket> create(int type);
    virtual ~LocalSocket() override;

    virtual KResult bind(const sockaddr*, socklen_t) override;
    virtual KResult connect(FileDescription&, const sockaddr*, socklen_t, ShouldBlock = ShouldBlock::Yes) override;
    virtual bool get_local_address(sockaddr*, socklen_t*) override;
    virtual bool get_peer_address(sockaddr*, socklen_t*) override;
    virtual void attach(FileDescription&) override;
    virtual void detach(FileDescription&) override;
    virtual bool can_read(FileDescription&) const override;
    virtual ssize_t read(FileDescription&, u8*, ssize_t) override;
    virtual ssize_t write(FileDescription&, const u8*, ssize_t) override;
    virtual bool can_write(FileDescription&) const override;
    virtual ssize_t sendto(FileDescription&, const void*, size_t, int, const sockaddr*, socklen_t) override;
    virtual ssize_t recvfrom(FileDescription&, void*, size_t, int flags, sockaddr*, socklen_t*) override;

private:
    explicit LocalSocket(int type);
    virtual bool is_local() const override { return true; }
    bool has_attached_peer(const FileDescription&) const;

    RefPtr<FileDescription> m_file;

    bool m_bound { false };
    int m_accepted_fds_open { 0 };
    int m_connected_fds_open { 0 };
    int m_connecting_fds_open { 0 };
    sockaddr_un m_address;

    DoubleBuffer m_for_client;
    DoubleBuffer m_for_server;
};

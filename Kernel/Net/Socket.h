#pragma once

#include <AK/HashTable.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <Kernel/FileSystem/File.h>
#include <Kernel/KResult.h>
#include <Kernel/Lock.h>
#include <Kernel/UnixTypes.h>

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

    virtual KResult bind(const sockaddr*, socklen_t) = 0;
    virtual KResult connect(FileDescription&, const sockaddr*, socklen_t, ShouldBlock) = 0;
    virtual KResult listen(int) = 0;
    virtual bool get_local_address(sockaddr*, socklen_t*) = 0;
    virtual bool get_peer_address(sockaddr*, socklen_t*) = 0;
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

    timeval receive_deadline() const { return m_receive_deadline; }
    timeval send_deadline() const { return m_send_deadline; }

    Lock& lock() { return m_lock; }

    // ^File
    virtual ssize_t read(FileDescription&, u8*, ssize_t) override final;
    virtual ssize_t write(FileDescription&, const u8*, ssize_t) override final;
    virtual String absolute_path(const FileDescription&) const override = 0;

protected:
    Socket(int domain, int type, int protocol);

    KResult queue_connection_from(NonnullRefPtr<Socket>);

    void load_receive_deadline();
    void load_send_deadline();

    int backlog() const { return m_backlog; }
    void set_backlog(int backlog) { m_backlog = backlog; }

    virtual const char* class_name() const override { return "Socket"; }

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
    int m_backlog { 0 };
    SetupState m_setup_state { SetupState::Unstarted };
    bool m_connected { false };

    timeval m_receive_timeout { 0, 0 };
    timeval m_send_timeout { 0, 0 };

    timeval m_receive_deadline { 0, 0 };
    timeval m_send_deadline { 0, 0 };

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

#include <LibCore/CNotifier.h>
#include <LibCore/CSocket.h>
#include <arpa/inet.h>
#include <errno.h>
#include <fcntl.h>
#include <netdb.h>
#include <netinet/in.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/socket.h>
#include <unistd.h>

CSocket::CSocket(Type type, CObject* parent)
    : CIODevice(parent)
    , m_type(type)
{
}

CSocket::~CSocket()
{
    close();
}

bool CSocket::connect(const String& hostname, int port)
{
    auto* hostent = gethostbyname(hostname.characters());
    if (!hostent) {
        dbg() << "CSocket::connect: Unable to resolve '" << hostname << "'";
        return false;
    }

    IPv4Address host_address((const u8*)hostent->h_addr_list[0]);
    dbg() << "CSocket::connect: Resolved '" << hostname << "' to " << host_address;
    return connect(host_address, port);
}

void CSocket::set_blocking(bool blocking)
{
    int flags = fcntl(fd(), F_GETFL, 0);
    ASSERT(flags >= 0);
    if (blocking)
        flags = fcntl(fd(), F_SETFL, flags | O_NONBLOCK);
    else
        flags = fcntl(fd(), F_SETFL, flags & O_NONBLOCK);
    ASSERT(flags >= 0);
}

bool CSocket::connect(const CSocketAddress& address, int port)
{
    ASSERT(!is_connected());
    ASSERT(address.type() == CSocketAddress::Type::IPv4);
    dbg() << *this << " connecting to " << address << "...";

    ASSERT(port > 0 && port <= 65535);

    struct sockaddr_in addr;
    memset(&addr, 0, sizeof(addr));
    auto ipv4_address = address.ipv4_address();
    memcpy(&addr.sin_addr.s_addr, &ipv4_address, sizeof(IPv4Address));
    addr.sin_family = AF_INET;
    addr.sin_port = htons(port);

    m_destination_address = address;
    m_destination_port = port;

    return common_connect((struct sockaddr*)&addr, sizeof(addr));
}

bool CSocket::connect(const CSocketAddress& address)
{
    ASSERT(!is_connected());
    ASSERT(address.type() == CSocketAddress::Type::Local);
    dbg() << *this << " connecting to " << address << "...";

    sockaddr_un saddr;
    saddr.sun_family = AF_LOCAL;
    strcpy(saddr.sun_path, address.to_string().characters());

    return common_connect((const sockaddr*)&saddr, sizeof(saddr));
}

bool CSocket::common_connect(const struct sockaddr* addr, socklen_t addrlen)
{
    int rc = ::connect(fd(), addr, addrlen);
    if (rc < 0) {
        if (errno == EINPROGRESS) {
            dbg() << *this << " connection in progress (EINPROGRESS)";
            m_notifier = CNotifier::construct(fd(), CNotifier::Event::Write, this);
            m_notifier->on_ready_to_write = [this] {
                dbg() << *this << " connected!";
                m_connected = true;
                ensure_read_notifier();
                m_notifier->set_event_mask(CNotifier::Event::None);
                if (on_connected)
                    on_connected();
            };
            return true;
        }
        perror("CSocket::common_connect: connect");
        return false;
    }
    dbg() << *this << " connected ok!";
    m_connected = true;
    ensure_read_notifier();
    if (on_connected)
        on_connected();
    return true;
}

ByteBuffer CSocket::receive(int max_size)
{
    auto buffer = read(max_size);
    if (eof()) {
        dbg() << *this << " connection appears to have closed in receive().";
        m_connected = false;
    }
    return buffer;
}

bool CSocket::send(const ByteBuffer& data)
{
    int nsent = ::send(fd(), data.pointer(), data.size(), 0);
    if (nsent < 0) {
        set_error(errno);
        return false;
    }
    ASSERT(nsent == data.size());
    return true;
}

void CSocket::did_update_fd(int fd)
{
    if (fd < 0) {
        m_read_notifier = nullptr;
        return;
    }
    if (m_connected) {
        ensure_read_notifier();
    } else {
        // I don't think it would be right if we updated the fd while not connected *but* while having a notifier..
        ASSERT(!m_read_notifier);
    }
}

void CSocket::ensure_read_notifier()
{
    ASSERT(m_connected);
    m_read_notifier = CNotifier::construct(fd(), CNotifier::Event::Read, this);
    m_read_notifier->on_ready_to_read = [this] {
        if (on_ready_to_read)
            on_ready_to_read();
    };
}

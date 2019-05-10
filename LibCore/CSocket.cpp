#include <LibCore/CSocket.h>
#include <LibCore/CNotifier.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <stdlib.h>
#include <stdio.h>
#include <netdb.h>
#include <errno.h>

CSocket::CSocket(Type type, CObject* parent)
    : CIODevice(parent)
    , m_type(type)
{
}

CSocket::~CSocket()
{
}

bool CSocket::connect(const String& hostname, int port)
{
    auto* hostent = gethostbyname(hostname.characters());
    if (!hostent) {
        dbgprintf("CSocket::connect: Unable to resolve '%s'\n", hostname.characters());
        return false;
    }

    IPv4Address host_address((const byte*)hostent->h_addr_list[0]);
    dbgprintf("CSocket::connect: Resolved '%s' to %s\n", hostname.characters(), host_address.to_string().characters());
    return connect(host_address, port);
}

bool CSocket::connect(const CSocketAddress& address, int port)
{
    ASSERT(!is_connected());
    ASSERT(address.type() == CSocketAddress::Type::IPv4);
    ASSERT(port > 0 && port <= 65535);

    struct sockaddr_in addr;
    memset(&addr, 0, sizeof(addr));
    auto ipv4_address = address.ipv4_address();
    memcpy(&addr.sin_addr.s_addr, &ipv4_address, sizeof(IPv4Address));
    addr.sin_family = AF_INET;
    addr.sin_port = htons(port);

    m_destination_address = address;
    m_destination_port = port;

    dbgprintf("Connecting to %s...", address.to_string().characters());
    fflush(stdout);
    int rc = ::connect(fd(), (struct sockaddr*)&addr, sizeof(addr));
    if (rc < 0) {
        if (errno == EINPROGRESS) {
            dbgprintf("in progress.\n");
            m_notifier = make<CNotifier>(fd(), CNotifier::Event::Write);
            m_notifier->on_ready_to_write = [this] {
                dbgprintf("%s{%p} connected!\n", class_name(), this);
                m_connected = true;
                m_notifier->set_event_mask(CNotifier::Event::None);
                if (on_connected)
                    on_connected();
            };
            return true;
        }
        perror("connect");
        exit(1);
    }
    dbgprintf("ok!\n");
    m_connected = true;
    return true;
}

ByteBuffer CSocket::receive(int max_size)
{
    auto buffer = read(max_size);
    if (eof()) {
        dbgprintf("CSocket{%p}: Connection appears to have closed in receive().\n", this);
        m_connected = false;
    }
    return buffer;
}

bool CSocket::send(const ByteBuffer& data)
{
    int nsent = ::send(fd(), data.pointer(), data.size(), 0);
    if (nsent < 0) {
        set_error(nsent);
        return false;
    }
    ASSERT(nsent == data.size());
    return true;
}

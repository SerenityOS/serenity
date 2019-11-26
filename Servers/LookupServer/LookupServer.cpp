#include "LookupServer.h"
#include "DNSPacket.h"
#include "DNSRecord.h"
#include <AK/BufferStream.h>
#include <AK/ByteBuffer.h>
#include <AK/HashMap.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <Kernel/Net/IPv4.h>
#include <LibCore/CConfigFile.h>
#include <LibCore/CEventLoop.h>
#include <LibCore/CFile.h>
#include <LibCore/CLocalServer.h>
#include <LibCore/CLocalSocket.h>
#include <LibCore/CSyscallUtils.h>
#include <arpa/inet.h>
#include <netinet/in.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/select.h>
#include <sys/socket.h>
#include <sys/time.h>
#include <time.h>
#include <unistd.h>

#define T_A 1
#define T_NS 2
#define T_CNAME 5
#define T_SOA 6
#define T_PTR 12
#define T_MX 15

#define C_IN 1

LookupServer::LookupServer()
{
    auto config = CConfigFile::get_for_system("LookupServer");
    dbg() << "Using network config file at " << config->file_name();
    m_dns_ip = config->read_entry("DNS", "IPAddress", "127.0.0.53");

    load_etc_hosts();

    m_local_server = CLocalServer::construct(this);
    m_local_server->on_ready_to_accept = [this]() {
        auto socket = m_local_server->accept();
        socket->on_ready_to_read = [this, socket]() {
            service_client(socket);
            RefPtr<CLocalSocket> keeper = socket;
            const_cast<CLocalSocket&>(*socket).on_ready_to_read = []{};
        };
    };
    bool ok = m_local_server->take_over_from_system_server();
    ASSERT(ok);
}

void LookupServer::load_etc_hosts()
{
    dbg() << "Loading hosts from /etc/hosts";
    auto file = CFile::construct("/etc/hosts");
    if (!file->open(CIODevice::ReadOnly))
        return;
    while (!file->eof()) {
        auto line = file->read_line(1024);
        if (line.is_empty())
            break;
        auto str_line = String((const char*)line.data(), line.size() - 1, Chomp);
        auto fields = str_line.split('\t');

        auto sections = fields[0].split('.');
        IPv4Address addr {
            (u8)atoi(sections[0].characters()),
            (u8)atoi(sections[1].characters()),
            (u8)atoi(sections[2].characters()),
            (u8)atoi(sections[3].characters()),
        };

        auto name = fields[1];
        m_dns_custom_hostnames.set(name, addr.to_string());

        IPv4Address reverse_addr {
            (u8)atoi(sections[3].characters()),
            (u8)atoi(sections[2].characters()),
            (u8)atoi(sections[1].characters()),
            (u8)atoi(sections[0].characters()),
        };
        StringBuilder builder;
        builder.append(reverse_addr.to_string());
        builder.append(".in-addr.arpa");
        m_dns_custom_hostnames.set(builder.to_string(), name);
    }
}

void LookupServer::service_client(RefPtr<CLocalSocket> socket)
{
    u8 client_buffer[1024];
    int nrecv = socket->read(client_buffer, sizeof(client_buffer) - 1);
    if (nrecv < 0) {
        perror("read");
        return;
    }

    client_buffer[nrecv] = '\0';

    char lookup_type = client_buffer[0];
    if (lookup_type != 'L' && lookup_type != 'R') {
        dbg() << "Invalid lookup_type " << lookup_type;
        return;
    }
    auto hostname = String((const char*)client_buffer + 1, nrecv - 1, Chomp);
    dbg() << "Got request for '" << hostname << "' (using IP " << m_dns_ip << ")";

    Vector<String> responses;

    if (auto known_host = m_dns_custom_hostnames.get(hostname)) {
        responses.append(known_host.value());
    } else if (!hostname.is_empty()) {
        bool did_timeout;
        int retries = 3;
        do {
            did_timeout = false;
            if (lookup_type == 'L')
                responses = lookup(hostname, did_timeout, T_A);
            else if (lookup_type == 'R')
                responses = lookup(hostname, did_timeout, T_PTR);
            if (!did_timeout)
                break;
        } while (--retries);
        if (did_timeout) {
            fprintf(stderr, "LookupServer: Out of retries :(\n");
            return;
        }
    }

    if (responses.is_empty()) {
        int nsent = socket->write("Not found.\n");
        if (nsent < 0)
            perror("write");
        return;
    }
    for (auto& response : responses) {
        auto line = String::format("%s\n", response.characters());
        int nsent = socket->write(line);
        if (nsent < 0) {
            perror("write");
            break;
        }
    }
}

String LookupServer::parse_dns_name(const u8* data, int& offset, int max_offset)
{
    Vector<char, 128> buf;
    while (offset < max_offset) {
        u8 ch = data[offset];
        if (ch == '\0') {
            ++offset;
            break;
        }
        if ((ch & 0xc0) == 0xc0) {
            ASSERT_NOT_REACHED();
            // FIXME: Parse referential names.
            offset += 2;
        }
        for (int i = 0; i < ch; ++i) {
            buf.append(data[offset + i + 1]);
        }
        buf.append('.');
        offset += ch + 1;
    }
    return String::copy(buf);
}

Vector<String> LookupServer::lookup(const String& hostname, bool& did_timeout, unsigned short record_type)
{
    if (auto it = m_lookup_cache.find(hostname); it != m_lookup_cache.end()) {
        auto& cached_lookup = it->value;
        if (cached_lookup.record_type == record_type && cached_lookup.timestamp < (time(nullptr) + 60)) {
            return it->value.responses;
        }
        m_lookup_cache.remove(it);
    }

    DNSPacket request_header;
    request_header.set_id(get_next_id());
    request_header.set_is_query();
    request_header.set_opcode(0);
    request_header.set_truncated(false);
    request_header.set_recursion_desired(true);
    request_header.set_question_count(1);

    auto buffer = ByteBuffer::create_uninitialized(1024);
    {
        BufferStream stream(buffer);

        stream << ByteBuffer::wrap(&request_header, sizeof(request_header));
        auto parts = hostname.split('.');
        for (auto& part : parts) {
            stream << (u8)part.length();
            stream << part;
        }
        stream << '\0';
        stream << htons(record_type);
        stream << htons(C_IN);
        stream.snip();
    }

    int fd = socket(AF_INET, SOCK_DGRAM, 0);
    if (fd < 0) {
        perror("socket");
        return {};
    }

    struct timeval timeout {
        1, 0
    };
    int rc = setsockopt(fd, SOL_SOCKET, SO_RCVTIMEO, &timeout, sizeof(timeout));
    if (rc < 0) {
        perror("setsockopt");
        close(fd);
        return {};
    }

    struct sockaddr_in dst_addr;
    memset(&dst_addr, 0, sizeof(dst_addr));

    dst_addr.sin_family = AF_INET;
    dst_addr.sin_port = htons(53);
    rc = inet_pton(AF_INET, m_dns_ip.characters(), &dst_addr.sin_addr);

    int nsent = sendto(fd, buffer.data(), buffer.size(), 0, (const struct sockaddr*)&dst_addr, sizeof(dst_addr));
    if (nsent < 0) {
        perror("sendto");
        return {};
    }
    ASSERT(nsent == buffer.size());

    struct sockaddr_in src_addr;
    socklen_t src_addr_len = sizeof(src_addr);
    u8 response_buffer[4096];
    ssize_t nrecv = recvfrom(fd, response_buffer, sizeof(response_buffer) - 1, 0, (struct sockaddr*)&src_addr, &src_addr_len);
    if (nrecv < 0) {
        if (errno == EAGAIN) {
            did_timeout = true;
        } else {
            perror("recvfrom");
        }
        close(fd);
        return {};
    }
    close(fd);

    response_buffer[nrecv] = '\0';

    if (nrecv < (int)sizeof(DNSPacket)) {
        dbgprintf("LookupServer: Response not big enough (%d) to be a DNS packet :(\n", nrecv);
        return {};
    }

    auto& response_header = *(DNSPacket*)(response_buffer);
    dbgprintf("Got response (ID: %u)\n", response_header.id());
    dbgprintf("  Question count: %u\n", response_header.question_count());
    dbgprintf("  Answer count: %u\n", response_header.answer_count());
    dbgprintf(" Authority count: %u\n", response_header.authority_count());
    dbgprintf("Additional count: %u\n", response_header.additional_count());

    if (response_header.id() != request_header.id()) {
        dbgprintf("LookupServer: ID mismatch (%u vs %u) :(\n", response_header.id(), request_header.id());
        return {};
    }
    if (response_header.question_count() != 1) {
        dbgprintf("LookupServer: Question count (%u vs %u) :(\n", response_header.question_count(), request_header.question_count());
        return {};
    }
    if (response_header.answer_count() < 1) {
        dbgprintf("LookupServer: Not enough answers (%u) :(\n", response_header.answer_count());
        return {};
    }

    int offset = 0;
    auto question = parse_dns_name((const u8*)response_header.payload(), offset, nrecv);
    offset += 4;

    Vector<String> addresses;

    for (u16 i = 0; i < response_header.answer_count(); ++i) {
        auto& record = *(const DNSRecord*)(&((const u8*)response_header.payload())[offset]);
        dbgprintf("LookupServer:     Answer #%u: (question: %s), type=%u, ttl=%u, length=%u, data=",
            i,
            question.characters(),
            record.type(),
            record.ttl(),
            record.data_length());

        offset += sizeof(DNSRecord) + record.data_length();
        if (record.type() == T_PTR) {
            int dummy = 0;
            auto name = parse_dns_name((const u8*)record.data(), dummy, record.data_length());
            dbgprintf("%s\n", name.characters());
            addresses.append(name);
        } else if (record.type() == T_A) {
            auto ipv4_address = IPv4Address((const u8*)record.data());
            dbgprintf("%s\n", ipv4_address.to_string().characters());
            addresses.append(ipv4_address.to_string());
        } else {
            dbgprintf("(unimplemented)\n");
            dbgprintf("LookupServer: FIXME: Handle record type %u\n", record.type());
        }
        // FIXME: Parse some other record types perhaps?
    }

    m_lookup_cache.set(hostname, { time(nullptr), record_type, addresses });
    return addresses;
}

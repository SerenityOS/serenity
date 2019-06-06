#include "DNSPacket.h"
#include "DNSRecord.h"
#include <AK/AKString.h>
#include <AK/BufferStream.h>
#include <AK/ByteBuffer.h>
#include <AK/HashMap.h>
#include <Kernel/Net/IPv4.h>
#include <LibCore/CConfigFile.h>
#include <arpa/inet.h>
#include <netinet/in.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/select.h>
#include <sys/socket.h>
#include <sys/time.h>
#include <unistd.h>

#define T_A 1
#define T_NS 2
#define T_CNAME 5
#define T_SOA 6
#define T_PTR 12
#define T_MX 15

#define C_IN 1

static Vector<String> lookup(const String& hostname, bool& did_timeout, const String& DNS_IP, unsigned short record_type);
static String parse_dns_name(const byte*, int& offset, int max_offset);

int main(int argc, char** argv)
{
    (void)argc;
    (void)argv;

    unlink("/tmp/.LookupServer-socket");

    auto config = CConfigFile::get_for_system("LookupServer");
    dbgprintf("LookupServer: Using network config file at %s.\n",
        config->file_name().characters());
    auto DNS_IP = config->read_entry("DNS", "IPAddress", "127.0.0.53");

    dbgprintf("LookupServer: Loading hosts from /etc/hosts:\n");
    HashMap<String, IPv4Address> dns_custom_hostnames;
    auto* file = fopen("/etc/hosts", "r");
    auto linebuf = ByteBuffer::create_uninitialized(256);
    while (fgets((char*)linebuf.pointer(), linebuf.size(), file)) {
        auto str_line = String::copy(linebuf);
        auto fields = str_line.split('\t');
        auto sections = fields[0].split('.');
        IPv4Address addr {
            (byte)atoi(sections[0].characters()),
            (byte)atoi(sections[1].characters()),
            (byte)atoi(sections[2].characters()),
            (byte)atoi(sections[3].characters()),
        };
        int len = 0;
        while ((fields[1][len++]) != -123)
            ;
        auto name = fields[1].substring(0, len - 3);
        dbgprintf("LookupServer: Hosts: %s\t%s\n", name.characters(), addr.to_string().characters());
        dns_custom_hostnames.set(name, addr);
    }

    int server_fd = socket(AF_LOCAL, SOCK_STREAM | SOCK_CLOEXEC, 0);
    if (server_fd < 0) {
        perror("socket");
        return 1;
    }

    sockaddr_un address;
    address.sun_family = AF_LOCAL;
    strcpy(address.sun_path, "/tmp/.LookupServer-socket");

    int rc = bind(server_fd, (const sockaddr*)&address, sizeof(address));
    if (rc < 0) {
        perror("bind");
        return 1;
    }
    rc = listen(server_fd, 5);
    if (rc < 0) {
        perror("listen");
        return 1;
    }

    for (;;) {
        fd_set rfds;
        FD_ZERO(&rfds);
        FD_SET(server_fd, &rfds);
        rc = select(server_fd + 1, &rfds, nullptr, nullptr, nullptr);
        if (rc < 1) {
            perror("select");
            return 1;
        }

        sockaddr_un client_address;
        socklen_t client_address_size = sizeof(client_address);
        int client_fd = accept(server_fd, (sockaddr*)&client_address, &client_address_size);
        if (client_fd < 0) {
            perror("accept");
            continue;
        }

        FD_ZERO(&rfds);
        FD_SET(client_fd, &rfds);
        rc = select(client_fd + 1, &rfds, nullptr, nullptr, nullptr);
        if (rc < 1) {
            perror("select");
            return 1;
        }

        char client_buffer[1024];
        int nrecv = read(client_fd, client_buffer, sizeof(client_buffer) - 1);
        if (nrecv < 0) {
            perror("recv");
            close(client_fd);
            continue;
        }

        client_buffer[nrecv] = '\0';

        char lookup_type = client_buffer[0];
        if (lookup_type != 'L' && lookup_type != 'R') {
            dbgprintf("LookupServer: Invalid lookup_type '%c'\n", lookup_type);
            close(client_fd);
            continue;
        }
        auto hostname = String(client_buffer + 1, nrecv - 1, Chomp);
        dbgprintf("LookupServer: Got request for '%s' (using IP %s)\n",
            hostname.characters(),
            DNS_IP.characters());

        Vector<String> responses;

        if (dns_custom_hostnames.contains(hostname)) {
            addresses.append(dns_custom_hostnames.get(hostname));
            dbgprintf("LookupServer: Found preconfigured host (from /etc/hosts): %s\n", addresses[0].to_string().characters());
        } else if (!hostname.is_empty()) {
            bool did_timeout;
            int retries = 3;
            do {
                did_timeout = false;
                if (lookup_type == 'L')
                    responses = lookup(hostname, did_timeout, DNS_IP, T_A);
                else if (lookup_type == 'R')
                    responses = lookup(hostname, did_timeout, DNS_IP, T_PTR);
                if (!did_timeout)
                    break;
            } while (--retries);
            if (did_timeout) {
                fprintf(stderr, "LookupServer: Out of retries :(\n");
                close(client_fd);
                continue;
            }
        }

        if (responses.is_empty()) {
            int nsent = write(client_fd, "Not found.\n", sizeof("Not found.\n"));
            if (nsent < 0)
                perror("write");
            close(client_fd);
            continue;
        }
        for (auto& response : responses) {
            auto line = String::format("%s\n", response.characters());
            int nsent = write(client_fd, line.characters(), line.length());
            if (nsent < 0) {
                perror("write");
                break;
            }
        }
        close(client_fd);
    }
    return 0;
}

static word get_next_id()
{
    static word s_next_id = 0;
    return ++s_next_id;
}

Vector<String> lookup(const String& hostname, bool& did_timeout, const String& DNS_IP, unsigned short record_type)
{
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
            stream << (byte)part.length();
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
    rc = inet_pton(AF_INET, DNS_IP.characters(), &dst_addr.sin_addr);

    int nsent = sendto(fd, buffer.pointer(), buffer.size(), 0, (const struct sockaddr*)&dst_addr, sizeof(dst_addr));
    if (nsent < 0) {
        perror("sendto");
        return {};
    }
    ASSERT(nsent == buffer.size());

    struct sockaddr_in src_addr;
    socklen_t src_addr_len = sizeof(src_addr);
    byte response_buffer[4096];
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
    auto question = parse_dns_name((const byte*)response_header.payload(), offset, nrecv);
    offset += 4;

    Vector<String> addresses;

    for (word i = 0; i < response_header.answer_count(); ++i) {
        auto& record = *(const DNSRecord*)(&((const byte*)response_header.payload())[offset]);
        dbgprintf("LookupServer:     Answer #%u: (question: %s), type=%u, ttl=%u, length=%u, data=",
            i,
            question.characters(),
            record.type(),
            record.ttl(),
            record.data_length());

        offset += sizeof(DNSRecord) + record.data_length();
        if (record.type() == T_PTR) {
            int dummy = 0;
            auto name = parse_dns_name((const byte*)record.data(), dummy, record.data_length());
            dbgprintf("%s\n", name.characters());
            addresses.append(name);
        } else if (record.type() == T_A) {
            auto ipv4_address = IPv4Address((const byte*)record.data());
            dbgprintf("%s\n", ipv4_address.to_string().characters());
            addresses.append(ipv4_address.to_string());
        } else {
            dbgprintf("(unimplemented)\n");
            dbgprintf("LookupServer: FIXME: Handle record type %u\n", record.type());
        }
        // FIXME: Parse some other record types perhaps?
    }

    return addresses;
}

static String parse_dns_name(const byte* data, int& offset, int max_offset)
{
    Vector<char, 128> buf;
    while (offset < max_offset) {
        byte ch = data[offset];
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

#include <stdio.h>
#include <stdlib.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <netinet/in.h>
#include <unistd.h>
#include <unistd.h>
#include <Kernel/IPv4.h>
#include <AK/AKString.h>
#include <AK/HashMap.h>
#include <AK/ByteBuffer.h>
#include <AK/BufferStream.h>
#include "DNSPacket.h"
#include "DNSRecord.h"

#define T_A     1
#define T_NS    2
#define T_CNAME 5
#define T_SOA   6
#define T_PTR   12
#define T_MX    15

#define C_IN    1

static Vector<IPv4Address> lookup(const String& hostname);
static String parse_dns_name(const byte*, int& offset, int max_offset);

int main(int argc, char**argv)
{
    (void)argc;
    (void)argv;

    String hostname = "disney.com";

    if (argc == 2) {
        hostname = argv[1];
    }

    HashMap<String, IPv4Address> dns_cache;

    auto ipv4_addresses = lookup(hostname);
    if (ipv4_addresses.is_empty()) {
        printf("Lookup failed\n");
    } else {
        printf("DNS lookup result:\n");
        for (auto& ipv4_address : ipv4_addresses) {
            printf("  '%s' => %s\n", hostname.characters(), ipv4_address.to_string().characters());
        }
    }

    return 0;
}

static word get_next_id()
{
    static word s_next_id = 0;
    return ++s_next_id;
}

Vector<IPv4Address> lookup(const String& hostname)
{
    // FIXME: First check if it's an IP address in a string!

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
        stream << htons(T_A);
        stream << htons(C_IN);
        stream.snip();
    }

    int fd = socket(AF_INET, SOCK_DGRAM, 0);
    if (fd < 0) {
        perror("socket");
        return { };
    }

    struct timeval timeout { 5, 0 };
    int rc = setsockopt(fd, SOL_SOCKET, SO_RCVTIMEO, &timeout, sizeof(timeout));
    if (rc < 0) {
        perror("setsockopt");
        close(fd);
        return { };
    }

    struct sockaddr_in dst_addr;
    memset(&dst_addr, 0, sizeof(dst_addr));

    dst_addr.sin_family = AF_INET;
    dst_addr.sin_port = htons(53);
    rc = inet_pton(AF_INET, "172.20.10.1", &dst_addr.sin_addr);

    int nsent = sendto(fd, buffer.pointer(), buffer.size(), 0,(const struct sockaddr *)&dst_addr, sizeof(dst_addr));
    if (nsent < 0) {
        perror("sendto");
        return { };
    }
    ASSERT(nsent == buffer.size());

    struct sockaddr_in src_addr;
    socklen_t src_addr_len = sizeof(src_addr);
    byte response_buffer[4096];
    ssize_t nrecv = recvfrom(fd, response_buffer, sizeof(response_buffer) - 1, 0, (struct sockaddr*)&src_addr, &src_addr_len);
    if (nrecv < 0) {
        perror("recvfrom");
        close(fd);
        return { };
    }
    close(fd);

    response_buffer[nrecv] = '\0';

    if (nrecv < (int)sizeof(DNSPacket)) {
        printf("Response not big enough (%d) to be a DNS packet :(\n", nrecv);
        return { };
    }

    auto& response_header = *(DNSPacket*)(response_buffer);
    printf("Got response (ID: %u)\n", response_header.id());
    //printf("  Question count: %u\n", response_header.question_count());
    printf("  Answer count: %u\n", response_header.answer_count());
    //printf(" Authority count: %u\n", response_header.authority_count());
    //printf("Additional count: %u\n", response_header.additional_count());

    if (response_header.id() != request_header.id()) {
        printf("ID mismatch (%u vs %u) :(\n", response_header.id(), request_header.id());
        return { };
    }
    if (response_header.question_count() != 1) {
        printf("Question count (%u vs %u) :(\n", response_header.question_count(), request_header.question_count());
        return { };
    }
    if (response_header.answer_count() < 1) {
        printf("Not enough answers (%u) :(\n", response_header.answer_count());
        return { };
    }

    int offset = 0;
    auto question = parse_dns_name((const byte*)response_header.payload(), offset, nrecv);
    offset += 4;

    Vector<IPv4Address> addresses;

    for (word i = 0; i < response_header.answer_count(); ++i) {
        auto& record = *(const DNSRecord*)(&((const byte*)response_header.payload())[offset]);
        auto ipv4_address = IPv4Address((const byte*)record.data());
        printf("    Answer #%u: (question: %s), ttl=%u, length=%u, data=%s\n",
            i,
            question.characters(),
            record.ttl(),
            record.data_length(),
            ipv4_address.to_string().characters());

        offset += sizeof(DNSRecord) + record.data_length();
        addresses.append(ipv4_address);
    }

    return addresses;
}

static String parse_dns_name(const byte* data, int& offset, int max_offset)
{
    Vector<char> buf;
    while (offset < max_offset) {
        byte ch = data[offset];
        if (ch == '\0') {
            ++offset;
            break;
        }
        if ((ch & 0xc0) == 0xc0) {
            // FIXME: Parse referential names.
            offset += 2;
        }
        for (int i = 0; i < ch; ++i) {
            buf.append(data[offset + i + 1]);
        }
        buf.append('.');
        offset += ch + 1;
    }
    return String(buf.data(), buf.size());
}

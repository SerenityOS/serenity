/*
 * Copyright (c) 2026, Jose
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>
#include <errno.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <unistd.h>

static constexpr u16 port = 1338;

struct [[gnu::packed]] UDPHeader {
    u16 source_port;
    u16 destination_port;
    u16 length;
    u16 checksum;
};
static_assert(sizeof(UDPHeader) == 8);

// Regression test for UDP datagrams whose Length field disagrees with the IPv4
// payload. They used to be handed to the socket, where UDPSocket sized its copy
// from the unvalidated Length field: a length below sizeof(UDPPacket) tripped a
// VERIFY() in protocol_receive(), and a length above the IPv4 payload made it
// read past the queued packet.
TEST_CASE(udp_packets_with_invalid_length_are_dropped)
{
    int server_fd = socket(AF_INET, SOCK_DGRAM, 0);
    EXPECT(server_fd >= 0);

    sockaddr_in address {};
    address.sin_family = AF_INET;
    address.sin_port = htons(port);
    address.sin_addr.s_addr = htonl(INADDR_LOOPBACK);
    EXPECT_EQ(bind(server_fd, reinterpret_cast<sockaddr*>(&address), sizeof(address)), 0);

    int raw_fd = socket(AF_INET, SOCK_RAW, IPPROTO_UDP);
    EXPECT(raw_fd >= 0);

    auto send_raw = [&](void const* data, size_t size) {
        return sendto(raw_fd, data, size, 0, reinterpret_cast<sockaddr*>(&address), sizeof(address));
    };

    // UDP Length smaller than a UDP header.
    UDPHeader too_short {};
    too_short.source_port = htons(1234);
    too_short.destination_port = htons(port);
    too_short.length = htons(0);
    EXPECT_EQ(send_raw(&too_short, sizeof(too_short)), static_cast<ssize_t>(sizeof(too_short)));

    // UDP Length larger than the IPv4 payload.
    UDPHeader too_long {};
    too_long.source_port = htons(1234);
    too_long.destination_port = htons(port);
    too_long.length = htons(0xffff);
    EXPECT_EQ(send_raw(&too_long, sizeof(too_long)), static_cast<ssize_t>(sizeof(too_long)));

    // A well-formed datagram must still be delivered. Loopback delivery is
    // FIFO, so receiving this one proves the two malformed datagrams were
    // dropped instead of being queued ahead of it.
    struct [[gnu::packed]] ValidDatagram {
        UDPHeader header;
        u8 payload;
    } valid {};
    valid.header.source_port = htons(1234);
    valid.header.destination_port = htons(port);
    valid.header.length = htons(sizeof(UDPHeader) + 1);
    valid.payload = 'A';
    EXPECT_EQ(send_raw(&valid, sizeof(valid)), static_cast<ssize_t>(sizeof(valid)));

    u8 buffer[16];
    EXPECT_EQ(recv(server_fd, buffer, sizeof(buffer), 0), 1);
    EXPECT_EQ(buffer[0], 'A');

    // Nothing else should be queued.
    errno = 0;
    EXPECT_EQ(recv(server_fd, buffer, sizeof(buffer), MSG_DONTWAIT), -1);
    EXPECT_EQ(errno, EAGAIN);

    EXPECT_EQ(close(raw_fd), 0);
    EXPECT_EQ(close(server_fd), 0);
}

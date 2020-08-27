/*
 * Copyright (c) 2020, Nico Weber <thakis@chromium.org>
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

#define _GNU_SOURCE
#include <LibCore/ArgsParser.h>
#include <arpa/inet.h>
#include <endian.h>
#include <netdb.h>
#include <netinet/in.h>
#include <stdio.h>
#include <string.h>
#include <sys/socket.h>
#include <sys/time.h>

struct [[gnu::packed]] NtpPacket {
    uint8_t li_vn_mode;
    uint8_t stratum;
    int8_t poll;
    int8_t precision;

    uint32_t root_delay;
    uint32_t root_dispersion;
    uint32_t reference_id;

    uint64_t reference_timestamp;
    uint64_t origin_timestamp;
    uint64_t receive_timestamp;
    uint64_t transmit_timestamp;
};

static_assert(sizeof(NtpPacket) == 48);

int main(int argc, char** argv)
{
    if (pledge("stdio inet dns", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    // FIXME: Request serenityos.pool.ntp.org here https://manage.ntppool.org/manage/vendor
    // and then use that as the default value.
    // Until then, explicitly pass this as `ntpquery pool.ntp.org`.
    const char* host = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(host, "NTP server", "host", Core::ArgsParser::Required::Yes);
    args_parser.parse(argc, argv);

    auto* hostent = gethostbyname(host);
    if (!hostent) {
        fprintf(stderr, "Lookup failed for '%s'\n", host);
        return 1;
    }

    if (pledge("stdio inet", nullptr) < 0) {
        perror("pledge");
        return 1;
    }
    unveil(nullptr, nullptr);

    int fd = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP);
    if (fd < 0) {
        perror("socket");
        return 1;
    }

    struct timeval timeout {
        5, 0
    };
    if (setsockopt(fd, SOL_SOCKET, SO_RCVTIMEO, &timeout, sizeof(timeout)) < 0) {
        perror("setsockopt");
        return 1;
    }

    sockaddr_in peer_address;
    memset(&peer_address, 0, sizeof(peer_address));
    peer_address.sin_family = AF_INET;
    peer_address.sin_port = htons(123);
    peer_address.sin_addr.s_addr = *(const in_addr_t*)hostent->h_addr_list[0];

    NtpPacket packet;
    memset(&packet, 0, sizeof(packet));
    packet.li_vn_mode = (4 << 3) | 3; // Version 4, client connection.

    // The server will copy the transmit_timestamp to origin_timestamp in the reply.
    timeval t;
    gettimeofday(&t, nullptr);
    packet.transmit_timestamp = htobe64(ntp_timestamp_from_timeval(t));

    ssize_t rc;
    rc = sendto(fd, &packet, sizeof(packet), 0, (const struct sockaddr*)&peer_address, sizeof(peer_address));
    if (rc < 0) {
        perror("sendto");
        return 1;
    }
    if ((size_t)rc < sizeof(packet)) {
        fprintf(stderr, "incomplete packet send\n");
        return 1;
    }

    socklen_t peer_address_size = sizeof(peer_address);
    rc = recvfrom(fd, &packet, sizeof(packet), 0, (struct sockaddr*)&peer_address, &peer_address_size);
    if (rc < 0) {
        perror("recvfrom");
        return 1;
    }
    if ((size_t)rc < sizeof(packet)) {
        fprintf(stderr, "incomplete packet recv\n");
        return 1;
    }

    printf("NTP response from %s:\n", inet_ntoa(peer_address.sin_addr));
    printf("Leap Information: %d\n", packet.li_vn_mode >> 6);
    printf("Version Number: %d\n", (packet.li_vn_mode >> 3) & 7);
    printf("Mode: %d\n", packet.li_vn_mode & 7);
    printf("Stratum: %d\n", packet.stratum);
    printf("Poll: %d\n", packet.stratum);
    printf("Precision: %d\n", packet.precision);
    printf("Root delay: %#x\n", ntohl(packet.root_delay));
    printf("Root dispersion: %#x\n", ntohl(packet.root_dispersion));
    printf("Reference ID: %#x\n", ntohl(packet.reference_id));
    printf("Reference timestamp: %#016llx\n", be64toh(packet.reference_timestamp));
    printf("Origin timestamp:    %#016llx\n", be64toh(packet.origin_timestamp));
    printf("Receive timestamp:   %#016llx\n", be64toh(packet.receive_timestamp));
    printf("Transmit timestamp:  %#016llx\n", be64toh(packet.transmit_timestamp));
}

/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <LibCore/ArgsParser.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <netinet/in.h>
#include <netinet/ip_icmp.h>
#include <stdio.h>
#include <string.h>
#include <sys/socket.h>
#include <sys/time.h>
#include <time.h>
#include <unistd.h>

static uint16_t internet_checksum(const void* ptr, size_t count)
{
    uint32_t checksum = 0;
    auto* w = (const uint16_t*)ptr;
    while (count > 1) {
        checksum += ntohs(*w++);
        if (checksum & 0x80000000)
            checksum = (checksum & 0xffff) | (checksum >> 16);
        count -= 2;
    }
    while (checksum >> 16)
        checksum = (checksum & 0xffff) + (checksum >> 16);
    return htons(~checksum);
}

int main(int argc, char** argv)
{
    if (pledge("stdio id inet dns", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    const char* host = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(host, "Host to ping", "host");
    args_parser.parse(argc, argv);

    int fd = socket(AF_INET, SOCK_RAW, IPPROTO_ICMP);
    if (fd < 0) {
        perror("socket");
        return 1;
    }

    if (setgid(getgid()) || setuid(getuid())) {
        fprintf(stderr, "Failed to drop privileges.\n");
        return 1;
    }

    if (pledge("stdio inet dns", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    struct timeval timeout {
        1, 0
    };
    int rc = setsockopt(fd, SOL_SOCKET, SO_RCVTIMEO, &timeout, sizeof(timeout));
    if (rc < 0) {
        perror("setsockopt");
        return 1;
    }

    auto* hostent = gethostbyname(host);
    if (!hostent) {
        printf("Lookup failed for '%s'\n", host);
        return 1;
    }

    if (pledge("stdio inet", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    pid_t pid = getpid();

    sockaddr_in peer_address;
    memset(&peer_address, 0, sizeof(peer_address));
    peer_address.sin_family = AF_INET;
    peer_address.sin_port = 0;

    peer_address.sin_addr.s_addr = *(const in_addr_t*)hostent->h_addr_list[0];

    struct PingPacket {
        struct icmphdr header;
        char msg[64 - sizeof(struct icmphdr)];
    };

    uint16_t seq = 1;

    for (;;) {
        PingPacket ping_packet;
        PingPacket pong_packet;
        memset(&ping_packet, 0, sizeof(PingPacket));

        ping_packet.header.type = 8; // Echo request
        ping_packet.header.code = 0;
        ping_packet.header.un.echo.id = htons(pid);
        ping_packet.header.un.echo.sequence = htons(seq++);

        bool fits = String("Hello there!\n").copy_characters_to_buffer(ping_packet.msg, sizeof(ping_packet.msg));
        // It's a constant string, we can be sure that it fits.
        ASSERT(fits);

        ping_packet.header.checksum = internet_checksum(&ping_packet, sizeof(PingPacket));

        struct timeval tv_send;
        gettimeofday(&tv_send, nullptr);

        rc = sendto(fd, &ping_packet, sizeof(PingPacket), 0, (const struct sockaddr*)&peer_address, sizeof(sockaddr_in));
        if (rc < 0) {
            perror("sendto");
            return 1;
        }

        for (;;) {
            socklen_t peer_address_size = sizeof(peer_address);
            rc = recvfrom(fd, &pong_packet, sizeof(PingPacket), 0, (struct sockaddr*)&peer_address, &peer_address_size);
            if (rc < 0) {
                if (errno == EAGAIN) {
                    printf("Request (seq=%u) timed out.\n", ntohs(ping_packet.header.un.echo.sequence));
                    break;
                }
                perror("recvfrom");
                return 1;
            }

            if (pong_packet.header.type != 0)
                continue;
            if (pong_packet.header.code != 0)
                continue;
            if (ntohs(pong_packet.header.un.echo.id) != pid)
                continue;

            struct timeval tv_receive;
            gettimeofday(&tv_receive, nullptr);

            struct timeval tv_diff;
            timersub(&tv_receive, &tv_send, &tv_diff);

            int ms = tv_diff.tv_sec * 1000 + tv_diff.tv_usec / 1000;

            char addr_buf[64];
            printf("Pong from %s: id=%u, seq=%u%s, time=%dms\n",
                inet_ntop(AF_INET, &peer_address.sin_addr, addr_buf, sizeof(addr_buf)),
                ntohs(pong_packet.header.un.echo.id),
                ntohs(pong_packet.header.un.echo.sequence),
                pong_packet.header.un.echo.sequence != ping_packet.header.un.echo.sequence ? "(!)" : "",
                ms);

            // If this was a response to an earlier packet, we still need to wait for the current one.
            if (pong_packet.header.un.echo.sequence != ping_packet.header.un.echo.sequence)
                continue;
            break;
        }

        sleep(1);
    }

    return 0;
}

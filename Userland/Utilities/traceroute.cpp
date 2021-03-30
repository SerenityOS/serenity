/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@gmail.com>
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

#include <AK/String.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/ElapsedTimer.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <netinet/in.h>
#include <netinet/ip_icmp.h>
#include <serenity.h>
#include <stdio.h>
#include <string.h>
#include <sys/socket.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio id inet unix", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    const char* host_name;
    int max_hops = 30;
    int max_retries = 3;
    int echo_timeout = 5;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(host_name, "destination", "destination", Core::ArgsParser::Required::Yes);
    args_parser.add_option(max_hops, "use at most <hops> to the destination", "max-hops", 'h', "hops");
    args_parser.add_option(max_retries, "retry TTL at most <tries> times", "max-retries", 'r', "tries");
    args_parser.add_option(echo_timeout, "wait at most <seconds> for a response", "timeout", 't', "seconds");
    args_parser.parse(argc, argv);

    if (max_hops < 1 || max_hops > 255) {
        warnln("Invalid maximum hops amount");
        return 1;
    }

    if (max_retries < 1) {
        warnln("Invalid maximum retries amount");
        return 1;
    }

    auto* hostent = gethostbyname(host_name);
    if (!hostent) {
        warnln("Lookup failed for '{}'", host_name);
        return 1;
    }
    sockaddr_in host_address {};
    memset(&host_address, 0, sizeof(host_address));
    host_address.sin_family = AF_INET;
    host_address.sin_port = 44444;
    host_address.sin_addr.s_addr = *(const in_addr_t*)hostent->h_addr_list[0];

    int fd = socket(AF_INET, SOCK_RAW, IPPROTO_ICMP);
    if (fd < 0) {
        perror("socket");
        return 1;
    }

    if (setgid(getgid()) || setuid(getuid())) {
        warnln("Failed to drop privileges");
        return 1;
    }

    if (pledge("stdio inet unix", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    timeval timeout { echo_timeout, 0 };
    if (setsockopt(fd, SOL_SOCKET, SO_RCVTIMEO, &timeout, sizeof(timeout)) < 0) {
        perror("setsockopt");
        return 1;
    }

    struct icmp_request {
        struct icmphdr header;
        char msg[64 - sizeof(struct icmphdr)];
    };

    struct icmp_response {
        u8 ip_header[20];
        struct icmphdr header;
        char msg[64 - sizeof(struct icmphdr)];
    };

    // 1: reached target
    // 0: got ttl exhausted response
    // -1: error or no response
    auto try_reach_host = [&](int ttl) -> int {
        Core::ElapsedTimer m_timer { true };
        for (auto i = 0; i < max_retries; i++) {
            icmp_request request {};
            request.header = { ICMP_ECHO, 0, 0, { { 0, 0 } } };
            bool fits = String::number(ttl).copy_characters_to_buffer(request.msg, sizeof(request.msg));
            VERIFY(fits);
            request.header.checksum = internet_checksum(&request, sizeof(request));

            m_timer.start();
            if (sendto(fd, &request, sizeof(request), 0, (sockaddr*)&host_address, sizeof(host_address)) < 0)
                return -1;

            icmp_response response {};
            sockaddr_in peer_address {};
            memset(&peer_address, 0, sizeof(peer_address));
            size_t peer_address_size = sizeof(peer_address);
            int result = recvfrom(fd, &response, sizeof(response), 0, (sockaddr*)&peer_address, (socklen_t*)&peer_address_size);
            if (result < 0) {
                if (result == EAGAIN)
                    return -1;
                continue;
            }

            if (response.header.type != ICMP_ECHOREPLY && response.header.type != ICMP_TIME_EXCEEDED)
                continue;

            auto* peer = gethostbyaddr(&peer_address.sin_addr, sizeof(peer_address.sin_addr), AF_INET);

            String peer_name;
            if (peer) {
                peer_name = peer->h_name;
            } else {
                char addr_buf[INET_ADDRSTRLEN];
                peer_name = inet_ntop(AF_INET, &peer_address.sin_addr, addr_buf, sizeof(addr_buf));
            }
            outln("{:2}:  {:50}  {:4}ms", ttl, peer_name, m_timer.elapsed());

            if (response.header.type == ICMP_TIME_EXCEEDED)
                return 0;
            if (response.header.type == ICMP_ECHOREPLY)
                return 1;
        }
        return -1;
    };

    for (auto ttl = 1; ttl <= max_hops; ttl++) {
        if (setsockopt(fd, IPPROTO_IP, IP_TTL, &ttl, sizeof(ttl)) < 0) {
            perror("setsockopt");
            return 1;
        }

        int result = try_reach_host(ttl);
        if (result < 0) {
            outln("{:2}:  no reply", ttl);
        } else if (result == 1) {
            outln("     Hops: {}", ttl);
            return 0;
        }
    }
    outln("     Too many hops: {}", max_hops);
    return 0;
}

/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/ByteString.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/ElapsedTimer.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <arpa/inet.h>
#include <errno.h>
#include <netdb.h>
#include <netinet/in.h>
#include <netinet/ip_icmp.h>
#include <serenity.h>
#include <stdio.h>
#include <string.h>
#include <sys/socket.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio id inet unix"));

    ByteString host_name;
    int max_hops = 30;
    int max_retries = 3;
    int echo_timeout = 5;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(host_name, "destination", "destination", Core::ArgsParser::Required::Yes);
    args_parser.add_option(max_hops, "use at most <hops> to the destination", "max-hops", 'h', "hops");
    args_parser.add_option(max_retries, "retry TTL at most <tries> times", "max-retries", 'r', "tries");
    args_parser.add_option(echo_timeout, "wait at most <seconds> for a response", "timeout", 't', "seconds");
    args_parser.parse(arguments);

    if (max_hops < 1 || max_hops > 255) {
        return Error::from_string_literal("Invalid maximum hops amount");
    }

    if (max_retries < 1) {
        return Error::from_string_literal("Invalid maximum retries amount");
    }

    auto* hostent = gethostbyname(host_name.characters());
    if (!hostent) {
        warnln("Lookup failed for '{}'", host_name);
        return 1;
    }
    sockaddr_in host_address {};
    host_address.sin_family = AF_INET;
    host_address.sin_port = 44444;
    host_address.sin_addr.s_addr = *(in_addr_t const*)hostent->h_addr_list[0];

    int fd = TRY(Core::System::socket(AF_INET, SOCK_RAW, IPPROTO_ICMP));

    TRY(Core::System::drop_privileges());
    TRY(Core::System::pledge("stdio inet unix"));

    timeval timeout { echo_timeout, 0 };
    TRY(Core::System::setsockopt(fd, SOL_SOCKET, SO_RCVTIMEO, &timeout, sizeof(timeout)));

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
    auto try_reach_host = [&](int ttl) -> ErrorOr<int> {
        Core::ElapsedTimer m_timer { Core::TimerType::Precise };
        auto ttl_number = ByteString::number(ttl);
        for (auto i = 0; i < max_retries; i++) {
            icmp_request request {};
            request.header = { ICMP_ECHO, 0, 0, { { 0, 0 } } };
            bool fits = ttl_number.copy_characters_to_buffer(request.msg, sizeof(request.msg));
            VERIFY(fits);
            request.header.checksum = internet_checksum(&request, sizeof(request));

            m_timer.start();
            TRY(Core::System::sendto(fd, &request, sizeof(request), 0, (sockaddr*)&host_address, sizeof(host_address)));

            icmp_response response {};
            sockaddr_in peer_address {};
            socklen_t peer_address_size = sizeof(peer_address);

            auto result = Core::System::recvfrom(fd, &response, sizeof(response), 0, (sockaddr*)&peer_address, &peer_address_size);
            if (result.is_error()) {
                if (result.error().code() == EAGAIN)
                    return result.release_error();
                continue;
            }

            if (response.header.type != ICMP_ECHOREPLY && response.header.type != ICMP_TIME_EXCEEDED)
                continue;

            auto response_time = m_timer.elapsed();
            auto* peer = gethostbyaddr(&peer_address.sin_addr, sizeof(peer_address.sin_addr), AF_INET);

            ByteString peer_name;
            if (peer) {
                peer_name = peer->h_name;
            } else {
                char addr_buf[INET_ADDRSTRLEN];
                peer_name = inet_ntop(AF_INET, &peer_address.sin_addr, addr_buf, sizeof(addr_buf));
            }
            outln("{:2}:  {:50}  {:4}ms", ttl, peer_name, response_time);

            if (response.header.type == ICMP_TIME_EXCEEDED)
                return 0;
            if (response.header.type == ICMP_ECHOREPLY)
                return 1;
        }
        return -1;
    };

    for (auto ttl = 1; ttl <= max_hops; ttl++) {
        TRY(Core::System::setsockopt(fd, IPPROTO_IP, IP_TTL, &ttl, sizeof(ttl)));

        int result = TRY(try_reach_host(ttl));
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

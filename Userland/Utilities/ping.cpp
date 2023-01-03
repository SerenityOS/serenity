/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/ByteBuffer.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <arpa/inet.h>
#include <errno.h>
#include <netdb.h>
#include <netinet/in.h>
#include <netinet/ip.h>
#include <netinet/ip_icmp.h>
#include <serenity.h>
#include <signal.h>
#include <stdio.h>
#include <string.h>
#include <sys/socket.h>
#include <sys/time.h>
#include <time.h>
#include <unistd.h>

static uint32_t total_pings;
static int successful_pings;
static Optional<size_t> count;
static uint32_t total_ms;
static int min_ms;
static int max_ms;
static char const* host;
static int payload_size = -1;
// variable part of header can be 0 to 40 bytes
// https://datatracker.ietf.org/doc/html/rfc791#section-3.1
static constexpr int max_optional_header_size_in_bytes = 40;
static constexpr int min_header_size_in_bytes = 5;

static void closing_statistics()
{
    int packet_loss = 100;

    outln();
    outln("--- {} ping statistics ---", host);

    if (total_pings)
        packet_loss -= 100.0f * successful_pings / total_pings;
    outln("{} packets transmitted, {} received, {}% packet loss",
        total_pings, successful_pings, packet_loss);

    int average_ms = 0;
    if (successful_pings)
        average_ms = total_ms / successful_pings;
    outln("rtt min/avg/max = {}/{}/{} ms", min_ms, average_ms, max_ms);

    exit(0);
};

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio id inet unix sigaction"));

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(host, "Host to ping", "host");
    args_parser.add_option(count, "Stop after sending specified number of ECHO_REQUEST packets.", "count", 'c', "count");
    args_parser.add_option(payload_size, "Amount of bytes to send as payload in the ECHO_REQUEST packets.", "size", 's', "size");
    args_parser.parse(arguments);

    if (count.has_value() && (count.value() < 1 || count.value() > UINT32_MAX)) {
        warnln("invalid count argument: '{}': out of range: 1 <= value <= {}", count.value(), UINT32_MAX);
        return 1;
    }

    if (payload_size < 0) {
        // Use the default.
        payload_size = 32 - sizeof(struct icmphdr);
    }

    int fd = TRY(Core::System::socket(AF_INET, SOCK_RAW, IPPROTO_ICMP));

    TRY(Core::System::drop_privileges());
    TRY(Core::System::pledge("stdio inet unix sigaction"));

    struct timeval timeout {
        1, 0
    };

    TRY(Core::System::setsockopt(fd, SOL_SOCKET, SO_RCVTIMEO, &timeout, sizeof(timeout)));

    auto* hostent = gethostbyname(host);
    if (!hostent) {
        warnln("Lookup failed for '{}'", host);
        return 1;
    }

    TRY(Core::System::pledge("stdio inet sigaction"));

    pid_t pid = getpid();

    sockaddr_in peer_address {};
    peer_address.sin_family = AF_INET;
    peer_address.sin_port = 0;
    peer_address.sin_addr.s_addr = *(in_addr_t const*)hostent->h_addr_list[0];

    uint16_t seq = 1;

    TRY(Core::System::signal(SIGINT, [](int) {
        closing_statistics();
    }));

    for (;;) {
        auto ping_packet_result = ByteBuffer::create_zeroed(sizeof(struct icmphdr) + payload_size);
        if (ping_packet_result.is_error()) {
            warnln("failed to allocate a large enough buffer for the ping packet");
            return 1;
        }
        auto& ping_packet = ping_packet_result.value();
        struct icmphdr* ping_hdr = reinterpret_cast<struct icmphdr*>(ping_packet.data());
        ping_hdr->type = ICMP_ECHO;
        ping_hdr->code = 0;
        ping_hdr->un.echo.id = htons(pid);
        ping_hdr->un.echo.sequence = htons(seq++);

        // Fill payload
        for (int i = 0; i < payload_size; i++) {
            ping_packet[i + sizeof(struct icmphdr)] = i & 0xFF;
        }

        ping_hdr->checksum = internet_checksum(ping_packet.data(), ping_packet.size());

        struct timeval tv_send;
        gettimeofday(&tv_send, nullptr);

        if (count.has_value() && total_pings == count.value())
            closing_statistics();
        else
            total_pings++;

        TRY(Core::System::sendto(fd, ping_packet.data(), ping_packet.size(), 0, (const struct sockaddr*)&peer_address, sizeof(sockaddr_in)));

        for (;;) {
            auto pong_packet_result = ByteBuffer::create_uninitialized(
                sizeof(struct ip) + max_optional_header_size_in_bytes + sizeof(struct icmphdr) + payload_size);
            if (pong_packet_result.is_error()) {
                warnln("failed to allocate a large enough buffer for the pong packet");
                return 1;
            }
            auto& pong_packet = pong_packet_result.value();
            socklen_t peer_address_size = sizeof(peer_address);
            auto result = Core::System::recvfrom(fd, pong_packet.data(), pong_packet.size(), 0, (struct sockaddr*)&peer_address, &peer_address_size);
            if (result.is_error()) {
                if (result.error().code() == EAGAIN) {
                    outln("Request (seq={}) timed out.", ntohs(ping_hdr->un.echo.sequence));
                    break;
                }
                return result.release_error();
            }

            i8 internet_header_length = *pong_packet.data() & 0x0F;
            if (internet_header_length < min_header_size_in_bytes) {
                outln("ping: illegal ihl field value {:x}", internet_header_length);
                continue;
            }

            struct icmphdr* pong_hdr = reinterpret_cast<struct icmphdr*>(pong_packet.data() + (internet_header_length * 4));
            if (pong_hdr->type != ICMP_ECHOREPLY)
                continue;
            if (pong_hdr->code != 0)
                continue;
            if (ntohs(pong_hdr->un.echo.id) != pid)
                continue;

            struct timeval tv_receive;
            gettimeofday(&tv_receive, nullptr);

            struct timeval tv_diff;
            timersub(&tv_receive, &tv_send, &tv_diff);

            int ms = tv_diff.tv_sec * 1000 + tv_diff.tv_usec / 1000;
            successful_pings++;
            int seq_dif = ntohs(ping_hdr->un.echo.sequence) - ntohs(pong_hdr->un.echo.sequence);

            // Approximation about the timeout of the out of order packet
            if (seq_dif)
                ms += seq_dif * 1000 * timeout.tv_sec;

            total_ms += ms;
            if (min_ms == 0)
                min_ms = max_ms = ms;
            else if (ms < min_ms)
                min_ms = ms;
            else if (ms > max_ms)
                max_ms = ms;

            char addr_buf[INET_ADDRSTRLEN];
            outln("Pong from {}: id={}, seq={}{}, time={}ms, size={}",
                inet_ntop(AF_INET, &peer_address.sin_addr, addr_buf, sizeof(addr_buf)),
                ntohs(pong_hdr->un.echo.id),
                ntohs(pong_hdr->un.echo.sequence),
                pong_hdr->un.echo.sequence != ping_hdr->un.echo.sequence ? "(!)" : "",
                ms, result.value());

            // If this was a response to an earlier packet, we still need to wait for the current one.
            if (pong_hdr->un.echo.sequence != ping_hdr->un.echo.sequence)
                continue;
            break;
        }

        sleep(1);
    }
}

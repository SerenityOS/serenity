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
static ByteString host;
static int payload_size = -1;
static bool adaptive = false;
static bool flood = false;
static bool quiet = false;
static Optional<size_t> ttl;
static timespec interval_timespec {
    .tv_sec = 1, .tv_nsec = 0
};
struct timeval timeout {
    .tv_sec = 1, .tv_usec = 0
};
// variable part of header can be 0 to 40 bytes
// https://datatracker.ietf.org/doc/html/rfc791#section-3.1
static constexpr int max_optional_header_size_in_bytes = 40;
static constexpr int min_header_size_in_bytes = 5;

static void print_closing_statistics()
{
    int packet_loss = 100;

    if (!quiet)
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
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio id inet unix sigaction"));

    auto parse_interval_string = [](StringView interval_in_seconds_string, time_t& whole_seconds, double& fractional_seconds) -> bool {
        if (interval_in_seconds_string.is_empty())
            return false;

        auto interval_in_seconds = interval_in_seconds_string.to_number<double>();
        if (!interval_in_seconds.has_value() || interval_in_seconds.value() < 0 || interval_in_seconds.value() > UINT32_MAX)
            return false;

        whole_seconds = static_cast<time_t>(interval_in_seconds.value());
        fractional_seconds = interval_in_seconds.value() - static_cast<double>(whole_seconds);

        return true;
    };
    bool user_specified_request_interval = false;
    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(host, "Host to ping", "host");
    args_parser.add_option(count, "Stop after sending specified number of ECHO_REQUEST packets", "count", 'c', "count");
    args_parser.add_option(Core::ArgsParser::Option {
        .argument_mode = Core::ArgsParser::OptionArgumentMode::Required,
        .help_string = "Wait `interval` seconds between sending each packet. Fractional seconds are allowed",
        .short_name = 'i',
        .value_name = "interval",
        .accept_value = [&parse_interval_string, &user_specified_request_interval](StringView interval_in_seconds_string) {
            time_t whole_seconds {};
            double fractional_seconds {};

            if (!parse_interval_string(interval_in_seconds_string, whole_seconds, fractional_seconds))
                return false;

            interval_timespec = {
                .tv_sec = whole_seconds,
                .tv_nsec = static_cast<long>(fractional_seconds * 1'000'000'000)
            };
            user_specified_request_interval = true;
            return true;
        },
    });
    args_parser.add_option(Core::ArgsParser::Option {
        .argument_mode = Core::ArgsParser::OptionArgumentMode::Required,
        .help_string = "Time to wait for response",
        .short_name
        = 'W',
        .value_name = "interval",
        .accept_value = [&parse_interval_string](StringView interval_in_seconds_string) {
            time_t whole_seconds {};
            double fractional_seconds {};

            if (!parse_interval_string(interval_in_seconds_string, whole_seconds, fractional_seconds))
                return false;

            timeout = {
                .tv_sec = whole_seconds,
                .tv_usec = static_cast<long>(fractional_seconds * 1'000'000)
            };

            return true;
        },
    });
    args_parser.add_option(payload_size, "Amount of bytes to send as payload in the ECHO_REQUEST packets", "size", 's', "size");
    args_parser.add_option(quiet, "Quiet mode. Only display summary when finished", "quiet", 'q');
    args_parser.add_option(ttl, "Set the TTL (time-to-live) value on the ICMP packets", nullptr, 't', "ttl");
    args_parser.add_option(adaptive, "Use adaptive ping", "adaptive", 'A');
    args_parser.add_option(flood, "Flood ping", "flood", 'f');
    args_parser.parse(arguments);

    if (count.has_value() && (count.value() < 1 || count.value() > UINT32_MAX)) {
        warnln("invalid count argument: '{}': out of range: 1 <= value <= {}", count.value(), UINT32_MAX);
        return 1;
    }

    if (ttl.has_value() && (ttl.value() < 1 || ttl.value() > 255)) {
        warnln("invalid TTL argument: '{}': out of range: 1 <= value <= 255", ttl.value());
        return 1;
    }

    if (payload_size < 0) {
        // Use the default.
        payload_size = 32 - sizeof(struct icmphdr);
    }

    int fd = TRY(Core::System::socket(AF_INET, SOCK_RAW, IPPROTO_ICMP));

    TRY(Core::System::drop_privileges());
    TRY(Core::System::pledge("stdio inet unix sigaction"));

    // Set the time to wait for each response
    TRY(Core::System::setsockopt(fd, SOL_SOCKET, SO_RCVTIMEO, &timeout, sizeof(timeout)));

    if (flood && !user_specified_request_interval) {
        interval_timespec = { .tv_sec = 0, .tv_nsec = 0 };
    }

    if (interval_timespec.tv_sec == 0 && interval_timespec.tv_nsec < 2'000'000 && geteuid() != 0) {
        warnln("Minimal interval for normal users is 0.2ms!");
        return 1;
    }

    if (ttl.has_value()) {
        TRY(Core::System::setsockopt(fd, IPPROTO_IP, IP_TTL, &ttl.value(), sizeof(ttl.value())));
    }

    auto* hostent = gethostbyname(host.characters());
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
        print_closing_statistics();
        exit(0);
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

        TRY(Core::System::sendto(fd, ping_packet.data(), ping_packet.size(), 0, (const struct sockaddr*)&peer_address, sizeof(sockaddr_in)));
        // In flood mode, for each request output a '.'
        if (flood)
            out(".");

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

            uint8_t const pong_ttl = pong_packet[8];
            if (result.is_error()) {
                if (result.error().code() == EAGAIN) {
                    if (!quiet && !flood)
                        outln("Request (seq={}) timed out.", ntohs(ping_hdr->un.echo.sequence));

                    break;
                }
                return result.release_error();
            }

            i8 internet_header_length = *pong_packet.data() & 0x0F;
            if (internet_header_length < min_header_size_in_bytes) {
                if (!quiet)
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
            if (adaptive && !flood)
                TIMEVAL_TO_TIMESPEC(&tv_diff, &interval_timespec);
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
            if (!quiet && !flood && seq_dif == 0)
                outln("Pong from {}: id={}, seq={}{}, ttl={}, time={}ms, size={}",
                    inet_ntop(AF_INET, &peer_address.sin_addr, addr_buf, sizeof(addr_buf)),
                    ntohs(pong_hdr->un.echo.id),
                    ntohs(pong_hdr->un.echo.sequence),
                    pong_hdr->un.echo.sequence != ping_hdr->un.echo.sequence ? "(!)" : "",
                    pong_ttl,
                    ms, result.value());

            // If this was a response to an earlier packet, we still need to wait for the current one.
            if (pong_hdr->un.echo.sequence != ping_hdr->un.echo.sequence)
                continue;

            // In flood mode, for each response we print a backspace
            if (flood)
                out("{}", "\b ");

            break;
        }

        total_pings++;
        if (count.has_value() && total_pings == count.value()) {
            print_closing_statistics();
            break;
        }

        clock_nanosleep(CLOCK_MONOTONIC, 0, &interval_timespec, nullptr);
    }

    return 0;
}

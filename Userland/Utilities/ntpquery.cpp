/*
 * Copyright (c) 2020, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#define _BSD_SOURCE
#define _DEFAULT_SOURCE
#include <AK/Assertions.h>
#include <AK/Endian.h>
#include <AK/Random.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <arpa/inet.h>
#include <inttypes.h>
#include <math.h>
#include <netdb.h>
#include <netinet/in.h>
#include <stdio.h>
#include <string.h>
#include <sys/socket.h>
#include <sys/time.h>
#include <sys/uio.h>
#include <time.h>

// An NtpTimestamp is a 64-bit integer that's a 32.32 binary-fixed point number.
// The integral part in the upper 32 bits represents seconds since 1900-01-01.
// The fractional part in the lower 32 bits stores fractional bits times 2 ** 32.
using NtpTimestamp = uint64_t;

using AK::convert_between_host_and_network_endian;

struct [[gnu::packed]] NtpPacket {
    uint8_t li_vn_mode;
    uint8_t stratum;
    int8_t poll;
    int8_t precision;

    uint32_t root_delay;
    uint32_t root_dispersion;
    uint32_t reference_id;

    NtpTimestamp reference_timestamp;
    NtpTimestamp origin_timestamp;
    NtpTimestamp receive_timestamp;
    NtpTimestamp transmit_timestamp;

    uint8_t leap_information() const { return li_vn_mode >> 6; }
    uint8_t version_number() const { return (li_vn_mode >> 3) & 7; }
    uint8_t mode() const { return li_vn_mode & 7; }
};
static_assert(AssertSize<NtpPacket, 48>());

// NTP measures time in seconds since 1900-01-01, POSIX in seconds since 1970-01-01.
// 1900 wasn't a leap year, so there are 70/4 leap years between 1900 and 1970.
// Overflows a 32-bit signed int, but not a 32-bit unsigned int.
unsigned const SecondsFrom1900To1970 = (70u * 365u + 70u / 4u) * 24u * 60u * 60u;

static NtpTimestamp ntp_timestamp_from_timeval(timeval const& t)
{
    VERIFY(t.tv_usec >= 0 && t.tv_usec < 1'000'000); // Fits in 20 bits when normalized.

    // Seconds just need translation to the different origin.
    uint32_t seconds = t.tv_sec + SecondsFrom1900To1970;

    // Fractional bits are decimal fixed point (*1'000'000) in timeval, but binary fixed-point (* 2**32) in NTP timestamps.
    uint32_t fractional_bits = static_cast<uint32_t>((static_cast<uint64_t>(t.tv_usec) << 32) / 1'000'000);

    return (static_cast<NtpTimestamp>(seconds) << 32) | fractional_bits;
}

static timeval timeval_from_ntp_timestamp(NtpTimestamp const& ntp_timestamp)
{
    timeval t;
    t.tv_sec = static_cast<time_t>(ntp_timestamp >> 32) - SecondsFrom1900To1970;
    t.tv_usec = static_cast<suseconds_t>((static_cast<uint64_t>(ntp_timestamp & 0xFFFFFFFFu) * 1'000'000) >> 32);
    return t;
}

static ByteString format_ntp_timestamp(NtpTimestamp ntp_timestamp)
{
    char buffer[28]; // YYYY-MM-DDTHH:MM:SS.UUUUUUZ is 27 characters long.
    timeval t = timeval_from_ntp_timestamp(ntp_timestamp);
    struct tm tm;
    gmtime_r(&t.tv_sec, &tm);
    size_t written = strftime(buffer, sizeof(buffer), "%Y-%m-%dT%T.", &tm);
    VERIFY(written == 20);
    written += snprintf(buffer + written, sizeof(buffer) - written, "%06d", (int)t.tv_usec);
    VERIFY(written == 26);
    buffer[written++] = 'Z';
    buffer[written] = '\0';
    return buffer;
}
ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio inet unix settime wpath rpath"));

    bool adjust_time = false;
    bool set_time = false;
    bool verbose = false;
    // FIXME: Change to serenityos.pool.ntp.org once https://manage.ntppool.org/manage/vendor/zone?a=km5a8h&id=vz-14154g is approved.
    // Other NTP servers:
    // - time.nist.gov
    // - time.apple.com
    // - time.cloudflare.com (has NTS), https://blog.cloudflare.com/secure-time/
    // - time.windows.com
    //
    // Leap seconds smearing NTP servers:
    // - time.facebook.com , https://engineering.fb.com/production-engineering/ntp-service/ , sine-smears over 18 hours
    // - time.google.com , https://developers.google.com/time/smear , linear-smears over 24 hours
    ByteString host = "time.google.com"sv;
    Core::ArgsParser args_parser;
    args_parser.add_option(adjust_time, "Gradually adjust system time (requires root)", "adjust", 'a');
    args_parser.add_option(set_time, "Immediately set system time (requires root)", "set", 's');
    args_parser.add_option(verbose, "Verbose output", "verbose", 'v');
    args_parser.add_positional_argument(host, "NTP server", "host", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    TRY(Core::System::unveil("/tmp/portal/lookup", "rw"));
    TRY(Core::System::unveil("/etc/timezone", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    if (adjust_time && set_time) {
        warnln("-a and -s are mutually exclusive");
        return 1;
    }

    if (!adjust_time && !set_time) {
        TRY(Core::System::pledge("stdio inet unix rpath"));
    }

    auto* hostent = gethostbyname(host.characters());
    if (!hostent) {
        warnln("Lookup failed for '{}'", host);
        return 1;
    }

    TRY(Core::System::pledge((adjust_time || set_time) ? "stdio inet settime wpath rpath"sv : "stdio inet rpath"sv));

    int fd = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP);
    if (fd < 0) {
        perror("socket");
        return 1;
    }

    struct timeval timeout {
        5, 0
    };
    if (setsockopt(fd, SOL_SOCKET, SO_RCVTIMEO, &timeout, sizeof(timeout)) < 0) {
        perror("setsockopt(SO_RCVTIMEO)");
        warnln("Continuing without a timeout");
    }

#ifdef SO_TIMESTAMP
    int enable = 1;
    if (setsockopt(fd, SOL_SOCKET, SO_TIMESTAMP, &enable, sizeof(enable)) < 0) {
        perror("setsockopt");
        return 1;
    }
#endif

    sockaddr_in peer_address;
    memset(&peer_address, 0, sizeof(peer_address));
    peer_address.sin_family = AF_INET;
    peer_address.sin_port = convert_between_host_and_network_endian<short>(123);
    peer_address.sin_addr.s_addr = *(in_addr_t const*)hostent->h_addr_list[0];

    NtpPacket packet;
    memset(&packet, 0, sizeof(packet));
    packet.li_vn_mode = (4 << 3) | 3; // Version 4, client connection.

    // The server will copy the transmit_timestamp to origin_timestamp in the reply.
    // To not leak the local time, keep the time we sent the packet locally and
    // send random bytes to the server.
    auto random_transmit_timestamp = get_random<NtpTimestamp>();
    timeval local_transmit_time;
    gettimeofday(&local_transmit_time, nullptr);
    packet.transmit_timestamp = random_transmit_timestamp;

    ssize_t rc;
    rc = sendto(fd, &packet, sizeof(packet), 0, (const struct sockaddr*)&peer_address, sizeof(peer_address));
    if (rc < 0) {
        perror("sendto");
        return 1;
    }
    if ((size_t)rc < sizeof(packet)) {
        warnln("incomplete packet send");
        return 1;
    }

    iovec iov { &packet, sizeof(packet) };
    char control_message_buffer[CMSG_SPACE(sizeof(timeval))];
    msghdr msg = {};
    msg.msg_name = &peer_address;
    msg.msg_namelen = sizeof(peer_address);
    msg.msg_iov = &iov;
    msg.msg_iovlen = 1;
    msg.msg_control = control_message_buffer;
    msg.msg_controllen = sizeof(control_message_buffer);
    msg.msg_flags = 0;

    rc = recvmsg(fd, &msg, 0);
    if (rc < 0) {
        perror("recvmsg");
        return 1;
    }
    timeval userspace_receive_time;
    gettimeofday(&userspace_receive_time, nullptr);
    if ((size_t)rc < sizeof(packet)) {
        warnln("incomplete packet recv");
        return 1;
    }

#ifdef SO_TIMESTAMP
    cmsghdr* cmsg = CMSG_FIRSTHDR(&msg);
    VERIFY(cmsg->cmsg_level == SOL_SOCKET);
    VERIFY(cmsg->cmsg_type == SCM_TIMESTAMP);
    VERIFY(!CMSG_NXTHDR(&msg, cmsg));
    timeval kernel_receive_time;
    memcpy(&kernel_receive_time, CMSG_DATA(cmsg), sizeof(kernel_receive_time));
#endif

    // Checks 3 and 4 from end of section 5 of rfc4330.
    if (packet.version_number() != 3 && packet.version_number() != 4) {
        warnln("unexpected version number {}", packet.version_number());
        return 1;
    }
    if (packet.mode() != 4) { // 4 means "server", which should be the reply to our 3 ("client") request.
        warnln("unexpected mode {}", packet.mode());
        return 1;
    }
    if (packet.stratum == 0 || packet.stratum >= 16) {
        warnln("unexpected stratum value {}", packet.stratum);
        return 1;
    }
    if (packet.origin_timestamp != random_transmit_timestamp) {
        warnln("expected {:#016x} as origin timestamp, got {:#016x}", random_transmit_timestamp, packet.origin_timestamp);
        return 1;
    }
    if (packet.transmit_timestamp == 0) {
        warnln("got transmit_timestamp 0");
        return 1;
    }

    NtpTimestamp origin_timestamp = ntp_timestamp_from_timeval(local_transmit_time);
    NtpTimestamp receive_timestamp = convert_between_host_and_network_endian(packet.receive_timestamp);
    NtpTimestamp transmit_timestamp = convert_between_host_and_network_endian(packet.transmit_timestamp);
#ifdef SO_TIMESTAMP
    NtpTimestamp destination_timestamp = ntp_timestamp_from_timeval(kernel_receive_time);

    timeval kernel_to_userspace_latency;
    timersub(&userspace_receive_time, &kernel_receive_time, &kernel_to_userspace_latency);
#else
    NtpTimestamp destination_timestamp = ntp_timestamp_from_timeval(userspace_receive_time);
#endif

    if (set_time) {
        // FIXME: Do all the time filtering described in 5905, or at least correct for time of flight.
        timeval t = timeval_from_ntp_timestamp(transmit_timestamp);
        if (settimeofday(&t, nullptr) < 0) {
            perror("settimeofday");
            return 1;
        }
    }

    if (verbose) {
        outln("NTP response from {}:", inet_ntoa(peer_address.sin_addr));
        outln("Leap Information: {}", packet.leap_information());
        outln("Version Number: {}", packet.version_number());
        outln("Mode: {}", packet.mode());
        outln("Stratum: {}", packet.stratum);
        outln("Poll: {}", packet.stratum);
        outln("Precision: {}", packet.precision);
        outln("Root delay: {:x}", convert_between_host_and_network_endian(packet.root_delay));
        outln("Root dispersion: {:x}", convert_between_host_and_network_endian(packet.root_dispersion));

        u32 ref_id = convert_between_host_and_network_endian(packet.reference_id);
        out("Reference ID: {:x}", ref_id);
        if (packet.stratum == 1) {
            out(" ('{:c}{:c}{:c}{:c}')", (ref_id & 0xff000000) >> 24, (ref_id & 0xff0000) >> 16, (ref_id & 0xff00) >> 8, ref_id & 0xff);
        }
        outln();

        outln("Reference timestamp:   {:#016x} ({})", convert_between_host_and_network_endian(packet.reference_timestamp), format_ntp_timestamp(convert_between_host_and_network_endian(packet.reference_timestamp)).characters());
        outln("Origin timestamp:      {:#016x} ({})", origin_timestamp, format_ntp_timestamp(origin_timestamp).characters());
        outln("Receive timestamp:     {:#016x} ({})", receive_timestamp, format_ntp_timestamp(receive_timestamp).characters());
        outln("Transmit timestamp:    {:#016x} ({})", transmit_timestamp, format_ntp_timestamp(transmit_timestamp).characters());
        outln("Destination timestamp: {:#016x} ({})", destination_timestamp, format_ntp_timestamp(destination_timestamp).characters());

#ifdef SO_TIMESTAMP
        // When the system isn't under load, user-space t and packet_t are identical. If a shell with `yes` is running, it can be as high as 30ms in this program,
        // which gets user-space time immediately after the recvmsg() call. In programs that have an event loop reading from multiple sockets, it could be higher.
        outln("Receive latency: {}.{:06} s", (i64)kernel_to_userspace_latency.tv_sec, (int)kernel_to_userspace_latency.tv_usec);
#endif
    }

    // Parts of the "Clock Filter" computations, https://tools.ietf.org/html/rfc5905#section-10
    NtpTimestamp T1 = origin_timestamp;
    NtpTimestamp T2 = receive_timestamp;
    NtpTimestamp T3 = transmit_timestamp;
    NtpTimestamp T4 = destination_timestamp;
    auto timestamp_difference_in_seconds = [](NtpTimestamp from, NtpTimestamp to) {
        return static_cast<i64>(to - from) >> 32;
    };

    // The network round-trip time of the request.
    // T4-T1 is the wall clock roundtrip time, in local ticks.
    // T3-T2 is the server side processing time, in server ticks.
    double delay_s = timestamp_difference_in_seconds(T1, T4) - timestamp_difference_in_seconds(T2, T3);

    // The offset from local time to server time, ignoring network delay.
    // Both T2-T1 and T3-T4 estimate this; this takes the average of both.
    // Or, equivalently, (T1+T4)/2 estimates local time, (T2+T3)/2 estimate server time, this is the difference.
    double offset_s = 0.5 * (timestamp_difference_in_seconds(T1, T2) + timestamp_difference_in_seconds(T4, T3));
    if (verbose)
        outln("Delay: {}", delay_s);
    outln("Offset: {}", offset_s);

    if (adjust_time) {
        long delta_us = lround(offset_s * 1'000'000);
        timeval delta_timeval;
        delta_timeval.tv_sec = delta_us / 1'000'000;
        delta_timeval.tv_usec = delta_us % 1'000'000;
        if (delta_timeval.tv_usec < 0) {
            delta_timeval.tv_sec--;
            delta_timeval.tv_usec += 1'000'000;
        }
        if (adjtime(&delta_timeval, nullptr) < 0) {
            perror("adjtime set");
            return 1;
        }
    }

    return 0;
}

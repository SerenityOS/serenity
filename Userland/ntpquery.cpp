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

// An NtpTimestamp is a 64-bit integer that's a 32.32 binary-fixed point number.
// The integral part in the upper 32 bits represents seconds since 1900-01-01.
// The fractional part in the lower 32 bits stores fractional bits times 2 ** 32.
typedef uint64_t NtpTimestamp;

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
};
static_assert(sizeof(NtpPacket) == 48);

// NTP measures time in seconds since 1900-01-01, POSIX in seconds since 1970-01-01.
// 1900 wasn't a leap year, so there are 70/4 leap years between 1900 and 1970.
// Overflows a 32-bit signed int, but not a 32-bit unsigned int.
const unsigned SecondsFrom1900To1970 = (70u * 365u + 70u / 4u) * 24u * 60u * 60u;

static NtpTimestamp ntp_timestamp_from_timeval(const timeval& t)
{
    ASSERT(t.tv_usec >= 0 && t.tv_usec < 1'000'000); // Fits in 20 bits when normalized.

    // Seconds just need translation to the different origin.
    uint32_t seconds = t.tv_sec + SecondsFrom1900To1970;

    // Fractional bits are decimal fixed point (*1'000'000) in timeval, but binary fixed-point (* 2**32) in NTP timestamps.
    uint32_t fractional_bits = static_cast<uint32_t>((static_cast<uint64_t>(t.tv_usec) << 32) / 1'000'000);

    return (static_cast<NtpTimestamp>(seconds) << 32) | fractional_bits;
}

static timeval timeval_from_ntp_timestamp(const NtpTimestamp& ntp_timestamp)
{
    timeval t;
    t.tv_sec = static_cast<time_t>(ntp_timestamp >> 32) - SecondsFrom1900To1970;
    t.tv_usec = static_cast<suseconds_t>((static_cast<uint64_t>(ntp_timestamp & 0xFFFFFFFFu) * 1'000'000) >> 32);
    return t;
}

static String format_ntp_timestamp(NtpTimestamp ntp_timestamp)
{
    char buffer[28]; // YYYY-MM-DDTHH:MM:SS.UUUUUUZ is 27 characters long.
    timeval t = timeval_from_ntp_timestamp(ntp_timestamp);
    struct tm tm;
    gmtime_r(&t.tv_sec, &tm);
    size_t written = strftime(buffer, sizeof(buffer), "%Y-%m-%dT%T.", &tm);
    ASSERT(written == 20);
    written += snprintf(buffer + written, sizeof(buffer) - written, "%06d", t.tv_usec);
    ASSERT(written == 26);
    buffer[written++] = 'Z';
    buffer[written] = '\0';
    return buffer;
}

int main(int argc, char** argv)
{
    if (pledge("stdio inet dns", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    // FIXME: Change to serenityos.pool.ntp.org once https://manage.ntppool.org/manage/vendor/zone?a=km5a8h&id=vz-14154g is approved.
    const char* host = "time.google.com";
    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(host, "NTP server", "host", Core::ArgsParser::Required::No);
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
    gettimeofday(&t, nullptr);
    if (rc < 0) {
        perror("recvfrom");
        return 1;
    }
    if ((size_t)rc < sizeof(packet)) {
        fprintf(stderr, "incomplete packet recv\n");
        return 1;
    }

    NtpTimestamp origin_timestamp = be64toh(packet.origin_timestamp);
    NtpTimestamp receive_timestamp = be64toh(packet.receive_timestamp);
    NtpTimestamp transmit_timestamp = be64toh(packet.transmit_timestamp);
    NtpTimestamp destination_timestamp = ntp_timestamp_from_timeval(t);

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
    printf("Reference timestamp:   %#016llx (%s)\n", be64toh(packet.reference_timestamp), format_ntp_timestamp(be64toh(packet.reference_timestamp)).characters());
    printf("Origin timestamp:      %#016llx (%s)\n", origin_timestamp, format_ntp_timestamp(origin_timestamp).characters());
    printf("Receive timestamp:     %#016llx (%s)\n", receive_timestamp, format_ntp_timestamp(receive_timestamp).characters());
    printf("Transmit timestamp:    %#016llx (%s)\n", transmit_timestamp, format_ntp_timestamp(transmit_timestamp).characters());
    printf("Destination timestamp: %#016llx (%s)\n", destination_timestamp, format_ntp_timestamp(destination_timestamp).characters());
}

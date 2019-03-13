#include <sys/socket.h>
#include <arpa/inet.h>
#include <netinet/ip_icmp.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <time.h>

uint16_t internet_checksum(const void* ptr, size_t count)
{
    uint32_t  checksum = 0;
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

inline void timersub(struct timeval* a, struct timeval* b, struct timeval* result)
{
    result->tv_sec = a->tv_sec - b->tv_sec;
    result->tv_usec = a->tv_usec - b->tv_usec;
    if (result->tv_usec < 0) {
        --result->tv_sec;
        result->tv_usec += 1000000;
    }
}

int main(int argc, char** argv)
{
    int fd = socket(AF_INET, SOCK_RAW, IPPROTO_ICMP);
    if (fd < 0) {
        perror("socket");
        return 1;
    }

    const char* addr_str = "192.168.5.1";
    if (argc > 1)
        addr_str = argv[1];

    pid_t pid = getpid();

    sockaddr_in peer_address;
    memset(&peer_address, 0, sizeof(peer_address));
    peer_address.sin_family = AF_INET;
    peer_address.sin_port = 0;

    int rc = inet_pton(AF_INET, addr_str, &peer_address.sin_addr);

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
        strcpy(ping_packet.msg, "Hello there!\n");

        ping_packet.header.checksum = internet_checksum(&ping_packet, sizeof(PingPacket));

        struct timeval tv_send;
        gettimeofday(&tv_send, nullptr);

        rc = sendto(fd, &ping_packet, sizeof(PingPacket), 0, (const struct sockaddr*)&peer_address, sizeof(sockaddr_in));
        if (rc < 0) {
            perror("sendto");
            return 1;
        }

        for (;;) {
            rc = recvfrom(fd, &pong_packet, sizeof(PingPacket), 0, (const struct sockaddr*)&peer_address, sizeof(sockaddr_in));
            if (rc < 0) {
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
            printf("Pong from %s: id=%u, seq=%u, time=%dms\n",
                inet_ntop(AF_INET, &peer_address.sin_addr, addr_buf, sizeof(addr_buf)),
                ntohs(pong_packet.header.un.echo.id),
                ntohs(pong_packet.header.un.echo.sequence),
                ms
            );
            break;
        }

        sleep(1);
    }

    return 0;
}

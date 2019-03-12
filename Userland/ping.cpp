#include <sys/socket.h>
#include <arpa/inet.h>
#include <netinet/ip_icmp.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <Kernel/NetworkOrdered.h>

NetworkOrdered<word> internet_checksum(const void* ptr, size_t count)
{
    dword checksum = 0;
    auto* w = (const word*)ptr;
    while (count > 1) {
        checksum += convert_between_host_and_network(*w++);
        if (checksum & 0x80000000)
            checksum = (checksum & 0xffff) | (checksum >> 16);
        count -= 2;
    }
    while (checksum >> 16)
        checksum = (checksum & 0xffff) + (checksum >> 16);
    return ~checksum & 0xffff;
}


int main(int argc, char** argv)
{
    int fd = socket(AF_INET, SOCK_RAW, IPPROTO_ICMP);
    if (fd < 0) {
        perror("socket");
        return 1;
    }

    sockaddr_in peer_address;
    memset(&peer_address, 0, sizeof(peer_address));
    peer_address.sin_family = AF_INET;
    peer_address.sin_port = 0;
    peer_address.sin_addr.s_addr = 0x0105a8c0; // 192.168.5.1

    struct PingPacket {
        struct icmphdr header;
        char msg[64 - sizeof(struct icmphdr)];
    };

    PingPacket ping_packet;
    PingPacket pong_packet;
    memset(&ping_packet, 0, sizeof(PingPacket));

    ping_packet.header.type = 8; // Echo request
    ping_packet.header.code = 0;
    ping_packet.header.un.echo.id = htons(getpid());
    ping_packet.header.un.echo.sequence = htons(1);
    strcpy(ping_packet.msg, "Hello there!\n");

    ping_packet.header.checksum = htons(internet_checksum(&ping_packet, sizeof(PingPacket)));

    int rc = sendto(fd, &ping_packet, sizeof(PingPacket), 0, (const struct sockaddr*)&peer_address, sizeof(sockaddr_in));
    if (rc < 0) {
        perror("sendto");
        return 1;
    }

    rc = recvfrom(fd, &pong_packet, sizeof(PingPacket), 0, (const struct sockaddr*)&peer_address, sizeof(sockaddr_in));
    if (rc < 0) {
        perror("recvfrom");
        return 1;
    }

    printf("received %p (%d)\n", &pong_packet, rc);

    return 0;
}

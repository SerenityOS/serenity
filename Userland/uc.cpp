#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <netinet/in.h>

int main(int argc, char** argv)
{
    (void)argc;
    (void)argv;

    int fd = socket(AF_INET, SOCK_DGRAM, 0);
    if (fd < 0) {
        perror("socket");
        return 1;
    } 

    struct timeval timeout { 5, 0 };
    int rc = setsockopt(fd, SOL_SOCKET, SO_RCVTIMEO, &timeout, sizeof(timeout));
    if (rc < 0) {
        perror("setsockopt");
        return 1;
    }

    struct sockaddr_in dst_addr;
    memset(&dst_addr, 0, sizeof(dst_addr));

    dst_addr.sin_family = AF_INET;
    dst_addr.sin_port = htons(8080);
    dst_addr.sin_addr.s_addr = INADDR_ANY;

    char buffer[BUFSIZ];
    const char* msg = "Test message";

    sendto(fd, (const char *)msg, strlen(msg), 0,(const struct sockaddr *)&dst_addr, sizeof(dst_addr));
    printf("Message sent.\n");

    struct sockaddr_in src_addr;
    socklen_t src_addr_len = sizeof(src_addr);
    ssize_t nrecv = recvfrom(fd, (char *)buffer, sizeof(buffer), 0, (struct sockaddr*)&src_addr, &src_addr_len);
    if (nrecv < 0) {
        perror("recvfrom");
        return 1;
    }
    buffer[nrecv] = '\0';
    printf("Server: %s\n", buffer);

    close(fd);
    return 0;
}

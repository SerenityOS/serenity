#include <arpa/inet.h>
#include <netinet/in.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <sys/time.h>
#include <sys/types.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    const char* addr_str = "127.0.0.1";
    if (argc > 1)
        addr_str = argv[1];

    int fd = socket(AF_INET, SOCK_STREAM, 0);
    if (fd < 0) {
        perror("socket");
        return 1;
    } 

    struct timeval timeout { 3, 0 };
    int rc = setsockopt(fd, SOL_SOCKET, SO_RCVTIMEO, &timeout, sizeof(timeout));
    if (rc < 0) {
        perror("setsockopt");
        return 1;
    }
    rc = setsockopt(fd, SOL_SOCKET, SO_SNDTIMEO, &timeout, sizeof(timeout));
    if (rc < 0) {
        perror("setsockopt");
        return 1;
    }

    struct sockaddr_in dst_addr;
    memset(&dst_addr, 0, sizeof(dst_addr));

    dst_addr.sin_family = AF_INET;
    dst_addr.sin_port = htons(80);
    rc = inet_pton(AF_INET, addr_str, &dst_addr.sin_addr);
    if (rc <= 0) {
        perror("inet_pton");
        return 1;
    }

    printf("Connecting to %s...", addr_str);
    fflush(stdout);
    rc = connect(fd, (struct sockaddr*)&dst_addr, sizeof(dst_addr));
    if (rc < 0) {
        perror("connect");
        return 1;
    }
    printf("ok!\n");

    char buffer[BUFSIZ];
    const char* msg = "GET / HTTP/1.0\r\n\r\n";

    printf("Sending a greeting...");
    rc = send(fd, (const char*)msg, strlen(msg), 0);
    if (rc < 0) {
        perror("send");
        return 1;
    }
    printf("ok!\n");

    printf("Waiting for response...");
    size_t total_recv = 0;
    for (;;) {
        ssize_t nrecv = recv(fd, buffer, sizeof(buffer), 0);
        if (nrecv < 0) {
            perror("recvfrom");
            return 1;
        }
        if (nrecv == 0)
            break;
        total_recv += nrecv;
        buffer[nrecv] = '\0';
        printf("\033[36;1m%s\033[0m", buffer);
    }

    printf("(%u bytes received)\n", total_recv);
    rc = close(fd);
    if (rc < 0) {
        perror("close");
        return 1;
    }
    return 0;
}

#include <arpa/inet.h>
#include <netinet/in.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/select.h>
#include <sys/socket.h>
#include <sys/time.h>
#include <sys/types.h>
#include <unistd.h>

#define max(a, b) ((a > b) ? a : b)

int main(int argc, char** argv)
{
    if (argc < 3) {
        fprintf(stderr, "Usage: nc <host> <port>\n");
        return -1;
    }

    const char* addr_str = argv[1];
    int port = atoi(argv[2]);

    int fd = socket(AF_INET, SOCK_STREAM, 0);
    if (fd < 0) {
        perror("socket");
        return 1;
    }

    struct timeval timeout {
        3, 0
    };
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
    dst_addr.sin_port = htons(port);
    rc = inet_pton(AF_INET, addr_str, &dst_addr.sin_addr);
    if (rc <= 0) {
        perror("inet_pton");
        return 1;
    }

    rc = connect(fd, (struct sockaddr*)&dst_addr, sizeof(dst_addr));
    if (rc < 0) {
        perror("connect");
        return 1;
    }

    fd_set readfds, writefds, exceptfds;

    for (;;) {
        FD_ZERO(&readfds);
        FD_ZERO(&writefds);
        FD_ZERO(&exceptfds);

        int highest_fd = 0;

        FD_SET(STDIN_FILENO, &readfds);
        FD_SET(STDIN_FILENO, &exceptfds);
        highest_fd = max(highest_fd, STDIN_FILENO);
        FD_SET(fd, &readfds);
        FD_SET(fd, &exceptfds);
        highest_fd = max(highest_fd, fd);

        int ready = select(highest_fd + 1, &readfds, &writefds, &exceptfds, NULL);
        if (ready == -1) {
            if (errno == EINTR)
                continue;

            perror("select");
            return 1;
        }

        if (FD_ISSET(STDIN_FILENO, &readfds)) {
            char buf[1024];
            int nread = read(STDIN_FILENO, buf, sizeof(buf));
            if (nread < 0) {
                perror("read(STDIN_FILENO)");
                return 1;
            }

            // stdin closed
            if (nread == 0) {
                return 0;
            }

            if (write(fd, buf, nread) < 0) {
                perror("write(fd)");
                return 1;
            }
        }

        if (FD_ISSET(fd, &readfds)) {
            char buf[1024];
            int nread = read(fd, buf, sizeof(buf));
            if (nread < 0) {
                perror("read(fd)");
                return 1;
            }

            // remote end closed
            if (nread == 0) {
                return 0;
            }

            if (write(STDOUT_FILENO, buf, nread) < 0) {
                perror("write(STDOUT_FILENO)");
                return 1;
            }
        }
    }

    return 0;
}

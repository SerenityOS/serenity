#pragma once

#include <bits/stdint.h>
#include <sys/cdefs.h>
#include <sys/types.h>
#include <sys/un.h>

__BEGIN_DECLS

#define AF_MASK 0xff
#define AF_UNSPEC 0
#define AF_LOCAL 1
#define AF_UNIX AF_LOCAL
#define AF_INET 2
#define PF_LOCAL AF_LOCAL
#define PF_UNIX PF_LOCAL
#define PF_INET AF_INET

#define SOCK_TYPE_MASK 0xff
#define SOCK_STREAM 1
#define SOCK_DGRAM 2
#define SOCK_RAW 3
#define SOCK_NONBLOCK 04000
#define SOCK_CLOEXEC 02000000

#define IPPROTO_IP 0
#define IPPROTO_ICMP 1
#define IPPROTO_TCP 6
#define IPPROTO_UDP 17

#define MSG_DONTWAIT 0x40

struct sockaddr {
    uint16_t sa_family;
    char sa_data[14];
};

struct in_addr {
    uint32_t s_addr;
};

struct sockaddr_in {
    uint16_t sin_family;
    uint16_t sin_port;
    struct in_addr sin_addr;
    char sin_zero[8];
};

struct ucred {
    pid_t pid;
    uid_t uid;
    gid_t gid;
};

#define SOL_SOCKET 1
#define SOMAXCONN 128

#define SO_RCVTIMEO 1
#define SO_SNDTIMEO 2
#define SO_KEEPALIVE 3
#define SO_ERROR 4
#define SO_PEERCRED 5

int socket(int domain, int type, int protocol);
int bind(int sockfd, const struct sockaddr* addr, socklen_t);
int listen(int sockfd, int backlog);
int accept(int sockfd, struct sockaddr*, socklen_t*);
int connect(int sockfd, const struct sockaddr*, socklen_t);
ssize_t send(int sockfd, const void*, size_t, int flags);
ssize_t sendto(int sockfd, const void*, size_t, int flags, const struct sockaddr*, socklen_t);
ssize_t recv(int sockfd, void*, size_t, int flags);
ssize_t recvfrom(int sockfd, void*, size_t, int flags, struct sockaddr*, socklen_t*);
int getsockopt(int sockfd, int level, int option, void*, socklen_t*);
int setsockopt(int sockfd, int level, int option, const void*, socklen_t);
int getsockname(int sockfd, struct sockaddr*, socklen_t*);
int getpeername(int sockfd, struct sockaddr*, socklen_t*);

__END_DECLS

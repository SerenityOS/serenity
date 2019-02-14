#pragma once

#include <sys/cdefs.h>
#include <sys/types.h>

__BEGIN_DECLS

#define AF_MASK 0xff
#define AF_UNSPEC 0
#define AF_LOCAL 1

#define SOCK_TYPE_MASK 0xff
#define SOCK_STREAM 1
#define SOCK_NONBLOCK 04000
#define SOCK_CLOEXEC 02000000

struct sockaddr {
    unsigned short sa_family;
    char sa_data[14];
};

#define UNIX_PATH_MAX 108
struct sockaddr_un {
    unsigned short sun_family;
    char sun_path[UNIX_PATH_MAX];
};

int socket(int domain, int type, int protocol);
int bind(int sockfd, const sockaddr* addr, socklen_t);
int listen(int sockfd, int backlog);
int accept(int sockfd, sockaddr*, socklen_t*);
int connect(int sockfd, const sockaddr*, socklen_t);

__END_DECLS


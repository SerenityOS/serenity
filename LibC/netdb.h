#pragma once

#include <sys/cdefs.h>
#include <sys/types.h>

__BEGIN_DECLS

struct hostent {
    char* h_name;
    char** h_aliases;
    int h_addrtype;
    int h_length;
    char** h_addr_list;
#define h_addr h_addr_list[0]
};

struct hostent* gethostbyname(const char*);
struct hostent* gethostbyaddr(const void* addr, socklen_t len, int type);

struct servent {
    char* s_name;
    char** s_aliases;
    int s_port;
    char* s_proto;
};

struct servent* getservbyname(const char* name, const char* protocol);

__END_DECLS

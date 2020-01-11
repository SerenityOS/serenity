#include <arpa/inet.h>
#include <netdb.h>
#include <netinet/in.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio dns", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (argc < 2) {
        printf("usage: host <hostname>\n");
        return 0;
    }

    // If input looks like an IPv4 address, we should do a reverse lookup.
    struct sockaddr_in addr;
    memset(&addr, 0, sizeof(addr));
    addr.sin_family = AF_INET;
    addr.sin_port = htons(53);
    int rc = inet_pton(AF_INET, argv[1], &addr.sin_addr);
    if (rc == 1) {
        // Okay, let's do a reverse lookup.
        auto* hostent = gethostbyaddr(&addr.sin_addr, sizeof(in_addr), AF_INET);
        if (!hostent) {
            fprintf(stderr, "Reverse lookup failed for '%s'\n", argv[1]);
            return 1;
        }
        printf("%s is %s\n", argv[1], hostent->h_name);
        return 0;
    }

    auto* hostent = gethostbyname(argv[1]);
    if (!hostent) {
        fprintf(stderr, "Lookup failed for '%s'\n", argv[1]);
        return 1;
    }

    char buffer[32];
    const char* ip_str = inet_ntop(AF_INET, hostent->h_addr_list[0], buffer, sizeof(buffer));

    printf("%s is %s\n", argv[1], ip_str);
    return 0;
}

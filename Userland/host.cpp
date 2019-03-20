#include <netdb.h>
#include <arpa/inet.h>
#include <netinet/in.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    if (argc < 2) {
        printf("usage: host <hostname>\n");
        return 0;
    }

    auto* hostent = gethostbyname(argv[1]);
    if (!hostent) {
        printf("Lookup failed for '%s'\n", argv[1]);
        return 1;
    }

    char buffer[32];
    const char* ip_str = inet_ntop(AF_INET, hostent->h_addr_list[0], buffer, sizeof(buffer));

    printf("%s is %s\n", argv[1], ip_str);
    return 0;
}

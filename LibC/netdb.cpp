#include <netdb.h>
#include <stdio.h>
#include <string.h>
#include <arpa/inet.h>
#include <netinet/in.h>
#include <unistd.h>
#include <AK/Assertions.h>
#include <AK/AKString.h>
#include <Kernel/IPv4.h>

extern "C" {

static hostent __gethostbyname_buffer;
static char __gethostbyname_name_buffer[512];
static in_addr_t __gethostbyname_address;
static in_addr_t* __gethostbyname_address_list_buffer[2];

hostent* gethostbyname(const char* name)
{
    int fd = socket(AF_LOCAL, SOCK_STREAM | SOCK_CLOEXEC, 0);
    if (fd < 0) {
        perror("socket");
        return nullptr;
    }

    sockaddr_un address;
    address.sun_family = AF_LOCAL;
    strcpy(address.sun_path, "/tmp/.DNSLookupServer-socket");

    int retries = 3;
    int rc = 0;
    while (retries) {
        rc = connect(fd, (const sockaddr*)&address, sizeof(address));
        if (rc == 0)
            break;
        --retries;
        sleep(1);
    }

    if (rc < 0) {
        close(fd);
        return nullptr;
    }

    auto line = String::format("%s\n", name);
    int nsent = write(fd, line.characters(), line.length());
    if (nsent < 0) {
        perror("send");
        close(fd);
        return nullptr;
    }

    ASSERT(nsent == line.length());

    char buffer[1024];
    int nrecv = read(fd, buffer, sizeof(buffer) - 1);
    if (nrecv < 0) {
        perror("recv");
        close(fd);
        return nullptr;
    }
    buffer[nrecv] = '\0';
    close(fd);

    if (!memcmp(buffer, "Not found.", sizeof("Not found.") - 1))
        return nullptr;

    rc = inet_pton(AF_INET, buffer, &__gethostbyname_address);
    if (rc < 0)
        return nullptr;

    strncpy(__gethostbyname_name_buffer, name, strlen(name));

    __gethostbyname_buffer.h_name = __gethostbyname_name_buffer;
    __gethostbyname_buffer.h_aliases = nullptr;
    __gethostbyname_buffer.h_addrtype = AF_INET;
    __gethostbyname_address_list_buffer[0] = &__gethostbyname_address;
    __gethostbyname_address_list_buffer[1] = nullptr;
    __gethostbyname_buffer.h_addr_list = (char**)__gethostbyname_address_list_buffer;
    __gethostbyname_buffer.h_length = 4;

    return &__gethostbyname_buffer;
}

}

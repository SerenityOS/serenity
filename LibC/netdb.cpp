#include <netdb.h>
#include <stdio.h>
#include <string.h>
#include <arpa/inet.h>
#include <netinet/in.h>

extern "C" {

static hostent __gethostbyname_buffer;
static in_addr_t __gethostbyname_address;
static in_addr_t* __gethostbyname_address_list_buffer[2];

hostent* gethostbyname(const char* name)
{
    if (!strcmp(name, "boards.4channel.org")) {
        __gethostbyname_buffer.h_name = "boards.4channel.org";
        __gethostbyname_buffer.h_aliases = nullptr;
        __gethostbyname_buffer.h_addrtype = AF_INET;
        __gethostbyname_address = 0x4b4f1168;
        __gethostbyname_address_list_buffer[0] = &__gethostbyname_address;
        __gethostbyname_address_list_buffer[1] = nullptr;
        __gethostbyname_buffer.h_addr_list = (char**)__gethostbyname_address_list_buffer;
        __gethostbyname_buffer.h_length = 4;
        return &__gethostbyname_buffer;
    }
    dbgprintf("FIXME(LibC): gethostbyname(%s)\n", name);
    return nullptr;
}

}


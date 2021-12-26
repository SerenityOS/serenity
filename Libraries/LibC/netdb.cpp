/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/Assertions.h>
#include <AK/ByteBuffer.h>
#include <AK/ScopeGuard.h>
#include <AK/String.h>
#include <Kernel/Net/IPv4.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <netinet/in.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

extern "C" {

int h_errno;

static hostent __gethostbyname_buffer;
static char __gethostbyname_name_buffer[512];
static in_addr_t __gethostbyname_address;
static in_addr_t* __gethostbyname_address_list_buffer[2];

static hostent __gethostbyaddr_buffer;
static char __gethostbyaddr_name_buffer[512];
static in_addr_t* __gethostbyaddr_address_list_buffer[2];

//Get service entry buffers and file information for the getservent() family of functions
static FILE* services_file = nullptr;
static const char* services_path = "/etc/services";

static bool fill_getserv_buffers(char* line, ssize_t read);
static servent __getserv_buffer;
static char __getserv_name_buffer[512];
static char __getserv_protocol_buffer[10];
static int __getserv_port_buffer;
static Vector<ByteBuffer> __getserv_alias_list_buffer;
static Vector<char*> __getserv_alias_list;
static bool keep_service_file_open = false;
static ssize_t service_file_offset = 0;

//Get protocol entry buffers and file information for the getprotent() family of functions
static FILE* protocols_file = nullptr;
static const char* protocols_path = "/etc/protocols";

static bool fill_getproto_buffers(char* line, ssize_t read);
static protoent __getproto_buffer;
static char __getproto_name_buffer[512];
static Vector<ByteBuffer> __getproto_alias_list_buffer;
static Vector<char*> __getproto_alias_list;
static int __getproto_protocol_buffer;
static bool keep_protocols_file_open = false;
static ssize_t protocol_file_offset = 0;

static int connect_to_lookup_server()
{
    int fd = socket(AF_LOCAL, SOCK_STREAM | SOCK_CLOEXEC, 0);
    if (fd < 0) {
        perror("socket");
        return -1;
    }

    sockaddr_un address;
    address.sun_family = AF_LOCAL;
    strcpy(address.sun_path, "/tmp/portal/lookup");

    if (connect(fd, (const sockaddr*)&address, sizeof(address)) < 0) {
        perror("connect_to_lookup_server");
        close(fd);
        return -1;
    }
    return fd;
}

hostent* gethostbyname(const char* name)
{
    auto ipv4_address = IPv4Address::from_string(name);
    if (ipv4_address.has_value()) {
        sprintf(__gethostbyname_name_buffer, "%s", ipv4_address.value().to_string().characters());
        __gethostbyname_buffer.h_name = __gethostbyname_name_buffer;
        __gethostbyname_buffer.h_aliases = nullptr;
        __gethostbyname_buffer.h_addrtype = AF_INET;
        new (&__gethostbyname_address) IPv4Address(ipv4_address.value());
        __gethostbyname_address_list_buffer[0] = &__gethostbyname_address;
        __gethostbyname_address_list_buffer[1] = nullptr;
        __gethostbyname_buffer.h_addr_list = (char**)__gethostbyname_address_list_buffer;
        __gethostbyname_buffer.h_length = 4;

        return &__gethostbyname_buffer;
    }

    int fd = connect_to_lookup_server();
    if (fd < 0)
        return nullptr;

    auto close_fd_on_exit = ScopeGuard([fd] {
        close(fd);
    });

    auto line = String::format("L%s\n", name);
    int nsent = write(fd, line.characters(), line.length());
    if (nsent < 0) {
        perror("write");
        return nullptr;
    }

    ASSERT((size_t)nsent == line.length());

    char buffer[1024];
    int nrecv = read(fd, buffer, sizeof(buffer) - 1);
    if (nrecv < 0) {
        perror("recv");
        return nullptr;
    }

    if (!memcmp(buffer, "Not found.", sizeof("Not found.") - 1))
        return nullptr;

    auto responses = String(buffer, nrecv).split('\n');
    if (responses.is_empty())
        return nullptr;

    auto& response = responses[0];

    int rc = inet_pton(AF_INET, response.characters(), &__gethostbyname_address);
    if (rc <= 0)
        return nullptr;

    strncpy(__gethostbyname_name_buffer, name, sizeof(__gethostbyaddr_name_buffer) - 1);

    __gethostbyname_buffer.h_name = __gethostbyname_name_buffer;
    __gethostbyname_buffer.h_aliases = nullptr;
    __gethostbyname_buffer.h_addrtype = AF_INET;
    __gethostbyname_address_list_buffer[0] = &__gethostbyname_address;
    __gethostbyname_address_list_buffer[1] = nullptr;
    __gethostbyname_buffer.h_addr_list = (char**)__gethostbyname_address_list_buffer;
    __gethostbyname_buffer.h_length = 4;

    return &__gethostbyname_buffer;
}

hostent* gethostbyaddr(const void* addr, socklen_t addr_size, int type)
{
    if (type != AF_INET) {
        errno = EAFNOSUPPORT;
        return nullptr;
    }

    if (addr_size < sizeof(in_addr)) {
        errno = EINVAL;
        return nullptr;
    }

    int fd = connect_to_lookup_server();
    if (fd < 0)
        return nullptr;

    auto close_fd_on_exit = ScopeGuard([fd] {
        close(fd);
    });

    IPv4Address ipv4_address((const u8*)&((const in_addr*)addr)->s_addr);

    auto line = String::format("R%d.%d.%d.%d.in-addr.arpa\n",
        ipv4_address[3],
        ipv4_address[2],
        ipv4_address[1],
        ipv4_address[0]);
    int nsent = write(fd, line.characters(), line.length());
    if (nsent < 0) {
        perror("write");
        return nullptr;
    }

    ASSERT((size_t)nsent == line.length());

    char buffer[1024];
    int nrecv = read(fd, buffer, sizeof(buffer) - 1);
    if (nrecv < 0) {
        perror("recv");
        return nullptr;
    }

    if (!memcmp(buffer, "Not found.", sizeof("Not found.") - 1))
        return nullptr;

    auto responses = String(buffer, nrecv).split('\n');
    if (responses.is_empty())
        return nullptr;

    auto& response = responses[0];

    strncpy(__gethostbyaddr_name_buffer, response.characters(), max(sizeof(__gethostbyaddr_name_buffer), response.length()));

    __gethostbyaddr_buffer.h_name = __gethostbyaddr_name_buffer;
    __gethostbyaddr_buffer.h_aliases = nullptr;
    __gethostbyaddr_buffer.h_addrtype = AF_INET;
    // FIXME: Should we populate the hostent's address list here with a sockaddr_in for the provided host?
    __gethostbyaddr_address_list_buffer[0] = nullptr;
    __gethostbyaddr_buffer.h_addr_list = (char**)__gethostbyaddr_address_list_buffer;
    __gethostbyaddr_buffer.h_length = 4;

    return &__gethostbyaddr_buffer;
}

struct servent* getservent()
{
    //If the services file is not open, attempt to open it and return null if it fails.
    if (!services_file) {
        services_file = fopen(services_path, "r");

        if (!services_file) {
            perror("error opening services file");
            return nullptr;
        }
    }

    if (fseek(services_file, service_file_offset, SEEK_SET) != 0) {
        perror("error seeking file");
        fclose(services_file);
        return nullptr;
    }
    char* line = nullptr;
    size_t len = 0;
    ssize_t read;

    auto free_line_on_exit = ScopeGuard([line] {
        if (line) {
            free(line);
        }
    });

    //Read lines from services file until an actual service name is found.
    do {
        read = getline(&line, &len, services_file);
        service_file_offset += read;
        if (read > 0 && (line[0] >= 65 && line[0] <= 122)) {
            break;
        }
    } while (read != -1);
    if (read == -1) {
        fclose(services_file);
        services_file = nullptr;
        service_file_offset = 0;
        return nullptr;
    }

    servent* service_entry = nullptr;
    if (!fill_getserv_buffers(line, read))
        return nullptr;

    __getserv_buffer.s_name = __getserv_name_buffer;
    __getserv_buffer.s_port = __getserv_port_buffer;
    __getserv_buffer.s_proto = __getserv_protocol_buffer;

    __getserv_alias_list.clear();
    for (auto& alias : __getserv_alias_list_buffer) {
        __getserv_alias_list.append((char*)alias.data());
    }

    __getserv_buffer.s_aliases = __getserv_alias_list.data();
    service_entry = &__getserv_buffer;

    if (!keep_service_file_open) {
        endservent();
    }
    return service_entry;
}
struct servent* getservbyname(const char* name, const char* protocol)
{
    bool previous_file_open_setting = keep_service_file_open;
    setservent(1);
    struct servent* current_service = nullptr;
    auto service_file_handler = ScopeGuard([previous_file_open_setting] {
        if (!previous_file_open_setting) {
            endservent();
        }
    });

    while (true) {
        current_service = getservent();
        if (current_service == nullptr)
            break;
        else if (!protocol && strcmp(current_service->s_name, name) == 0)
            break;
        else if (strcmp(current_service->s_name, name) == 0 && strcmp(current_service->s_proto, protocol) == 0)
            break;
    }

    return current_service;
}
struct servent* getservbyport(int port, const char* protocol)
{
    bool previous_file_open_setting = keep_service_file_open;
    setservent(1);
    struct servent* current_service = nullptr;
    auto service_file_handler = ScopeGuard([previous_file_open_setting] {
        if (!previous_file_open_setting) {
            endservent();
        }
    });
    while (true) {
        current_service = getservent();
        if (current_service == nullptr)
            break;
        else if (!protocol && current_service->s_port == port)
            break;
        else if (current_service->s_port == port && (strcmp(current_service->s_proto, protocol) == 0))
            break;
    }

    return current_service;
}
void setservent(int stay_open)
{
    if (!services_file) {
        services_file = fopen(services_path, "r");

        if (!services_file) {
            perror("error opening services file");
            return;
        }
    }
    rewind(services_file);
    keep_service_file_open = stay_open;
    service_file_offset = 0;
}
void endservent()
{
    if (!services_file) {
        return;
    }
    fclose(services_file);
    services_file = nullptr;
}

//Fill the service entry buffer with the information contained in the currently read line, returns true if successfull, false if failure occurs.
static bool fill_getserv_buffers(char* line, ssize_t read)
{
    //Splitting the line by tab delimiter and filling the servent buffers name, port, and protocol members.
    String string_line = String(line, read);
    string_line.replace(" ", "\t", true);
    auto split_line = string_line.split('\t');

    //This indicates an incorrect file format. Services file entries should always at least contain name and port/protocol, seperated by tabs.
    if (split_line.size() < 2) {
        perror("malformed services file: entry");
        return false;
    }
    if (sizeof(__getserv_name_buffer) >= split_line[0].length() + 1) {
        strncpy(__getserv_name_buffer, split_line[0].characters(), split_line[0].length() + 1);
    } else {
        perror("invalid buffer length: service name");
        return false;
    }

    auto port_protocol_split = String(split_line[1]).split('/');
    if (port_protocol_split.size() < 2) {
        perror("malformed services file: port/protocol");
        return false;
    }
    auto number = port_protocol_split[0].to_int();
    if (!number.has_value())
        return false;

    __getserv_port_buffer = number.value();

    //Removing any annoying whitespace at the end of the protocol.
    port_protocol_split[1].replace(" ", "", true);
    port_protocol_split[1].replace("\t", "", true);
    port_protocol_split[1].replace("\n", "", true);

    if (sizeof(__getserv_protocol_buffer) >= port_protocol_split[1].length()) {
        strncpy(__getserv_protocol_buffer, port_protocol_split[1].characters(), port_protocol_split[1].length() + 1);
    } else {
        perror("malformed services file: protocol");
        return false;
    }

    __getserv_alias_list_buffer.clear();

    //If there are aliases for the service, we will fill the alias list buffer.
    if (split_line.size() > 2 && !split_line[2].starts_with('#')) {

        for (size_t i = 2; i < split_line.size(); i++) {
            if (split_line[i].starts_with('#')) {
                break;
            }
            auto alias = split_line[i].to_byte_buffer();
            alias.append("\0", sizeof(char));
            __getserv_alias_list_buffer.append(alias);
        }
    }

    return true;
}

struct protoent* getprotoent()
{
    //If protocols file isn't open, attempt to open and return null on failure.
    if (!protocols_file) {
        protocols_file = fopen(protocols_path, "r");

        if (!protocols_file) {
            perror("error opening protocols file");
            return nullptr;
        }
    }

    if (fseek(protocols_file, protocol_file_offset, SEEK_SET) != 0) {
        perror("error seeking protocols file");
        fclose(protocols_file);
        return nullptr;
    }

    char* line = nullptr;
    size_t len = 0;
    ssize_t read;

    auto free_line_on_exit = ScopeGuard([line] {
        if (line) {
            free(line);
        }
    });

    do {
        read = getline(&line, &len, protocols_file);
        protocol_file_offset += read;
        if (read > 0 && (line[0] >= 65 && line[0] <= 122)) {
            break;
        }
    } while (read != -1);

    if (read == -1) {
        fclose(protocols_file);
        protocols_file = nullptr;
        protocol_file_offset = 0;
        return nullptr;
    }

    struct protoent* protocol_entry = nullptr;
    if (!fill_getproto_buffers(line, read))
        return nullptr;

    __getproto_buffer.p_name = __getproto_name_buffer;
    __getproto_buffer.p_proto = __getproto_protocol_buffer;

    __getproto_alias_list.clear();

    for (auto& alias : __getproto_alias_list_buffer) {
        __getproto_alias_list.append((char*)alias.data());
    }

    __getproto_buffer.p_aliases = __getproto_alias_list.data();
    protocol_entry = &__getproto_buffer;

    if (!keep_protocols_file_open)
        endprotoent();

    return protocol_entry;
}

struct protoent* getprotobyname(const char* name)
{
    bool previous_file_open_setting = keep_protocols_file_open;
    setprotoent(1);
    struct protoent* current_protocol = nullptr;
    auto protocol_file_handler = ScopeGuard([previous_file_open_setting] {
        if (!previous_file_open_setting) {
            endprotoent();
        }
    });

    while (true) {
        current_protocol = getprotoent();
        if (current_protocol == nullptr)
            break;
        else if (strcmp(current_protocol->p_name, name) == 0)
            break;
    }

    return current_protocol;
}

struct protoent* getprotobynumber(int proto)
{
    bool previous_file_open_setting = keep_protocols_file_open;
    setprotoent(1);
    struct protoent* current_protocol = nullptr;
    auto protocol_file_handler = ScopeGuard([previous_file_open_setting] {
        if (!previous_file_open_setting) {
            endprotoent();
        }
    });

    while (true) {
        current_protocol = getprotoent();
        if (current_protocol == nullptr)
            break;
        else if (current_protocol->p_proto == proto)
            break;
    }

    return current_protocol;
}

void setprotoent(int stay_open)
{
    if (!protocols_file) {
        protocols_file = fopen(protocols_path, "r");

        if (!protocols_file) {
            perror("error opening protocols file");
            return;
        }
    }
    rewind(protocols_file);
    keep_protocols_file_open = stay_open;
    protocol_file_offset = 0;
}

void endprotoent()
{
    if (!protocols_file) {
        return;
    }
    fclose(protocols_file);
    protocols_file = nullptr;
}

static bool fill_getproto_buffers(char* line, ssize_t read)
{
    String string_line = String(line, read);
    string_line.replace(" ", "\t", true);
    auto split_line = string_line.split('\t');

    //This indicates an incorrect file format. Protocols file entries should always have at least a name and a protocol.
    if (split_line.size() < 2) {
        perror("malformed protocols file: entry");
        return false;
    }
    if (sizeof(__getproto_name_buffer) >= split_line[0].length() + 1) {
        strncpy(__getproto_name_buffer, split_line[0].characters(), split_line[0].length() + 1);
    } else {
        perror("invalid buffer length: protocol name");
        return false;
    }

    auto number = split_line[1].to_int();
    if (!number.has_value())
        return false;

    __getproto_protocol_buffer = number.value();

    __getproto_alias_list_buffer.clear();

    //If there are aliases for the protocol, we will fill the alias list buffer.
    if (split_line.size() > 2 && !split_line[2].starts_with('#')) {

        for (size_t i = 2; i < split_line.size(); i++) {
            if (split_line[i].starts_with('#')) {
                break;
            }
            auto alias = split_line[i].to_byte_buffer();
            alias.append("\0", sizeof(char));
            __getproto_alias_list_buffer.append(alias);
        }
    }

    return true;
}
}

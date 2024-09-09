/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/ByteBuffer.h>
#include <AK/ByteString.h>
#include <AK/ScopeGuard.h>
#include <Kernel/Net/IP/IPv4.h>
#include <arpa/inet.h>
#include <errno.h>
#include <netdb.h>
#include <netinet/in.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

extern "C" {

__thread int h_errno;

static hostent __gethostbyname_buffer;
static in_addr_t __gethostbyname_address;
static in_addr_t* __gethostbyname_address_list_buffer[2];
static char* __gethostbyname_alias_list_buffer[1];

static hostent __gethostbyaddr_buffer;
static in_addr_t* __gethostbyaddr_address_list_buffer[2];
static char* __gethostbyaddr_alias_list_buffer[1];
// IPCCompiler depends on LibC. Because of this, it cannot be compiled
// before LibC is. However, the lookup magic can only be obtained from the
// endpoint itself if IPCCompiler has compiled the IPC file, so this creates
// a chicken-and-egg situation. Because of this, the LookupServer endpoint magic
// is hardcoded here.
// Keep the name synchronized with LookupServer/LookupServer.ipc.
static constexpr u32 lookup_server_endpoint_magic = "LookupServer"sv.hash();

// Get service entry buffers and file information for the getservent() family of functions.
static FILE* services_file = nullptr;
static char const* services_path = "/etc/services";

struct ServiceFileLine {
    String name;
    String protocol;
    int port;
    Vector<ByteBuffer> aliases;
};

static ErrorOr<Optional<ServiceFileLine>> parse_service_file_line(char const* line, ssize_t read);
static servent __getserv_buffer;
static ByteString __getserv_name_buffer;
static ByteString __getserv_protocol_buffer;
static int __getserv_port_buffer;
static Vector<ByteBuffer> __getserv_alias_list_buffer;
static Vector<char*> __getserv_alias_list;
static bool keep_service_file_open = false;
static ssize_t service_file_offset = 0;

// Get protocol entry buffers and file information for the getprotent() family of functions.
static FILE* protocols_file = nullptr;
static char const* protocols_path = "/etc/protocols";

static bool fill_getproto_buffers(char const* line, ssize_t read);
static protoent __getproto_buffer;
static ByteString __getproto_name_buffer;
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

    sockaddr_un address {
        AF_LOCAL,
        "/tmp/portal/lookup"
    };

    if (connect(fd, (sockaddr const*)&address, sizeof(address)) < 0) {
        perror("connect_to_lookup_server");
        close(fd);
        return -1;
    }
    return fd;
}

static ByteString gethostbyname_name_buffer;

hostent* gethostbyname(char const* name)
{
    struct hostent ret = {};
    struct hostent* result = nullptr;
    size_t buffer_size = 1024;
    char* buffer = nullptr;

    auto free_buffer_on_exit = ScopeGuard([buffer] {
        if (buffer != nullptr)
            free(buffer);
    });

    while (true) {
        buffer = (char*)realloc(buffer, buffer_size);
        if (buffer == nullptr) {
            // NOTE: Since gethostbyname usually can't fail because of memory,
            //       it has no way of representing OOM or allocation failure.
            //       NO_RECOVERY is the next best thing.
            h_errno = NO_RECOVERY;
            return NULL;
        }

        int rc = gethostbyname_r(name, &ret, buffer, buffer_size, &result, &h_errno);
        if (rc == ERANGE) {
            buffer_size *= 2;
            continue;
        }

        if (rc < 0)
            return nullptr;

        break;
    }

    gethostbyname_name_buffer = name;
    __gethostbyname_buffer.h_name = const_cast<char*>(gethostbyname_name_buffer.characters());
    __gethostbyname_alias_list_buffer[0] = nullptr;
    __gethostbyname_buffer.h_aliases = __gethostbyname_alias_list_buffer;
    __gethostbyname_buffer.h_addrtype = AF_INET;
    memcpy(&__gethostbyname_address, result->h_addr_list[0], sizeof(in_addr_t));
    __gethostbyname_address_list_buffer[0] = &__gethostbyname_address;
    __gethostbyname_address_list_buffer[1] = nullptr;
    __gethostbyname_buffer.h_addr_list = (char**)__gethostbyname_address_list_buffer;
    __gethostbyname_buffer.h_length = result->h_length;

    return &__gethostbyname_buffer;
}

int gethostbyname_r(char const* __restrict name, struct hostent* __restrict ret, char* buffer, size_t buffer_size, struct hostent** __restrict result, int* __restrict h_errnop)
{
    *h_errnop = 0;
    *result = nullptr;
    size_t buffer_offset = 0;
    memset(buffer, 0, buffer_size);

    auto add_string_to_buffer = [&](char const* data) -> Optional<char*> {
        size_t data_lenth = strlen(data);

        if (buffer_offset + data_lenth + 1 >= buffer_size)
            return {};

        auto* buffer_beginning = buffer + buffer_offset;

        memcpy(buffer + buffer_offset, data, data_lenth);
        buffer_offset += data_lenth;
        buffer[buffer_offset++] = '\0';

        buffer_offset += 8 - (buffer_offset % 8);

        return buffer_beginning;
    };

    auto add_data_to_buffer = [&](void const* data, size_t size, size_t count = 1) -> Optional<void*> {
        auto bytes = size * count;

        if (buffer_offset + bytes >= buffer_size)
            return {};

        auto* buffer_beginning = buffer + buffer_offset;

        memcpy(buffer + buffer_offset, data, bytes);
        buffer_offset += bytes;

        buffer_offset += 8 - (buffer_offset % 8);

        return buffer_beginning;
    };

    auto add_ptr_to_buffer = [&](void* ptr) -> Optional<void*> {
        return add_data_to_buffer(&ptr, sizeof(ptr));
    };

    auto populate_ret = [&](char const* name, in_addr_t address) -> int {
        auto h_name = add_string_to_buffer(name);
        if (!h_name.has_value())
            return ERANGE;

        ret->h_name = static_cast<char*>(h_name.value());

        auto null_list_item = add_ptr_to_buffer(nullptr);
        if (!null_list_item.has_value())
            return ERANGE;

        ret->h_aliases = static_cast<char**>(null_list_item.value());

        auto address_item = add_data_to_buffer(&address, sizeof(address));
        if (!address_item.has_value())
            return ERANGE;

        auto address_list = add_ptr_to_buffer(address_item.value());
        if (!address_list.has_value())
            return ERANGE;

        if (!add_ptr_to_buffer(nullptr).has_value())
            return ERANGE;

        ret->h_addr_list = static_cast<char**>(address_list.value());

        ret->h_addrtype = AF_INET;
        ret->h_length = 4;

        *result = ret;
        return 0;
    };

    auto ipv4_address = IPv4Address::from_string({ name, strlen(name) });

    if (ipv4_address.has_value()) {
        return populate_ret(ipv4_address.value().to_byte_string().characters(), ipv4_address.value().to_in_addr_t());
    }

    int fd = connect_to_lookup_server();
    if (fd < 0) {
        *h_errnop = TRY_AGAIN;
        return -TRY_AGAIN;
    }

    auto close_fd_on_exit = ScopeGuard([fd] {
        close(fd);
    });

    auto name_length = strlen(name);
    VERIFY(name_length <= NumericLimits<i32>::max());

    struct [[gnu::packed]] {
        u32 message_size;
        u32 endpoint_magic;
        i32 message_id;
        u32 name_length;
    } request_header = {
        (u32)(sizeof(request_header) - sizeof(request_header.message_size) + name_length),
        lookup_server_endpoint_magic,
        1,
        static_cast<u32>(name_length),
    };
    if (auto nsent = write(fd, &request_header, sizeof(request_header)); nsent < 0) {
        *h_errnop = TRY_AGAIN;
        return -TRY_AGAIN;
    } else if (nsent != sizeof(request_header)) {
        *h_errnop = NO_RECOVERY;
        return -NO_RECOVERY;
    }

    if (auto nsent = write(fd, name, name_length); nsent < 0) {
        *h_errnop = TRY_AGAIN;
        return -TRY_AGAIN;
    } else if (static_cast<size_t>(nsent) != name_length) {
        *h_errnop = NO_RECOVERY;
        return -NO_RECOVERY;
    }

    struct [[gnu::packed]] {
        u32 message_size;
        u32 endpoint_magic;
        i32 message_id;
        i32 code;
        u32 addresses_count;
    } response_header;

    if (auto nreceived = read(fd, &response_header, sizeof(response_header)); nreceived < 0) {
        *h_errnop = TRY_AGAIN;
        return -TRY_AGAIN;
    } else if (nreceived != sizeof(response_header)) {
        *h_errnop = NO_RECOVERY;
        return -NO_RECOVERY;
    }
    if (response_header.endpoint_magic != lookup_server_endpoint_magic || response_header.message_id != 2) {
        *h_errnop = NO_RECOVERY;
        return -NO_RECOVERY;
    }
    if (response_header.code != 0) {
        *h_errnop = NO_RECOVERY;
        return -NO_RECOVERY;
    }
    if (response_header.addresses_count == 0) {
        *h_errnop = HOST_NOT_FOUND;
        return -HOST_NOT_FOUND;
    }
    i32 response_length;
    if (auto nreceived = read(fd, &response_length, sizeof(response_length)); nreceived < 0) {
        *h_errnop = TRY_AGAIN;
        return -TRY_AGAIN;
    } else if (nreceived != sizeof(response_length)
        || response_length != sizeof(in_addr_t)) {
        *h_errnop = NO_RECOVERY;
        return -NO_RECOVERY;
    }

    in_addr_t address;
    if (auto nreceived = read(fd, &address, response_length); nreceived < 0) {
        *h_errnop = TRY_AGAIN;
        return -TRY_AGAIN;
    } else if (nreceived != response_length) {
        *h_errnop = NO_RECOVERY;
        return -NO_RECOVERY;
    }

    return populate_ret(name, address);
}

static ByteString gethostbyaddr_name_buffer;

hostent* gethostbyaddr(void const* addr, socklen_t addr_size, int type)
{
    h_errno = 0;

    if (type != AF_INET) {
        errno = EAFNOSUPPORT;
        return nullptr;
    }

    if (addr_size < sizeof(in_addr)) {
        errno = EINVAL;
        return nullptr;
    }

    int fd = connect_to_lookup_server();
    if (fd < 0) {
        h_errno = TRY_AGAIN;
        return nullptr;
    }

    auto close_fd_on_exit = ScopeGuard([fd] {
        close(fd);
    });

    in_addr_t const& in_addr = ((const struct in_addr*)addr)->s_addr;

    struct [[gnu::packed]] {
        u32 message_size;
        u32 endpoint_magic;
        i32 message_id;
        i32 address_length;
    } request_header = {
        sizeof(request_header) - sizeof(request_header.message_size) + sizeof(in_addr),
        lookup_server_endpoint_magic,
        3,
        sizeof(in_addr),
    };
    if (auto nsent = write(fd, &request_header, sizeof(request_header)); nsent < 0) {
        h_errno = TRY_AGAIN;
        return nullptr;
    } else if (nsent != sizeof(request_header)) {
        h_errno = NO_RECOVERY;
        return nullptr;
    }
    if (auto nsent = write(fd, &in_addr, sizeof(in_addr)); nsent < 0) {
        h_errno = TRY_AGAIN;
        return nullptr;
    } else if (nsent != sizeof(in_addr)) {
        h_errno = TRY_AGAIN;
        return nullptr;
    }

    struct [[gnu::packed]] {
        u32 message_size;
        u32 endpoint_magic;
        i32 message_id;
        i32 code;
        u32 name_length;
    } response_header;

    if (auto nreceived = read(fd, &response_header, sizeof(response_header)); nreceived < 0) {
        h_errno = TRY_AGAIN;
        return nullptr;
    } else if (nreceived != sizeof(response_header)) {
        h_errno = NO_RECOVERY;
        return nullptr;
    }
    if (response_header.endpoint_magic != lookup_server_endpoint_magic
        || response_header.message_id != 4
        || response_header.code != 0) {
        h_errno = NO_RECOVERY;
        return nullptr;
    }

    ssize_t nreceived;

    gethostbyaddr_name_buffer = ByteString::create_and_overwrite(response_header.name_length, [&](Bytes bytes) {
        nreceived = read(fd, bytes.data(), bytes.size());
    });

    if (nreceived < 0) {
        h_errno = TRY_AGAIN;
        return nullptr;
    } else if (static_cast<u32>(nreceived) != response_header.name_length) {
        h_errno = NO_RECOVERY;
        return nullptr;
    }

    __gethostbyaddr_buffer.h_name = const_cast<char*>(gethostbyaddr_name_buffer.characters());
    __gethostbyaddr_alias_list_buffer[0] = nullptr;
    __gethostbyaddr_buffer.h_aliases = __gethostbyaddr_alias_list_buffer;
    __gethostbyaddr_buffer.h_addrtype = AF_INET;
    // FIXME: Should we populate the hostent's address list here with a sockaddr_in for the provided host?
    __gethostbyaddr_address_list_buffer[0] = nullptr;
    __gethostbyaddr_buffer.h_addr_list = (char**)__gethostbyaddr_address_list_buffer;
    __gethostbyaddr_buffer.h_length = 4;

    return &__gethostbyaddr_buffer;
}

struct servent* getservent()
{
    // If the services file is not open, attempt to open it and return null if it fails.
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

    Optional<ServiceFileLine> service_file_line = {};

    // Read lines from services file until an actual service name is found.
    do {
        read = getline(&line, &len, services_file);
        service_file_offset += read;

        auto service_file_line_or_error = parse_service_file_line(line, read);
        if (service_file_line_or_error.is_error())
            return nullptr;

        service_file_line = service_file_line_or_error.release_value();

        if (service_file_line.has_value())
            break;

    } while (read != -1);
    if (read == -1) {
        fclose(services_file);
        services_file = nullptr;
        service_file_offset = 0;
        return nullptr;
    }

    if (!service_file_line.has_value())
        return nullptr;

    servent* service_entry = nullptr;

    __getserv_name_buffer = service_file_line.value().name.to_byte_string();
    __getserv_port_buffer = service_file_line.value().port;
    __getserv_protocol_buffer = service_file_line.value().protocol.to_byte_string();
    __getserv_alias_list_buffer = service_file_line.value().aliases;

    __getserv_buffer.s_name = const_cast<char*>(__getserv_name_buffer.characters());
    __getserv_buffer.s_port = htons(__getserv_port_buffer);
    __getserv_buffer.s_proto = const_cast<char*>(__getserv_protocol_buffer.characters());

    __getserv_alias_list.clear_with_capacity();
    __getserv_alias_list.ensure_capacity(__getserv_alias_list_buffer.size() + 1);
    for (auto& alias : __getserv_alias_list_buffer)
        __getserv_alias_list.unchecked_append(reinterpret_cast<char*>(alias.data()));
    __getserv_alias_list.unchecked_append(nullptr);

    __getserv_buffer.s_aliases = __getserv_alias_list.data();
    service_entry = &__getserv_buffer;

    if (!keep_service_file_open) {
        endservent();
    }
    return service_entry;
}

struct servent* getservbyname(char const* name, char const* protocol)
{
    if (name == nullptr)
        return nullptr;

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

struct servent* getservbyport(int port, char const* protocol)
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

static ErrorOr<Optional<ServiceFileLine>> parse_service_file_line(char const* line, ssize_t read)
{
    // If the line isn't a service (eg. empty or a comment)
    if (read <= 0 || line[0] < 65 || line[0] > 122)
        return { Optional<ServiceFileLine> {} };

    auto split_line = StringView(line, read).replace(" "sv, "\t"sv, ReplaceMode::All).split('\t');

    if (split_line.size() < 2)
        return Error::from_string_view("malformed service file"sv);

    auto name = TRY(String::from_byte_string(split_line[0]));

    auto port_protocol = TRY(String::from_byte_string(split_line[1]));
    auto port_protocol_split = TRY(port_protocol.split('/'));

    if (port_protocol_split.size() < 2)
        return Error::from_string_view("malformed service file"sv);

    auto port = port_protocol_split[0].to_number<int>();
    if (!port.has_value())
        return Error::from_string_view("port isn't a number"sv);

    // Remove whitespace at the end of the protocol
    auto protocol = TRY(port_protocol_split[1].replace(" "sv, ""sv, ReplaceMode::All));
    protocol = TRY(protocol.replace("\t"sv, ""sv, ReplaceMode::All));
    protocol = TRY(protocol.replace("\n"sv, ""sv, ReplaceMode::All));

    Vector<ByteBuffer> aliases;

    // If there are aliases for the service, we will fill the aliases list
    if (split_line.size() > 2 && !split_line[2].starts_with('#')) {
        for (size_t i = 2; i < split_line.size(); i++) {
            if (split_line[i].starts_with('#')) {
                break;
            }
            auto alias = split_line[i].to_byte_buffer();
            if (alias.try_append("\0", sizeof(char)).is_error())
                return Error::from_string_view("Failed to add null-byte to service alias"sv);

            aliases.append(move(alias));
        }
    }

    return ServiceFileLine {
        name, protocol, port.value(), aliases
    };
}

struct protoent* getprotoent()
{
    // If protocols file isn't open, attempt to open and return null on failure.
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

    __getproto_buffer.p_name = const_cast<char*>(__getproto_name_buffer.characters());
    __getproto_buffer.p_proto = __getproto_protocol_buffer;

    __getproto_alias_list.clear_with_capacity();
    __getproto_alias_list.ensure_capacity(__getproto_alias_list_buffer.size() + 1);
    for (auto& alias : __getproto_alias_list_buffer)
        __getproto_alias_list.unchecked_append(reinterpret_cast<char*>(alias.data()));
    __getserv_alias_list.unchecked_append(nullptr);

    __getproto_buffer.p_aliases = __getproto_alias_list.data();
    protocol_entry = &__getproto_buffer;

    if (!keep_protocols_file_open)
        endprotoent();

    return protocol_entry;
}

struct protoent* getprotobyname(char const* name)
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
            perror("setprotoent(): error opening protocols file");
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

static bool fill_getproto_buffers(char const* line, ssize_t read)
{
    ByteString string_line = ByteString(line, read);
    auto split_line = string_line.replace(" "sv, "\t"sv, ReplaceMode::All).split('\t');

    // This indicates an incorrect file format. Protocols file entries should
    // always have at least a name and a protocol.
    if (split_line.size() < 2) {
        warnln("getprotoent(): malformed protocols file");
        return false;
    }
    __getproto_name_buffer = split_line[0];

    auto number = split_line[1].to_number<int>();
    if (!number.has_value())
        return false;

    __getproto_protocol_buffer = number.value();

    __getproto_alias_list_buffer.clear();

    // If there are aliases for the protocol, we will fill the alias list buffer.
    if (split_line.size() > 2 && !split_line[2].starts_with('#')) {

        for (size_t i = 2; i < split_line.size(); i++) {
            if (split_line[i].starts_with('#'))
                break;
            auto alias = split_line[i].to_byte_buffer();
            if (alias.try_append("\0", sizeof(char)).is_error())
                return false;
            __getproto_alias_list_buffer.append(move(alias));
        }
    }

    return true;
}

int getaddrinfo(char const* __restrict node, char const* __restrict service, const struct addrinfo* __restrict hints, struct addrinfo** __restrict res)
{
    *res = nullptr;

    if (hints && hints->ai_family != AF_INET && hints->ai_family != AF_UNSPEC)
        return EAI_FAMILY;

    if (!node) {
        if (hints && hints->ai_flags & AI_PASSIVE)
            node = "0.0.0.0";
        else
            node = "127.0.0.1";
    }

    size_t buffer_size = 1024;
    char* buffer = nullptr;
    int gethostbyname_errno = 0;
    struct hostent ret = {};
    struct hostent* host_ent = nullptr;

    while (true) {
        buffer = (char*)realloc(buffer, buffer_size);

        if (buffer == nullptr)
            return EAI_MEMORY;

        int rc = gethostbyname_r(node, &ret, buffer, buffer_size, &host_ent, &gethostbyname_errno);
        if (rc == ERANGE) {
            buffer_size *= 2;
            continue;
        }

        if (!host_ent)
            return EAI_FAIL;
        break;
    }

    char const* proto = nullptr;
    if (hints && hints->ai_socktype) {
        switch (hints->ai_socktype) {
        case SOCK_STREAM:
            proto = "tcp";
            break;
        case SOCK_DGRAM:
            proto = "udp";
            break;
        default:
            return EAI_SOCKTYPE;
        }
    }

    long port;
    int socktype;

    Optional<ServiceFileLine> service_file_line = {};

    if ((!hints || (hints->ai_flags & AI_NUMERICSERV) == 0) && service) {
        services_file = fopen(services_path, "r");
        if (!services_file) {
            return EAI_FAIL;
        }

        auto close_services_file_handler = ScopeGuard([&] {
            fclose(services_file);
        });

        char* line = nullptr;
        size_t length = 0;
        ssize_t read;

        while (true) {
            do {
                read = getline(&line, &length, services_file);

                auto service_file_line_or_error = parse_service_file_line(line, read);
                if (service_file_line_or_error.is_error())
                    return EAI_SYSTEM;

                service_file_line = service_file_line_or_error.release_value();

                if (service_file_line.has_value())
                    break;
            } while (read != -1);

            if (read == -1 || !service_file_line.has_value())
                break;

            if (service_file_line.value().name != service)
                continue;

            if (service_file_line.value().protocol != proto)
                continue;

            break;
        }
    }

    if (!service_file_line.has_value()) {
        if (service) {
            char* end;
            port = strtol(service, &end, 10);
            if (*end)
                return EAI_FAIL;
        } else {
            port = 0;
        }

        if (hints && hints->ai_socktype != 0)
            socktype = hints->ai_socktype;
        else
            socktype = SOCK_STREAM;
    } else {
        port = service_file_line.value().port;
        socktype = service_file_line.value().protocol == "tcp" ? SOCK_STREAM : SOCK_DGRAM;
    }

    addrinfo* first_info = nullptr;
    addrinfo* prev_info = nullptr;

    for (int host_index = 0; host_ent->h_addr_list[host_index]; host_index++) {
        sockaddr_in* sin = new sockaddr_in;
        sin->sin_family = AF_INET;
        sin->sin_port = htons(port);
        memcpy(&sin->sin_addr.s_addr, host_ent->h_addr_list[host_index], host_ent->h_length);

        addrinfo* info = new addrinfo;
        info->ai_flags = 0;
        info->ai_family = AF_INET;
        info->ai_socktype = socktype;
        info->ai_protocol = PF_INET;
        info->ai_addrlen = sizeof(*sin);
        info->ai_addr = reinterpret_cast<sockaddr*>(sin);

        if (hints && hints->ai_flags & AI_CANONNAME)
            info->ai_canonname = strdup(host_ent->h_name);
        else
            info->ai_canonname = nullptr;

        info->ai_next = nullptr;

        if (!first_info)
            first_info = info;

        if (prev_info)
            prev_info->ai_next = info;

        prev_info = info;
    }

    if (first_info) {
        *res = first_info;
        return 0;
    } else
        return EAI_NONAME;
}

void freeaddrinfo(struct addrinfo* res)
{
    if (res) {
        delete reinterpret_cast<sockaddr_in*>(res->ai_addr);
        free(res->ai_canonname);
        freeaddrinfo(res->ai_next);
        delete res;
    }
}

char const* gai_strerror(int errcode)
{
    switch (errcode) {
    case EAI_ADDRFAMILY:
        return "no address for this address family available";
    case EAI_AGAIN:
        return "name server returned temporary failure";
    case EAI_BADFLAGS:
        return "invalid flags";
    case EAI_FAIL:
        return "name server returned permanent failure";
    case EAI_FAMILY:
        return "unsupported address family";
    case EAI_MEMORY:
        return "out of memory";
    case EAI_NODATA:
        return "no address available";
    case EAI_NONAME:
        return "node or service is not known";
    case EAI_SERVICE:
        return "service not available";
    case EAI_SOCKTYPE:
        return "unsupported socket type";
    case EAI_SYSTEM:
        return "system error";
    case EAI_OVERFLOW:
        return "buffer too small";
    default:
        return "invalid error code";
    }
}

int getnameinfo(const struct sockaddr* __restrict addr, socklen_t addrlen, char* __restrict host, socklen_t hostlen, char* __restrict serv, socklen_t servlen, int flags)
{
    if (addr->sa_family != AF_INET || addrlen < sizeof(sockaddr_in))
        return EAI_FAMILY;

    sockaddr_in const* sin = reinterpret_cast<sockaddr_in const*>(addr);

    if (host && hostlen > 0) {
        if (flags != 0)
            dbgln("getnameinfo flags are not implemented: {:#x}", flags);

        if (!inet_ntop(AF_INET, &sin->sin_addr, host, hostlen)) {
            if (errno == ENOSPC)
                return EAI_OVERFLOW;
            else
                return EAI_SYSTEM;
        }
    }

    if (serv && servlen > 0) {
        if (snprintf(serv, servlen, "%d", (int)ntohs(sin->sin_port)) > (int)servlen)
            return EAI_OVERFLOW;
    }

    return 0;
}

void herror(char const* s)
{
    dbgln("herror(): {}: {}", s, hstrerror(h_errno));
    warnln("{}: {}", s, hstrerror(h_errno));
}

char const* hstrerror(int err)
{
    switch (err) {
    case HOST_NOT_FOUND:
        return "The specified host is unknown.";
    case NO_DATA:
        return "The requested name is valid but does not have an IP address.";
    case NO_RECOVERY:
        return "A nonrecoverable name server error occurred.";
    case TRY_AGAIN:
        return "A temporary error occurred on an authoritative name server. Try again later.";
    default:
        return "Unknown error.";
    }
}
}

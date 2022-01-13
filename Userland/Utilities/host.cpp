/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <netinet/in.h>
#include <string.h>

ErrorOr<int> serenity_main(Main::Arguments args)
{
    TRY(Core::System::pledge("stdio unix", nullptr));

    const char* name_or_ip = nullptr;
    Core::ArgsParser args_parser;
    args_parser.set_general_help("Convert between domain name and IPv4 address.");
    args_parser.add_positional_argument(name_or_ip, "Domain name or IPv4 address", "name");
    args_parser.parse(args);

    // If input looks like an IPv4 address, we should do a reverse lookup.
    struct sockaddr_in addr;
    memset(&addr, 0, sizeof(addr));
    addr.sin_family = AF_INET;
    addr.sin_port = htons(53);
    int rc = inet_pton(AF_INET, name_or_ip, &addr.sin_addr);
    if (rc == 1) {
        // Okay, let's do a reverse lookup.
        auto* hostent = gethostbyaddr(&addr.sin_addr, sizeof(in_addr), AF_INET);
        if (!hostent) {
            warnln("Reverse lookup failed for '{}'", name_or_ip);
            return 1;
        }
        outln("{} is {}", name_or_ip, hostent->h_name);
        return 0;
    }

    auto* hostent = gethostbyname(name_or_ip);
    if (!hostent) {
        warnln("Lookup failed for '{}'", name_or_ip);
        return 1;
    }

    char buffer[INET_ADDRSTRLEN];
    const char* ip_str = inet_ntop(AF_INET, hostent->h_addr_list[0], buffer, sizeof(buffer));

    outln("{} is {}", name_or_ip, ip_str);
    return 0;
}

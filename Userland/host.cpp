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

#include <LibCore/ArgsParser.h>
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

    const char* name_or_ip = nullptr;
    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(name_or_ip, "Domain name or IPv4 address", "name");
    args_parser.parse(argc, argv);

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
            fprintf(stderr, "Reverse lookup failed for '%s'\n", name_or_ip);
            return 1;
        }
        printf("%s is %s\n", name_or_ip, hostent->h_name);
        return 0;
    }

    auto* hostent = gethostbyname(name_or_ip);
    if (!hostent) {
        fprintf(stderr, "Lookup failed for '%s'\n", name_or_ip);
        return 1;
    }

    char buffer[32];
    const char* ip_str = inet_ntop(AF_INET, hostent->h_addr_list[0], buffer, sizeof(buffer));

    printf("%s is %s\n", name_or_ip, ip_str);
    return 0;
}

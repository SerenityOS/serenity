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

#include "Client.h"
#include <AK/MappedFile.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibCore/File.h>
#include <LibCore/TCPServer.h>
#include <stdio.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    u16 default_port = 8000;
    const char* root_path = "/www";

    int port = default_port;

    Core::ArgsParser args_parser;
    args_parser.add_option(port, "Port to listen on", "port", 'p', "port");
    args_parser.add_positional_argument(root_path, "Path to serve the contents of", "path", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    if ((u16)port != port) {
        printf("Warning: invalid port number: %d\n", port);
        port = default_port;
    }

    auto real_root_path = Core::File::real_path_for(root_path);

    if (!Core::File::exists(real_root_path)) {
        fprintf(stderr, "Root path does not exist: '%s'\n", root_path);
        return 1;
    }

    if (pledge("stdio accept rpath inet unix cpath fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    Core::EventLoop loop;

    auto server = Core::TCPServer::construct();

    server->on_ready_to_accept = [&] {
        auto client_socket = server->accept();
        ASSERT(client_socket);
        auto client = WebServer::Client::construct(client_socket.release_nonnull(), real_root_path, server);
        client->start();
    };

    if (!server->listen({}, port)) {
        warnln("Failed to listen on 0.0.0.0:{}", port);
        return 1;
    }

    outln("Listening on 0.0.0.0:{}", port);

    if (unveil("/res/icons", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil(real_root_path.characters(), "r") < 0) {
        perror("unveil");
        return 1;
    }

    unveil(nullptr, nullptr);

    if (pledge("stdio accept rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    return loop.exec();
}

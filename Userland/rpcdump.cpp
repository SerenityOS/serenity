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

#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <LibCore/EventLoop.h>
#include <LibCore/LocalSocket.h>
#include <stdio.h>
#include <stdlib.h>

int main(int argc, char** argv)
{
    if (pledge("stdio unix cpath fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/tmp", "rwc") < 0) {
        perror("unveil");
        return 1;
    }

    unveil(nullptr, nullptr);

    if (argc != 2) {
        printf("usage: %s <pid>\n", argv[0]);
        return 0;
    }

    Core::EventLoop loop;

    int pid = atoi(argv[1]);

    auto socket = Core::LocalSocket::construct();

    if (pledge("stdio unix", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    socket->on_connected = [&] {
        dbg() << "Connected to PID " << pid;

        JsonObject request;
        request.set("type", "GetAllObjects");
        auto serialized = request.to_string();
        i32 length = serialized.length();
        socket->write((const u8*)&length, sizeof(length));
        socket->write(serialized);
    };

    socket->on_ready_to_read = [&] {
        if (socket->eof()) {
            dbg() << "Disconnected from PID " << pid;
            loop.quit(0);
            return;
        }

        auto data = socket->read_all();

        for (size_t i = 0; i < data.size(); ++i)
            putchar(data[i]);
        printf("\n");

        loop.quit(0);
    };

    auto success = socket->connect(Core::SocketAddress::local(String::format("/tmp/rpc.%d", pid)));
    if (!success) {
        fprintf(stderr, "Couldn't connect to PID %d\n", pid);
        return 1;
    }

    return loop.exec();
}

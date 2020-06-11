/*
 * Copyright (c) 2020, The SerenityOS developers.
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

#include "DHCPv4Client.h"
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/String.h>
#include <AK/Types.h>
#include <LibCore/EventLoop.h>
#include <LibCore/File.h>
#include <LibCore/LocalServer.h>
#include <stdio.h>
#include <string.h>

static u8 mac_part(const Vector<String>& parts, size_t index)
{
    auto chars = parts.at(index).characters();
    return (chars[0] - '0') * 16 + (chars[1] - '0');
}

static MACAddress mac_from_string(const String& str)
{
    auto chunks = str.split(':');
    ASSERT(chunks.size() == 6); // should we...worry about this?
    return {
        mac_part(chunks, 0), mac_part(chunks, 1), mac_part(chunks, 2),
        mac_part(chunks, 3), mac_part(chunks, 4), mac_part(chunks, 5)
    };
}

int main(int argc, char** argv)
{
    (void)argc;
    (void)argv;

    if (pledge("stdio unix inet cpath rpath fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    Core::EventLoop event_loop;

    if (unveil("/proc/net/", "r") < 0) {
        perror("unveil");
        return 1;
    }

    unveil(nullptr, nullptr);

    auto file = Core::File::construct("/proc/net/adapters");
    if (!file->open(Core::IODevice::ReadOnly)) {
        fprintf(stderr, "Error: %s\n", file->error_string());
        return 1;
    }

    auto file_contents = file->read_all();
    auto json = JsonValue::from_string(file_contents);
    ASSERT(json.has_value());
    Vector<InterfaceDescriptor> ifnames;
    json.value().as_array().for_each([&ifnames](auto& value) {
        auto if_object = value.as_object();

        if (if_object.get("class_name").to_string() == "LoopbackAdapter")
            return;

        auto name = if_object.get("name").to_string();
        auto mac = if_object.get("mac_address").to_string();
        ifnames.append({ name, mac_from_string(mac) });
    });

    auto client = DHCPv4Client::construct(move(ifnames));

    if (pledge("stdio inet", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    return event_loop.exec();
}

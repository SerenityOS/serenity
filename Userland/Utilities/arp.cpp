/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/ByteBuffer.h>
#include <AK/JsonObject.h>
#include <LibCore/File.h>

int main()
{
    auto file = Core::File::construct("/proc/net/arp");
    if (!file->open(Core::OpenMode::ReadOnly)) {
        warnln("Failed to open {}: {}", file->name(), file->error_string());
        return 1;
    }

    outln("Address          HWaddress");
    auto file_contents = file->read_all();
    auto json = JsonValue::from_string(file_contents);
    VERIFY(json.has_value());
    json.value().as_array().for_each([](auto& value) {
        auto& if_object = value.as_object();

        auto ip_address = if_object.get("ip_address").to_string();
        auto mac_address = if_object.get("mac_address").to_string();

        outln("{:15}  {:17}", ip_address, mac_address);
    });

    return 0;
}

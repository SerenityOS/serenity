/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteBuffer.h>
#include <AK/JsonObject.h>
#include <AK/String.h>
#include <LibCore/File.h>
#include <stdio.h>

int main()
{
    auto file = Core::File::construct("/proc/net/arp");
    if (!file->open(Core::IODevice::ReadOnly)) {
        fprintf(stderr, "Error: %s\n", file->error_string());
        return 1;
    }

    printf("Address          HWaddress\n");
    auto file_contents = file->read_all();
    auto json = JsonValue::from_string(file_contents);
    VERIFY(json.has_value());
    json.value().as_array().for_each([](auto& value) {
        auto if_object = value.as_object();

        auto ip_address = if_object.get("ip_address").to_string();
        auto mac_address = if_object.get("mac_address").to_string();

        printf("%-15s  ", ip_address.characters());
        printf("%-17s  ", mac_address.characters());
        printf("\n");
    });

    return 0;
}

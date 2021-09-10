/*
 * Copyright (c) 2021, Peter Elliott <pelliott@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonObject.h>
#include <LibCore/File.h>
#include <stdio.h>
#include <unistd.h>

int main()
{
    if (pledge("stdio rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto file = Core::File::construct("/proc/cpuinfo");
    if (!file->open(Core::OpenMode::ReadOnly)) {
        perror("Core::File::open()");
        return 1;
    }

    auto buffer = file->read_all();
    auto json = JsonValue::from_string({ buffer });
    auto cpuinfo_array = json.value().as_array();
    outln("{}", cpuinfo_array.size());

    return 0;
}

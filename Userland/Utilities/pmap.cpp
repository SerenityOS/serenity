/*
 * Copyright (c) 2020, the SerenityOS developers.
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

#include <AK/ByteBuffer.h>
#include <AK/JsonObject.h>
#include <AK/QuickSort.h>
#include <AK/String.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    if (pledge("stdio rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/proc", "r") < 0) {
        perror("unveil");
        return 1;
    }

    unveil(nullptr, nullptr);

    const char* pid;
    static bool extended = false;

    Core::ArgsParser args_parser;
    args_parser.add_option(extended, "Extended output", nullptr, 'x');
    args_parser.add_positional_argument(pid, "PID", "PID", Core::ArgsParser::Required::Yes);
    args_parser.parse(argc, argv);

    auto file = Core::File::construct(String::formatted("/proc/{}/vm", pid));
    if (!file->open(Core::IODevice::ReadOnly)) {
        fprintf(stderr, "Error: %s\n", file->error_string());
        return 1;
    }

    printf("%s:\n", pid);

    if (extended) {
        printf("Address         Size   Resident      Dirty Access  VMObject Type  Purgeable   CoW Pages Name\n");
    } else {
        printf("Address         Size Access  Name\n");
    }

    auto file_contents = file->read_all();
    auto json = JsonValue::from_string(file_contents);
    VERIFY(json.has_value());

    Vector<JsonValue> sorted_regions = json.value().as_array().values();
    quick_sort(sorted_regions, [](auto& a, auto& b) {
        return a.as_object().get("address").to_u32() < b.as_object().get("address").to_u32();
    });

    for (auto& value : sorted_regions) {
        auto map = value.as_object();
        auto address = map.get("address").to_int();
        auto size = map.get("size").to_string();

        auto access = String::formatted("{}{}{}{}{}",
            (map.get("readable").to_bool() ? "r" : "-"),
            (map.get("writable").to_bool() ? "w" : "-"),
            (map.get("executable").to_bool() ? "x" : "-"),
            (map.get("shared").to_bool() ? "s" : "-"),
            (map.get("syscall").to_bool() ? "c" : "-"));

        printf("%08x  ", address);
        printf("%10s ", size.characters());
        if (extended) {
            auto resident = map.get("amount_resident").to_string();
            auto dirty = map.get("amount_dirty").to_string();
            auto vmobject = map.get("vmobject").to_string();
            if (vmobject.ends_with("VMObject"))
                vmobject = vmobject.substring(0, vmobject.length() - 8);
            auto purgeable = map.get("purgeable").to_string();
            auto cow_pages = map.get("cow_pages").to_string();
            printf("%10s ", resident.characters());
            printf("%10s ", dirty.characters());
            printf("%-6s ", access.characters());
            printf("%-14s ", vmobject.characters());
            printf("%-10s ", purgeable.characters());
            printf("%10s ", cow_pages.characters());
        } else {
            printf("%-6s ", access.characters());
        }
        auto name = map.get("name").to_string();
        printf("%-20s", name.characters());
        printf("\n");
    }

    return 0;
}

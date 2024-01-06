/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteString.h>
#include <AK/JsonObject.h>
#include <AK/QuickSort.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath"));
    TRY(Core::System::unveil("/proc", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    StringView pid;
    static bool extended = false;

    Core::ArgsParser args_parser;
    args_parser.add_option(extended, "Extended output", nullptr, 'x');
    args_parser.add_positional_argument(pid, "PID", "PID", Core::ArgsParser::Required::Yes);
    args_parser.parse(arguments);

    auto file = TRY(Core::File::open(ByteString::formatted("/proc/{}/vm", pid), Core::File::OpenMode::Read));

    outln("{}:", pid);

    auto padding = "        ";

    if (extended) {
        outln("Address{}           Size   Resident      Dirty Access  VMObject Type  Purgeable   CoW Pages Name", padding);
    } else {
        outln("Address{}           Size Access  Name", padding);
    }

    auto file_contents = TRY(file->read_until_eof());
    auto json = TRY(JsonValue::from_string(file_contents));

    Vector<JsonValue> sorted_regions = json.as_array().values();
    quick_sort(sorted_regions, [](auto& a, auto& b) {
        return a.as_object().get_addr("address"sv).value_or(0) < b.as_object().get_addr("address"sv).value_or(0);
    });

    for (auto& value : sorted_regions) {
        auto& map = value.as_object();
        auto address = map.get_addr("address"sv).value_or(0);
        auto size = map.get_u64("size"sv).value();

        auto access = ByteString::formatted("{}{}{}{}{}",
            (map.get_bool("readable"sv).value_or(false) ? "r" : "-"),
            (map.get_bool("writable"sv).value_or(false) ? "w" : "-"),
            (map.get_bool("executable"sv).value_or(false) ? "x" : "-"),
            (map.get_bool("shared"sv).value_or(false) ? "s" : "-"),
            (map.get_bool("syscall"sv).value_or(false) ? "c" : "-"));

        out("{:p}  ", address);
        out("{:>10} ", size);
        if (extended) {
            auto resident = map.get_u64("amount_resident"sv).value();
            auto dirty = map.get_u64("amount_dirty"sv).value();
            auto vmobject = map.get_byte_string("vmobject"sv).value();
            if (vmobject.ends_with("VMObject"sv))
                vmobject = vmobject.substring(0, vmobject.length() - 8);
            auto purgeable = map.get_u64("purgeable"sv).value();
            auto cow_pages = map.get_u64("cow_pages"sv).value();
            out("{:>10} ", resident);
            out("{:>10} ", dirty);
            out("{:6} ", access);
            out("{:14} ", vmobject);
            out("{:10} ", purgeable);
            out("{:>10} ", cow_pages);
        } else {
            out("{:6} ", access);
        }
        auto name = map.get_byte_string("name"sv).value_or({});
        out("{:20}", name);
        outln();
    }

    return 0;
}

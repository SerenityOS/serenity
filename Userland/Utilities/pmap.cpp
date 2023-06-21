/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/DeprecatedString.h>
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

    auto file = TRY(Core::File::open(DeprecatedString::formatted("/proc/{}/vm", pid), Core::File::OpenMode::Read));

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
        auto size = map.get("size"sv).value_or({}).to_deprecated_string();

        auto access = DeprecatedString::formatted("{}{}{}{}{}",
            (map.get_bool("readable"sv).value_or(false) ? "r" : "-"),
            (map.get_bool("writable"sv).value_or(false) ? "w" : "-"),
            (map.get_bool("executable"sv).value_or(false) ? "x" : "-"),
            (map.get_bool("shared"sv).value_or(false) ? "s" : "-"),
            (map.get_bool("syscall"sv).value_or(false) ? "c" : "-"));

        out("{:p}  ", address);
        out("{:>10} ", size);
        if (extended) {
            auto resident = map.get("amount_resident"sv).value_or({}).to_deprecated_string();
            auto dirty = map.get("amount_dirty"sv).value_or({}).to_deprecated_string();
            auto vmobject = map.get_deprecated_string("vmobject"sv).value_or({});
            if (vmobject.ends_with("VMObject"sv))
                vmobject = vmobject.substring(0, vmobject.length() - 8);
            auto purgeable = map.get("purgeable"sv).value_or({}).to_deprecated_string();
            auto cow_pages = map.get("cow_pages"sv).value_or({}).to_deprecated_string();
            out("{:>10} ", resident);
            out("{:>10} ", dirty);
            out("{:6} ", access);
            out("{:14} ", vmobject);
            out("{:10} ", purgeable);
            out("{:>10} ", cow_pages);
        } else {
            out("{:6} ", access);
        }
        auto name = map.get_deprecated_string("name"sv).value_or({});
        out("{:20}", name);
        outln();
    }

    return 0;
}

/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/DeprecatedString.h>
#include <AK/JsonObject.h>
#include <AK/QuickSort.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/Stream.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath"));
    TRY(Core::System::unveil("/proc", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    char const* pid;
    static bool extended = false;

    Core::ArgsParser args_parser;
    args_parser.add_option(extended, "Extended output", nullptr, 'x');
    args_parser.add_positional_argument(pid, "PID", "PID", Core::ArgsParser::Required::Yes);
    args_parser.parse(arguments);

    auto file = TRY(Core::Stream::File::open(DeprecatedString::formatted("/proc/{}/vm", pid), Core::Stream::OpenMode::Read));

    outln("{}:", pid);

#if ARCH(I386)
    auto padding = "";
#else
    auto padding = "        ";
#endif

    if (extended) {
        outln("Address{}           Size   Resident      Dirty Access  VMObject Type  Purgeable   CoW Pages Name", padding);
    } else {
        outln("Address{}           Size Access  Name", padding);
    }

    auto file_contents = TRY(file->read_all());
    auto json = TRY(JsonValue::from_string(file_contents));

    Vector<JsonValue> sorted_regions = json.as_array().values();
    quick_sort(sorted_regions, [](auto& a, auto& b) {
        return a.as_object().get("address"sv).to_addr() < b.as_object().get("address"sv).to_addr();
    });

    for (auto& value : sorted_regions) {
        auto& map = value.as_object();
        auto address = map.get("address"sv).to_addr();
        auto size = map.get("size"sv).to_string();

        auto access = DeprecatedString::formatted("{}{}{}{}{}",
            (map.get("readable"sv).to_bool() ? "r" : "-"),
            (map.get("writable"sv).to_bool() ? "w" : "-"),
            (map.get("executable"sv).to_bool() ? "x" : "-"),
            (map.get("shared"sv).to_bool() ? "s" : "-"),
            (map.get("syscall"sv).to_bool() ? "c" : "-"));

        out("{:p}  ", address);
        out("{:>10} ", size);
        if (extended) {
            auto resident = map.get("amount_resident"sv).to_string();
            auto dirty = map.get("amount_dirty"sv).to_string();
            auto vmobject = map.get("vmobject"sv).to_string();
            if (vmobject.ends_with("VMObject"sv))
                vmobject = vmobject.substring(0, vmobject.length() - 8);
            auto purgeable = map.get("purgeable"sv).to_string();
            auto cow_pages = map.get("cow_pages"sv).to_string();
            out("{:>10} ", resident);
            out("{:>10} ", dirty);
            out("{:6} ", access);
            out("{:14} ", vmobject);
            out("{:10} ", purgeable);
            out("{:>10} ", cow_pages);
        } else {
            out("{:6} ", access);
        }
        auto name = map.get("name"sv).to_string();
        out("{:20}", name);
        outln();
    }

    return 0;
}

/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/NumberFormat.h>
#include <AK/StringView.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath"));
    TRY(Core::System::unveil("/sys/kernel/chardev_major_allocs", "r"));
    TRY(Core::System::unveil("/sys/kernel/blockdev_major_allocs", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    Core::ArgsParser args_parser;
    args_parser.set_general_help("List major device number allocations.");
    args_parser.parse(arguments);

    outln("Character devices:");
    {
        auto file = TRY(Core::File::open("/sys/kernel/chardev_major_allocs"sv, Core::File::OpenMode::Read));
        auto file_contents = TRY(file->read_until_eof());
        auto json_result = TRY(JsonValue::from_string(file_contents));
        auto const& json = json_result.as_array();
        json.for_each([&](auto& value) {
            auto& major_number_allocation_object = value.as_object();
            auto family_name = major_number_allocation_object.get_byte_string("family_name"sv).value_or({});
            auto allocated_number = major_number_allocation_object.get_u64("allocated_number"sv).value_or(0);
            outln("{:3d} {}", allocated_number, family_name);
        });
    }

    outln();
    outln("Block devices:");
    {
        auto file = TRY(Core::File::open("/sys/kernel/blockdev_major_allocs"sv, Core::File::OpenMode::Read));
        auto file_contents = TRY(file->read_until_eof());
        auto json_result = TRY(JsonValue::from_string(file_contents));
        auto const& json = json_result.as_array();
        json.for_each([&](auto& value) {
            auto& major_number_allocation_object = value.as_object();
            auto family_name = major_number_allocation_object.get_byte_string("family_name"sv).value_or({});
            auto allocated_number = major_number_allocation_object.get_u64("allocated_number"sv).value_or(0);
            outln("{:3d} {}", allocated_number, family_name);
        });
    }

    return 0;
}

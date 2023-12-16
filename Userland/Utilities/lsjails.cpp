/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments)
{
    TRY(Core::System::pledge("stdio rpath"));
    TRY(Core::System::unveil("/sys/kernel/jails", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto jails_data = TRY(Core::File::open("/sys/kernel/jails"sv, Core::File::OpenMode::Read));

    TRY(Core::System::pledge("stdio"));

    outln("Index    Name");
    auto file_contents = TRY(jails_data->read_until_eof());
    auto json = TRY(JsonValue::from_string(file_contents));
    json.as_array().for_each([](auto& value) {
        auto& jail = value.as_object();
        auto index = jail.get_byte_string("index"sv).value_or({});
        auto name = jail.get_byte_string("name"sv).value_or({});

        outln("{:4}     {:10}", index, name);
    });

    return 0;
}

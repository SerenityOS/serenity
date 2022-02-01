/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
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
    TRY(Core::System::unveil("/proc/interrupts", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto proc_interrupts = TRY(Core::File::open("/proc/interrupts", Core::OpenMode::ReadOnly));

    TRY(Core::System::pledge("stdio"));

    outln("      CPU0");
    auto file_contents = proc_interrupts->read_all();
    auto json = TRY(JsonValue::from_string(file_contents));
    json.as_array().for_each([](auto& value) {
        auto& handler = value.as_object();
        auto purpose = handler.get("purpose").to_string();
        auto interrupt = handler.get("interrupt_line").to_string();
        auto controller = handler.get("controller").to_string();
        auto call_count = handler.get("call_count").to_string();

        outln("{:>4}: {:10} {:10}  {:30}", interrupt, call_count, controller, purpose);
    });

    return 0;
}

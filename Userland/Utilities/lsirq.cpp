/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
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
    TRY(Core::System::unveil("/sys/kernel/interrupts", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto proc_interrupts = TRY(Core::File::open("/sys/kernel/interrupts"sv, Core::File::OpenMode::Read));

    TRY(Core::System::pledge("stdio"));

    auto file_contents = TRY(proc_interrupts->read_until_eof());
    auto json = TRY(JsonValue::from_string(file_contents));

    auto cpu_count = json.as_array().at(0).as_object().get_array("per_cpu_call_counts"sv)->size();

    out("      "sv);
    for (size_t i = 0; i < cpu_count; ++i) {
        out("{:>10}", ByteString::formatted("CPU{}", i));
    }
    outln("");

    json.as_array().for_each([cpu_count](JsonValue const& value) {
        auto& handler = value.as_object();
        auto purpose = handler.get_byte_string("purpose"sv).value_or({});
        auto interrupt = handler.get_u8("interrupt_line"sv).value();
        auto controller = handler.get_byte_string("controller"sv).value_or({});
        auto call_counts = handler.get_array("per_cpu_call_counts"sv).value();

        out("{:>4}: ", interrupt);

        for (size_t i = 0; i < cpu_count; ++i)
            out("{:>10}", call_counts[i].as_integer<u64>());

        outln("  {:10}  {:30}", controller, purpose);
    });

    return 0;
}

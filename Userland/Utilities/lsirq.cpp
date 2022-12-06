/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <LibCore/Stream.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments)
{
    TRY(Core::System::pledge("stdio rpath"));
    TRY(Core::System::unveil("/sys/kernel/interrupts", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto proc_interrupts = TRY(Core::Stream::File::open("/sys/kernel/interrupts"sv, Core::Stream::OpenMode::Read));

    TRY(Core::System::pledge("stdio"));

    auto file_contents = TRY(proc_interrupts->read_all());
    auto json = TRY(JsonValue::from_string(file_contents));

    auto cpu_count = json.as_array().at(0).as_object().get("per_cpu_call_counts"sv).as_array().size();

    out("      "sv);
    for (size_t i = 0; i < cpu_count; ++i) {
        out("{:>10}", DeprecatedString::formatted("CPU{}", i));
    }
    outln("");

    json.as_array().for_each([cpu_count](JsonValue const& value) {
        auto& handler = value.as_object();
        auto purpose = handler.get("purpose"sv).to_deprecated_string();
        auto interrupt = handler.get("interrupt_line"sv).to_deprecated_string();
        auto controller = handler.get("controller"sv).to_deprecated_string();
        auto call_counts = handler.get("per_cpu_call_counts"sv).as_array();

        out("{:>4}: ", interrupt);

        for (size_t i = 0; i < cpu_count; ++i)
            out("{:>10}", call_counts[i].to_deprecated_string());

        outln("  {:10}  {:30}", controller, purpose);
    });

    return 0;
}

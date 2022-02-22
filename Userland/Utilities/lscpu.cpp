/*
 * Copyright (c) 2022, Undefine <undefine@undefine.pl>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

static void print_cpu_info(JsonObject const& value)
{
    outln("CPU {}:", value.get("processor").as_u32());
    outln("\tVendor ID: {}", value.get("cpuid").as_string());
    outln("\tModel: {}", value.get("brand").as_string());
    outln("\tFamily: {}", value.get("family").as_u32());
    outln("\tModel: {}", value.get("model").as_u32());
    outln("\tStepping: {}", value.get("stepping").as_u32());
    outln("\tType: {}", value.get("type").as_u32());
    out("\tFeatures: ");

    auto& features = value.get("features").as_array();

    for (auto const& feature : features.values())
        out("{} ", feature.as_string());

    outln();
}

ErrorOr<int> serenity_main(Main::Arguments)
{
    TRY(Core::System::pledge("stdio rpath"));

    TRY(Core::System::unveil("/proc/cpuinfo", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto file = TRY(Core::File::open("/proc/cpuinfo", Core::OpenMode::ReadOnly));
    auto json = TRY(JsonValue::from_string(file->read_all()));
    auto& array = json.as_array();

    for (size_t i = 0; i < array.size(); i++) {
        print_cpu_info(array.at(i).as_object());
        if (i != array.size() - 1)
            outln();
    }

    return 0;
}

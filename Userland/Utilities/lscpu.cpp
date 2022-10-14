/*
 * Copyright (c) 2022, Undefine <undefine@undefine.pl>
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/NumberFormat.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

static void print_cache_info(StringView description, JsonObject const& cache_object)
{
    outln("\t{}:", description);
    outln("\t\tSize: {}", human_readable_size(cache_object.get("size"sv).as_u32()));
    outln("\t\tLine size: {}", human_readable_size(cache_object.get("line_size"sv).as_u32()));
};

static void print_cpu_info(JsonObject const& value)
{
    outln("CPU {}:", value.get("processor"sv).as_u32());
    outln("\tVendor ID: {}", value.get("vendor_id"sv).as_string());
    if (value.has("hypervisor_vendor_id"sv))
        outln("\tHypervisor Vendor ID: {}", value.get("hypervisor_vendor_id"sv).as_string());
    outln("\tBrand: {}", value.get("brand"sv).as_string());
    outln("\tFamily: {}", value.get("family"sv).as_u32());
    outln("\tModel: {}", value.get("model"sv).as_u32());
    outln("\tStepping: {}", value.get("stepping"sv).as_u32());
    outln("\tType: {}", value.get("type"sv).as_u32());

    auto& caches = value.get("caches"sv).as_object();
    if (caches.has("l1_data"sv))
        print_cache_info("L1 data cache"sv, caches.get("l1_data"sv).as_object());
    if (caches.has("l1_instruction"sv))
        print_cache_info("L1 instruction cache"sv, caches.get("l1_instruction"sv).as_object());
    if (caches.has("l2"sv))
        print_cache_info("L2 cache"sv, caches.get("l2"sv).as_object());
    if (caches.has("l3"sv))
        print_cache_info("L3 cache"sv, caches.get("l3"sv).as_object());

    out("\tFeatures: ");

    auto& features = value.get("features"sv).as_array();

    for (auto const& feature : features.values())
        out("{} ", feature.as_string());

    outln();
}

ErrorOr<int> serenity_main(Main::Arguments)
{
    TRY(Core::System::pledge("stdio rpath"));

    TRY(Core::System::unveil("/sys/kernel/cpuinfo", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto file = TRY(Core::File::open("/sys/kernel/cpuinfo", Core::OpenMode::ReadOnly));
    auto json = TRY(JsonValue::from_string(file->read_all()));
    auto& array = json.as_array();

    for (size_t i = 0; i < array.size(); i++) {
        print_cpu_info(array.at(i).as_object());
        if (i != array.size() - 1)
            outln();
    }

    return 0;
}

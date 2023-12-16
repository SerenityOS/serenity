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
    outln("\t\tSize: {}", human_readable_size(cache_object.get_u32("size"sv).value()));
    outln("\t\tLine size: {}", human_readable_size(cache_object.get_u32("line_size"sv).value()));
}

static void print_cpu_info(JsonObject const& value)
{
    outln("CPU {}:", value.get_u32("processor"sv).value());
    outln("\tVendor ID: {}", value.get_byte_string("vendor_id"sv).value());
    if (value.has("hypervisor_vendor_id"sv))
        outln("\tHypervisor Vendor ID: {}", value.get_byte_string("hypervisor_vendor_id"sv).value());
    outln("\tBrand: {}", value.get_byte_string("brand"sv).value());
    outln("\tFamily: {}", value.get_u32("family"sv).value());
    outln("\tModel: {}", value.get_u32("model"sv).value());
    outln("\tStepping: {}", value.get_u32("stepping"sv).value());
    outln("\tType: {}", value.get_u32("type"sv).value());

    auto& caches = value.get_object("caches"sv).value();
    if (caches.has("l1_data"sv))
        print_cache_info("L1 data cache"sv, caches.get_object("l1_data"sv).value());
    if (caches.has("l1_instruction"sv))
        print_cache_info("L1 instruction cache"sv, caches.get_object("l1_instruction"sv).value());
    if (caches.has("l2"sv))
        print_cache_info("L2 cache"sv, caches.get_object("l2"sv).value());
    if (caches.has("l3"sv))
        print_cache_info("L3 cache"sv, caches.get_object("l3"sv).value());

    out("\tFeatures: ");

    auto& features = value.get_array("features"sv).value();

    for (auto const& feature : features.values())
        out("{} ", feature.as_string());

    outln();
}

ErrorOr<int> serenity_main(Main::Arguments)
{
    TRY(Core::System::pledge("stdio rpath"));

    TRY(Core::System::unveil("/sys/kernel/cpuinfo", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto file = TRY(Core::File::open("/sys/kernel/cpuinfo"sv, Core::File::OpenMode::Read));
    auto file_contents = TRY(file->read_until_eof());
    auto json = TRY(JsonValue::from_string(file_contents));
    auto const& array = json.as_array();

    for (size_t i = 0; i < array.size(); i++) {
        print_cpu_info(array.at(i).as_object());
        if (i != array.size() - 1)
            outln();
    }

    return 0;
}

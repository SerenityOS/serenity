/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/NumberFormat.h>
#include <AK/String.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <unistd.h>

static inline u64 page_count_to_bytes(size_t count)
{
    return count * PAGE_SIZE;
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::unveil("/sys/kernel/memstat", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    TRY(Core::System::pledge("stdio rpath"));

    bool flag_human_readable = false;
    Core::ArgsParser args_parser;
    args_parser.add_option(flag_human_readable, "Print human-readable sizes", "human-readable", 'h');
    args_parser.parse(arguments);

    auto proc_memstat = TRY(Core::File::open("/sys/kernel/memstat"sv, Core::File::OpenMode::Read));
    auto file_contents = TRY(proc_memstat->read_until_eof());
    auto json_result = TRY(JsonValue::from_string(file_contents));
    auto const& json = json_result.as_object();

    u32 kmalloc_allocated = json.get_u32("kmalloc_allocated"sv).value_or(0);
    u32 kmalloc_available = json.get_u32("kmalloc_available"sv).value_or(0);
    u64 physical_allocated = json.get_u64("physical_allocated"sv).value_or(0);
    u64 physical_available = json.get_u64("physical_available"sv).value_or(0);
    u64 physical_committed = json.get_u64("physical_committed"sv).value_or(0);
    u64 physical_uncommitted = json.get_u64("physical_uncommitted"sv).value_or(0);
    u32 kmalloc_call_count = json.get_u32("kmalloc_call_count"sv).value_or(0);
    u32 kfree_call_count = json.get_u32("kfree_call_count"sv).value_or(0);

    u64 kmalloc_bytes_total = kmalloc_allocated + kmalloc_available;
    u64 physical_pages_total = physical_allocated + physical_available;
    u64 physical_pages_in_use = physical_allocated;

    if (flag_human_readable) {
        outln("Kmalloc allocated: {}", TRY(String::formatted("{} / {}", human_readable_size_long(kmalloc_allocated, UseThousandsSeparator::Yes), human_readable_size_long(kmalloc_bytes_total, UseThousandsSeparator::Yes))));
        outln("Physical pages (in use) count: {}", TRY(String::formatted("{} / {}", human_readable_size_long(page_count_to_bytes(physical_pages_in_use), UseThousandsSeparator::Yes), human_readable_size_long(page_count_to_bytes(physical_pages_total), UseThousandsSeparator::Yes))));
        outln("Physical pages (committed) count: {}", TRY(String::formatted("{}", human_readable_size_long(page_count_to_bytes(physical_committed), UseThousandsSeparator::Yes))));
        outln("Physical pages (uncommitted) count: {}", TRY(String::formatted("{}", human_readable_size_long(page_count_to_bytes(physical_uncommitted), UseThousandsSeparator::Yes))));
        outln("Physical pages (total) count: {:'}", physical_pages_total);
    } else {
        outln("Kmalloc allocated: {}", TRY(String::formatted("{}/{}", kmalloc_allocated, kmalloc_bytes_total)));
        outln("Physical pages (in use) count: {}", TRY(String::formatted("{}/{}", page_count_to_bytes(physical_pages_in_use), page_count_to_bytes(physical_pages_total))));
        outln("Physical pages (committed) count: {}", TRY(String::formatted("{}", page_count_to_bytes(physical_committed))));
        outln("Physical pages (uncommitted) count: {}", TRY(String::formatted("{}", page_count_to_bytes(physical_uncommitted))));
        outln("Physical pages (total) count: {}", physical_pages_total);
    }
    outln("Kmalloc call count: {}", kmalloc_call_count);
    outln("Kfree call count: {}", kfree_call_count);
    outln("Kmalloc/Kfree delta: {}", TRY(String::formatted("{:+}", kmalloc_call_count - kfree_call_count)));
    return 0;
}

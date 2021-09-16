/*
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteBuffer.h>
#include <AK/JsonObject.h>
#include <AK/JsonParser.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/Stream.h>
#include <stdio.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/proc/memstat", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil(nullptr, nullptr) < 0) {
        perror("unveil");
        return 1;
    }

    bool megabytes = false;

    Core::ArgsParser parser;
    parser.set_general_help("Displays the used and available memory on the system.");
    parser.add_option(megabytes, "Display values in megabytes", "megabytes", 'm');

    parser.parse(argc, argv);

    auto maybe_file = Core::Stream::File::open("/proc/memstat"sv, Core::Stream::OpenMode::Read);
    if (maybe_file.is_error()) {
        warnln("Could not open /proc/memstat: {}", maybe_file.error().to_string());
        return 1;
    }
    auto file = maybe_file.release_value();

    auto buffer = ByteBuffer::create_uninitialized(16384).release_value();
    auto maybe_nread = file.read(buffer);
    VERIFY(!maybe_nread.is_error());
    auto nread = maybe_nread.value();

    JsonParser json_parser { { buffer.data(), nread } };
    auto stats = json_parser.parse().release_value().as_object();

    unsigned kmalloc_allocated = stats.get("kmalloc_allocated").to_u32();
    unsigned kmalloc_available = stats.get("kmalloc_available").to_u32();
    unsigned user_physical_allocated = stats.get("user_physical_allocated").to_u64();
    unsigned user_physical_committed = stats.get("user_physical_committed").to_u64();
    unsigned user_physical_uncommitted = stats.get("user_physical_uncommitted").to_u64();

    size_t kmalloc_bytes_total = kmalloc_allocated + kmalloc_available;
    size_t total_userphysical_and_swappable_pages = user_physical_allocated + user_physical_committed + user_physical_uncommitted;

    size_t total_memory = (total_userphysical_and_swappable_pages)*4096 + kmalloc_bytes_total;
    size_t available_memory = stats.get("user_physical_available").to_u64() * 4096;
    size_t used_memory = total_memory - available_memory;

    unsigned divider = megabytes ? 1048576 : 1;

    outln("               total        used   available");
    outln("Mem:     {: >11} {: >11} {: >11}", total_memory / divider, used_memory / divider, available_memory / divider);

    return 0;
}

/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/NumberFormat.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibMain/Main.h>
#include <inttypes.h>
#include <stdlib.h>

struct FileSystem {
    ByteString fs;
    size_t total_block_count { 0 };
    size_t free_block_count { 0 };
    size_t total_inode_count { 0 };
    size_t free_inode_count { 0 };
    size_t block_size { 0 };
    ByteString mount_point;
};

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    bool flag_human_readable = false;
    bool flag_human_readable_si = false;
    bool flag_inode_info = false;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Display free disk space of each partition.");
    args_parser.add_option(flag_human_readable, "Print human-readable sizes", "human-readable", 'h');
    args_parser.add_option(flag_human_readable_si, "Print human-readable sizes in SI units", "si", 'H');
    args_parser.add_option(flag_inode_info, "Show inode information as well", "inodes", 'i');
    args_parser.parse(arguments);

    auto file = TRY(Core::File::open("/sys/kernel/df"sv, Core::File::OpenMode::Read));

    Vector<StringView> headers;
    TRY(headers.try_append(flag_human_readable ? "Size"sv : "Blocks"sv));
    TRY(headers.try_append("Used"sv));
    TRY(headers.try_append("Available"sv));
    TRY(headers.try_append("Used%"sv));
    if (flag_inode_info) {
        TRY(headers.try_append("Inodes"sv));
        TRY(headers.try_append("IUsed"sv));
        TRY(headers.try_append("IAvailable"sv));
        TRY(headers.try_append("IUsed%"sv));
    }
    TRY(headers.try_append("Mount point"sv));

    out("{:12} ", "Filesystem");

    for (auto& header : headers)
        out("{:>12} ", header);
    outln();

    auto file_contents = TRY(file->read_until_eof());
    auto json_result = TRY(JsonValue::from_string(file_contents));
    auto const& json = json_result.as_array();
    json.for_each([&](auto& value) {
        auto& fs_object = value.as_object();
        auto fs = fs_object.get_byte_string("class_name"sv).value_or({});
        auto total_block_count = fs_object.get_u64("total_block_count"sv).value_or(0);
        auto free_block_count = fs_object.get_u64("free_block_count"sv).value_or(0);
        auto used_block_count = total_block_count - free_block_count;
        auto total_inode_count = fs_object.get_u64("total_inode_count"sv).value_or(0);
        auto free_inode_count = fs_object.get_u64("free_inode_count"sv).value_or(0);
        auto used_inode_count = total_inode_count - free_inode_count;
        auto block_size = fs_object.get_u64("block_size"sv).value_or(0);
        auto mount_point = fs_object.get_byte_string("mount_point"sv).value_or({});

        auto used_percentage = 100;
        if (total_block_count != 0)
            used_percentage = (used_block_count * 100) / total_block_count;

        auto used_inode_percentage = 100;
        if (total_inode_count != 0)
            used_inode_percentage = (used_inode_count * 100) / total_inode_count;

        out("{:12} ", fs);

        bool human_readable = flag_human_readable || flag_human_readable_si;
        auto human_readable_based_on = flag_human_readable_si ? AK::HumanReadableBasedOn::Base10 : AK::HumanReadableBasedOn::Base2;

        if (human_readable) {
            out("{:>12} ", human_readable_size(total_block_count * block_size, human_readable_based_on));
            out("{:>12} ", human_readable_size(used_block_count * block_size, human_readable_based_on));
            out("{:>12} ", human_readable_size(free_block_count * block_size, human_readable_based_on));
            out("{:>11}% ", used_percentage);
        } else {
            out("{:>12} ", (uint64_t)total_block_count);
            out("{:>12} ", (uint64_t)used_block_count);
            out("{:>12} ", (uint64_t)free_block_count);
            out("{:>11}% ", used_percentage);
        }

        if (flag_inode_info) {
            if (human_readable) {
                out("{:>12} ", human_readable_quantity(total_inode_count, human_readable_based_on));
                out("{:>12} ", human_readable_quantity(used_inode_count, human_readable_based_on));
                out("{:>12} ", human_readable_quantity(free_inode_count, human_readable_based_on));
                out("{:>11}% ", used_inode_percentage);
            } else {
                out("{:>12} ", (uint64_t)total_inode_count);
                out("{:>12} ", (uint64_t)used_inode_count);
                out("{:>12} ", (uint64_t)free_inode_count);
                out("{:>11}% ", used_inode_percentage);
            }
        }

        out("{}", mount_point);
        outln();
    });

    return 0;
}

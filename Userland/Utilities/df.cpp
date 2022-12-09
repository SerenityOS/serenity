/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/NumberFormat.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/Stream.h>
#include <LibMain/Main.h>
#include <inttypes.h>
#include <stdlib.h>

static bool flag_human_readable = false;

struct FileSystem {
    DeprecatedString fs;
    size_t total_block_count { 0 };
    size_t free_block_count { 0 };
    size_t total_inode_count { 0 };
    size_t free_inode_count { 0 };
    size_t block_size { 0 };
    DeprecatedString mount_point;
};

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    Core::ArgsParser args_parser;
    args_parser.set_general_help("Display free disk space of each partition.");
    args_parser.add_option(flag_human_readable, "Print human-readable sizes", "human-readable", 'h');
    args_parser.parse(arguments);

    auto file = TRY(Core::Stream::File::open("/sys/kernel/df"sv, Core::Stream::OpenMode::Read));

    if (flag_human_readable) {
        outln("Filesystem      Size        Used    Available        Used%   Mount point");
    } else {
        outln("Filesystem    Blocks        Used    Available        Used%   Mount point");
    }

    auto file_contents = TRY(file->read_until_eof());
    auto json_result = TRY(JsonValue::from_string(file_contents));
    auto const& json = json_result.as_array();
    json.for_each([](auto& value) {
        auto& fs_object = value.as_object();
        auto fs = fs_object.get("class_name"sv).to_deprecated_string();
        auto total_block_count = fs_object.get("total_block_count"sv).to_u64();
        auto free_block_count = fs_object.get("free_block_count"sv).to_u64();
        auto used_block_count = total_block_count - free_block_count;
        [[maybe_unused]] auto total_inode_count = fs_object.get("total_inode_count"sv).to_u64();
        [[maybe_unused]] auto free_inode_count = fs_object.get("free_inode_count"sv).to_u64();
        auto block_size = fs_object.get("block_size"sv).to_u64();
        auto mount_point = fs_object.get("mount_point"sv).to_deprecated_string();

        auto used_percentage = 100;
        if (total_block_count != 0)
            used_percentage = (used_block_count * 100) / total_block_count;

        out("{:10}", fs);

        if (flag_human_readable) {
            out("{:>10}  ", human_readable_size(total_block_count * block_size));
            out("{:>10}   ", human_readable_size(used_block_count * block_size));
            out("{:>10}   ", human_readable_size(free_block_count * block_size));
            out("{:>9}%   ", used_percentage);
        } else {
            out("{:>10}  ", (uint64_t)total_block_count);
            out("{:>10}   ", (uint64_t)used_block_count);
            out("{:>10}   ", (uint64_t)free_block_count);
            out("{:>9}%   ", used_percentage);
        }

        out("{}", mount_point);
        outln();
    });

    return 0;
}

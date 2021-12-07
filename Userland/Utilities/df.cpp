/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/NumberFormat.h>
#include <AK/String.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibMain/Main.h>
#include <inttypes.h>
#include <stdlib.h>

static bool flag_human_readable = false;

struct FileSystem {
    String fs;
    size_t total_block_count { 0 };
    size_t free_block_count { 0 };
    size_t total_inode_count { 0 };
    size_t free_inode_count { 0 };
    size_t block_size { 0 };
    String mount_point;
};

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    Core::ArgsParser args_parser;
    args_parser.set_general_help("Display free disk space of each partition.");
    args_parser.add_option(flag_human_readable, "Print human-readable sizes", "human-readable", 'h');
    args_parser.parse(arguments);

    auto file = TRY(Core::File::open("/proc/df", Core::OpenMode::ReadOnly));

    if (flag_human_readable) {
        outln("Filesystem      Size        Used    Available   Mount point");
    } else {
        outln("Filesystem    Blocks        Used    Available   Mount point");
    }

    auto file_contents = file->read_all();
    auto json_result = TRY(JsonValue::from_string(file_contents));
    auto const& json = json_result.as_array();
    json.for_each([](auto& value) {
        auto& fs_object = value.as_object();
        auto fs = fs_object.get("class_name").to_string();
        auto total_block_count = fs_object.get("total_block_count").to_u64();
        auto free_block_count = fs_object.get("free_block_count").to_u64();
        [[maybe_unused]] auto total_inode_count = fs_object.get("total_inode_count").to_u64();
        [[maybe_unused]] auto free_inode_count = fs_object.get("free_inode_count").to_u64();
        auto block_size = fs_object.get("block_size").to_u64();
        auto mount_point = fs_object.get("mount_point").to_string();

        out("{:10}", fs);

        if (flag_human_readable) {
            out("{:>10}  ", human_readable_size(total_block_count * block_size));
            out("{:>10}   ", human_readable_size((total_block_count - free_block_count) * block_size));
            out("{:>10}   ", human_readable_size(free_block_count * block_size));
        } else {
            out("{:>10}  ", (uint64_t)total_block_count);
            out("{:>10}   ", (uint64_t)(total_block_count - free_block_count));
            out("{:>10}   ", (uint64_t)free_block_count);
        }

        out("{}", mount_point);
        outln();
    });

    return 0;
}

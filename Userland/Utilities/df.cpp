/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteBuffer.h>
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/NumberFormat.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <fcntl.h>
#include <inttypes.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

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

int main(int argc, char** argv)
{
    Core::ArgsParser args_parser;
    args_parser.set_general_help("Display free disk space of each partition.");
    args_parser.add_option(flag_human_readable, "Print human-readable sizes", "human-readable", 'h');
    args_parser.parse(argc, argv);

    auto file = Core::File::construct("/proc/df");
    if (!file->open(Core::IODevice::ReadOnly)) {
        fprintf(stderr, "Failed to open /proc/df: %s\n", file->error_string());
        return 1;
    }

    if (flag_human_readable) {
        printf("Filesystem      Size        Used    Available   Mount point\n");
    } else {
        printf("Filesystem    Blocks        Used    Available   Mount point\n");
    }

    auto file_contents = file->read_all();
    auto json_result = JsonValue::from_string(file_contents);
    VERIFY(json_result.has_value());
    auto json = json_result.value().as_array();
    json.for_each([](auto& value) {
        auto fs_object = value.as_object();
        auto fs = fs_object.get("class_name").to_string();
        auto total_block_count = fs_object.get("total_block_count").to_u64();
        auto free_block_count = fs_object.get("free_block_count").to_u64();
        [[maybe_unused]] auto total_inode_count = fs_object.get("total_inode_count").to_u64();
        [[maybe_unused]] auto free_inode_count = fs_object.get("free_inode_count").to_u64();
        auto block_size = fs_object.get("block_size").to_u64();
        auto mount_point = fs_object.get("mount_point").to_string();

        printf("%-10s", fs.characters());

        if (flag_human_readable) {
            printf("%10s  ", human_readable_size(total_block_count * block_size).characters());
            printf("%10s   ", human_readable_size((total_block_count - free_block_count) * block_size).characters());
            printf("%10s   ", human_readable_size(free_block_count * block_size).characters());
        } else {
            printf("%10" PRIu64 "  ", (uint64_t)total_block_count);
            printf("%10" PRIu64 "   ", (uint64_t)(total_block_count - free_block_count));
            printf("%10" PRIu64 "   ", (uint64_t)free_block_count);
        }

        printf("%s", mount_point.characters());
        printf("\n");
    });

    return 0;
}

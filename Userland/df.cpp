/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/String.h>
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/Vector.h>
#include <LibCore/CFile.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

struct FileSystem {
    String fs;
    size_t total_block_count { 0 };
    size_t free_block_count { 0 };
    size_t total_inode_count { 0 };
    size_t free_inode_count { 0 };
    String mount_point;
};

int main(int, char**)
{
    auto file = Core::File::construct("/proc/df");
    if (!file->open(Core::IODevice::ReadOnly)) {
        fprintf(stderr, "Failed to open /proc/df: %s\n", file->error_string());
        return 1;
    }
    printf("Filesystem    Blocks        Used    Available   Mount point\n");

    auto file_contents = file->read_all();
    auto json = JsonValue::from_string(file_contents).as_array();
    json.for_each([](auto& value) {
        auto fs_object = value.as_object();
        auto fs = fs_object.get("class_name").to_string();
        auto total_block_count = fs_object.get("total_block_count").to_u32();
        auto free_block_count = fs_object.get("free_block_count").to_u32();
        auto total_inode_count = fs_object.get("total_inode_count").to_u32();
        auto free_inode_count = fs_object.get("free_inode_count").to_u32();
        auto mount_point = fs_object.get("mount_point").to_string();

        (void)total_inode_count;
        (void)free_inode_count;

        printf("%-10s", fs.characters());
        printf("%10u  ", total_block_count);
        printf("%10u   ", total_block_count - free_block_count);
        printf("%10u   ", free_block_count);
        printf("%s", mount_point.characters());
        printf("\n");
    });

    return 0;
}

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
    CFile file("/proc/df");
    if (!file.open(CIODevice::ReadOnly)) {
        fprintf(stderr, "Failed to open /proc/df: %s\n", file.error_string());
        return 1;
    }
    printf("Filesystem    Blocks        Used    Available   Mount point\n");

    auto file_contents = file.read_all();
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

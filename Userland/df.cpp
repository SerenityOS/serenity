#include <AK/AKString.h>
#include <AK/Vector.h>
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
    FILE* fp = fopen("/proc/df", "r");
    if (!fp) {
        perror("failed to open /proc/df");
        return 1;
    }
    printf("Filesystem    Blocks        Used    Available   Mount point\n");
    for (;;) {
        char buf[4096];
        char* ptr = fgets(buf, sizeof(buf), fp);
        if (!ptr)
            break;
        auto parts = String(buf, Chomp).split(',');
        if (parts.size() < 6)
            break;
        bool ok;
        String fs = parts[0];
        unsigned total_block_count = parts[1].to_uint(ok);
        ASSERT(ok);
        unsigned free_block_count = parts[2].to_uint(ok);
        ASSERT(ok);
        unsigned total_inode_count = parts[3].to_uint(ok);
        ASSERT(ok);
        unsigned free_inode_count = parts[4].to_uint(ok);
        ASSERT(ok);
        String mount_point = parts[5];

        (void)total_inode_count;
        (void)free_inode_count;

        printf("%-10s", fs.characters());
        printf("%10u  ", total_block_count);
        printf("%10u   ", total_block_count - free_block_count);
        printf("%10u   ", free_block_count);
        printf("%s", mount_point.characters());
        printf("\n");
    }
    int rc = fclose(fp);
    ASSERT(rc == 0);
    return 0;
}

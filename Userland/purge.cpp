#include <Kernel/Syscall.h>
#include <serenity.h>
#include <stdio.h>
#include <string.h>

int main(int argc, char** argv)
{
    int mode = 0;
    if (argc == 1) {
        mode = PURGE_ALL_VOLATILE | PURGE_ALL_CLEAN_INODE;
    } else {
        if (!strcmp(argv[1], "-c")) {
            mode = PURGE_ALL_CLEAN_INODE;
        } else if (!strcmp(argv[1], "-v")) {
            mode = PURGE_ALL_VOLATILE;
        } else {
            fprintf(stderr, "Unknown option: %s\n", argv[1]);
            return 1;
        }
    }
    int purged_page_count = purge(mode);
    if (purged_page_count < 0) {
        perror("purge");
        return 1;
    }
    printf("Purged page count: %d\n", purged_page_count);
    return 0;
}

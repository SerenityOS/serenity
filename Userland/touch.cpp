#include <stdio.h>
#include <utime.h>
#include <sys/types.h>

int main(int argc, char** argv)
{
    if (argc != 2) {
        fprintf(stderr, "usage: touch <path>\n");
        return 1;
    }
    int rc = utime(argv[1], nullptr);
    if (rc < 0)
        perror("utime");
    return 0;
}


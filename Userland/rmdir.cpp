#include <errno.h>
#include <stdio.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (argc != 2) {
        fprintf(stderr, "usage: rmdir <path>\n");
        return 1;
    }
    int rc = rmdir(argv[1]);
    if (rc < 0) {
        perror("rmdir");
        return 1;
    }
    return 0;
}

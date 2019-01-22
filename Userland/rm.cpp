#include <stdio.h>
#include <errno.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (argc != 2) {
        fprintf(stderr, "usage: rm <path>\n");
        return 1;
    }
    int rc = unlink(argv[1]);
    if (rc < 0) {
        perror("unlink");
        return 1;
    }
    return 0;
}


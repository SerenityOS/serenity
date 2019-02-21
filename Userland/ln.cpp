#include <stdio.h>
#include <errno.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (argc != 3) {
        fprintf(stderr, "usage: ln <old-path> <new-path>\n");
        return 1;
    }
    int rc = link(argv[1], argv[2]);
    if (rc < 0) {
        perror("link");
        return 1;
    }
    return 0;
}


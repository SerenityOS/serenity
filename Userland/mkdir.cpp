#include <stdio.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <stdlib.h>
#include <assert.h>
#include <sys/stat.h>

int main(int argc, char** argv)
{
    if (argc != 2) {
        printf("usage: mkdir <path>\n");
        return 1;
    }
    int rc = mkdir(argv[1], 0755);
    if (rc < 0) {
        perror("mkdir");
        return 1;
    }
    return 0;
}

#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <unistd.h>
#include <utime.h>

static bool file_exists(const char* path)
{
    struct stat st;
    int rc = stat(path, &st);
    if (rc < 0) {
        if (errno == ENOENT)
            return false;
    }
    if (rc == 0) {
        return true;
    }
    perror("stat");
    exit(1);
}

int main(int argc, char** argv)
{
    if (argc != 2) {
        fprintf(stderr, "usage: touch <path>\n");
        return 1;
    }
    if (file_exists(argv[1])) {
        int rc = utime(argv[1], nullptr);
        if (rc < 0)
            perror("utime");
    } else {
        int fd = open(argv[1], O_CREAT, 0100644);
        if (fd < 0) {
            perror("open");
            return 1;
        }
        int rc = close(fd);
        if (rc < 0) {
            perror("close");
            return 1;
        }
    }
    return 0;
}

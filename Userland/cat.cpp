#include <stdio.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <stdlib.h>
#include <fcntl.h>
#include <assert.h>

int main(int argc, char** argv)
{
    if (argc != 2) {
        printf("usage: cat <file>\n");
        return 1;
    }
    int fd = open(argv[1], O_RDONLY);
    if (fd == -1) {
        printf("failed to open %s: %s\n", argv[1], strerror(errno));
        return 1;
    }
    for (;;) {
        char buf[4096];
        ssize_t nread = read(fd, buf, sizeof(buf));
        if (nread == 0)
            break;
        if (nread < 0) {
            printf("read() error: %s\n", strerror(errno));
            return 2;
        }
        write(1, buf, nread);
    }
    return 0;
}

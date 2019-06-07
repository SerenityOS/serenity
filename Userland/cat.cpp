#include <assert.h>
#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    int fd = argc > 1 ? open(argv[1], O_RDONLY) : 0;
    if (fd == -1) {
        fprintf(stderr, "Failed to open %s: %s\n", argv[1], strerror(errno));
        return 1;
    }
    for (;;) {
        char buf[4096];
        ssize_t nread = read(fd, buf, sizeof(buf));
        if (nread == 0)
            break;
        if (nread < 0) {
            perror("read");
            return 2;
        }
        ssize_t nwritten = write(1, buf, nread);
        if (nwritten < 0) {
            perror("write");
            return 3;
        }
        ASSERT(nwritten == nread);
    }
    return 0;
}

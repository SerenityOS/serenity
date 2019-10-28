#include <assert.h>
#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    int i = 1;
    int fd = 0;
    char buf[4096];
    do {
        if (argc > 1 && (fd = open(argv[i], O_RDONLY)) == -1) {
            fprintf(stderr, "Failed to open %s: %s\n", argv[i], strerror(errno));
            continue;
        }
        for (;;) {
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
    } while (++i < argc);
    return 0;
}

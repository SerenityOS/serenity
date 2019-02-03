#include <stdio.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <stdlib.h>
#include <fcntl.h>
#include <assert.h>

int main(int argc, char** argv)
{
    const char* input_file = argc > 1 ? argv[1] : "/dev/stdin";
    int fd = open(input_file, O_RDONLY);
    if (fd == -1) {
        printf("failed to open %s: %s\n", input_file, strerror(errno));
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

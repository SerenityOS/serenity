#include <LibC/stdio.h>
#include <LibC/unistd.h>

int main(int c, char** v)
{
    int fd = open("/proc/summary");
    if (fd == -1) {
        printf("failed to open /proc/summary :(\n");
        return 1;
    }
    for (;;) {
        char buf[16];
        ssize_t nread = read(fd, buf, sizeof(buf));
        if (nread == 0)
            break;
        if (nread < 0) {
            printf("failed to read :(\n");
            return 2;
        }
        for (ssize_t i = 0; i < nread; ++i) {
            putchar(buf[i]);
        }
    }
    return 0;
}

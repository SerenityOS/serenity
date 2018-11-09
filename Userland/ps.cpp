#include <LibC/stdio.h>
#include <LibC/unistd.h>

int main(int argc, char** argv)
{
    (void) argc;
    (void) argv;
    int fd = open("/proc/summary", O_RDONLY);
    if (fd == -1) {
        perror("failed to open /proc/summary");
        return 1;
    }
    for (;;) {
        char buf[128];
        ssize_t nread = read(fd, buf, sizeof(buf));
        if (nread == 0)
            break;
        if (nread < 0) {
            perror("failed to read");
            return 2;
        }
        for (ssize_t i = 0; i < nread; ++i) {
            putchar(buf[i]);
        }
    }
    return 0;
}

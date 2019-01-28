#include <stdio.h>
#include <unistd.h>
#include <fcntl.h>
#include <assert.h>

int main(int argc, char** argv)
{
    (void) argc;
    (void) argv;
    int fd = open("/proc/dmesg", O_RDONLY);
    if (fd < 0) {
        perror("open /proc/dmesg");
        return 1;
    }
    for (;;) {
        char buffer[BUFSIZ];
        ssize_t nread = read(fd, buffer, sizeof(buffer));
        if (nread < 0) {
            perror("read");
            return 1;
        }
        if (nread == 0) {
            break;
        }
        ssize_t nwritten = write(1, buffer, nread);
        assert(nwritten == nread);
    }
    int rc = close(fd);
    assert(rc == 0);
    return 0;
}

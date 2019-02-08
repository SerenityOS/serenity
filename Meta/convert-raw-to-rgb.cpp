#include <stdio.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <fcntl.h>

int main(int argc, char**argv)
{
    int fd = open(argv[1], O_RDONLY);
    if (fd < 0) {
        perror("open");
        return 1;
    }
    for (;;) {
        unsigned buffer;
        ssize_t nread = read(fd, &buffer, sizeof(buffer));
        if (nread == 0)
            break;
        if (nread < 0) {
            perror("read");
            return 1;
        }
        unsigned converted = buffer & 0xff00ff00;
        converted |= (buffer & 0xff0000) >> 16;
        converted |= (buffer & 0xff) << 16;
        write(1, &converted, sizeof(unsigned));
    }
    int rc = close(fd);
    if (rc < 0) {
        perror("close");
        return 1;
    }
    return 0;
}

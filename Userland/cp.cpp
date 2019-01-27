#include <unistd.h>
#include <fcntl.h>
#include <assert.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    if (argc != 3) {
        printf("usage: cp <source> <destination>\n");
        return 0;
    }
    int src_fd = open(argv[1], O_RDONLY);
    if (src_fd < 0) {
        perror("open src");
        return 1;
    }
    int dst_fd = open(argv[2], O_WRONLY | O_CREAT);
    if (dst_fd < 0) {
        perror("open dst");
        return 1;
    }

    for (;;) {
        char buffer[BUFSIZ];
        ssize_t nread = read(src_fd, buffer, sizeof(buffer));
        if (nread < 0) {
            perror("read src");
            return 1;
        }
        if (nread == 0)
            break;
        ssize_t nwritten = write(dst_fd, buffer, nread);
        if (nwritten < 0) {
            perror("write dst");
            return 1;
        }
        assert(nwritten != 0);
    }
    close(src_fd);
    close(dst_fd);
    return 0;
}

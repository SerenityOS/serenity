#include <AK/LogStream.h>
#include <fcntl.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    const char* path = argc > 1 ? argv[1] : ".";
    int watch_fd = watch_file(path, strlen(path));
    if (watch_fd < 0) {
        perror("Unable to watch");
        return 1;
    }
    for (;;) {
        char buffer[256];
        int rc = read(watch_fd, buffer, sizeof(buffer));
        if (rc < 0) {
            perror("read");
            return 1;
        }
        if (rc == 0) {
            printf("End-of-file.\n");
            return 0;
        }
        printf("Something changed about '%s'\n", path);
    }
    return 0;
}

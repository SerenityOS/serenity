#include <fcntl.h>
#include <stdio.h>
#include <sys/ioctl.h>
#include <unistd.h>

static int key_fd;

void wait_for_key()
{
    printf("\033[7m--[ more ]--\033[0m");
    fflush(stdout);
    char dummy;
    read(key_fd, &dummy, 1);
    printf("\n");
}

int main(int argc, char** argv)
{
    (void)argc;
    (void)argv;

    if (pledge("stdio rpath tty", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    key_fd = open(ttyname(1), O_RDONLY);
    if (key_fd < 0) {
        perror("open");
        return 1;
    }

    struct winsize ws;
    ioctl(1, TIOCGWINSZ, &ws);

    if (pledge("stdio", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    unsigned lines_printed = 0;
    while (!feof(stdin)) {
        char buffer[BUFSIZ];
        auto* str = fgets(buffer, sizeof(buffer), stdin);
        if (!str)
            break;
        printf("%s", str);
        ++lines_printed;
        if ((lines_printed % (ws.ws_row - 1)) == 0) {
            wait_for_key();
        }
    }

    close(key_fd);
    return 0;
}

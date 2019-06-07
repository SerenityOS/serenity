#include <stdio.h>
#include <sys/ioctl.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    (void)argc;
    (void)argv;

    struct winsize ws;
    int rc = ioctl(0, TIOCGWINSZ, &ws);
    if (rc < 0) {
        perror("ioctl(TIOCGWINSZ)");
    }
    printf("TTY is %s\n", ttyname(0));
    printf("Terminal size is %ux%u\n", ws.ws_col, ws.ws_row);

    printf("Counting to 100000: \033[s");
    for (unsigned i = 0; i <= 100000; ++i) {
        printf("\033[u\033[s%u", i);
    }
    printf("\n");
    return 0;
}

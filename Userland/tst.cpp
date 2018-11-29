#include <stdio.h>
#include <sys/ioctl.h>

int main(int argc, char** argv)
{
    (void) argc;
    (void) argv;

    struct winsize ws;
    ioctl(0, TIOCGWINSZ, &ws);
    printf("Terminal is %ux%u\n", ws.ws_col, ws.ws_row);

    printf("Counting to 100000: \033[s");
    for (unsigned i = 0; i <= 100000; ++i) {
        printf("\033[u\033[s%u", i);
    }
    printf("\n");
    return 0;
}

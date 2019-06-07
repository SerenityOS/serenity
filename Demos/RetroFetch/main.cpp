#include <AK/Assertions.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

static void moveto(int row, int column)
{
    printf("\033[%d;%dH", row, column);
    fflush(stdout);
}

int main()
{
    printf("\033[3J\033[H\033[2J");
    fflush(stdout);
    int fd = open("/res/serenity.ansi.txt", O_RDONLY);
    if (fd < 0) {
        perror("open");
        return 0;
    }
    for (;;) {
        char buffer[BUFSIZ];
        int nread = read(fd, buffer, sizeof(buffer));
        if (nread < 0) {
            perror("read");
            return 1;
        }
        if (nread == 0)
            break;
        int nwritten = write(STDOUT_FILENO, buffer, nread);
        if (nwritten < 0) {
            perror("write");
            return 1;
        }
        ASSERT(nwritten == nread);
    }
    close(fd);

    printf("\n");
    printf("\033[s");

    char hostname[128];
    int rc = gethostname(hostname, sizeof(hostname));
    if (rc < 0) {
        perror("gethostname");
        return 1;
    }

    int column = 42;

    moveto(3, column);
    printf("%s@%s\n", getlogin(), hostname);

    moveto(4, column);
    printf("\033[34;1mOS:\033[0m Serenity\n");

    moveto(5, column);
    printf("\033[34;1mKernel:\033[0m ");
    fflush(stdout);
    system("uname -nrm");

    moveto(6, column);
    printf("\033[34;1mUptime:\033[0m ");
    fflush(stdout);
    system("uptime");

    printf("\033[u\n");
    return 0;
}

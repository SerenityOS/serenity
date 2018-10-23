#include <LibC/stdio.h>
#include <LibC/unistd.h>
#include <LibC/process.h>

static void prompt()
{
    if (getuid() == 0)
        printf("# ");
    else
        printf("$ ");
}

static int runcmd(char* cmd)
{
    //printf("command: '%s'\n", cmd);
    char buf[128];
    sprintf(buf, "/bin/%s", cmd);
    int ret = spawn(buf);
    if (ret == -1) {
        printf("spawn failed: %s\n", cmd);
        return 1;
    }
    waitpid(ret);
    return 0;
}

int main(int c, char** v)
{
    char linebuf[128];
    int linedx = 0;
    linebuf[0] = '\0';

    int fd = open("/dev/keyboard");
    if (fd == -1) {
        printf("failed to open /dev/keyboard :(\n");
        return 1;
    }
    prompt();
    for (;;) {
        char keybuf[16];
        ssize_t nread = read(fd, keybuf, sizeof(keybuf));
        if (nread < 0) {
            printf("failed to read :(\n");
            return 2;
        }
        if (nread > 2)
            printf("read %u bytes\n", nread);
        if (nread > (ssize_t)sizeof(keybuf)) {
            printf("read() overran the buffer i gave it!\n");
            return 3;
        }
        for (ssize_t i = 0; i < nread; ++i) {
            putchar(keybuf[i]);
            if (keybuf[i] != '\n') {
                linebuf[linedx++] = keybuf[i];
                linebuf[linedx] = '\0';
            } else {
                runcmd(linebuf);
                linebuf[0] = '\0';
                linedx = 0;
                prompt();
            }
        }
    }
    return 0;
}

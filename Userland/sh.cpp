#include <LibC/stdio.h>
#include <LibC/unistd.h>
#include <LibC/process.h>
#include <LibC/errno.h>
#include <LibC/string.h>
#include <LibC/stdlib.h>

char* g_cwd = nullptr;

static void prompt()
{
    if (getuid() == 0)
        printf("# ");
    else
        printf("$ ");
}

static int sh_pwd(int, const char**)
{
    printf("cwd: %s\n", g_cwd);
}

static int sh_cd(int argc, const char** argv)
{
    if (argc == 1) {
        printf("usage: cd <path>\n");
        return 0;
    }

    char pathbuf[128];
    if (argv[1][1] == '/')
        memcpy(pathbuf, argv[1], strlen(argv[1]));
    else
        sprintf(pathbuf, "%s/%s", g_cwd, argv[1]);
    struct stat st;
    int rc = lstat(pathbuf, &st);
    if (rc < 0) {
        printf("lstat(%s) failed: %s\n", pathbuf, strerror(errno));
        return 1;
    }
    if (!S_ISDIR(st.st_mode)) {
        printf("Not a directory: %s\n", pathbuf);
        return 1;
    }
    rc = chdir(pathbuf);
    if (rc < 0) {
        printf("chdir(%s) failed: %s\n", pathbuf, strerror(errno));
        return 1;
    }
    memcpy(g_cwd, pathbuf, strlen(pathbuf));
    return 0;
}

static bool handle_builtin(int argc, const char** argv, int& retval)
{
    if (argc == 0)
        return false;
    if (!strcmp(argv[0], "cd")) {
        retval = sh_cd(argc, argv);
        return true;
    }
    if (!strcmp(argv[0], "pwd")) {
        retval = sh_pwd(argc, argv);
        return true;
    }
    return false;
}

static int runcmd(char* cmd)
{
    if (cmd[0] == 0)
        return 0;
    char buf[128];
    memcpy(buf, cmd, 128);

    const char* argv[32];
    size_t argi = 1;
    argv[0] = &buf[0];
    size_t buflen = strlen(buf);
    for (size_t i = 0; i < buflen; ++i) {
        if (buf[i] == ' ') {
            buf[i] = '\0';
            argv[argi++] = &buf[i + 1];
        }
    }
    argv[argi + 1] = nullptr;

    int retval = 0;
    if (handle_builtin(argi, argv, retval)) {
        return 0;
    }

    const char* search_path = "/bin";

    char pathbuf[128];
    sprintf(pathbuf, "%s/%s", search_path, argv[0]);
    int ret = spawn(pathbuf, argv);
    if (ret == -1) {
        printf("spawn failed: %s (%s)\n", cmd, strerror(errno));
        return 1;
    }
    // FIXME: waitpid should give us the spawned process's exit status
    waitpid(ret);
    return retval;
}

int main(int, char**)
{
    char linebuf[128];
    int linedx = 0;
    linebuf[0] = '\0';

    int fd = open("/dev/keyboard");
    if (fd == -1) {
        printf("failed to open /dev/keyboard :(\n");
        return 1;
    }
    g_cwd = (char*)malloc(1024);
    g_cwd[0] = '/';
    g_cwd[1] = '\0';
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

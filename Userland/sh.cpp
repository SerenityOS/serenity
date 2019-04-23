#include <errno.h>
#include <pwd.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#include <termios.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <sys/utsname.h>
#include <AK/FileSystemPath.h>

struct GlobalState {
    String cwd;
    String username;
    char ttyname[32];
    char hostname[32];
    pid_t sid;
    uid_t uid;
    struct termios termios;
    bool was_interrupted { false };
};
static GlobalState* g;

static void prompt()
{
    if (g->uid == 0)
        printf("# ");
    else {
        printf("\033]0;%s@%s:%s\007", g->username.characters(), g->hostname, g->cwd.characters());
        printf("\033[31;1m%s\033[0m@\033[37;1m%s\033[0m:\033[32;1m%s\033[0m$> ", g->username.characters(), g->hostname, g->cwd.characters());
    }
    fflush(stdout);
}

static int sh_pwd(int, char**)
{
    printf("%s\n", g->cwd.characters());
    return 0;
}

static volatile bool g_got_signal = false;

void did_receive_signal(int signum)
{
    printf("\nMy word, I've received a signal with number %d\n", signum);
    g_got_signal = true;
}

void handle_sigint(int)
{
    g->was_interrupted = true;
}

static int sh_exit(int, char**)
{
    printf("Good-bye!\n");
    exit(0);
    return 0;
}

static int sh_cd(int argc, char** argv)
{
    if (argc == 1) {
        printf("usage: cd <path>\n");
        return 0;
    }

    char pathbuf[128];
    if (argv[1][0] == '/')
        memcpy(pathbuf, argv[1], strlen(argv[1]) + 1);
    else
        sprintf(pathbuf, "%s/%s", g->cwd.characters(), argv[1]);

    FileSystemPath canonical_path(pathbuf);
    if (!canonical_path.is_valid()) {
        printf("FileSystemPath failed to canonicalize '%s'\n", pathbuf);
        return 1;
    }
    const char* path = canonical_path.string().characters();

    struct stat st;
    int rc = stat(path, &st);
    if (rc < 0) {
        printf("lstat(%s) failed: %s\n", path, strerror(errno));
        return 1;
    }
    if (!S_ISDIR(st.st_mode)) {
        printf("Not a directory: %s\n", path);
        return 1;
    }
    rc = chdir(path);
    if (rc < 0) {
        printf("chdir(%s) failed: %s\n", path, strerror(errno));
        return 1;
    }
    g->cwd = canonical_path.string();
    return 0;
}

static bool handle_builtin(int argc, char** argv, int& retval)
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
    if (!strcmp(argv[0], "exit")) {
        retval = sh_exit(argc, argv);
        return true;
    }
    return false;
}

static int try_exec(const char* path, char** argv)
{
    int ret = execve(path, argv, environ);
    assert(ret < 0);

    {
    const char* search_path = "/bin";
    char pathbuf[128];
    sprintf(pathbuf, "%s/%s", search_path, argv[0]);
    ret = execve(pathbuf, argv, environ);
    assert(ret < 0);
    }

    {
    const char* search_path = "/usr/bin";
    char pathbuf[128];
    sprintf(pathbuf, "%s/%s", search_path, argv[0]);
    ret = execve(pathbuf, argv, environ);
    }
    if (ret == -1)
        return -1;
    return ret;
}

static int runcmd(char* cmd)
{
    if (cmd[0] == 0)
        return 0;
    char buf[128];
    memcpy(buf, cmd, 128);

    char* argv[32];
    size_t argc = 1;
    argv[0] = &buf[0];
    size_t buflen = strlen(buf);
    for (size_t i = 0; i < buflen; ++i) {
        if (buf[i] == ' ') {
            buf[i] = '\0';
            argv[argc++] = &buf[i + 1];
        }
    }
    argv[argc] = nullptr;

    int retval = 0;
    if (handle_builtin(argc, argv, retval)) {
        return 0;
    }

    struct termios trm;
    tcgetattr(0, &trm);

    pid_t child = fork();
    if (!child) {
        setpgid(0, 0);
        tcsetpgrp(0, getpid());
        int ret = try_exec(argv[0], argv);
        if (ret < 0) {
            printf("exec failed: %s (%s)\n", cmd, strerror(errno));
            exit(1);
        }
        // We should never get here!
        ASSERT_NOT_REACHED();
    }

    int wstatus = 0;
    int rc;
    do {
        rc = waitpid(child, &wstatus, 0);
        if (rc < 0 && errno != EINTR) {
            perror("waitpid");
            break;
        }
    } while(errno == EINTR);

    // FIXME: Should I really have to tcsetpgrp() after my child has exited?
    //        Is the terminal controlling pgrp really still the PGID of the dead process?
    tcsetpgrp(0, getpid());

    tcsetattr(0, TCSANOW, &trm);

    if (WIFEXITED(wstatus)) {
        if (WEXITSTATUS(wstatus) != 0)
            printf("Exited with status %d\n", WEXITSTATUS(wstatus));
    } else {
        if (WIFSIGNALED(wstatus)) {
            switch (WTERMSIG(wstatus)) {
            case SIGINT:
                printf("Interrupted\n");
                break;
            default:
                printf("Terminated by signal %d\n", WTERMSIG(wstatus));
                break;
            }
        } else {
            printf("Exited abnormally\n");
        }
    }
    return retval;
}

int main(int argc, char** argv)
{
    g = new GlobalState;
    g->uid = getuid();
    g->sid = setsid();
    tcsetpgrp(0, getpgrp());
    tcgetattr(0, &g->termios);

    {
        struct sigaction sa;
        sa.sa_handler = handle_sigint;
        sa.sa_flags = 0;
        sa.sa_mask = 0;
        int rc = sigaction(SIGINT, &sa, nullptr);
        assert(rc == 0);
    }

    int rc = gethostname(g->hostname, sizeof(g->hostname));
    if (rc < 0)
        perror("gethostname");
    rc = ttyname_r(0, g->ttyname, sizeof(g->ttyname));
    if (rc < 0)
        perror("ttyname_r");

    {
        auto* pw = getpwuid(getuid());
        if (pw)
            g->username = pw->pw_name;
        endpwent();
    }

    if (argc > 1 && !strcmp(argv[1], "-c")) {
        fprintf(stderr, "FIXME: Implement /bin/sh -c\n");
        return 1;
    }

    char linebuf[128];
    int linedx = 0;
    linebuf[0] = '\0';

    {
        char cwdbuf[1024];
        getcwd(cwdbuf, sizeof(cwdbuf));
        g->cwd = cwdbuf;
    }
    prompt();
    for (;;) {
        char keybuf[16];
        ssize_t nread = read(0, keybuf, sizeof(keybuf));
        if (nread == 0)
            return 0;
        if (nread < 0) {
            if (errno == EINTR) {
                if (g->was_interrupted) {
                    if (linedx != 0)
                        printf("^C");
                }
                g->was_interrupted = false;
                linebuf[0] = '\0';
                linedx = 0;
                putchar('\n');
                prompt();
                continue;
            } else {
                perror("read failed");
                return 2;
            }
        }
        for (ssize_t i = 0; i < nread; ++i) {
            char ch = keybuf[i];
            if (ch == 0)
                continue;
            if (ch == 8 || ch == g->termios.c_cc[VERASE]) {
                if (linedx == 0)
                    continue;
                linebuf[--linedx] = '\0';
                putchar(8);
                fflush(stdout);
                continue;
            }
            if (ch == g->termios.c_cc[VKILL]) {
                if (linedx == 0)
                    continue;
                for (; linedx; --linedx)
                    putchar(0x8);
                linebuf[0] = '\0';
                fflush(stdout);
                continue;
            }
            putchar(ch);
            fflush(stdout);
            if (ch != '\n') {
                linebuf[linedx++] = ch;
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

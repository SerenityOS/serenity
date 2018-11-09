#include <stdio.h>
#include <unistd.h>
#include <process.h>
#include <errno.h>
#include <string.h>
#include <stdlib.h>
#include <utsname.h>
#include <pwd.h>
#include <sys/mman.h>
#include <signal.h>
#include <AK/FileSystemPath.h>

struct GlobalState {
    String cwd;
    String username;
    const char* ttyname_short { nullptr };
    char ttyname[32];
    char hostname[32];
    pid_t sid;
};
static GlobalState* g;

static void prompt()
{
    if (getuid() == 0)
        printf("# ");
    else
        printf("\033[31;1m%s\033[0m@\033[37;1m%s\033[0m:\033[32;1m%s\033[0m$> ", g->username.characters(), g->hostname, g->cwd.characters());
    fflush(stdout);
}

static int sh_pwd(int, const char**)
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
    printf("Interrupt received by sh\n");
}

static int sh_busy(int, const char**)
{
    struct sigaction sa;
    sa.sa_handler = did_receive_signal;
    sa.sa_flags = 0;
    sa.sa_mask = 0;
    sa.sa_restorer = nullptr;
    int rc = sigaction(SIGUSR1, &sa, nullptr);
    assert(rc == 0);
    printf("listening for signal SIGUSR1 while looping in userspace...\n");
    for (;;) {
        for (volatile int i = 0; i < 100000; ++i)
            ;
        if (g_got_signal)
            break;
    }
    g_got_signal = false;
    return 0;
}

static int sh_fork(int, const char**)
{
    pid_t pid = fork();
    printf("getpid()=%d, fork()=%d\n", getpid(), pid);
    return 0;
}

static int sh_fe(int, const char**)
{
    pid_t pid = fork();
    if (!pid) {
        int rc = execve("/bin/ps", nullptr, nullptr);
        if (rc < 0) {
            perror("execve");
            exit(1);
        }
    }
    return 0;
}

static int sh_fef(int, const char**)
{
    pid_t pid = fork();
    if (!pid) {
        int rc = execve("/bin/psx", nullptr, nullptr);
        if (rc < 0) {
            perror("execve");
            exit(1);
        }
    }
    return 0;
}

static int sh_wt(int, const char**)
{
    const char* rodata_ptr = "foo";
    printf("Writing to rodata=%p...\n", rodata_ptr);
    *const_cast<char*>(rodata_ptr) = 0;

    char* text_ptr = (char*)sh_fef;
    printf("Writing to text=%p...\n", text_ptr);
    *text_ptr = 0;
    return 0;
}

static int sh_mf(int, const char**)
{
    int rc;
    int fd = open("/Banner.txt", O_RDONLY);
    if (fd < 0) {
        perror("open(/Banner.txt)");
        return 1;
    }
    printf("opened /Banner.txt, calling mmap...\n");
    byte* data = (byte*)mmap(nullptr, getpagesize(), PROT_READ, MAP_PRIVATE, fd, 0);
    if (data == MAP_FAILED) {
        perror("mmap()");
        goto close_it;
    }
    printf("mapped file @ %p\n", data);
    printf("contents: %b %b %b %b\n", data[0], data[1], data[2], data[3]);

    rc = munmap(data, getpagesize());
    printf("munmap() returned %d\n", rc);

close_it:
    rc = close(fd);
    printf("close() returned %d\n", rc);
    return 0;
}

static int sh_mp(int, const char**)
{
    int fd = open("/kernel.map", O_RDONLY);
    if (fd < 0) {
        perror("open(/kernel.map)");
        return 1;
    }
    printf("opened /kernel.map, calling mmap...\n");
    byte* data = (byte*)mmap(nullptr, getpagesize() * 10, PROT_READ, MAP_PRIVATE, fd, 0);
    if (data == MAP_FAILED) {
        perror("mmap()");
        return 1;
    }
    printf("mapped file @ %p\n", data);
    printf("contents: %c%c%c%c%c%c%c...\n", data[0], data[1], data[2], data[3], data[4], data[5], data[6], data[7]);

    printf("leaving it open :)\n");
    return 0;
}

static int sh_exit(int, const char**)
{
    printf("Good-bye!\n");
    exit(0);
    return 0;
}

static int sh_cd(int argc, const char** argv)
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

    FileSystemPath canonicalPath(pathbuf);
    if (!canonicalPath.isValid()) {
        printf("FileSystemPath failed to canonicalize '%s'\n", pathbuf);
        return 1;
    }
    const char* path = canonicalPath.string().characters();

    struct stat st;
    int rc = lstat(path, &st);
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
    g->cwd = canonicalPath.string();
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
    if (!strcmp(argv[0], "exit")) {
        retval = sh_exit(argc, argv);
        return true;
    }
    if (!strcmp(argv[0], "fe")) {
        retval = sh_fe(argc, argv);
        return true;
    }
    if (!strcmp(argv[0], "fef")) {
        retval = sh_fef(argc, argv);
        return true;
    }
    if (!strcmp(argv[0], "busy")) {
        retval = sh_busy(argc, argv);
        return true;
    }
    if (!strcmp(argv[0], "wt")) {
        retval = sh_wt(argc, argv);
        return true;
    }
    if (!strcmp(argv[0], "mf")) {
        retval = sh_mf(argc, argv);
        return true;
    }
    if (!strcmp(argv[0], "mp")) {
        retval = sh_mp(argc, argv);
        return true;
    }
    if (!strcmp(argv[0], "fork")) {
        retval = sh_fork(argc, argv);
        return true;
    }
    return false;
}

static int try_exec(const char* path, const char** argv)
{
    int ret = execve(path, argv, nullptr);
    assert(ret < 0);

    const char* search_path = "/bin";
    char pathbuf[128];
    sprintf(pathbuf, "%s/%s", search_path, argv[0]);
    ret = execve(pathbuf, argv, nullptr);
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

    const char* argv[32];
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
        assert(false);
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

    if (WIFEXITED(wstatus)) {
        //printf("Exited normally with status %d\n", WEXITSTATUS(wstatus));
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

static void greeting()
{
    utsname uts;
    int rc = uname(&uts);
    if (rc < 0) {
        perror("uname");
        return;
    }
    printf("\n%s/%s on %s\n\n", uts.sysname, uts.machine, g->ttyname_short);
}

int main(int, char**)
{
    g = new GlobalState;
    g->sid = setsid();
    tcsetpgrp(0, getpgrp());

    {
        struct sigaction sa;
        sa.sa_handler = handle_sigint;
        sa.sa_flags = 0;
        sa.sa_mask = 0;
        sa.sa_restorer = nullptr;
        int rc = sigaction(SIGINT, &sa, nullptr);
        assert(rc == 0);
    }

    int rc = gethostname(g->hostname, sizeof(g->hostname));
    if (rc < 0)
        perror("gethostname");
    rc = ttyname_r(0, g->ttyname, sizeof(g->ttyname));
    if (rc < 0)
        perror("ttyname_r");
    else
        g->ttyname_short = strrchr(g->ttyname, '/') + 1;
    {
        auto* pw = getpwuid(getuid());
        if (pw)
            g->username = pw->pw_name;
        endpwent();
    }

    greeting();

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
        if (nread < 0) {
            if (errno == EINTR) {
                // Ignore. :^)
            } else {
                perror("read failed");
                return 2;
            }
        }
        for (ssize_t i = 0; i < nread; ++i) {
            putchar(keybuf[i]);
            fflush(stdout);
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

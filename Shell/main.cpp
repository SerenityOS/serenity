#include <errno.h>
#include <pwd.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#include <termios.h>
#include <ctype.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <sys/utsname.h>
#include <AK/FileSystemPath.h>
#include <LibCore/CElapsedTimer.h>
#include "Parser.h"

//#define SH_DEBUG

struct GlobalState {
    String cwd;
    String username;
    String home;
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

static int sh_export(int argc, char** argv)
{
    if (argc == 1) {
        for (int i = 0; environ[i]; ++i)
            puts(environ[i]);
        return 0;
    }
    auto parts = String(argv[1]).split('=');
    if (parts.size() != 2) {
        fprintf(stderr, "usage: export variable=value\n");
        return 1;
    }
    putenv(const_cast<char*>(String::format("%s=%s", parts[0].characters(), parts[1].characters()).characters()));
    return 0;
}

static int sh_cd(int argc, char** argv)
{
    char pathbuf[PATH_MAX];

    if (argc == 1) {
        strcpy(pathbuf, g->home.characters());
    } else {
        if (argv[1][0] == '/')
            memcpy(pathbuf, argv[1], strlen(argv[1]) + 1);
        else
            sprintf(pathbuf, "%s/%s", g->cwd.characters(), argv[1]);
    }

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
    if (!strcmp(argv[0], "export")) {
        retval = sh_export(argc, argv);
        return true;
    }
    return false;
}

class FileDescriptorCollector {
public:
    FileDescriptorCollector() { }
    ~FileDescriptorCollector() { collect(); }

    void collect()
    {
        for (auto fd : m_fds)
            close(fd);
        m_fds.clear();
    }
    void add(int fd) { m_fds.append(fd); }

private:
    Vector<int, 32> m_fds;
};

struct CommandTimer {
    CommandTimer()
    {
        timer.start();
    }
    ~CommandTimer()
    {
        dbgprintf("sh: command finished in %d ms\n", timer.elapsed());
    }

    CElapsedTimer timer;
};

static int run_command(const String& cmd)
{
    if (cmd.is_empty())
        return 0;

    auto subcommands = Parser(cmd).parse();

#ifdef SH_DEBUG
    for (int i = 0; i < subcommands.size(); ++i) {
        for (int j = 0; j < i; ++j)
            printf("    ");
        for (auto& arg : subcommands[i].args) {
            printf("<%s> ", arg.characters());
        }
        printf("\n");
        for (auto& redirecton : subcommands[i].redirections) {
            for (int j = 0; j < i; ++j)
                printf("    ");
            printf("  ");
            switch (redirecton.type) {
            case Redirection::Pipe:
                printf("Pipe\n");
                break;
            case Redirection::FileRead:
                printf("fd:%d = FileRead: %s\n", redirecton.fd, redirecton.path.characters());
                break;
            case Redirection::FileWrite:
                printf("fd:%d = FileWrite: %s\n", redirecton.fd, redirecton.path.characters());
                break;
            default:
                break;
            }
        }
    }
#endif

    if (subcommands.is_empty())
        return 0;

    FileDescriptorCollector fds;

    for (int i = 0; i < subcommands.size(); ++i) {
        auto& subcommand = subcommands[i];
        for (auto& redirection : subcommand.redirections) {
            if (redirection.type == Redirection::Pipe) {
                int pipefd[2];
                int rc = pipe(pipefd);
                if (rc < 0) {
                    perror("pipe");
                    return 1;
                }
                subcommand.redirections.append({ Redirection::Rewire, STDOUT_FILENO, pipefd[1] });
                auto& next_command = subcommands[i + 1];
                next_command.redirections.append({ Redirection::Rewire, STDIN_FILENO, pipefd[0] });
                fds.add(pipefd[0]);
                fds.add(pipefd[1]);
            }
            if (redirection.type == Redirection::FileWrite) {
                int fd = open(redirection.path.characters(), O_WRONLY | O_CREAT, 0666);
                if (fd < 0) {
                    perror("open");
                    return 1;
                }
                subcommand.redirections.append({ Redirection::Rewire, redirection.fd, fd });
                fds.add(fd);
            }
            if (redirection.type == Redirection::FileRead) {
                int fd = open(redirection.path.characters(), O_RDONLY);
                if (fd < 0) {
                    perror("open");
                    return 1;
                }
                subcommand.redirections.append({ Redirection::Rewire, redirection.fd, fd });
                fds.add(fd);
            }
        }
    }

    struct termios trm;
    tcgetattr(0, &trm);

    Vector<pid_t> children;

    CommandTimer timer;

    for (int i = 0; i < subcommands.size(); ++i) {
        auto& subcommand = subcommands[i];
        Vector<const char*> argv;
        for (auto& arg : subcommand.args)
            argv.append(arg.characters());
        argv.append(nullptr);

        int retval = 0;
        if (handle_builtin(argv.size() - 1, const_cast<char**>(argv.data()), retval))
            return retval;

        pid_t child = fork();
        if (!child) {
            setpgid(0, 0);
            tcsetpgrp(0, getpid());
            for (auto& redirection : subcommand.redirections) {
                if (redirection.type == Redirection::Rewire) {
#ifdef SH_DEBUG
                    dbgprintf("in %s<%d>, dup2(%d, %d)\n", argv[0], getpid(), redirection.rewire_fd, redirection.fd);
#endif
                    int rc = dup2(redirection.rewire_fd, redirection.fd);
                    if (rc < 0) {
                        perror("dup2");
                        return 1;
                    }
                }
            }

            fds.collect();

            int rc = execvp(argv[0], const_cast<char* const*>(argv.data()));
            if (rc < 0) {
                perror("execvp");
                exit(1);
            }
            ASSERT_NOT_REACHED();
        }
        children.append(child);
    }

#ifdef SH_DEBUG
    dbgprintf("Closing fds in shell process:\n");
#endif
    fds.collect();

#ifdef SH_DEBUG
    dbgprintf("Now we gotta wait on children:\n");
    for (auto& child : children)
        dbgprintf("  %d\n", child);
#endif


    int wstatus = 0;
    int rc;

    for (auto& child : children) {
        do {
            rc = waitpid(child, &wstatus, 0);
            if (rc < 0 && errno != EINTR) {
                perror("waitpid");
                break;
            }
        } while(errno == EINTR);
    }

    // FIXME: Should I really have to tcsetpgrp() after my child has exited?
    //        Is the terminal controlling pgrp really still the PGID of the dead process?
    tcsetpgrp(0, getpid());

    tcsetattr(0, TCSANOW, &trm);

    if (WIFEXITED(wstatus)) {
        if (WEXITSTATUS(wstatus) != 0)
            printf("Exited with status %d\n", WEXITSTATUS(wstatus));
        return WEXITSTATUS(wstatus);
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
            return 1;
        }
    }
    return 0;
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
        if (pw) {
            g->username = pw->pw_name;
            g->home = pw->pw_dir;
            putenv(const_cast<char*>(String::format("HOME=%s", pw->pw_dir).characters()));
        }
        endpwent();
    }

    if (argc > 1 && !strcmp(argv[1], "-c")) {
        fprintf(stderr, "FIXME: Implement /bin/sh -c\n");
        return 1;
    }

    Vector<char, 256> line_buffer;

    {
        auto* cwd = getcwd(nullptr, 0);
        g->cwd = cwd;
        free(cwd);
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
                    if (!line_buffer.is_empty())
                        printf("^C");
                }
                g->was_interrupted = false;
                line_buffer.clear();
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
                if (line_buffer.is_empty())
                    continue;
                line_buffer.take_last();
                putchar(8);
                fflush(stdout);
                continue;
            }
            if (ch == g->termios.c_cc[VWERASE]) {
                bool has_seen_nonspace = false;
                while (!line_buffer.is_empty()) {
                    if (isspace(line_buffer.last())) {
                        if (has_seen_nonspace)
                            break;
                    } else {
                        has_seen_nonspace = true;
                    }
                    putchar(0x8);
                    line_buffer.take_last();
                }
                fflush(stdout);
                continue;
            }
            if (ch == g->termios.c_cc[VKILL]) {
                if (line_buffer.is_empty())
                    continue;
                for (int i = 0; i < line_buffer.size(); ++i)
                    putchar(0x8);
                line_buffer.clear();
                fflush(stdout);
                continue;
            }
            putchar(ch);
            fflush(stdout);
            if (ch != '\n') {
                line_buffer.append(ch);
            } else {
                run_command(String::copy(line_buffer));
                line_buffer.clear();
                prompt();
            }
        }
    }
    return 0;
}

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
#include <LibCore/CElapsedTimer.h>
#include "GlobalState.h"
#include "Parser.h"
#include "LineEditor.h"

//#define SH_DEBUG

GlobalState g;
static LineEditor editor;

static void prompt()
{
    if (g.uid == 0)
        printf("# ");
    else {
        printf("\033]0;%s@%s:%s\007", g.username.characters(), g.hostname, g.cwd.characters());
        printf("\033[31;1m%s\033[0m@\033[37;1m%s\033[0m:\033[32;1m%s\033[0m$> ", g.username.characters(), g.hostname, g.cwd.characters());
    }
    fflush(stdout);
}

static int sh_pwd(int, char**)
{
    printf("%s\n", g.cwd.characters());
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
    g.was_interrupted = true;
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

    // FIXME: Yes, this leaks.
    // Maybe LibCore should grow a CEnvironment which is secretly a map to char*,
    // so it can keep track of the environment pointers as needed?
    const auto& s = String::format("%s=%s", parts[0].characters(), parts[1].characters());
    char *ev = strndup(s.characters(), s.length());
    putenv(ev);
    return 0;
}

static int sh_unset(int argc, char** argv)
{
    if (argc != 2) {
        fprintf(stderr, "usage: unset variable\n");
        return 1;
    }

    unsetenv(argv[1]);
    return 0;
}

static int sh_cd(int argc, char** argv)
{
    char pathbuf[PATH_MAX];

    if (argc == 1) {
        strcpy(pathbuf, g.home.characters());
    } else {
        if (argv[1][0] == '/')
            memcpy(pathbuf, argv[1], strlen(argv[1]) + 1);
        else
            sprintf(pathbuf, "%s/%s", g.cwd.characters(), argv[1]);
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
    g.cwd = canonical_path.string();
    return 0;
}

static int sh_history(int, char**)
{
    for (int i = 0; i < editor.history().size(); ++i) {
        printf("%6d  %s\n", i, editor.history()[i].characters());
    }
    return 0;
}

static int sh_umask(int argc, char** argv)
{
    if (argc == 1) {
        mode_t old_mask = umask(0);
        printf("%#o\n", old_mask);
        umask(old_mask);
        return 0;
    }
    if (argc == 2) {
        unsigned mask;
        int matches = sscanf(argv[1], "%o", &mask);
        if (matches == 1) {
            umask(mask);
            return 0;
        }
    }
    printf("usage: umask <octal-mask>\n");
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
    if (!strcmp(argv[0], "unset")) {
        retval = sh_unset(argc, argv);
        return true;
    }
    if (!strcmp(argv[0], "history")) {
        retval = sh_history(argc, argv);
        return true;
    }
    if (!strcmp(argv[0], "umask")) {
        retval = sh_umask(argc, argv);
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
            dbgprintf("    ");
        for (auto& arg : subcommands[i].args) {
            dbgprintf("<%s> ", arg.characters());
        }
        dbgprintf("\n");
        for (auto& redirecton : subcommands[i].redirections) {
            for (int j = 0; j < i; ++j)
                dbgprintf("    ");
            dbgprintf("  ");
            switch (redirecton.type) {
            case Redirection::Pipe:
                dbgprintf("Pipe\n");
                break;
            case Redirection::FileRead:
                dbgprintf("fd:%d = FileRead: %s\n", redirecton.fd, redirecton.path.characters());
                break;
            case Redirection::FileWrite:
                dbgprintf("fd:%d = FileWrite: %s\n", redirecton.fd, redirecton.path.characters());
                break;
            case Redirection::FileWriteAppend:
                dbgprintf("fd:%d = FileWriteAppend: %s\n", redirecton.fd, redirecton.path.characters());
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
            switch (redirection.type) {
                case Redirection::Pipe: {
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
                    break;
                }
                case Redirection::FileWriteAppend: {
                    int fd = open(redirection.path.characters(), O_WRONLY | O_CREAT | O_APPEND, 0666);
                    if (fd < 0) {
                        perror("open");
                        return 1;
                    }
                    subcommand.redirections.append({ Redirection::Rewire, redirection.fd, fd });
                    fds.add(fd);
                    break;
                }
                case Redirection::FileWrite: {
                    int fd = open(redirection.path.characters(), O_WRONLY | O_CREAT | O_TRUNC, 0666);
                    if (fd < 0) {
                        perror("open");
                        return 1;
                    }
                    subcommand.redirections.append({ Redirection::Rewire, redirection.fd, fd });
                    fds.add(fd);
                    break;
                }
                case Redirection::FileRead: {
                    int fd = open(redirection.path.characters(), O_RDONLY);
                    if (fd < 0) {
                        perror("open");
                        return 1;
                    }
                    subcommand.redirections.append({ Redirection::Rewire, redirection.fd, fd });
                    fds.add(fd);
                    break;
                }
                case Redirection::Rewire:
                    break; // ignore
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
#ifdef SH_DEBUGsh
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
                fprintf(stderr, "execvp(%s): %s\n", argv[0], strerror(errno));
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
            puts(strsignal(WTERMSIG(wstatus)));
        } else {
            printf("Exited abnormally\n");
            return 1;
        }
    }
    return 0;
}

int main(int argc, char** argv)
{
    g.uid = getuid();
    g.sid = setsid();
    tcsetpgrp(0, getpgrp());
    tcgetattr(0, &g.termios);

    {
        struct sigaction sa;
        sa.sa_handler = handle_sigint;
        sa.sa_flags = 0;
        sa.sa_mask = 0;
        int rc = sigaction(SIGINT, &sa, nullptr);
        assert(rc == 0);
    }

    int rc = gethostname(g.hostname, sizeof(g.hostname));
    if (rc < 0)
        perror("gethostname");
    rc = ttyname_r(0, g.ttyname, sizeof(g.ttyname));
    if (rc < 0)
        perror("ttyname_r");

    {
        auto* pw = getpwuid(getuid());
        if (pw) {
            g.username = pw->pw_name;
            g.home = pw->pw_dir;
            const auto& s = String::format("HOME=%s", pw->pw_dir);
            char *ev = strndup(s.characters(), s.length());
            putenv(ev);
        }
        endpwent();
    }

    if (argc > 2 && !strcmp(argv[1], "-c")) {
        dbgprintf("sh -c '%s'\n", argv[2]);
        run_command(argv[2]);
        return 0;
    }

    {
        auto* cwd = getcwd(nullptr, 0);
        g.cwd = cwd;
        free(cwd);
    }

    for (;;) {
        prompt();
        auto line = editor.get_line();
        if (line.is_empty())
            continue;
        run_command(line);
        editor.add_to_history(line);
    }

    return 0;
}

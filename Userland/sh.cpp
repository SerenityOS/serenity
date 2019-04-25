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

struct Redirection {
    enum Type { Pipe, File, Rewire };
    Type type;
    int fd { -1 };
    int rewire_fd { -1 };
};

struct Subcommand {
    Vector<String> args;
    Vector<Redirection> redirections;
};

class Parser {
public:
    explicit Parser(const String& input) : m_input(input) { }

    Vector<Subcommand> parse();

private:
    void commit_token();
    void commit_subcommand();
    void do_pipe();
    void begin_redirect_fd(int fd);

    enum State {
        Free,
        InSingleQuotes,
        InDoubleQuotes,
        InRedirectionPath,
    };
    State m_state { Free };
    String m_input;

    Vector<Subcommand> m_subcommands;
    Vector<String> m_tokens;
    Vector<Redirection> m_redirections;
    Vector<char> m_token;
};

void Parser::commit_token()
{
    if (m_token.is_empty())
        return;
    m_tokens.append(String::copy(m_token));
    m_token.clear_with_capacity();
};

void Parser::commit_subcommand()
{
    if (m_tokens.is_empty())
        return;
    m_subcommands.append({ move(m_tokens), move(m_redirections) });
}

void Parser::do_pipe()
{
    m_redirections.append({ Redirection::Pipe, STDOUT_FILENO });
    commit_subcommand();
}

void Parser::begin_redirect_fd(int fd)
{
    m_redirections.append({ Redirection::File, fd });
}

Vector<Subcommand> Parser::parse()
{
    for (int i = 0; i < m_input.length(); ++i) {
        char ch = m_input.characters()[i];
        switch (m_state) {
        case State::Free:
            if (ch == ' ') {
                commit_token();
                break;
            }
            if (ch == '|') {
                commit_token();
                if (m_tokens.is_empty()) {
                    fprintf(stderr, "Syntax error: Nothing before pipe (|)\n");
                    return { };
                }
                do_pipe();
                break;
            }
            if (ch == '>') {
                begin_redirect_fd(STDOUT_FILENO);
                m_state = State::InRedirectionPath;
                break;
            }
            if (ch == '<') {
                begin_redirect_fd(STDIN_FILENO);
                m_state = State::InRedirectionPath;
                break;
            }
            if (ch == '\'') {
                m_state = State::InSingleQuotes;
                break;
            }
            if (ch == '\"') {
                m_state = State::InDoubleQuotes;
                break;
            }
            m_token.append(ch);
            break;
        case State::InRedirectionPath:
            if (ch == ' ') {
                commit_token();
                break;
            }
            break;
        case State::InSingleQuotes:
            if (ch == '\'') {
                commit_token();
                m_state = State::Free;
                break;
            }
            m_token.append(ch);
            break;
        case State::InDoubleQuotes:
            if (ch == '\"') {
                commit_token();
                m_state = State::Free;
                break;
            }
            m_token.append(ch);
            break;
        };
    }
    commit_token();
    commit_subcommand();

    if (!m_subcommands.is_empty()) {
        for (auto& redirection : m_subcommands.last().redirections) {
            if (redirection.type == Redirection::Pipe) {
                fprintf(stderr, "Syntax error: Nothing after last pipe (|)\n");
                return { };
            }
        }
    }

    return move(m_subcommands);
}

static int runcmd(char* cmd)
{
    if (cmd[0] == 0)
        return 0;
    char buf[128];
    memcpy(buf, cmd, 128);

    auto subcommands = Parser(cmd).parse();

#ifdef PARSE_DEBUG
    for (int i = 0; i < subcommands.size(); ++i) {
        for (int j = 0; j < i; ++j)
            printf("  ");
        for (auto& arg : subcommands[i].args) {
            printf("<%s> ", arg.characters());
        }
        printf("\n");
    }
#endif

    if (subcommands.is_empty())
        return 0;

    Vector<int> fds;

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
                fds.append(pipefd[0]);
                fds.append(pipefd[1]);
            }
        }
    }

    struct termios trm;
    tcgetattr(0, &trm);

    Vector<pid_t> children;

    for (int i = 0; i < subcommands.size(); ++i) {
        auto& subcommand = subcommands[i];
        Vector<const char*> argv;
        for (auto& arg : subcommand.args)
            argv.append(arg.characters());
        argv.append(nullptr);

        int retval = 0;
        if (handle_builtin(argv.size() - 1, (char**)argv.data(), retval))
            return retval;

        pid_t child = fork();
        if (!child) {
            setpgid(0, 0);
            tcsetpgrp(0, getpid());
            for (auto& redirection : subcommand.redirections) {
                if (redirection.type == Redirection::Rewire) {
                    dbgprintf("in %s<%d>, dup2(%d, %d)\n", argv[0], getpid(), redirection.rewire_fd, redirection.fd);
                    int rc = dup2(redirection.rewire_fd, redirection.fd);
                    if (rc < 0) {
                        perror("dup2");
                        return 1;
                    }
                }
            }

            for (auto& fd : fds)
                close(fd);

            int rc = execvp(argv[0], (char* const*)argv.data());
            if (rc < 0) {
                perror("execvp");
                exit(1);
            }
            ASSERT_NOT_REACHED();
        }
        children.append(child);
    }

    dbgprintf("Closing fds in shell process:\n");
    for (auto& fd : fds) {
        dbgprintf("  %d\n", fd);
        close(fd);
    }

    dbgprintf("Now we gotta wait on children:\n");
    for (auto& child : children)
        dbgprintf("  %d\n", child);

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

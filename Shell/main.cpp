#include "GlobalState.h"
#include "LineEditor.h"
#include "Parser.h"
#include <AK/FileSystemPath.h>
#include <AK/StringBuilder.h>
#include <LibCore/CDirIterator.h>
#include <LibCore/CElapsedTimer.h>
#include <LibCore/CFile.h>
#include <errno.h>
#include <fcntl.h>
#include <pwd.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <sys/utsname.h>
#include <sys/wait.h>
#include <termios.h>
#include <unistd.h>

//#define SH_DEBUG

GlobalState g;
static LineEditor editor;

static String prompt()
{
    if (g.uid == 0)
        return "# ";

    StringBuilder builder;
    builder.appendf("\033]0;%s@%s:%s\007", g.username.characters(), g.hostname, g.cwd.characters());
    builder.appendf("\033[31;1m%s\033[0m@\033[37;1m%s\033[0m:\033[32;1m%s\033[0m$> ", g.username.characters(), g.hostname, g.cwd.characters());
    return builder.to_string();
}

static int sh_pwd(int, char**)
{
    printf("%s\n", g.cwd.characters());
    return 0;
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

    return setenv(parts[0].characters(), parts[1].characters(), 1);
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
        printf("stat(%s) failed: %s\n", path, strerror(errno));
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

class FileDescriptionCollector {
public:
    FileDescriptionCollector() {}
    ~FileDescriptionCollector() { collect(); }

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

static bool is_glob(const StringView& s)
{
    for (int i = 0; i < s.length(); i++) {
        char c = s.characters_without_null_termination()[i];
        if (c == '*' || c == '?')
            return true;
    }
    return false;
}

static Vector<StringView> split_path(const StringView &path)
{
    Vector<StringView> parts;

    ssize_t substart = 0;
    for (ssize_t i = 0; i < path.length(); i++) {
        char ch = path.characters_without_null_termination()[i];
        if (ch != '/')
            continue;
        ssize_t sublen = i - substart;
        if (sublen != 0)
            parts.append(path.substring_view(substart, sublen));
        parts.append(path.substring_view(i, 1));
        substart = i + 1;
    }

    ssize_t taillen = path.length() - substart;
    if (taillen != 0)
        parts.append(path.substring_view(substart, taillen));

    return parts;
}

static Vector<String> expand_globs(const StringView& path, const StringView& base)
{
    auto parts = split_path(path);

    StringBuilder builder;
    builder.append(base);
    Vector<String> res;

    for (int i = 0; i < parts.size(); ++i) {
        auto& part = parts[i];
        if (!is_glob(part)) {
            builder.append(part);
            continue;
        }

        // Found a glob.
        String new_base = builder.to_string();
        StringView new_base_v = new_base;
        if (new_base_v.is_empty())
            new_base_v = ".";
        CDirIterator di(new_base_v, CDirIterator::NoFlags);

        if (di.has_error()) {
            return res;
        }

        while (di.has_next()) {
            String name = di.next_path();

            // Dotfiles have to be explicitly requested
            if (name[0] == '.' && part[0] != '.')
                continue;

            // And even if they are, skip . and ..
            if (name == "." || name == "..")
                continue;

            if (name.matches(part, String::CaseSensitivity::CaseSensitive)) {

                StringBuilder nested_base;
                nested_base.append(new_base);
                nested_base.append(name);

                StringView remaining_path = path.substring_view_starting_after_substring(part);
                Vector<String> nested_res = expand_globs(remaining_path, nested_base.to_string());
                for (auto& s : nested_res)
                    res.append(s);
            }
        }
        return res;
    }

    // Found no globs.
    String new_path = builder.to_string();
    if (access(new_path.characters(), F_OK) == 0)
        res.append(new_path);
    return res;
}

static Vector<String> process_arguments(const Vector<String>& args)
{
    Vector<String> argv_string;
    for (auto& arg : args) {
        auto expanded = expand_globs(arg, "");
        if (expanded.is_empty())
            argv_string.append(arg);
        else
            for (auto& path : expand_globs(arg, ""))
                argv_string.append(path);
    }

    return argv_string;
}

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

    FileDescriptionCollector fds;

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
                subcommand.rewirings.append({ STDOUT_FILENO, pipefd[1] });
                auto& next_command = subcommands[i + 1];
                next_command.rewirings.append({ STDIN_FILENO, pipefd[0] });
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
                subcommand.rewirings.append({ redirection.fd, fd });
                fds.add(fd);
                break;
            }
            case Redirection::FileWrite: {
                int fd = open(redirection.path.characters(), O_WRONLY | O_CREAT | O_TRUNC, 0666);
                if (fd < 0) {
                    perror("open");
                    return 1;
                }
                subcommand.rewirings.append({ redirection.fd, fd });
                fds.add(fd);
                break;
            }
            case Redirection::FileRead: {
                int fd = open(redirection.path.characters(), O_RDONLY);
                if (fd < 0) {
                    perror("open");
                    return 1;
                }
                subcommand.rewirings.append({ redirection.fd, fd });
                fds.add(fd);
                break;
            }
            }
        }
    }

    struct termios trm;
    tcgetattr(0, &trm);

    struct SpawnedProcess {
        String name;
        pid_t pid;
    };

    Vector<SpawnedProcess> children;

    CommandTimer timer;

    for (int i = 0; i < subcommands.size(); ++i) {
        auto& subcommand = subcommands[i];
        Vector<String> argv_string = process_arguments(subcommand.args);
        Vector<const char*> argv;
        argv.ensure_capacity(argv_string.size());
        for (const auto& s : argv_string) {
            argv.append(s.characters());
        }
        argv.append(nullptr);

#ifdef SH_DEBUG
        for (auto& arg : argv) {
            dbgprintf("<%s> ", arg);
        }
        dbgprintf("\n");
#endif

        int retval = 0;
        if (handle_builtin(argv.size() - 1, const_cast<char**>(argv.data()), retval))
            return retval;

        pid_t child = fork();
        if (!child) {
            setpgid(0, 0);
            tcsetpgrp(0, getpid());
            for (auto& rewiring : subcommand.rewirings) {
#ifdef SH_DEBUG
                dbgprintf("in %s<%d>, dup2(%d, %d)\n", argv[0], getpid(), redirection.rewire_fd, redirection.fd);
#endif
                int rc = dup2(rewiring.rewire_fd, rewiring.fd);
                if (rc < 0) {
                    perror("dup2");
                    return 1;
                }
            }

            fds.collect();

            int rc = execvp(argv[0], const_cast<char* const*>(argv.data()));
            if (rc < 0) {
                if (errno == ENOENT)
                    fprintf(stderr, "%s: Command not found.\n", argv[0]);
                else
                    fprintf(stderr, "execvp(%s): %s\n", argv[0], strerror(errno));
                exit(1);
            }
            ASSERT_NOT_REACHED();
        }
        children.append({ argv[0], child });
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
    int return_value = 0;

    for (int i = 0; i < children.size(); ++i) {
        auto& child = children[i];
        do {
            int rc = waitpid(child.pid, &wstatus, WEXITED | WSTOPPED);
            if (rc < 0 && errno != EINTR) {
                if (errno != ECHILD)
                    perror("waitpid");
                break;
            }
            if (WIFEXITED(wstatus)) {
                if (WEXITSTATUS(wstatus) != 0)
                    dbg() << "Shell: " << child.name << ":" << child.pid << " exited with status " << WEXITSTATUS(wstatus);
                if (i == 0)
                    return_value = WEXITSTATUS(wstatus);
            } else if (WIFSTOPPED(wstatus)) {
                printf("Shell: %s(%d) stopped.\n", child.name.characters(), child.pid);
            } else {
                if (WIFSIGNALED(wstatus)) {
                    printf("Shell: %s(%d) exited due to signal '%s'\n", child.name.characters(), child.pid, strsignal(WTERMSIG(wstatus)));
                } else {
                    printf("Shell: %s(%d) exited abnormally\n", child.name.characters(), child.pid);
                }
            }
        } while (errno == EINTR);
    }

    // FIXME: Should I really have to tcsetpgrp() after my child has exited?
    //        Is the terminal controlling pgrp really still the PGID of the dead process?
    tcsetpgrp(0, getpid());
    tcsetattr(0, TCSANOW, &trm);
    return return_value;
}

CFile get_history_file()
{
    StringBuilder sb;
    sb.append(g.home);
    sb.append("/.history");
    CFile f(sb.to_string());
    if (!f.open(CIODevice::ReadWrite)) {
        fprintf(stderr, "Error opening file '%s': '%s'\n", f.filename().characters(), f.error_string());
        exit(1);
    }
    return f;
}

void load_history()
{
    CFile history_file = get_history_file();
    while (history_file.can_read_line()) {
        const auto&b = history_file.read_line(1024);
        // skip the newline and terminating bytes
        editor.add_to_history(String(reinterpret_cast<const char*>(b.pointer()), b.size() - 2));
    }
}

void save_history()
{
    CFile history_file = get_history_file();
    for (const auto& line : editor.history()) {
        history_file.write(line);
        history_file.write("\n");
    }
}

int main(int argc, char** argv)
{
    g.uid = getuid();
    g.sid = setsid();
    tcsetpgrp(0, getpgrp());
    tcgetattr(0, &g.termios);

    signal(SIGINT, [](int) {
        g.was_interrupted = true;
    });

    signal(SIGHUP, [](int) {
        save_history();
    });

    signal(SIGWINCH, [](int) {
        g.was_resized = true;
    });

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
            setenv("HOME", pw->pw_dir, 1);
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

    load_history();
    atexit(save_history);

    for (;;) {
        auto line = editor.get_line(prompt());
        if (line.is_empty())
            continue;
        run_command(line);
        editor.add_to_history(line);
    }

    return 0;
}

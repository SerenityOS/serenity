/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/ByteBuffer.h>
#include <AK/String.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibMarkdown/Document.h>
#include <fcntl.h>
#include <spawn.h>
#include <stdio.h>
#include <sys/ioctl.h>
#include <sys/wait.h>
#include <unistd.h>

static pid_t pipe_to_pager(String const& command)
{
    char const* argv[] = { "sh", "-c", command.characters(), nullptr };

    int stdout_pipe[2] = {};
    if (pipe2(stdout_pipe, O_CLOEXEC)) {
        perror("pipe2");
        exit(1);
    }
    posix_spawn_file_actions_t action;
    posix_spawn_file_actions_init(&action);
    posix_spawn_file_actions_adddup2(&action, stdout_pipe[0], STDIN_FILENO);

    pid_t pid;
    if ((errno = posix_spawnp(&pid, argv[0], &action, nullptr, const_cast<char**>(argv), environ))) {
        perror("posix_spawn");
        exit(1);
    }
    posix_spawn_file_actions_destroy(&action);

    dup2(stdout_pipe[1], STDOUT_FILENO);
    close(stdout_pipe[1]);
    close(stdout_pipe[0]);
    return pid;
}

int main(int argc, char* argv[])
{
    int view_width = 0;
    if (isatty(STDOUT_FILENO)) {
        struct winsize ws;
        if (ioctl(STDOUT_FILENO, TIOCGWINSZ, &ws) == 0)
            view_width = ws.ws_col;
    }

    if (view_width == 0)
        view_width = 80;

    if (pledge("stdio rpath exec proc", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/usr/share/man", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/bin", "x") < 0) {
        perror("unveil");
        return 1;
    }

    unveil(nullptr, nullptr);

    const char* section = nullptr;
    const char* name = nullptr;
    const char* pager = nullptr;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Read manual pages. Try 'man man' to get started.");
    args_parser.add_positional_argument(section, "Section of the man page", "section", Core::ArgsParser::Required::No);
    args_parser.add_positional_argument(name, "Name of the man page", "name");
    args_parser.add_option(pager, "Pager to pipe the man page to", "pager", 'P', "pager");

    args_parser.parse(argc, argv);

    auto make_path = [name](const char* section) {
        return String::formatted("/usr/share/man/man{}/{}.md", section, name);
    };
    if (!section) {
        const char* sections[] = {
            "1",
            "2",
            "3",
            "4",
            "5",
            "6",
            "7",
            "8"
        };
        for (auto s : sections) {
            String path = make_path(s);
            if (access(path.characters(), R_OK) == 0) {
                section = s;
                break;
            }
        }
        if (!section) {
            warnln("No man page for {}", name);
            exit(1);
        }
    }

    auto file = Core::File::construct();
    file->set_filename(make_path(section));

    String pager_command;
    if (pager)
        pager_command = pager;
    else
        pager_command = String::formatted("less -P 'Manual Page {}({}) line %l?e (END):.'", StringView(name).replace("'", "'\\''"), StringView(section).replace("'", "'\\''"));
    pid_t pager_pid = pipe_to_pager(pager_command);

    if (!file->open(Core::OpenMode::ReadOnly)) {
        perror("Failed to open man page file");
        exit(1);
    }

    if (pledge("stdio proc", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    dbgln("Loading man page from {}", file->filename());
    auto buffer = file->read_all();
    auto source = String::copy(buffer);

    outln("{}({})\t\tSerenityOS manual", name, section);

    auto document = Markdown::Document::parse(source);
    VERIFY(document);

    String rendered = document->render_for_terminal(view_width);
    out("{}", rendered);

    // FIXME: Remove this wait, it shouldn't be necessary but Shell does not
    //        resume properly without it. This wait also breaks <C-z> backgrounding
    fclose(stdout);
    int wstatus;
    waitpid(pager_pid, &wstatus, 0);
}

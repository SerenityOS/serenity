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
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <LibMarkdown/Document.h>
#include <fcntl.h>
#include <spawn.h>
#include <stdio.h>
#include <sys/ioctl.h>
#include <sys/wait.h>
#include <unistd.h>

static ErrorOr<pid_t> pipe_to_pager(String const& command)
{
    char const* argv[] = { "sh", "-c", command.characters(), nullptr };

    auto stdout_pipe = TRY(Core::System::pipe2(O_CLOEXEC));

    posix_spawn_file_actions_t action;
    posix_spawn_file_actions_init(&action);
    posix_spawn_file_actions_adddup2(&action, stdout_pipe[0], STDIN_FILENO);

    pid_t pid;
    if ((errno = posix_spawnp(&pid, argv[0], &action, nullptr, const_cast<char**>(argv), environ))) {
        perror("posix_spawn");
        exit(1);
    }
    posix_spawn_file_actions_destroy(&action);

    TRY(Core::System::dup2(stdout_pipe[1], STDOUT_FILENO));
    TRY(Core::System::close(stdout_pipe[1]));
    TRY(Core::System::close(stdout_pipe[0]));
    return pid;
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    int view_width = 0;
    if (isatty(STDOUT_FILENO)) {
        struct winsize ws;
        if (ioctl(STDOUT_FILENO, TIOCGWINSZ, &ws) == 0)
            view_width = ws.ws_col;
    }

    if (view_width == 0)
        view_width = 80;

    TRY(Core::System::pledge("stdio rpath exec proc"));
    TRY(Core::System::unveil("/usr/share/man", "r"));
    TRY(Core::System::unveil("/bin", "x"));
    TRY(Core::System::unveil(nullptr, nullptr));

    const char* section = nullptr;
    const char* name = nullptr;
    const char* pager = nullptr;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Read manual pages. Try 'man man' to get started.");
    args_parser.add_positional_argument(section, "Section of the man page", "section", Core::ArgsParser::Required::No);
    args_parser.add_positional_argument(name, "Name of the man page", "name");
    args_parser.add_option(pager, "Pager to pipe the man page to", "pager", 'P', "pager");
    args_parser.parse(arguments);

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

    auto filename = make_path(section);

    String pager_command;
    if (pager)
        pager_command = pager;
    else
        pager_command = String::formatted("less -P 'Manual Page {}({}) line %l?e (END):.'", StringView(name).replace("'", "'\\''"), StringView(section).replace("'", "'\\''"));
    pid_t pager_pid = TRY(pipe_to_pager(pager_command));

    auto file = TRY(Core::File::open(filename, Core::OpenMode::ReadOnly));

    TRY(Core::System::pledge("stdio proc"));

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
    return 0;
}

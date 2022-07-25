/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/ByteBuffer.h>
#include <AK/String.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/Stream.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <LibManual/Node.h>
#include <LibManual/PageNode.h>
#include <LibManual/SectionNode.h>
#include <LibMarkdown/Document.h>
#include <spawn.h>
#include <sys/ioctl.h>
#include <sys/wait.h>
#include <unistd.h>

static ErrorOr<pid_t> pipe_to_pager(String const& command)
{
    dbgln(command);
    char const* argv[] = { "sh", "-c", command.characters(), nullptr };

    auto stdout_pipe = TRY(Core::System::pipe2(O_CLOEXEC));

    posix_spawn_file_actions_t action;
    posix_spawn_file_actions_init(&action);
    posix_spawn_file_actions_adddup2(&action, stdout_pipe[0], STDIN_FILENO);

    pid_t pid;
    if ((errno = posix_spawnp(&pid, argv[0], &action, nullptr, const_cast<char**>(argv), environ)) != 0)
        return Error::from_errno(errno);
    posix_spawn_file_actions_destroy(&action);

    TRY(Core::System::dup2(stdout_pipe[1], STDOUT_FILENO));
    TRY(Core::System::close(stdout_pipe[1]));
    TRY(Core::System::close(stdout_pipe[0]));
    return pid;
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    int view_width = 0;
    if (isatty(STDOUT_FILENO) != 0) {
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

    String section_argument;
    String name_argument;
    String pager;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Read manual pages. Try 'man man' to get started.");
    args_parser.add_positional_argument(section_argument, "Section of the man page", "section");
    args_parser.add_positional_argument(name_argument, "Name of the man page", "name", Core::ArgsParser::Required::No);
    args_parser.add_option(pager, "Pager to pipe the man page to", "pager", 'P', "pager");
    args_parser.parse(arguments);
    Vector<StringView, 2> query_parameters;
    if (!section_argument.is_empty())
        query_parameters.append(section_argument);
    if (!name_argument.is_empty())
        query_parameters.append(name_argument);

    auto page = TRY(Manual::Node::try_create_from_query(query_parameters));
    auto const* section = static_cast<Manual::SectionNode const*>(page->parent());

    if (pager.is_empty())
        pager = String::formatted("less -P 'Manual Page {}({}) line %l?e (END):.'",
            page->name().replace("'"sv, "'\\''"sv, ReplaceMode::FirstOnly),
            section->section_name().replace("'"sv, "'\\''"sv, ReplaceMode::FirstOnly));
    pid_t pager_pid = TRY(pipe_to_pager(pager));

    auto file = TRY(Core::Stream::File::open(page->path(), Core::Stream::OpenMode::Read));

    TRY(Core::System::pledge("stdio proc"));

    dbgln("Loading man page from {}", page->path());
    auto buffer = TRY(file->read_all());
    auto source = String::copy(buffer);

    auto title = String::formatted("SerenityOS {} Manual", section->category_name());

    int spaces = max(view_width / 2 - page->name().length() - section->section_name().length() - title.length() / 2 - 4, 0);
    outln("{}({}){}{}", page->name(), section->section_name(), String::repeated(' ', spaces), title);

    auto document = Markdown::Document::parse(source);
    VERIFY(document);

    String rendered = document->render_for_terminal(view_width);
    outln("{}", rendered);

    // FIXME: Remove this wait, it shouldn't be necessary but Shell does not
    //        resume properly without it. This wait also breaks <C-z> backgrounding
    fclose(stdout);
    int wstatus;
    waitpid(pager_pid, &wstatus, 0);
    return 0;
}

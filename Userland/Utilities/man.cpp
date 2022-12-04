/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/ByteBuffer.h>
#include <AK/DeprecatedString.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/Stream.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <LibMarkdown/Document.h>
#include <fcntl.h>
#include <spawn.h>
#include <stdio.h>
#include <sys/ioctl.h>
#include <sys/wait.h>
#include <unistd.h>

static ErrorOr<pid_t> pipe_to_pager(DeprecatedString const& command)
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

    StringView section {};
    StringView name {};
    StringView pager {};

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Read manual pages. Try 'man man' to get started.");
    args_parser.add_positional_argument(section, "Section of the man page", "section", Core::ArgsParser::Required::No);
    args_parser.add_positional_argument(name, "Name of the man page", "name");
    args_parser.add_option(pager, "Pager to pipe the man page to", "pager", 'P', "pager");
    args_parser.parse(arguments);

    auto make_path = [name](StringView section) {
        return DeprecatedString::formatted("/usr/share/man/man{}/{}.md", section, name);
    };
    if (section.is_empty()) {
        constexpr StringView sections[] = {
            "1"sv,
            "2"sv,
            "3"sv,
            "4"sv,
            "5"sv,
            "6"sv,
            "7"sv,
            "8"sv
        };
        for (auto s : sections) {
            DeprecatedString path = make_path(s);
            if (access(path.characters(), R_OK) == 0) {
                section = s;
                break;
            }
        }
    }
    auto filename = make_path(section);
    if (section == nullptr) {
        warnln("No man page for {}", name);
        exit(1);
    } else if (access(filename.characters(), R_OK) != 0) {
        warnln("No man page for {} in section {}", name, section);
        exit(1);
    }

    DeprecatedString pager_command;
    if (!pager.is_empty())
        pager_command = pager;
    else
        pager_command = DeprecatedString::formatted("less -P 'Manual Page {}({}) line %l?e (END):.'", StringView(name).replace("'"sv, "'\\''"sv, ReplaceMode::FirstOnly), StringView(section).replace("'"sv, "'\\''"sv, ReplaceMode::FirstOnly));
    pid_t pager_pid = TRY(pipe_to_pager(pager_command));

    auto file = TRY(Core::Stream::File::open(filename, Core::Stream::OpenMode::Read));

    TRY(Core::System::pledge("stdio proc"));

    dbgln("Loading man page from {}", filename);
    auto buffer = TRY(file->read_all());
    auto source = DeprecatedString::copy(buffer);

    const DeprecatedString title("SerenityOS manual");

    int spaces = view_width / 2 - DeprecatedString(name).length() - DeprecatedString(section).length() - title.length() / 2 - 4;
    if (spaces < 0)
        spaces = 0;
    out("{}({})", name, section);
    while (spaces--)
        out(" ");
    outln(title);

    auto document = Markdown::Document::parse(source);
    VERIFY(document);

    DeprecatedString rendered = document->render_for_terminal(view_width);
    outln("{}", rendered);

    // FIXME: Remove this wait, it shouldn't be necessary but Shell does not
    //        resume properly without it. This wait also breaks <C-z> backgrounding
    fclose(stdout);
    int wstatus;
    waitpid(pager_pid, &wstatus, 0);
    return 0;
}

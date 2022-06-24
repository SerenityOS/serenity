/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/ByteBuffer.h>
#include <AK/DeprecatedString.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/Stream.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <LibManual/PageNode.h>
#include <LibManual/SectionNode.h>
#include <LibMarkdown/Document.h>
#include <spawn.h>
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

    pid_t pid = TRY(Core::System::posix_spawnp("sh"sv, &action, nullptr, const_cast<char**>(argv), environ));
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

    DeprecatedString section;
    DeprecatedString name;
    DeprecatedString pager;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Read manual pages. Try 'man man' to get started.");
    args_parser.add_positional_argument(section, "Section of the man page", "section", Core::ArgsParser::Required::No);
    args_parser.add_positional_argument(name, "Name of the man page", "name");
    args_parser.add_option(pager, "Pager to pipe the man page to", "pager", 'P', "pager");
    args_parser.parse(arguments);

    Optional<NonnullRefPtr<Manual::PageNode>> page;
    if (section.is_empty()) {
        for (auto const& s : Manual::sections) {
            auto const maybe_page = make_ref_counted<Manual::PageNode>(s, TRY(String::from_utf8(name)));
            if (Core::File::exists(TRY(maybe_page->path()).to_deprecated_string())) {
                page = maybe_page;
                section = s->section_name().to_deprecated_string();
                break;
            }
        }
    } else {
        auto number_section = section.to_uint();
        if (number_section.has_value())
            page = make_ref_counted<Manual::PageNode>(Manual::sections[number_section.value() - 1], TRY(String::from_utf8(name)));
        else
            warnln("Section name '{}' invalid", section);
    }

    if (!page.has_value()) {
        warnln("No man page for {}", name);
        exit(1);
    } else if (!Core::File::exists(TRY((*page)->path()))) {
        warnln("No man page for {} in section {}", name, section);
        exit(1);
    }

    if (pager.is_empty())
        pager = TRY(String::formatted("less -P 'Manual Page {}({}) line %l?e (END):.'", StringView(name).replace("'"sv, "'\\''"sv, ReplaceMode::FirstOnly), StringView(section).replace("'"sv, "'\\''"sv, ReplaceMode::FirstOnly))).to_deprecated_string();
    pid_t pager_pid = TRY(pipe_to_pager(pager));

    auto file = TRY(Core::Stream::File::open(TRY((*page)->path()), Core::Stream::OpenMode::Read));

    TRY(Core::System::pledge("stdio proc"));

    dbgln("Loading man page from {}", (*page)->path());
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

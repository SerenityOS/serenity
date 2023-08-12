/*
 * Copyright (c) 2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <stdlib.h>
#include <unistd.h>

namespace {

ErrorOr<void> dup_out_file(int fd_to_redirect)
{
    int fd = -1;
    DeprecatedString path = "nohup.out";
    auto options = O_CREAT | O_WRONLY | O_APPEND;
    auto mode = S_IRUSR | S_IWUSR;
    auto fd_or_error = Core::System::open(path, options, mode);

    if (fd_or_error.is_error()) {
        auto* home_env = getenv("HOME");
        if (!home_env)
            return Error::from_string_view("unable to open for appending as $HOME is not set"sv);

        path = LexicalPath::join(LexicalPath::canonicalized_path(home_env), path).string();
        fd = TRY(Core::System::open(path, options, mode));
    } else {
        fd = fd_or_error.value();
    }

    TRY(Core::System::dup2(fd, fd_to_redirect));

    TRY(Core::System::close(fd));

    if (fd_to_redirect != STDERR_FILENO)
        outln(stderr, "appending output to {}", path);

    return {};
}

void exit_with_error(Error error)
{
    if (error.is_syscall())
        warnln("nohup: error while calling {}: {}", error.string_literal(), strerror(error.code()));
    else if (error.is_errno())
        warnln("nohup: {}", strerror(error.code()));
    else
        warnln("nohup: {}", error.string_literal());
    exit(127);
}

}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    if (auto pledge_result = Core::System::pledge("stdio wpath cpath rpath exec sigaction"); pledge_result.is_error())
        exit_with_error(pledge_result.release_error());

    StringView utility;
    Vector<StringView> args;

    Core::ArgsParser args_parser;
    args_parser.set_stop_on_first_non_option(true);
    args_parser.set_general_help("Invoke a utility that will ignore SIGHUPs");
    args_parser.add_positional_argument(utility, "Utility to be invoked", "utility");
    args_parser.add_positional_argument(args, "Arguments to pass to utility", "args", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    auto stdout_is_a_tty = false;
    auto tty_or_error = Core::System::isatty(STDOUT_FILENO);
    if (tty_or_error.is_error()) {
        if (tty_or_error.error().code() != EBADF)
            exit_with_error(tty_or_error.release_error());
    } else {
        stdout_is_a_tty = tty_or_error.value();
    }

    if (stdout_is_a_tty) {
        if (auto dup_result = dup_out_file(STDOUT_FILENO); dup_result.is_error())
            exit_with_error(dup_result.release_error());
    }

    auto stderr_is_a_tty = false;
    tty_or_error = Core::System::isatty(STDERR_FILENO);
    if (tty_or_error.is_error()) {
        if (tty_or_error.error().code() != EBADF)
            exit_with_error(tty_or_error.release_error());
    } else {
        stderr_is_a_tty = tty_or_error.value();
    }

    if (stderr_is_a_tty) {
        auto dup_or_error = Core::System::dup2(STDOUT_FILENO, STDERR_FILENO);
        if (dup_or_error.is_error()) {
            if (dup_or_error.error().code() != EBADF)
                exit_with_error(dup_or_error.release_error());

            // NOTE: Standard output must be closed, so "...the same output shall
            //       instead be appended to the end of the nohup.out file..."
            if (auto dup_result = dup_out_file(STDERR_FILENO); dup_result.is_error())
                exit_with_error(dup_result.release_error());
        }
    }

    if (auto ignore_hup_or_error = Core::System::signal(SIGHUP, SIG_IGN); ignore_hup_or_error.is_error())
        exit_with_error(ignore_hup_or_error.release_error());

    if (auto prepend_or_error = args.try_prepend(utility); prepend_or_error.is_error())
        exit_with_error(prepend_or_error.release_error());

    auto exec_or_error = Core::System::exec(utility, args.span(), Core::System::SearchInPath::Yes);
    if (exec_or_error.is_error()) {
        auto error = exec_or_error.release_error();
        warnln("nohup: error while executing {}: {}", utility, strerror(error.code()));
        return error.code() == ENOENT ? 127 : 126;
    }

    return 0;
}

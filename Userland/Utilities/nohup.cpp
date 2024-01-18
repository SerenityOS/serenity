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

void dup_out_file(int fd_to_redirect)
{
    int fd = -1;
    ByteString path = "nohup.out";
    auto options = O_CREAT | O_WRONLY | O_APPEND;
    auto mode = S_IRUSR | S_IWUSR;
    auto fd_or_error = Core::System::open(path, options, mode);

    if (fd_or_error.is_error()) {
        auto* home_env = getenv("HOME");
        if (!home_env) {
            warnln("nohup: unable to open for appending as $HOME is not set");
            exit(127);
        }

        path = LexicalPath::join(LexicalPath::canonicalized_path(home_env), path).string();
        fd_or_error = Core::System::open(path, options, mode);
        if (fd_or_error.is_error()) {
            warnln("nohup: unable to open {} for appending: {}", path, strerror(fd_or_error.error().code()));
            exit(127);
        }
    }

    fd = fd_or_error.value();

    auto maybe_error = Core::System::dup2(fd, fd_to_redirect);
    if (maybe_error.is_error()) {
        warnln("nohup: redirection failed: {}", strerror(maybe_error.error().code()));
        exit(127);
    }

    MUST(Core::System::close(fd));

    if (fd_to_redirect != STDERR_FILENO)
        outln(stderr, "appending output to {}", path);
}

}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    Main::set_return_code_for_errors(127);
    TRY(Core::System::pledge("stdio wpath cpath rpath exec sigaction"));

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
        auto error = tty_or_error.release_error();
        if (error.code() != EBADF) {
            warnln("nohup: error while performing tty check on stdout: {}", strerror(error.code()));
            return 127;
        }
    } else {
        stdout_is_a_tty = tty_or_error.value();
    }

    if (stdout_is_a_tty)
        dup_out_file(STDOUT_FILENO);

    auto stderr_is_a_tty = false;
    tty_or_error = Core::System::isatty(STDERR_FILENO);
    if (tty_or_error.is_error()) {
        auto error = tty_or_error.release_error();
        if (error.code() != EBADF) {
            warnln("nohup: error while performing tty check on stderr: {}", strerror(error.code()));
            return 127;
        }
    } else {
        stderr_is_a_tty = tty_or_error.value();
    }

    if (stderr_is_a_tty) {
        auto dup_or_error = Core::System::dup2(STDOUT_FILENO, STDERR_FILENO);
        if (dup_or_error.is_error()) {
            auto error = dup_or_error.release_error();
            if (error.code() != EBADF) {
                warnln("nohup: error redirecting stderr to stdout: {}", strerror(error.code()));
                return 127;
            }
            // NOTE: Standard output must be closed, so "...the same output shall
            //       instead be appended to the end of the nohup.out file..."
            dup_out_file(STDERR_FILENO);
        }
    }

    TRY(Core::System::signal(SIGHUP, SIG_IGN));

    TRY(args.try_prepend(utility));
    auto exec_or_error = Core::System::exec(utility, args.span(), Core::System::SearchInPath::Yes);
    if (exec_or_error.is_error()) {
        auto error = exec_or_error.release_error();
        warnln("nohup: error while calling exec: {}", strerror(error.code()));
        return error.code() == ENOENT ? 127 : 126;
    }

    return 0;
}

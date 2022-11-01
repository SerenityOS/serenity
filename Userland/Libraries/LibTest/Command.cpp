/*
 * Copyright (c) 2022, demostanis worlds <demostanis@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/Stream.h>
#include <LibCore/System.h>
#include <LibTest/Command.h>

namespace Test {

ErrorOr<CommandResult> run_command(String const& command, int timeout)
{
    auto parts = command.split_view(' ');
    if (parts.size() < 1)
        return Error::from_string_literal("Missing command");
    auto command_name = parts.first();

    auto stdout_pipe = TRY(Core::System::pipe2(0));
    auto stderr_pipe = TRY(Core::System::pipe2(0));
    auto pid = TRY(Core::System::fork());
    if (pid == 0) {
        // We're child
        if (timeout)
            alarm(timeout);

        TRY(Core::System::dup2(stdout_pipe[1], STDOUT_FILENO));
        TRY(Core::System::dup2(stderr_pipe[1], STDERR_FILENO));
        TRY(Core::System::exec(command_name, parts, Core::System::SearchInPath::Yes, {}));
        VERIFY_NOT_REACHED();
    }
    TRY(Core::System::close(stdout_pipe[1]));
    TRY(Core::System::close(stderr_pipe[1]));

    auto exit_code { -1 };
    auto status = TRY(Core::System::waitpid(pid, 0)).status;
    if (WIFSIGNALED(status) && WTERMSIG(status) == SIGALRM)
        return Error::from_string_literal("Timed out");
    if (WIFEXITED(status))
        exit_code = WEXITSTATUS(status);

    auto output_stream = TRY(Core::Stream::File::adopt_fd(stdout_pipe[0], Core::Stream::OpenMode::Read));
    auto output = TRY(output_stream->read_all());
    auto error_stream = TRY(Core::Stream::File::adopt_fd(stderr_pipe[0], Core::Stream::OpenMode::Read));
    auto error = TRY(error_stream->read_all());

    return CommandResult {
        String::copy(output),
        String::copy(error),
        exit_code,
        status,
    };
}

}

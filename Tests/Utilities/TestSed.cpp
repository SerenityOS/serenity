/*
 * Copyright (c) 2023, Rodrigo Tobar <rtobarc@gmail.com>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ScopeGuard.h>
#include <AK/StringView.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibTest/Macros.h>
#include <LibTest/TestCase.h>
#include <spawn.h>
#include <unistd.h>

class Process {
public:
    struct ProcessOutputs {
        AK::ByteBuffer standard_output;
        AK::ByteBuffer standard_error;
    };

    static ErrorOr<OwnPtr<Process>> create(StringView command, char const* const arguments[])
    {
        auto stdin_fds = TRY(Core::System::pipe2(O_CLOEXEC));
        auto stdout_fds = TRY(Core::System::pipe2(O_CLOEXEC));
        auto stderr_fds = TRY(Core::System::pipe2(O_CLOEXEC));

        posix_spawn_file_actions_t file_actions;
        posix_spawn_file_actions_init(&file_actions);
        posix_spawn_file_actions_adddup2(&file_actions, stdin_fds[0], STDIN_FILENO);
        posix_spawn_file_actions_adddup2(&file_actions, stdout_fds[1], STDOUT_FILENO);
        posix_spawn_file_actions_adddup2(&file_actions, stderr_fds[1], STDERR_FILENO);

        auto pid = TRY(Core::System::posix_spawnp(command, &file_actions, nullptr, const_cast<char**>(arguments), environ));

        posix_spawn_file_actions_destroy(&file_actions);
        ArmedScopeGuard runner_kill { [&pid] { kill(pid, SIGKILL); } };

        TRY(Core::System::close(stdin_fds[0]));
        TRY(Core::System::close(stdout_fds[1]));
        TRY(Core::System::close(stderr_fds[1]));

        auto stdin_file = TRY(Core::File::adopt_fd(stdin_fds[1], Core::File::OpenMode::Write));
        auto stdout_file = TRY(Core::File::adopt_fd(stdout_fds[0], Core::File::OpenMode::Read));
        auto stderr_file = TRY(Core::File::adopt_fd(stderr_fds[0], Core::File::OpenMode::Read));

        runner_kill.disarm();

        return make<Process>(pid, move(stdin_file), move(stdout_file), move(stderr_file));
    }

    Process(pid_t pid, NonnullOwnPtr<Core::File> stdin_file, NonnullOwnPtr<Core::File> stdout_file, NonnullOwnPtr<Core::File> stderr_file)
        : m_pid(pid)
        , m_stdin(move(stdin_file))
        , m_stdout(move(stdout_file))
        , m_stderr(move(stderr_file))
    {
    }

    ErrorOr<void> write(StringView input)
    {
        TRY(m_stdin->write_until_depleted(input.bytes()));
        m_stdin->close();
        return {};
    }

    bool write_lines(Span<DeprecatedString> lines)
    {
        // It's possible the process dies before we can write all the tests
        // to the stdin. So make sure that we don't crash but just stop writing.
        struct sigaction action_handler {
            .sa_handler = SIG_IGN, .sa_mask = {}, .sa_flags = 0,
        };
        struct sigaction old_action_handler;
        if (sigaction(SIGPIPE, &action_handler, &old_action_handler) < 0) {
            perror("sigaction");
            return false;
        }

        for (DeprecatedString const& line : lines) {
            if (m_stdin->write_until_depleted(DeprecatedString::formatted("{}\n", line).bytes()).is_error())
                break;
        }

        // Ensure that the input stream ends here, whether we were able to write all lines or not
        m_stdin->close();

        // It's not really a problem if this signal failed
        if (sigaction(SIGPIPE, &old_action_handler, nullptr) < 0)
            perror("sigaction");

        return true;
    }

    ErrorOr<ProcessOutputs> read_all()
    {
        return ProcessOutputs { TRY(m_stdout->read_until_eof()), TRY(m_stderr->read_until_eof()) };
    }

    enum class ProcessResult {
        Running,
        DoneWithZeroExitCode,
        Failed,
        FailedFromTimeout,
        Unknown,
    };

    ErrorOr<ProcessResult> status(int options = 0)
    {
        if (m_pid == -1)
            return ProcessResult::Unknown;

        m_stdin->close();

        auto wait_result = TRY(Core::System::waitpid(m_pid, options));
        if (wait_result.pid == 0) {
            // Attempt to kill it, since it has not finished yet somehow
            return ProcessResult::Running;
        }
        m_pid = -1;

        if (WIFSIGNALED(wait_result.status) && WTERMSIG(wait_result.status) == SIGALRM)
            return ProcessResult::FailedFromTimeout;

        if (WIFEXITED(wait_result.status) && WEXITSTATUS(wait_result.status) == 0)
            return ProcessResult::DoneWithZeroExitCode;

        return ProcessResult::Failed;
    }

private:
    pid_t m_pid;
    NonnullOwnPtr<Core::File> m_stdin;
    NonnullOwnPtr<Core::File> m_stdout;
    NonnullOwnPtr<Core::File> m_stderr;
};

static void run_sed(Vector<char const*>&& arguments, StringView standard_input, StringView expected_stdout)
{
    MUST(arguments.try_insert(0, "sed"));
    MUST(arguments.try_append(nullptr));
    auto sed = MUST(Process::create("sed"sv, arguments.data()));
    MUST(sed->write(standard_input));
    auto [stdout, stderr] = MUST(sed->read_all());
    auto status = MUST(sed->status());
    if (status != Process::ProcessResult::DoneWithZeroExitCode) {
        FAIL(DeprecatedString::formatted("sed didn't exit cleanly: status: {}, stdout:{}, stderr: {}", static_cast<int>(status), StringView { stdout.bytes() }, StringView { stderr.bytes() }));
    }
    EXPECT_EQ(StringView { expected_stdout.bytes() }, StringView { stdout.bytes() });
}

TEST_CASE(print_lineno)
{
    run_sed({ "=", "-n" }, "hi"sv, "1\n"sv);
    run_sed({ "=", "-n" }, "hi\n"sv, "1\n"sv);
    run_sed({ "=", "-n" }, "hi\nho"sv, "1\n2\n"sv);
    run_sed({ "=", "-n" }, "hi\nho\n"sv, "1\n2\n"sv);
}

TEST_CASE(s)
{
    run_sed({ "s/a/b/g" }, "aa\n"sv, "bb\n"sv);
    run_sed({ "s/././g" }, "aa\n"sv, "..\n"sv);
    run_sed({ "s/a/b/p" }, "a\n"sv, "b\nb\n"sv);
    run_sed({ "s/a/b/p", "-n" }, "a\n"sv, "b\n"sv);
    run_sed({ "1s/a/b/" }, "a\na"sv, "b\na\n"sv);
    run_sed({ "1s/a/b/p", "-n" }, "a\na"sv, "b\n"sv);
}

TEST_CASE(hold_space)
{
    run_sed({ "1h; 2x; 2p", "-n" }, "hi\nbye"sv, "hi\n"sv);
}

TEST_CASE(complex)
{
    run_sed({ "h; x; s/./*/gp; x; h; p; x; s/./*/gp", "-n" }, "hello serenity"sv, "**************\nhello serenity\n**************\n"sv);
}

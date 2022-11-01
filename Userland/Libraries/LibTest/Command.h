/*
 * Copyright (c) 2022, demostanis worlds <demostanis@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/Command.h>
#include <LibTest/Macros.h>
#include <LibTest/TestCase.h>
#include <sys/wait.h>
#include <unistd.h>

namespace Test {

struct CommandResult {
    String standard_output;
    String standard_error;
    int exit_code;
    int status;
};
ErrorOr<CommandResult> run_command(String const& command, int timeout = 10);

#define TRY_RUN_COMMAND(command)                                                  \
    ({                                                                            \
        auto _command_result = ::Test::run_command((command));                    \
        if (_command_result.is_error()) {                                         \
            ::AK::warnln("\033[31;1mFAIL\033[0m: {}:{}: TRY_RUN_COMMAND({}): {}", \
                __FILE__, __LINE__, command, _command_result.error());            \
            ::Test::current_test_case_did_fail();                                 \
            return;                                                               \
        }                                                                         \
        _command_result.release_value();                                          \
    })

#define EXPECT_COMMAND_OUTPUT_EQ(command, command_output) \
    EXPECT_EQ(TRY_RUN_COMMAND((command)).standard_output, (command_output));

#define EXPECT_SHELL_COMMAND_OUTPUT_EQ(command, command_output) \
    EXPECT_COMMAND_OUTPUT_EQ("/bin/Shell -c \"" command "\"", (command_output));

#define EXPECT_COMMAND_SUCCEEDS(command) \
    EXPECT_EQ(TRY_RUN_COMMAND((command)).exit_code, 0);

#define EXPECT_SHELL_COMMAND_SUCCEEDS(command) \
    EXPECT_COMMAND_SUCCEEDS("/bin/Shell -c \"" command "\"");

#define EXPECT_COMMAND_FAILS(command) \
    EXPECT_NE(TRY_RUN_COMMAND((command)).exit_code, 0);

#define EXPECT_SHELL_COMMAND_FAILS(command) \
    EXPECT_COMMAND_FAILS("/bin/Shell -c \"" command "\"");

// Same macros as before, but taking a number of seconds before which the command times out

#define TRY_RUN_COMMAND_WITH_TIMEOUT(command, timeout)                                             \
    ({                                                                                             \
        auto _command_result = ::Test::run_command((command), (timeout));                          \
        if (_command_result.is_error()) {                                                          \
            ::AK::warnln("\033[31;1mFAIL\033[0m: {}:{}: TRY_RUN_COMMAND_WITH_TIMEOUT({}, {}): {}", \
                __FILE__, __LINE__, command, timeout, _command_result.error());                    \
            ::Test::current_test_case_did_fail();                                                  \
            return;                                                                                \
        }                                                                                          \
        _command_result.release_value();                                                           \
    })

#define EXPECT_COMMAND_WITH_TIMEOUT_OUTPUT_EQ(command, command_output, timeout) \
    EXPECT_EQ(TRY_RUN_COMMAND((command), (timeout)).standard_output, (command_output));

#define EXPECT_SHELL_COMMAND_WITH_TIMEOUT_OUTPUT_EQ(command, command_output, timeout) \
    EXPECT_COMMAND_WITH_TIMEOUT_OUTPUT_EQ("/bin/Shell -c \"" command "\"", (command_output), timeout);

#define EXPECT_COMMAND_WITH_TIMEOUT_SUCCEEDS(command, timeout) \
    EXPECT_EQ(TRY_RUN_WITH_TIMEOUT_COMMAND((command), (timeout)).exit_code, 0);

#define EXPECT_SHELL_WITH_TIMEOUT_COMMAND_SUCCEEDS(command, timeout) \
    EXPECT_COMMAND_WITH_TIMEOUT_SUCCEEDS("/bin/Shell -c \"" command "\"", (timeout));

#define EXPECT_COMMAND_WITH_TIMEOUT_FAILS(command) \
    EXPECT_NE(TRY_RUN_WITH_TIMEOUT_COMMAND((command), (timeout)).exit_code, 0);

#define EXPECT_SHELL_COMMAND_WITH_TIMEOUT_FAILS(command, timeout) \
    EXPECT_COMMAND_FAILS("/bin/Shell -c \"" command "\"", (timeout));

}

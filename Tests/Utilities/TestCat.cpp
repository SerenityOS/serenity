/*
 * Copyright (c) 2023, Rodrigo Tobar <rtobarc@gmail.com>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ScopeGuard.h>
#include <AK/StringView.h>
#include <LibCore/Command.h>
#include <LibCore/File.h>
#include <LibTest/Macros.h>
#include <LibTest/TestCase.h>

static void run_cat(Vector<char const*>&& arguments, StringView standard_input, StringView expected_stdout)
{
    MUST(arguments.try_insert(0, "cat"));
    MUST(arguments.try_append(nullptr));
    auto cat = MUST(Core::Command::create("cat"sv, arguments.data()));
    MUST(cat->write(standard_input));
    auto [stdout, stderr] = MUST(cat->read_all());
    auto status = MUST(cat->status());
    if (status != Core::Command::ProcessResult::DoneWithZeroExitCode) {
        FAIL(ByteString::formatted("cat didn't exit cleanly: status: {}, stdout:{}, stderr: {}", static_cast<int>(status), StringView { stdout.bytes() }, StringView { stderr.bytes() }));
    }

    EXPECT_EQ(StringView { expected_stdout.bytes() }, StringView { stdout.bytes() });
}

TEST_CASE(show_lines)
{
    run_cat({ "-n" }, "hello"sv, "     1\thello"sv);
    run_cat({ "-n" }, "hello\nworld"sv, "     1\thello\n     2\tworld"sv);
    run_cat({ "-n" }, "hello\n\nworld"sv, "     1\thello\n     2\t\n     3\tworld"sv);
    run_cat({ "-n" }, "\nhello"sv, "     1\t\n     2\thello"sv);
    run_cat({ "-n" }, "hello\n"sv, "     1\thello\n"sv);
    run_cat({ "-n" }, "hello\n\n"sv, "     1\thello\n     2\t\n"sv);
}

TEST_CASE(show_only_non_blank_lines)
{
    run_cat({ "-b" }, "hello"sv, "     1\thello"sv);
    run_cat({ "-b" }, "hello\nworld"sv, "     1\thello\n     2\tworld"sv);
    run_cat({ "-b" }, "hello\n\nworld"sv, "     1\thello\n\n     2\tworld"sv);
    run_cat({ "-b" }, "\nhello"sv, "\n     1\thello"sv);
    run_cat({ "-b" }, "hello\n"sv, "     1\thello\n"sv);
    run_cat({ "-b" }, "hello\n\n"sv, "     1\thello\n\n"sv);
}

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

static void run_sed(Vector<char const*>&& arguments, StringView standard_input, StringView expected_stdout)
{
    MUST(arguments.try_insert(0, "sed"));
    MUST(arguments.try_append(nullptr));
    auto sed = MUST(Core::Command::create("sed"sv, arguments.data()));
    MUST(sed->write(standard_input));
    auto [stdout, stderr] = MUST(sed->read_all());
    auto status = MUST(sed->status());
    if (status != Core::Command::ProcessResult::DoneWithZeroExitCode) {
        FAIL(ByteString::formatted("sed didn't exit cleanly: status: {}, stdout:{}, stderr: {}", static_cast<int>(status), StringView { stdout.bytes() }, StringView { stderr.bytes() }));
    }
    EXPECT_EQ(StringView { expected_stdout.bytes() }, StringView { stdout.bytes() });
}

TEST_CASE(transform_command)
{
    run_sed({ "y/fb/FB/" }, "foobar\n"sv, "FooBar\n"sv);
    run_sed({ "y;fb;FB;" }, "foobar\n"sv, "FooBar\n"sv);
    run_sed({ "y///" }, "foobar\n"sv, "foobar\n"sv);
    run_sed({ "y/abcdefghijklmnopqrstuvwxyz/defghijklmnopqrstuvwxyzabc/" }, "attack at dawn\n"sv, "dwwdfn dw gdzq\n"sv);
}

TEST_CASE(comments)
{
    run_sed({ "# This is a comment! " }, "foo\nbar\nbaz\n"sv, "foo\nbar\nbaz\n"sv);
    run_sed({ "# This is a comment!\np" }, "foo\nbar\nbaz\n"sv, "foo\nfoo\nbar\nbar\nbaz\nbaz\n"sv);
}

TEST_CASE(quit_after_single_line)
{
    run_sed({ "q" }, "foo\n"sv, "foo\n"sv);
    run_sed({ "1q" }, "foo\n"sv, "foo\n"sv);
}

TEST_CASE(delete_single_line)
{
    run_sed({ "1d" }, "1\n2\n"sv, "2\n"sv);
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

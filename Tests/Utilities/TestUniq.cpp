/*
 * Copyright (c) 2023, Rodrigo Tobar <rtobarc@gmail.com>.
 * Copyright (c) 2024, Daniel Gaston <tfd@tuta.io>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ScopeGuard.h>
#include <AK/StringView.h>
#include <LibCore/Command.h>
#include <LibCore/File.h>
#include <LibTest/Macros.h>
#include <LibTest/TestCase.h>

static void run_uniq(Vector<char const*>&& arguments, StringView standard_input, StringView expected_stdout)
{
    MUST(arguments.try_insert(0, "uniq"));
    MUST(arguments.try_append(nullptr));
    auto uniq = MUST(Core::Command::create("uniq"sv, arguments.data()));
    MUST(uniq->write(standard_input));
    auto [stdout, stderr] = MUST(uniq->read_all());
    auto status = MUST(uniq->status());
    if (status != Core::Command::ProcessResult::DoneWithZeroExitCode) {
        FAIL(ByteString::formatted("uniq didn't exit cleanly: status: {}, stdout: {}, stderr: {}", static_cast<int>(status), StringView { stdout.bytes() }, StringView { stderr.bytes() }));
    }
    EXPECT_EQ(StringView { expected_stdout.bytes() }, StringView { stdout.bytes() });
}

TEST_CASE(two_duplicate_lines)
{
    run_uniq({}, "AAA\nAAA\n"sv, "AAA\n"sv);
}

TEST_CASE(two_unique_lines)
{
    run_uniq({}, "AAA\nAaA\n"sv, "AAA\nAaA\n"sv);
}

TEST_CASE(long_line)
{
    auto input = Array<u8, 4096> {};
    auto expected_output = Array<u8, 2048> {};
    // Create two lines of 2047 A's and a newline.
    input.fill('A');
    input[2047] = '\n';
    input[4095] = '\n';

    expected_output.fill('A');
    expected_output[2047] = '\n';

    run_uniq({}, StringView { input }, StringView { expected_output });
}

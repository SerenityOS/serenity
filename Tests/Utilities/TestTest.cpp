/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <LibCore/File.h>
#include <LibCore/Process.h>
#include <LibFileSystem/TempFile.h>
#include <LibTest/TestCase.h>

static void run_test(Vector<ByteString>&& arguments, int expected_exit_status)
{
    auto test = TRY_OR_FAIL(Core::Process::spawn(
        Core::ProcessSpawnOptions { .executable = "test"sv,
            .search_for_executable_in_path = true,
            .arguments = arguments }));
    auto exited_with_code_0 = TRY_OR_FAIL(test.wait_for_termination());
    EXPECT_EQ(expected_exit_status, exited_with_code_0 ? 0 : 1);
}

TEST_CASE(option_s)
{
    run_test({ "-s", "file_that_do_not_exist" }, 1);

    // empty file
    auto temp_file = TRY_OR_FAIL(FileSystem::TempFile::create_temp_file());
    run_test({ "-s", MUST(ByteString::from_utf8(temp_file->path())) }, 1);

    auto file = TRY_OR_FAIL(Core::File::open(temp_file->path(), Core::File::OpenMode::Write));
    TRY_OR_FAIL(file->write_until_depleted("file content\n"sv));
    run_test({ "-s", MUST(ByteString::from_utf8(temp_file->path())) }, 0);
}

/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/System.h>
#include <LibFileSystem/TempFile.h>
#include <LibTest/TestCase.h>

TEST_CASE(rename_directory_over_file_fails)
{
    auto directory = TRY_OR_FAIL(FileSystem::TempFile::create_temp_directory());
    auto file = TRY_OR_FAIL(FileSystem::TempFile::create_temp_file());

    auto error = Core::System::rename(directory->path(), file->path());
    EXPECT(error.is_error());
    EXPECT_EQ(error.error().code(), ENOTDIR);
}

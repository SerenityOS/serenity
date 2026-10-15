/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/System.h>
#include <LibFileSystem/TempFile.h>
#include <LibTest/TestCase.h>

TEST_CASE(rename_file_over_symlink)
{
    auto file = TRY_OR_FAIL(FileSystem::TempFile::create_temp_file());
    TRY_OR_FAIL(Core::System::truncate(file->path(), 256));

    static constexpr StringView LINK_PATH = "/tmp/link"sv;
    (void)Core::System::unlink(LINK_PATH);
    TRY_OR_FAIL(Core::System::symlink(file->path(), LINK_PATH));

    TRY_OR_FAIL(Core::System::rename(file->path(), LINK_PATH));

    auto stat = TRY_OR_FAIL(Core::System::lstat(LINK_PATH));
    EXPECT(!S_ISLNK(stat.st_mode));
    EXPECT(S_ISREG(stat.st_mode));
    EXPECT_EQ(stat.st_size, 256u);
}

TEST_CASE(rename_directory_over_file_fails)
{
    auto directory = TRY_OR_FAIL(FileSystem::TempFile::create_temp_directory());
    auto file = TRY_OR_FAIL(FileSystem::TempFile::create_temp_file());

    auto error = Core::System::rename(directory->path(), file->path());
    EXPECT(error.is_error());
    EXPECT_EQ(error.error().code(), ENOTDIR);
}

/*
 * Copyright (c) 2026, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/System.h>
#include <LibFileSystem/FileSystem.h>
#include <LibTest/TestCase.h>

static void test_symlink(StringView target, StringView link_path, StringView resolved_path)
{
    if (FileSystem::exists(link_path))
        TRY_OR_FAIL(Core::System::unlink(link_path));

    TRY_OR_FAIL(Core::System::symlink(target, link_path));
    auto real_path = TRY_OR_FAIL(FileSystem::real_path(link_path));
    EXPECT(real_path == resolved_path);
}

TEST_CASE(absolute_to_absolute_symlink)
{
    test_symlink("/"sv, "/tmp/test_absolute_to_absolute_symlink"sv, "/"sv);
}

TEST_CASE(absolute_to_relative_symlink)
{
    TRY_OR_FAIL(Core::System::chdir("/tmp"sv));
    test_symlink("/"sv, "test_absolute_to_relative_symlink"sv, "/"sv);
}

TEST_CASE(relative_to_absolute_symlink)
{
    TRY_OR_FAIL(Core::System::chdir("/tmp"sv));
    test_symlink("../"sv, "/tmp/test_relative_to_absolute_symlink"sv, "/"sv);
}

TEST_CASE(relative_to_relative_symlink)
{
    TRY_OR_FAIL(Core::System::chdir("/tmp"sv));
    test_symlink("../"sv, "test_relative_to_relative_symlink"sv, "/"sv);
}

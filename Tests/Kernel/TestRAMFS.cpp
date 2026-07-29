/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Time.h>
#include <LibCore/System.h>
#include <LibTest/TestCase.h>

TEST_CASE(link_count)
{
    static constexpr StringView FILE_PATH = "/tmp/test_link_count"sv;
    static constexpr StringView LINK_PATH = "/tmp/test_link_count.link"sv;

    (void)Core::System::unlink(FILE_PATH);
    (void)Core::System::unlink(LINK_PATH);

    int fd = TRY_OR_FAIL(Core::System::open(FILE_PATH, O_CREAT | O_WRONLY, 0600));
    close(fd);

    auto file_stat = TRY_OR_FAIL(Core::System::stat(FILE_PATH));
    EXPECT_EQ(file_stat.st_nlink, static_cast<nlink_t>(1));

    TRY_OR_FAIL(Core::System::link(FILE_PATH, LINK_PATH));

    file_stat = TRY_OR_FAIL(Core::System::stat(FILE_PATH));
    EXPECT_EQ(file_stat.st_nlink, static_cast<nlink_t>(2));

    auto link_stat = TRY_OR_FAIL(Core::System::stat(LINK_PATH));
    EXPECT_EQ(link_stat.st_nlink, static_cast<nlink_t>(2));

    // All names must resolve to the same inode.
    EXPECT_EQ(file_stat.st_ino, link_stat.st_ino);

    (void)Core::System::unlink(FILE_PATH);
    link_stat = TRY_OR_FAIL(Core::System::stat(LINK_PATH));
    EXPECT_EQ(link_stat.st_nlink, static_cast<nlink_t>(1));
}

TEST_CASE(child_updates_time)
{
    static constexpr StringView DIRECTORY_PATH = "/tmp/test_directory"sv;
    static constexpr StringView CHILD_PATH = "/tmp/test_directory/child"sv;

    (void)Core::System::unlink(CHILD_PATH);
    (void)Core::System::rmdir(DIRECTORY_PATH);

    TRY_OR_FAIL(Core::System::mkdir(DIRECTORY_PATH, 0755));

    auto before = TRY_OR_FAIL(Core::System::stat(DIRECTORY_PATH));

    usleep(10'000); // Sleep 10ms to ensure that the Kernel's internal time has updated.
    int fd = TRY_OR_FAIL(Core::System::open(CHILD_PATH, O_CREAT | O_WRONLY, 0600));
    close(fd);

    auto after = TRY_OR_FAIL(Core::System::stat(DIRECTORY_PATH));

    EXPECT_EQ(Duration::from_timespec(before.st_atim), Duration::from_timespec(after.st_atim));
    EXPECT(Duration::from_timespec(before.st_mtim) < Duration::from_timespec(after.st_mtim));
    EXPECT(Duration::from_timespec(before.st_ctim) < Duration::from_timespec(after.st_ctim));
    EXPECT_EQ(Duration::from_timespec(after.st_mtim), Duration::from_timespec(after.st_ctim));
}

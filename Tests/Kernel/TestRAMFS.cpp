/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

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

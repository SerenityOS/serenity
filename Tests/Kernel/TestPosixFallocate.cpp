/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/System.h>
#include <LibTest/TestCase.h>

TEST_CASE(posix_fallocate_basics)
{
    char pattern[] = "/tmp/posix_fallocate.XXXXXX";
    auto fd = MUST(Core::System::mkstemp(pattern));
    VERIFY(fd >= 0);

    {
        // Valid use, grows file to new size.
        auto result = Core::System::posix_fallocate(fd, 0, 1024);
        EXPECT_EQ(result.is_error(), false);

        auto stat = MUST(Core::System::fstat(fd));
        EXPECT_EQ(stat.st_size, 1024);
    }

    {
        // Invalid fd (-1)
        auto result = Core::System::posix_fallocate(-1, 0, 1024);
        EXPECT_EQ(result.is_error(), true);
        EXPECT_EQ(result.error().code(), EBADF);
    }

    {
        // Invalid length (-1)
        auto result = Core::System::posix_fallocate(fd, 0, -1);
        EXPECT_EQ(result.is_error(), true);
        EXPECT_EQ(result.error().code(), EINVAL);
    }

    {
        // Invalid length (0)
        auto result = Core::System::posix_fallocate(fd, 0, 0);
        EXPECT_EQ(result.is_error(), true);
        EXPECT_EQ(result.error().code(), EINVAL);
    }

    {
        // Invalid offset (-1)
        auto result = Core::System::posix_fallocate(fd, -1, 1024);
        EXPECT_EQ(result.is_error(), true);
        EXPECT_EQ(result.error().code(), EINVAL);
    }

    MUST(Core::System::close(fd));
}

TEST_CASE(posix_fallocate_on_device_file)
{
    auto fd = MUST(Core::System::open("/dev/zero"sv, O_RDWR));
    VERIFY(fd >= 0);
    auto result = Core::System::posix_fallocate(fd, 0, 100);
    EXPECT_EQ(result.is_error(), true);
    EXPECT_EQ(result.error().code(), ENODEV);
    MUST(Core::System::close(fd));
}

TEST_CASE(posix_fallocate_on_pipe)
{
    auto pipefds = MUST(Core::System::pipe2(0));
    auto result = Core::System::posix_fallocate(pipefds[1], 0, 100);
    EXPECT_EQ(result.is_error(), true);
    EXPECT_EQ(result.error().code(), ESPIPE);
    MUST(Core::System::close(pipefds[0]));
    MUST(Core::System::close(pipefds[1]));
}

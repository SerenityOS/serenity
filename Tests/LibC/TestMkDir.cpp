/*
 * Copyright (c) 2021, Max Wipfli <max.wipfli@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <errno.h>
#include <stdlib.h>
#include <sys/stat.h>
#include <time.h>
#include <unistd.h>

static ByteString random_dirname()
{
    return ByteString::formatted("/tmp/test_mkdir_{:04x}", (u16)rand());
}

TEST_SETUP
{
    srand(time(NULL));
}

TEST_CASE(basic)
{
    auto dirname = random_dirname();
    int res = mkdir(dirname.characters(), 0755);
    EXPECT(res == 0);

    res = mkdir(dirname.characters(), 0755);
    int cached_errno = errno;
    EXPECT(res < 0);
    EXPECT_EQ(cached_errno, EEXIST);
}

TEST_CASE(insufficient_permissions)
{
    VERIFY(getuid() != 0);
    int res = mkdir("/root/foo", 0755);
    int cached_errno = errno;
    EXPECT(res < 0);
    EXPECT_EQ(cached_errno, EACCES);
}

TEST_CASE(nonexistent_parent)
{
    auto parent = random_dirname();
    auto child = ByteString::formatted("{}/foo", parent);
    int res = mkdir(child.characters(), 0755);
    int cached_errno = errno;
    EXPECT(res < 0);
    EXPECT_EQ(cached_errno, ENOENT);
}

TEST_CASE(parent_is_file)
{
    int res = mkdir("/etc/passwd/foo", 0755);
    int cached_errno = errno;
    EXPECT(res < 0);
    EXPECT_EQ(cached_errno, ENOTDIR);
}

TEST_CASE(pledge)
{
    int res = pledge("stdio cpath", nullptr);
    EXPECT(res == 0);

    auto dirname = random_dirname();
    res = mkdir(dirname.characters(), 0755);
    EXPECT(res == 0);
    // FIXME: Somehow also check that mkdir() stops working when removing the cpath promise. This is currently
    //        not possible because this would prevent the unveil test case from properly working.
}

TEST_CASE(unveil)
{
    int res = unveil("/tmp", "rwc");
    EXPECT(res == 0);

    auto dirname = random_dirname();
    res = mkdir(dirname.characters(), 0755);
    EXPECT(res == 0);

    res = unveil("/tmp", "rw");
    EXPECT(res == 0);

    dirname = random_dirname();
    res = mkdir(dirname.characters(), 0755);
    int cached_errno = errno;
    EXPECT(res < 0);
    EXPECT_EQ(cached_errno, EACCES);

    res = unveil("/tmp", "");
    EXPECT(res == 0);

    dirname = random_dirname();
    res = mkdir(dirname.characters(), 0755);
    cached_errno = errno;
    EXPECT(res < 0);
    EXPECT_EQ(cached_errno, ENOENT);

    res = unveil(nullptr, nullptr);
    EXPECT(res == 0);
}

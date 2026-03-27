/*
 * Copyright (c) 2024, Space Meyer <git@the-space.agency>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Library/PathCanonicalization.h>
#include <Kernel/Sections.h>
#include <LibMain/Main.h>
#include <LibTest/Macros.h>
#include <LibTest/TestCase.h>
#include <unistd.h>

TEST_CASE(test_weird_paths_canonicalization)
{
    auto res = Kernel::canonicalize_absolute_path("/a//b///c/"sv);
    EXPECT_EQ(res, "/a/b/c"sv);

    res = Kernel::canonicalize_absolute_path("/a/./b/../c/"sv);
    EXPECT_EQ(res, "/a/c"sv);

    res = Kernel::canonicalize_absolute_path("/../"sv);
    EXPECT_EQ(res, "/"sv);

    res = Kernel::canonicalize_absolute_path("/../../.."sv);
    EXPECT_EQ(res, "/"sv);

    res = Kernel::canonicalize_absolute_path("/a/b/../../.."sv);
    EXPECT_EQ(res, "/"sv);

    res = Kernel::canonicalize_absolute_path("/a/../b/./c/"sv);
    EXPECT_EQ(res, "/b/c"sv);

    res = Kernel::canonicalize_absolute_path("////"sv);
    EXPECT_EQ(res, "/"sv);

    res = Kernel::canonicalize_absolute_path("/"sv);
    EXPECT_EQ(res, "/"sv);

    res = Kernel::canonicalize_absolute_path("/......./"sv);
    EXPECT_EQ(res, "/......."sv);

    res = Kernel::canonicalize_absolute_path("/a/.../b"sv);
    EXPECT_EQ(res, "/a/.../b"sv);

    res = Kernel::canonicalize_absolute_path("/a/..../b"sv);
    EXPECT_EQ(res, "/a/..../b"sv);

    res = Kernel::canonicalize_absolute_path("/./././"sv);
    EXPECT_EQ(res, "/"sv);

    res = Kernel::canonicalize_absolute_path("/../.././."sv);
    EXPECT_EQ(res, "/"sv);

    res = Kernel::canonicalize_absolute_path("/a/b/c/."sv);
    EXPECT_EQ(res, "/a/b/c"sv);

    res = Kernel::canonicalize_absolute_path("/a/b/c/.."sv);
    EXPECT_EQ(res, "/a/b"sv);

    res = Kernel::canonicalize_absolute_path("/././../test/./././../test2//////"sv);
    EXPECT_EQ(res, "/test2"sv);

    res = Kernel::canonicalize_absolute_path("/..../...../../"sv);
    EXPECT_EQ(res, "/...."sv);
}

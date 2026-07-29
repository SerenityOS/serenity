/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteString.h>
#include <LibCore/System.h>
#include <LibTest/TestCase.h>

#ifdef AK_OS_BSD_GENERIC
#    include <sys/syslimits.h>
#endif

TEST_CASE(path_too_long)
{
    StringView path_base = "/this/path/is/long"sv;
    auto long_path = ByteString::repeated(path_base, (PATH_MAX / path_base.length()) + 1);
    VERIFY(long_path.length() > PATH_MAX);
    EXPECT_EQ(Core::System::stat(long_path).error().code(), ENAMETOOLONG);
}

TEST_CASE(name_too_long)
{
    auto long_name = ByteString::formatted("/tmp/{}"sv, ByteString::repeated('a', NAME_MAX + 1));
    EXPECT_EQ(Core::System::stat(long_name).error().code(), ENAMETOOLONG);
    EXPECT_EQ(Core::System::stat(long_name.substring_view(0, long_name.length() - 1)).error().code(), ENOENT);
}

/*
 * Copyright (c) 2023, Gurkirat Singh <tbhaxor@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Slugify.h>
#include <LibTest/TestCase.h>

TEST_CASE(ignore_unicode_characters)
{
    EXPECT_EQ(MUST(slugify("Hello World!🎉"_string)), "hello-world"_string);
}

TEST_CASE(all_whitespace_empty_string)
{
    EXPECT_EQ(MUST(slugify("  "_string)), ""_string);
}

TEST_CASE(squeeze_multiple_whitespace)
{
    EXPECT_EQ(MUST(slugify("Hello   World"_string)), "hello-world"_string);
}

TEST_CASE(trim_trailing_whitelist)
{
    EXPECT_EQ(MUST(slugify("Hello   World    "_string)), "hello-world"_string);
}

TEST_CASE(lowercase_all_result)
{
    EXPECT_EQ(MUST(slugify("HelloWorld"_string)), "helloworld"_string);
}

TEST_CASE(slug_glue_change)
{
    EXPECT_EQ(MUST(slugify("Hello World"_string, '|')), "hello|world"_string);
}

TEST_CASE(multiple_glue_squeeze)
{
    EXPECT_EQ(MUST(slugify("Hello_ World"_string, '_')), "hello_world"_string);
}

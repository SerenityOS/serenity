/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/LexicalPath.h>
#include <AK/String.h>

TEST_CASE(construct)
{
    EXPECT_EQ(LexicalPath().is_valid(), false);
}

TEST_CASE(basic)
{
    LexicalPath path("/abc/def/ghi.txt");
    EXPECT_EQ(path.is_valid(), true);
    EXPECT_EQ(path.basename(), "ghi.txt");
    EXPECT_EQ(path.title(), "ghi");
    EXPECT_EQ(path.extension(), "txt");
    EXPECT_EQ(path.parts().size(), 3u);
    EXPECT_EQ(path.parts(), Vector<String>({ "abc", "def", "ghi.txt" }));
    EXPECT_EQ(path.string(), "/abc/def/ghi.txt");
    EXPECT_EQ(LexicalPath(".").string(), ".");
    EXPECT_EQ(LexicalPath("..").string(), "..");
}

TEST_CASE(dotdot_coalescing)
{
    EXPECT_EQ(LexicalPath("/home/user/../../not/home").string(), "/not/home");
    EXPECT_EQ(LexicalPath("/../../../../").string(), "/");
    EXPECT_EQ(LexicalPath("./../../../../").string(), "../../../..");
    EXPECT_EQ(LexicalPath("../../../../../").string(), "../../../../..");
}

TEST_CASE(has_extension)
{
    {
        LexicalPath path("/tmp/simple.png");
        EXPECT(path.has_extension(".png"));
        EXPECT(path.has_extension(".pnG"));
        EXPECT(path.has_extension(".PNG"));
    }

    {
        LexicalPath path("/TMP/SIMPLE.PNG");
        EXPECT(path.has_extension(".png"));
        EXPECT(path.has_extension(".pnG"));
        EXPECT(path.has_extension(".PNG"));
    }

    {
        LexicalPath path(".png");
        EXPECT(path.has_extension(".png"));
    }

    {
        LexicalPath path;
        EXPECT_EQ(path.has_extension(".png"), false);
    }

    {
        LexicalPath path("png");
        EXPECT_EQ(path.has_extension(".png"), false);
    }
}

TEST_CASE(relative_path)
{
    EXPECT_EQ(LexicalPath::relative_path("/tmp/abc.txt", "/tmp"), "abc.txt");
    EXPECT_EQ(LexicalPath::relative_path("/tmp/abc.txt", "/tmp/"), "abc.txt");
    EXPECT_EQ(LexicalPath::relative_path("/tmp/abc.txt", "/"), "tmp/abc.txt");
    EXPECT_EQ(LexicalPath::relative_path("/tmp/abc.txt", "/usr"), "/tmp/abc.txt");

    EXPECT_EQ(LexicalPath::relative_path("/tmp/foo.txt", "tmp"), String {});
    EXPECT_EQ(LexicalPath::relative_path("tmp/foo.txt", "/tmp"), String {});
}

TEST_CASE(dirname)
{
    EXPECT_EQ(LexicalPath(".").dirname(), ".");
    EXPECT_EQ(LexicalPath("/").dirname(), "/");
    EXPECT_EQ(LexicalPath("abc.txt").dirname(), ".");
    EXPECT_EQ(LexicalPath("/abc.txt").dirname(), "/");
}

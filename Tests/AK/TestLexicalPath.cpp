/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Max Wipfli <max.wipfli@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/DeprecatedString.h>
#include <AK/LexicalPath.h>

TEST_CASE(relative_path)
{
    EXPECT_EQ(MUST(LexicalPath::relative_path("/tmp/abc.txt"sv, "/tmp"sv)), "abc.txt"sv);
    EXPECT_EQ(MUST(LexicalPath::relative_path("/tmp/abc.txt"sv, "/tmp/"sv)), "abc.txt"sv);
    EXPECT_EQ(MUST(LexicalPath::relative_path("/tmp/abc.txt"sv, "/"sv)), "tmp/abc.txt"sv);
    EXPECT_EQ(MUST(LexicalPath::relative_path("/tmp/abc.txt"sv, "/usr"sv)), "/tmp/abc.txt"sv);

    EXPECT_EQ(MUST(LexicalPath::relative_path("/tmp/foo.txt"sv, "tmp"sv)), ""sv);
    EXPECT_EQ(MUST(LexicalPath::relative_path("tmp/foo.txt"sv, "/tmp"sv)), ""sv);

    EXPECT_EQ(MUST(LexicalPath::relative_path("/tmp/foo/bar/baz.txt"sv, "/tmp/bar/foo/"sv)), "../../foo/bar/baz.txt"sv);
}

TEST_CASE(regular_absolute_path)
{
    auto path = MUST(LexicalPath::from_string("/home/anon/foo.txt"sv));
    EXPECT_EQ(path.string(), "/home/anon/foo.txt");
    EXPECT_EQ(path.dirname(), "/home/anon");
    EXPECT_EQ(path.basename(), "foo.txt");
    EXPECT_EQ(path.title(), "foo");
    EXPECT_EQ(path.extension(), "txt");
    EXPECT(path.has_extension(".txt"sv));
    EXPECT(path.has_extension("txt"sv));
    EXPECT(!path.has_extension("txxt"sv));
    EXPECT_EQ(path.parts_view().size(), 3u);
    EXPECT_EQ(path.parts_view()[0], "home");
    EXPECT_EQ(path.parts_view()[1], "anon");
    EXPECT_EQ(path.parts_view()[2], "foo.txt");
}

TEST_CASE(regular_relative_path)
{
    auto path = MUST(LexicalPath::from_string("anon/foo.txt"sv));
    EXPECT_EQ(path.dirname(), "anon");
    EXPECT_EQ(path.basename(), "foo.txt");
    EXPECT_EQ(path.parts_view().size(), 2u);
    EXPECT_EQ(path.parts_view()[0], "anon");
    EXPECT_EQ(path.parts_view()[1], "foo.txt");
}

TEST_CASE(single_dot)
{
    {
        auto path = MUST(LexicalPath::from_string("/home/./anon/foo.txt"sv));
        EXPECT_EQ(path.string(), "/home/anon/foo.txt");
    }
    {
        auto path = MUST(LexicalPath::from_string("./test.txt"sv));
        EXPECT_EQ(path.string(), "test.txt");
    }
    {
        auto path = MUST(LexicalPath::from_string("./../test.txt"sv));
        EXPECT_EQ(path.string(), "../test.txt");
    }
}

TEST_CASE(relative_path_with_dotdot)
{
    auto path = MUST(LexicalPath::from_string("anon/../../foo.txt"sv));
    EXPECT_EQ(path.string(), "../foo.txt");
    EXPECT_EQ(path.dirname(), "..");
    EXPECT_EQ(path.basename(), "foo.txt");
    EXPECT_EQ(path.parts_view().size(), 2u);
    EXPECT_EQ(path.parts_view()[0], "..");
    EXPECT_EQ(path.parts_view()[1], "foo.txt");
}

TEST_CASE(absolute_path_with_dotdot)
{
    {
        auto path = MUST(LexicalPath::from_string("/test/../foo.txt"sv));
        EXPECT_EQ(path.string(), "/foo.txt");
    }
    {
        auto path = MUST(LexicalPath::from_string("/../../foo.txt"sv));
        EXPECT_EQ(path.string(), "/foo.txt");
    }
}

TEST_CASE(more_dotdot_paths)
{
    EXPECT_EQ(MUST(LexicalPath::canonicalized_path(MUST(String::from_utf8("/home/user/../../not/home"sv)))), "/not/home");
    EXPECT_EQ(MUST(LexicalPath::canonicalized_path(MUST(String::from_utf8("/../../../../"sv)))), "/");
    EXPECT_EQ(MUST(LexicalPath::canonicalized_path(MUST(String::from_utf8("./../../../../"sv)))), "../../../..");
    EXPECT_EQ(MUST(LexicalPath::canonicalized_path(MUST(String::from_utf8("../../../../../"sv)))), "../../../../..");
}

TEST_CASE(the_root_path)
{
    auto path = MUST(LexicalPath::from_string("/"sv));
    EXPECT_EQ(path.dirname(), "/");
    EXPECT_EQ(path.basename(), "/");
    EXPECT_EQ(path.title(), "/");
    EXPECT_EQ(path.parts_view().size(), 0u);
}

TEST_CASE(the_dot_path)
{
    auto path = MUST(LexicalPath::from_string("."sv));
    EXPECT_EQ(path.string(), ".");
    EXPECT_EQ(path.dirname(), ".");
    EXPECT_EQ(path.basename(), ".");
    EXPECT_EQ(path.title(), ".");
}

TEST_CASE(double_slash)
{
    auto path = MUST(LexicalPath::from_string("//home/anon/foo.txt"sv));
    EXPECT_EQ(path.string(), "/home/anon/foo.txt");
}

TEST_CASE(trailing_slash)
{
    auto path = MUST(LexicalPath::from_string("/home/anon/"sv));
    EXPECT_EQ(path.string(), "/home/anon");
    EXPECT_EQ(path.dirname(), "/home");
    EXPECT_EQ(path.basename(), "anon");
    EXPECT_EQ(path.parts_view().size(), 2u);
}

TEST_CASE(resolve_absolute_path)
{
    EXPECT_EQ(MUST(LexicalPath::absolute_path("/home/anon"sv, "foo.txt"sv)), "/home/anon/foo.txt");
    EXPECT_EQ(MUST(LexicalPath::absolute_path("/home/anon/"sv, "foo.txt"sv)), "/home/anon/foo.txt");
    EXPECT_EQ(MUST(LexicalPath::absolute_path("/home/anon"sv, "././foo.txt"sv)), "/home/anon/foo.txt");
    EXPECT_EQ(MUST(LexicalPath::absolute_path("/home/anon/quux"sv, "../foo.txt"sv)), "/home/anon/foo.txt");
    EXPECT_EQ(MUST(LexicalPath::absolute_path("/home/anon/quux"sv, "../test/foo.txt"sv)), "/home/anon/test/foo.txt");
    EXPECT_EQ(MUST(LexicalPath::absolute_path("quux"sv, "../test/foo.txt"sv)), "test/foo.txt");
    EXPECT_EQ(MUST(LexicalPath::absolute_path("quux"sv, "../../test/foo.txt"sv)), "../test/foo.txt");
    EXPECT_EQ(MUST(LexicalPath::absolute_path("quux/bar"sv, "../../test/foo.txt"sv)), "test/foo.txt");
    EXPECT_EQ(MUST(LexicalPath::absolute_path("quux/bar/"sv, "../../test/foo.txt"sv)), "test/foo.txt");
}

TEST_CASE(has_extension)
{
    {
        auto path = MUST(LexicalPath::from_string("/tmp/simple.png"sv));
        EXPECT(path.has_extension(".png"sv));
        EXPECT(path.has_extension(".pnG"sv));
        EXPECT(path.has_extension(".PNG"sv));
    }

    {
        auto path = MUST(LexicalPath::from_string("/TMP/SIMPLE.PNG"sv));
        EXPECT(path.has_extension(".png"sv));
        EXPECT(path.has_extension(".pnG"sv));
        EXPECT(path.has_extension(".PNG"sv));
    }

    {
        auto path = MUST(LexicalPath::from_string(".png"sv));
        EXPECT(path.has_extension(".png"sv));
    }

    {
        auto path = MUST(LexicalPath::from_string("png"sv));
        EXPECT_EQ(path.has_extension(".png"sv), false);
    }
}

TEST_CASE(join)
{
    EXPECT_EQ(MUST(LexicalPath::join("anon"sv, "foo.txt"sv)).string(), "anon/foo.txt"sv);
    EXPECT_EQ(MUST(LexicalPath::join("/home"sv, "anon/foo.txt"sv)).string(), "/home/anon/foo.txt"sv);
    EXPECT_EQ(MUST(LexicalPath::join("/"sv, "foo.txt"sv)).string(), "/foo.txt"sv);
    EXPECT_EQ(MUST(LexicalPath::join("/home"sv, "anon"sv, "foo.txt"sv)).string(), "/home/anon/foo.txt"sv);
}

TEST_CASE(append)
{
    auto path = MUST(LexicalPath::from_string("/home/anon/"sv));
    auto new_path = MUST(path.append("foo.txt"sv));
    EXPECT_EQ(new_path.string(), "/home/anon/foo.txt");
}

TEST_CASE(parent)
{
    {
        auto path = MUST(LexicalPath::from_string("/home/anon/foo.txt"sv));
        auto parent = MUST(path.parent());
        EXPECT_EQ(parent.string(), "/home/anon");
    }
    {
        auto path = MUST(LexicalPath::from_string("anon/foo.txt"sv));
        auto parent = MUST(path.parent());
        EXPECT_EQ(parent.string(), "anon");
    }
    {
        auto path = MUST(LexicalPath::from_string("foo.txt"sv));
        auto parent = MUST(path.parent());
        EXPECT_EQ(parent.string(), ".");
        auto parent_of_parent = MUST(parent.parent());
        EXPECT_EQ(parent_of_parent.string(), "..");
    }
    {
        auto path = MUST(LexicalPath::from_string("/"sv));
        auto parent = MUST(path.parent());
        EXPECT_EQ(parent.string(), "/");
    }
}

TEST_CASE(is_child_of)
{
    {
        auto parent = MUST(LexicalPath::from_string("/a/parent/directory"sv));
        auto child = MUST(LexicalPath::from_string("/a/parent/directory/a/child"sv));
        auto mismatching = MUST(LexicalPath::from_string("/not/a/child/directory"sv));
        EXPECT(child.is_child_of(parent));
        EXPECT(child.is_child_of(child));
        EXPECT(parent.is_child_of(parent));
        EXPECT(!parent.is_child_of(child));
        EXPECT(!mismatching.is_child_of(parent));

        EXPECT(parent.is_child_of(MUST(parent.parent())));
        EXPECT(MUST(MUST(child.parent()).parent()).is_child_of(parent));
        EXPECT(!MUST(MUST(MUST(child.parent()).parent()).parent()).is_child_of(parent));
    }
    {
        auto root = MUST(LexicalPath::from_string("/"sv));
        EXPECT(MUST(LexicalPath::from_string("/"sv)).is_child_of(root));
        EXPECT(MUST(LexicalPath::from_string("/any"sv)).is_child_of(root));
        EXPECT(MUST(LexicalPath::from_string("/child/directory"sv)).is_child_of(root));
    }
    {
        auto relative = MUST(LexicalPath::from_string("folder"sv));
        auto relative_child = MUST(LexicalPath::from_string("folder/sub"sv));
        auto absolute = MUST(LexicalPath::from_string("/folder"sv));
        auto absolute_child = MUST(LexicalPath::from_string("/folder/sub"sv));
        EXPECT(relative_child.is_child_of(relative));
        EXPECT(absolute_child.is_child_of(absolute));

        EXPECT(relative.is_child_of(absolute));
        EXPECT(relative.is_child_of(absolute_child));
        EXPECT(relative_child.is_child_of(absolute));
        EXPECT(relative_child.is_child_of(absolute_child));

        EXPECT(!absolute.is_child_of(relative));
        EXPECT(!absolute_child.is_child_of(relative));
        EXPECT(!absolute_child.is_child_of(relative_child));
    }
}

#include <AK/TestSuite.h>

#include <AK/String.h>
#include <AK/FileSystemPath.h>

TEST_CASE(construct)
{
    EXPECT_EQ(FileSystemPath().is_valid(), false);
}

TEST_CASE(basic)
{
    FileSystemPath path("/abc/def/ghi.txt");
    EXPECT_EQ(path.is_valid(), true);
    EXPECT_EQ(path.basename(), "ghi.txt");
    EXPECT_EQ(path.title(), "ghi");
    EXPECT_EQ(path.extension(), "txt");
    EXPECT_EQ(path.parts().size(), 3);
    EXPECT_EQ(path.parts(), Vector<String>({ "abc", "def", "ghi.txt" }));
    EXPECT_EQ(path.string(), "/abc/def/ghi.txt");
}

TEST_CASE(dotdot_coalescing)
{
    EXPECT_EQ(FileSystemPath("/home/user/../../not/home").string(), "/not/home");
    EXPECT_EQ(FileSystemPath("/../../../../").string(), "/");
}

TEST_CASE(relative_paths)
{
    {
        FileSystemPath path("simple");
        EXPECT_EQ(path.is_valid(), true);
        EXPECT_EQ(path.string(), "./simple");
        EXPECT_EQ(path.parts().size(), 2);
        EXPECT_EQ(path.basename(), "simple");
    }
    {
        FileSystemPath path("a/relative/path");
        EXPECT_EQ(path.is_valid(), true);
        EXPECT_EQ(path.string(), "./a/relative/path");
        EXPECT_EQ(path.parts().size(), 4);
        EXPECT_EQ(path.basename(), "path");
    }
    {
        FileSystemPath path("./././foo");
        EXPECT_EQ(path.is_valid(), true);
        EXPECT_EQ(path.string(), "./foo");
        EXPECT_EQ(path.parts().size(), 2);
        EXPECT_EQ(path.basename(), "foo");
    }

    {
        FileSystemPath path(".");
        EXPECT_EQ(path.is_valid(), true);
        EXPECT_EQ(path.string(), ".");
        EXPECT_EQ(path.parts().size(), 1);
        EXPECT_EQ(path.basename(), ".");
    }
}

TEST_MAIN(FileSystemPath)

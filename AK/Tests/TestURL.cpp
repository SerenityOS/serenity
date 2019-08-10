#include <AK/TestSuite.h>

#include <AK/URL.h>

TEST_CASE(construct)
{
    EXPECT_EQ(URL().is_valid(), false);
}

TEST_CASE(basic)
{
    {
        URL url("http://www.serenityos.org/index.html");
        EXPECT_EQ(url.is_valid(), true);
        EXPECT_EQ(url.protocol(), "http");
        EXPECT_EQ(url.port(), 80);
        EXPECT_EQ(url.path(), "/index.html");
    }
    {
        URL url("https://localhost:1234/~anon/test/page.html");
        EXPECT_EQ(url.is_valid(), true);
        EXPECT_EQ(url.protocol(), "https");
        EXPECT_EQ(url.port(), 1234);
        EXPECT_EQ(url.path(), "/~anon/test/page.html");
    }
}

TEST_CASE(some_bad_urls)
{
    EXPECT_EQ(URL("http:serenityos.org").is_valid(), false);
    EXPECT_EQ(URL("http:/serenityos.org").is_valid(), false);
    EXPECT_EQ(URL("http//serenityos.org").is_valid(), false);
    EXPECT_EQ(URL("http:///serenityos.org").is_valid(), false);
    EXPECT_EQ(URL("serenityos.org").is_valid(), false);
    EXPECT_EQ(URL("://serenityos.org").is_valid(), false);
    EXPECT_EQ(URL("http://serenityos.org:80:80/").is_valid(), false);
    EXPECT_EQ(URL("http://serenityos.org:80:80").is_valid(), false);
    EXPECT_EQ(URL("http://serenityos.org:abc").is_valid(), false);
    EXPECT_EQ(URL("http://serenityos.org:abc:80").is_valid(), false);
    EXPECT_EQ(URL("http://serenityos.org:abc:80/").is_valid(), false);
    EXPECT_EQ(URL("http://serenityos.org:/abc/").is_valid(), false);
}

TEST_CASE(serialization)
{
    EXPECT_EQ(URL("http://www.serenityos.org/").to_string(), "http://www.serenityos.org:80/");
}

TEST_MAIN(URL)

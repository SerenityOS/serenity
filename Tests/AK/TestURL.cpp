/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/Base64.h>
#include <AK/URL.h>
#include <AK/URLParser.h>

TEST_CASE(construct)
{
    EXPECT_EQ(URL().is_valid(), false);
}

TEST_CASE(basic)
{
    {
        URL url("http://www.serenityos.org"sv);
        EXPECT_EQ(url.is_valid(), true);
        EXPECT_EQ(url.scheme(), "http");
        EXPECT_EQ(url.host(), "www.serenityos.org");
        EXPECT_EQ(url.port_or_default(), 80);
        EXPECT_EQ(url.path(), "/");
        EXPECT(url.query().is_null());
        EXPECT(url.fragment().is_null());
    }
    {
        URL url("https://www.serenityos.org/index.html"sv);
        EXPECT_EQ(url.is_valid(), true);
        EXPECT_EQ(url.scheme(), "https");
        EXPECT_EQ(url.host(), "www.serenityos.org");
        EXPECT_EQ(url.port_or_default(), 443);
        EXPECT_EQ(url.path(), "/index.html");
        EXPECT(url.query().is_null());
        EXPECT(url.fragment().is_null());
    }
    {
        URL url("https://localhost:1234/~anon/test/page.html"sv);
        EXPECT_EQ(url.is_valid(), true);
        EXPECT_EQ(url.scheme(), "https");
        EXPECT_EQ(url.host(), "localhost");
        EXPECT_EQ(url.port_or_default(), 1234);
        EXPECT_EQ(url.path(), "/~anon/test/page.html");
        EXPECT(url.query().is_null());
        EXPECT(url.fragment().is_null());
    }
    {
        URL url("http://www.serenityos.org/index.html?#"sv);
        EXPECT_EQ(url.is_valid(), true);
        EXPECT_EQ(url.scheme(), "http");
        EXPECT_EQ(url.host(), "www.serenityos.org");
        EXPECT_EQ(url.port_or_default(), 80);
        EXPECT_EQ(url.path(), "/index.html");
        EXPECT_EQ(url.query(), "");
        EXPECT_EQ(url.fragment(), "");
    }
    {
        URL url("http://www.serenityos.org/index.html?foo=1&bar=2"sv);
        EXPECT_EQ(url.is_valid(), true);
        EXPECT_EQ(url.scheme(), "http");
        EXPECT_EQ(url.host(), "www.serenityos.org");
        EXPECT_EQ(url.port_or_default(), 80);
        EXPECT_EQ(url.path(), "/index.html");
        EXPECT_EQ(url.query(), "foo=1&bar=2");
        EXPECT(url.fragment().is_null());
    }
    {
        URL url("http://www.serenityos.org/index.html#fragment"sv);
        EXPECT_EQ(url.is_valid(), true);
        EXPECT_EQ(url.scheme(), "http");
        EXPECT_EQ(url.host(), "www.serenityos.org");
        EXPECT_EQ(url.port_or_default(), 80);
        EXPECT_EQ(url.path(), "/index.html");
        EXPECT(url.query().is_null());
        EXPECT_EQ(url.fragment(), "fragment");
    }
    {
        URL url("http://www.serenityos.org/index.html?foo=1&bar=2&baz=/?#frag/ment?test#"sv);
        EXPECT_EQ(url.is_valid(), true);
        EXPECT_EQ(url.scheme(), "http");
        EXPECT_EQ(url.host(), "www.serenityos.org");
        EXPECT_EQ(url.port_or_default(), 80);
        EXPECT_EQ(url.path(), "/index.html");
        EXPECT_EQ(url.query(), "foo=1&bar=2&baz=/?");
        EXPECT_EQ(url.fragment(), "frag/ment?test#");
    }
}

TEST_CASE(some_bad_urls)
{
    EXPECT_EQ(URL("http//serenityos.org"sv).is_valid(), false);
    EXPECT_EQ(URL("serenityos.org"sv).is_valid(), false);
    EXPECT_EQ(URL("://serenityos.org"sv).is_valid(), false);
    EXPECT_EQ(URL("://:80"sv).is_valid(), false);
    EXPECT_EQ(URL("http://serenityos.org:80:80/"sv).is_valid(), false);
    EXPECT_EQ(URL("http://serenityos.org:80:80"sv).is_valid(), false);
    EXPECT_EQ(URL("http://serenityos.org:abc"sv).is_valid(), false);
    EXPECT_EQ(URL("http://serenityos.org:abc:80"sv).is_valid(), false);
    EXPECT_EQ(URL("http://serenityos.org:abc:80/"sv).is_valid(), false);
    EXPECT_EQ(URL("data:"sv).is_valid(), false);
}

TEST_CASE(serialization)
{
    EXPECT_EQ(URL("http://www.serenityos.org/"sv).serialize(), "http://www.serenityos.org/");
    EXPECT_EQ(URL("http://www.serenityos.org:0/"sv).serialize(), "http://www.serenityos.org:0/");
    EXPECT_EQ(URL("http://www.serenityos.org:80/"sv).serialize(), "http://www.serenityos.org/");
    EXPECT_EQ(URL("http://www.serenityos.org:81/"sv).serialize(), "http://www.serenityos.org:81/");
    EXPECT_EQ(URL("https://www.serenityos.org:443/foo/bar.html?query#fragment"sv).serialize(), "https://www.serenityos.org/foo/bar.html?query#fragment");
}

TEST_CASE(file_url_with_hostname)
{
    URL url("file://courage/my/file"sv);
    EXPECT(url.is_valid());
    EXPECT_EQ(url.scheme(), "file");
    EXPECT_EQ(url.host(), "courage");
    EXPECT_EQ(url.port_or_default(), 0);
    EXPECT_EQ(url.path(), "/my/file");
    EXPECT_EQ(url.serialize(), "file://courage/my/file");
    EXPECT(url.query().is_null());
    EXPECT(url.fragment().is_null());
}

TEST_CASE(file_url_with_localhost)
{
    URL url("file://localhost/my/file"sv);
    EXPECT(url.is_valid());
    EXPECT_EQ(url.scheme(), "file");
    EXPECT_EQ(url.host(), "");
    EXPECT_EQ(url.path(), "/my/file");
    EXPECT_EQ(url.serialize(), "file:///my/file");
}

TEST_CASE(file_url_without_hostname)
{
    URL url("file:///my/file"sv);
    EXPECT(url.is_valid());
    EXPECT_EQ(url.scheme(), "file");
    EXPECT_EQ(url.host(), "");
    EXPECT_EQ(url.path(), "/my/file");
    EXPECT_EQ(url.serialize(), "file:///my/file");
}

TEST_CASE(file_url_with_encoded_characters)
{
    URL url("file:///my/file/test%23file.txt"sv);
    EXPECT(url.is_valid());
    EXPECT_EQ(url.scheme(), "file");
    EXPECT_EQ(url.path(), "/my/file/test#file.txt");
    EXPECT(url.query().is_null());
    EXPECT(url.fragment().is_null());
}

TEST_CASE(file_url_with_fragment)
{
    URL url("file:///my/file#fragment"sv);
    EXPECT(url.is_valid());
    EXPECT_EQ(url.scheme(), "file");
    EXPECT_EQ(url.path(), "/my/file");
    EXPECT(url.query().is_null());
    EXPECT_EQ(url.fragment(), "fragment");
}

TEST_CASE(file_url_with_root_path)
{
    URL url("file:///"sv);
    EXPECT(url.is_valid());
    EXPECT_EQ(url.scheme(), "file");
    EXPECT_EQ(url.path(), "/");
}

TEST_CASE(file_url_serialization)
{
    EXPECT_EQ(URL("file://courage/my/file"sv).serialize(), "file://courage/my/file");
    EXPECT_EQ(URL("file://localhost/my/file"sv).serialize(), "file:///my/file");
    EXPECT_EQ(URL("file:///my/file"sv).serialize(), "file:///my/file");
    EXPECT_EQ(URL("file:///my/directory/"sv).serialize(), "file:///my/directory/");
    EXPECT_EQ(URL("file:///my/file%23test"sv).serialize(), "file:///my/file%23test");
    EXPECT_EQ(URL("file:///my/file#fragment"sv).serialize(), "file:///my/file#fragment");
}

TEST_CASE(about_url)
{
    URL url("about:blank"sv);
    EXPECT(url.is_valid());
    EXPECT_EQ(url.scheme(), "about");
    EXPECT(url.host().is_null());
    EXPECT_EQ(url.path(), "blank");
    EXPECT(url.query().is_null());
    EXPECT(url.fragment().is_null());
    EXPECT_EQ(url.serialize(), "about:blank");
}

TEST_CASE(mailto_url)
{
    URL url("mailto:mail@example.com"sv);
    EXPECT(url.is_valid());
    EXPECT_EQ(url.scheme(), "mailto");
    EXPECT(url.host().is_null());
    EXPECT_EQ(url.port_or_default(), 0);
    EXPECT_EQ(url.paths().size(), 1u);
    EXPECT_EQ(url.paths()[0], "mail@example.com");
    EXPECT(url.query().is_null());
    EXPECT(url.fragment().is_null());
    EXPECT_EQ(url.serialize(), "mailto:mail@example.com");
}

TEST_CASE(data_url)
{
    URL url("data:text/html,test"sv);
    EXPECT(url.is_valid());
    EXPECT_EQ(url.scheme(), "data");
    EXPECT(url.host().is_null());
    EXPECT_EQ(url.data_mime_type(), "text/html");
    EXPECT_EQ(url.data_payload(), "test");
    EXPECT(!url.data_payload_is_base64());
    EXPECT_EQ(url.serialize(), "data:text/html,test");
}

TEST_CASE(data_url_default_mime_type)
{
    URL url("data:,test"sv);
    EXPECT(url.is_valid());
    EXPECT_EQ(url.scheme(), "data");
    EXPECT(url.host().is_null());
    EXPECT_EQ(url.data_mime_type(), "text/plain");
    EXPECT_EQ(url.data_payload(), "test");
    EXPECT(!url.data_payload_is_base64());
    EXPECT_EQ(url.serialize(), "data:text/plain,test");
}

TEST_CASE(data_url_encoded)
{
    URL url("data:text/html,Hello%20friends%2C%0X%X0"sv);
    EXPECT(url.is_valid());
    EXPECT_EQ(url.scheme(), "data");
    EXPECT(url.host().is_null());
    EXPECT_EQ(url.data_mime_type(), "text/html");
    EXPECT_EQ(url.data_payload(), "Hello friends,%0X%X0");
    EXPECT(!url.data_payload_is_base64());
    EXPECT_EQ(url.serialize(), "data:text/html,Hello friends,%0X%X0");
}

TEST_CASE(data_url_base64_encoded)
{
    URL url("data:text/html;base64,test"sv);
    EXPECT(url.is_valid());
    EXPECT_EQ(url.scheme(), "data");
    EXPECT(url.host().is_null());
    EXPECT_EQ(url.data_mime_type(), "text/html");
    EXPECT_EQ(url.data_payload(), "test");
    EXPECT(url.data_payload_is_base64());
    EXPECT_EQ(url.serialize(), "data:text/html;base64,test");
}

TEST_CASE(data_url_base64_encoded_default_mime_type)
{
    URL url("data:;base64,test"sv);
    EXPECT(url.is_valid());
    EXPECT_EQ(url.scheme(), "data");
    EXPECT(url.host().is_null());
    EXPECT_EQ(url.data_mime_type(), "text/plain");
    EXPECT_EQ(url.data_payload(), "test");
    EXPECT(url.data_payload_is_base64());
    EXPECT_EQ(url.serialize(), "data:text/plain;base64,test");
}

TEST_CASE(data_url_base64_encoded_with_whitespace)
{
    URL url("data: text/html ;     bAsE64 , test with whitespace "sv);
    EXPECT(url.is_valid());
    EXPECT_EQ(url.scheme(), "data");
    EXPECT(url.host().is_null());
    EXPECT_EQ(url.data_mime_type(), "text/html");
    EXPECT_EQ(url.data_payload(), " test with whitespace ");
    EXPECT(url.data_payload_is_base64());
    EXPECT_EQ(url.serialize(), "data:text/html;base64, test with whitespace ");
}

TEST_CASE(data_url_base64_encoded_with_inline_whitespace)
{
    URL url("data:text/javascript;base64,%20ZD%20Qg%0D%0APS%20An%20Zm91cic%0D%0A%207%20"sv);
    EXPECT(url.is_valid());
    EXPECT_EQ(url.scheme(), "data");
    EXPECT(url.host().is_null());
    EXPECT_EQ(url.data_mime_type(), "text/javascript");
    EXPECT(url.data_payload_is_base64());
    EXPECT_EQ(url.data_payload(), " ZD Qg\r\nPS An Zm91cic\r\n 7 "sv);
    auto decode_result = decode_base64(url.data_payload());
    EXPECT_EQ(decode_result.is_error(), false);
    EXPECT_EQ(StringView(decode_result.value()), "d4 = 'four';"sv);
}

TEST_CASE(trailing_slash_with_complete_url)
{
    EXPECT_EQ(URL("http://a/b/"sv).complete_url("c/"sv).serialize(), "http://a/b/c/");
    EXPECT_EQ(URL("http://a/b/"sv).complete_url("c"sv).serialize(), "http://a/b/c");
    EXPECT_EQ(URL("http://a/b"sv).complete_url("c/"sv).serialize(), "http://a/c/");
    EXPECT_EQ(URL("http://a/b"sv).complete_url("c"sv).serialize(), "http://a/c");
}

TEST_CASE(trailing_port)
{
    URL url("http://example.com:8086"sv);
    EXPECT_EQ(url.port_or_default(), 8086);
}

TEST_CASE(port_overflow)
{
    EXPECT_EQ(URL("http://example.com:123456789/"sv).is_valid(), false);
}

TEST_CASE(equality)
{
    EXPECT(URL("http://serenityos.org"sv).equals("http://serenityos.org#test"sv, URL::ExcludeFragment::Yes));
    EXPECT_EQ(URL("http://example.com/index.html"sv), URL("http://ex%61mple.com/index.html"sv));
    EXPECT_EQ(URL("file:///my/file"sv), URL("file://localhost/my/file"sv));
    EXPECT_NE(URL("http://serenityos.org/index.html"sv), URL("http://serenityos.org/test.html"sv));
}

TEST_CASE(create_with_file_scheme)
{
    auto url = URL::create_with_file_scheme("/home/anon/README.md");
    EXPECT(url.is_valid());
    EXPECT_EQ(url.scheme(), "file");
    EXPECT_EQ(url.port_or_default(), 0);
    EXPECT_EQ(url.paths().size(), 3u);
    EXPECT_EQ(url.paths()[0], "home");
    EXPECT_EQ(url.paths()[1], "anon");
    EXPECT_EQ(url.paths()[2], "README.md");
    EXPECT_EQ(url.path(), "/home/anon/README.md");
    EXPECT(url.query().is_null());
    EXPECT(url.fragment().is_null());

    url = URL::create_with_file_scheme("/home/anon/");
    EXPECT(url.is_valid());
    EXPECT_EQ(url.paths().size(), 3u);
    EXPECT_EQ(url.paths()[0], "home");
    EXPECT_EQ(url.paths()[1], "anon");
    EXPECT_EQ(url.paths()[2], "");
    EXPECT_EQ(url.path(), "/home/anon/");

    url = URL("file:///home/anon/"sv);
    EXPECT_EQ(url.path(), "/home/anon/");
}

TEST_CASE(complete_url)
{
    URL base_url("http://serenityos.org/index.html#fragment"sv);
    URL url = base_url.complete_url("test.html"sv);
    EXPECT(url.is_valid());
    EXPECT_EQ(url.scheme(), "http");
    EXPECT_EQ(url.host(), "serenityos.org");
    EXPECT_EQ(url.path(), "/test.html");
    EXPECT(url.query().is_null());
    EXPECT(url.query().is_null());
    EXPECT_EQ(url.cannot_be_a_base_url(), false);

    EXPECT(base_url.complete_url("../index.html#fragment"sv).equals(base_url));
}

TEST_CASE(leading_whitespace)
{
    URL url { "   https://foo.com/"sv };
    EXPECT(url.is_valid());
    EXPECT_EQ(url.to_deprecated_string(), "https://foo.com/");
}

TEST_CASE(trailing_whitespace)
{
    URL url { "https://foo.com/   "sv };
    EXPECT(url.is_valid());
    EXPECT_EQ(url.to_deprecated_string(), "https://foo.com/");
}

TEST_CASE(leading_and_trailing_whitespace)
{
    URL url { "      https://foo.com/   "sv };
    EXPECT(url.is_valid());
    EXPECT_EQ(url.to_deprecated_string(), "https://foo.com/");
}

TEST_CASE(unicode)
{
    URL url { "http://example.com/_ünicöde_téxt_©"sv };
    EXPECT(url.is_valid());
    EXPECT_EQ(url.path(), "/_ünicöde_téxt_©");
    EXPECT(url.query().is_null());
    EXPECT(url.fragment().is_null());
}

TEST_CASE(complete_file_url_with_base)
{
    URL url { "file:///home/index.html" };
    EXPECT(url.is_valid());
    EXPECT_EQ(url.path(), "/home/index.html");
    EXPECT_EQ(url.paths().size(), 2u);
    EXPECT_EQ(url.paths()[0], "home");
    EXPECT_EQ(url.paths()[1], "index.html");

    auto sub_url = url.complete_url("js/app.js"sv);
    EXPECT(sub_url.is_valid());
    EXPECT_EQ(sub_url.path(), "/home/js/app.js");
}

TEST_CASE(empty_url_with_base_url)
{
    URL base_url { "https://foo.com/"sv };
    URL parsed_url = URLParser::parse(""sv, &base_url);
    EXPECT_EQ(parsed_url.is_valid(), true);
    EXPECT(base_url.equals(parsed_url));
}

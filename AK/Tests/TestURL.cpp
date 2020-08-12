/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020, Benoit Lormeau <blormeau@outlook.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/TestSuite.h>

#include <AK/URL.h>

TEST_CASE(construct)
{
    EXPECT_EQ(URL().is_valid(), false);
}

TEST_CASE(basic)
{
    {
        URL url("http://www.serenityos.org");
        EXPECT(url.is_valid());
        EXPECT_EQ(url.scheme(), "http");
        EXPECT_EQ(url.host(), "www.serenityos.org");
        EXPECT_EQ(url.port(), 80);
        EXPECT_EQ(url.path(), "/");
        EXPECT(url.query().is_empty());
        EXPECT(url.fragment().is_empty());
    }
    {
        URL url("https://www.serenityos.org/index.html");
        EXPECT(url.is_valid());
        EXPECT_EQ(url.scheme(), "https");
        EXPECT_EQ(url.host(), "www.serenityos.org");
        EXPECT_EQ(url.port(), 443);
        EXPECT_EQ(url.path(), "/index.html");
        EXPECT(url.query().is_empty());
        EXPECT(url.fragment().is_empty());
    }
    {
        URL url("https://localhost:1234/~anon/test/page.html");
        EXPECT(url.is_valid());
        EXPECT_EQ(url.scheme(), "https");
        EXPECT_EQ(url.host(), "localhost");
        EXPECT_EQ(url.port(), 1234);
        EXPECT_EQ(url.path(), "/~anon/test/page.html");
        EXPECT(url.query().is_empty());
        EXPECT(url.fragment().is_empty());
    }
    {
        URL url("http://www.serenityos.org/index.html?#");
        EXPECT(url.is_valid());
        EXPECT_EQ(url.scheme(), "http");
        EXPECT_EQ(url.host(), "www.serenityos.org");
        EXPECT_EQ(url.port(), 80);
        EXPECT_EQ(url.path(), "/index.html");
        EXPECT(url.query().is_empty());
        EXPECT(url.fragment().is_empty());
    }
    {
        URL url("http://www.serenityos.org/index.html?foo=1&bar=2");
        EXPECT(url.is_valid());
        EXPECT_EQ(url.scheme(), "http");
        EXPECT_EQ(url.host(), "www.serenityos.org");
        EXPECT_EQ(url.port(), 80);
        EXPECT_EQ(url.path(), "/index.html");
        EXPECT_EQ(url.query(), "foo=1&bar=2");
        EXPECT(url.fragment().is_empty());

        auto query_fields = url.parse_query_fields();
        EXPECT(query_fields.contains("foo"));
        EXPECT(query_fields.contains("bar"));
        EXPECT_EQ(query_fields.get("foo").value(), "1");
        EXPECT_EQ(query_fields.get("bar").value(), "2");
    }
    {
        URL url("http://www.serenityos.org/index.html#fragment");
        EXPECT(url.is_valid());
        EXPECT_EQ(url.scheme(), "http");
        EXPECT_EQ(url.host(), "www.serenityos.org");
        EXPECT_EQ(url.port(), 80);
        EXPECT_EQ(url.path(), "/index.html");
        EXPECT(url.query().is_empty());
        EXPECT_EQ(url.fragment(), "fragment");
    }
    {
        URL url("http://www.serenityos.org/index.html?foo=1&bar&baz=/?#frag/ment?test");
        EXPECT(url.is_valid());
        EXPECT_EQ(url.scheme(), "http");
        EXPECT_EQ(url.host(), "www.serenityos.org");
        EXPECT_EQ(url.port(), 80);
        EXPECT_EQ(url.path(), "/index.html");
        EXPECT_EQ(url.query(), "foo=1&bar&baz=/?");
        EXPECT_EQ(url.fragment(), "frag/ment?test");

        auto query_fields = url.parse_query_fields();
        EXPECT(query_fields.contains("foo"));
        EXPECT(query_fields.contains("bar"));
        EXPECT(query_fields.contains("baz"));
        EXPECT_EQ(query_fields.get("foo").value(), "1");
        EXPECT(query_fields.get("bar").value().is_empty());
        EXPECT_EQ(query_fields.get("baz").value(), "/?");
    }
}

TEST_CASE(advanced)
{
    {
        URL url("http://www.ietf.org/rfc/rfc2396.txt");
        EXPECT(url.is_valid());
        EXPECT_EQ(url.scheme(), "http");
        EXPECT(url.username().is_empty());
        EXPECT(url.password().is_empty());
        EXPECT_EQ(url.host(), "www.ietf.org");
        EXPECT_EQ(url.port(), 80);
        EXPECT_EQ(url.path(), "/rfc/rfc2396.txt");
        EXPECT(url.query().is_empty());
        EXPECT(url.fragment().is_empty());
    }
    {
        URL url("mailto:John.Doe@example.com");
        EXPECT(url.is_valid());
        EXPECT_EQ(url.scheme(), "mailto");
        EXPECT(url.username().is_empty());
        EXPECT(url.password().is_empty());
        EXPECT(url.host().is_empty());
        EXPECT_EQ(url.port(), 0);
        EXPECT_EQ(url.path(), "John.Doe@example.com");
        EXPECT(url.query().is_empty());
        EXPECT(url.fragment().is_empty());
    }
    {
        URL url("news:comp.infosystems.www.servers.unix");
        EXPECT(url.is_valid());
        EXPECT_EQ(url.scheme(), "news");
        EXPECT(url.username().is_empty());
        EXPECT(url.password().is_empty());
        EXPECT(url.host().is_empty());
        EXPECT_EQ(url.port(), 0);
        EXPECT_EQ(url.path(), "comp.infosystems.www.servers.unix");
        EXPECT(url.query().is_empty());
        EXPECT(url.fragment().is_empty());
    }
    {
        URL url("tel:+1-816-555-1212");
        EXPECT(url.is_valid());
        EXPECT_EQ(url.scheme(), "tel");
        EXPECT(url.username().is_empty());
        EXPECT(url.password().is_empty());
        EXPECT(url.host().is_empty());
        EXPECT_EQ(url.port(), 0);
        EXPECT_EQ(url.path(), "+1-816-555-1212");
        EXPECT(url.query().is_empty());
        EXPECT(url.fragment().is_empty());
    }
    {
        URL url("telnet://192.0.2.16:80/");
        EXPECT(url.is_valid());
        EXPECT_EQ(url.scheme(), "telnet");
        EXPECT(url.username().is_empty());
        EXPECT(url.password().is_empty());
        EXPECT_EQ(url.host(), "192.0.2.16");
        EXPECT_EQ(url.port(), 80);
        EXPECT_EQ(url.path(), "/");
        EXPECT(url.query().is_empty());
        EXPECT(url.fragment().is_empty());
    }
    {
        URL url("urn:oasis:names:specification:docbook:dtd:xml:4.1.2");
        EXPECT(url.is_valid());
        EXPECT_EQ(url.scheme(), "urn");
        EXPECT(url.username().is_empty());
        EXPECT(url.password().is_empty());
        EXPECT(url.host().is_empty());
        EXPECT_EQ(url.port(), 0);
        EXPECT_EQ(url.path(), "oasis:names:specification:docbook:dtd:xml:4.1.2");
        EXPECT(url.query().is_empty());
        EXPECT(url.fragment().is_empty());
    }
    {
        URL url("https://[2001:db8:85a3:8d3:1319:8a2e:370:7348]/00/Weather/Los%20Angeles");
        EXPECT(url.is_valid());
        EXPECT_EQ(url.scheme(), "https");
        EXPECT(url.username().is_empty());
        EXPECT(url.password().is_empty());
        EXPECT_EQ(url.host(), "2001:db8:85a3:8d3:1319:8a2e:370:7348");
        EXPECT_EQ(url.port(), 443);
        EXPECT_EQ(url.path(), "/00/Weather/Los Angeles");
        EXPECT(url.query().is_empty());
        EXPECT(url.fragment().is_empty());
    }
    {
        URL url("ssh://user@example.com");
        EXPECT(url.is_valid());
        EXPECT_EQ(url.scheme(), "ssh");
        EXPECT_EQ(url.username(), "user");
        EXPECT(url.password().is_empty());
        EXPECT_EQ(url.host(), "example.com");
        EXPECT_EQ(url.port(), 22);
        EXPECT_EQ(url.path(), "/");
        EXPECT(url.query().is_empty());
        EXPECT(url.fragment().is_empty());
    }
    {
        URL url("http://resU:raBBit@www.example.com:8888/access/path.php?q=req&q2=req2");
        EXPECT(url.is_valid());
        EXPECT_EQ(url.scheme(), "http");
        EXPECT_EQ(url.username(), "resU");
        EXPECT_EQ(url.password(), "raBBit");
        EXPECT_EQ(url.host(), "www.example.com");
        EXPECT_EQ(url.port(), 8888);
        EXPECT_EQ(url.path(), "/access/path.php");
        EXPECT_EQ(url.query(), "q=req&q2=req2");
        EXPECT(url.fragment().is_empty());

        auto query_fields = url.parse_query_fields();
        EXPECT(query_fields.contains("q"));
        EXPECT(query_fields.contains("q2"));
        EXPECT_EQ(query_fields.get("q").value(), "req");
        EXPECT_EQ(query_fields.get("q2").value(), "req2");
    }
    {
        URL url("https://john.doe@www.example.com:123?tag=networking&order=newest#top");
        EXPECT(url.is_valid());
        EXPECT_EQ(url.scheme(), "https");
        EXPECT_EQ(url.username(), "john.doe");
        EXPECT(url.password().is_empty());
        EXPECT_EQ(url.host(), "www.example.com");
        EXPECT_EQ(url.port(), 123);
        EXPECT_EQ(url.path(), "/");
        EXPECT_EQ(url.query(), "tag=networking&order=newest");
        EXPECT_EQ(url.fragment(), "top");

        auto query_fields = url.parse_query_fields();
        EXPECT(query_fields.contains("tag"));
        EXPECT(query_fields.contains("order"));
        EXPECT_EQ(query_fields.get("tag").value(), "networking");
        EXPECT_EQ(query_fields.get("order").value(), "newest");
    }
}

TEST_CASE(some_bad_urls)
{
    EXPECT(!URL("http:serenityos.org").is_valid());
    EXPECT(!URL("http:/serenityos.org").is_valid());
    EXPECT(!URL("http//serenityos.org").is_valid());
    EXPECT(!URL("http:///serenityos.org").is_valid());
    EXPECT(!URL("serenityos.org").is_valid());
    EXPECT(!URL("://serenityos.org").is_valid());
    EXPECT(!URL("http://serenityos.org:80:80/").is_valid());
    EXPECT(!URL("http://serenityos.org:80:80").is_valid());
    EXPECT(!URL("http://serenityos.org:abc").is_valid());
    EXPECT(!URL("http://serenityos.org:abc:80").is_valid());
    EXPECT(!URL("http://serenityos.org:abc:80/").is_valid());
    EXPECT(!URL("http://serenityos.org?name=${name}").is_valid());
    EXPECT(!URL("http://serenityos.org#frag`ment").is_valid());
}

TEST_CASE(serialization)
{
    EXPECT_EQ(URL("http://www.serenityos.org/").to_string(), "http://www.serenityos.org/");
    EXPECT_EQ(URL("http://www.serenityos.org:81/").to_string(), "http://www.serenityos.org:81/");
    EXPECT_EQ(URL("https://www.serenityos.org:443/foo/bar.html?query#fragment").to_string(), "https://www.serenityos.org/foo/bar.html?query#fragment");
    EXPECT_EQ(URL("ssh://user@top-secret.net/").to_string(), "ssh://user@top-secret.net/");
    EXPECT_EQ(URL("ssh://user:@top-secret.net/").to_string(), "ssh://user:@top-secret.net/");
    EXPECT_EQ(URL("ssh://user:pass%25ord@top-secret.net/").to_string(), "ssh://user:pass%25ord@top-secret.net/");
}

TEST_CASE(file_url_with_hostname)
{
    URL url("file://localhost/my/file");
    EXPECT(url.is_valid());
    EXPECT_EQ(url.host(), "localhost");
    EXPECT_EQ(url.path(), "/my/file");
    EXPECT_EQ(url.to_string(), "file://localhost/my/file");
}

TEST_CASE(file_url_without_hostname)
{
    URL url("file:///my/file");
    EXPECT(url.is_valid());
    EXPECT(url.host().is_empty());
    EXPECT_EQ(url.path(), "/my/file");
    EXPECT_EQ(url.to_string(), "file:///my/file");
}

TEST_CASE(about_url)
{
    {
        URL url("about:");
        EXPECT(url.is_valid());
        EXPECT_EQ(url.scheme(), "about");
    }
    {
        URL url("about:blank");
        EXPECT(url.is_valid());
        EXPECT_EQ(url.scheme(), "about");
        EXPECT_EQ(url.path(), "blank");
        EXPECT_EQ(url.to_string(), "about:blank");
    }
}

TEST_CASE(data_url)
{
    {
        URL url("data:");
        EXPECT(!url.is_valid());
        EXPECT_EQ(url.scheme(), "data");
    }
    {
        URL url("data:,");
        EXPECT(url.is_valid());
        EXPECT_EQ(url.scheme(), "data");
        EXPECT_EQ(url.payload().mime_type(), "text/plain");
        EXPECT(url.payload().data().is_empty());
        EXPECT_EQ(url.payload().encoding(), URL::Payload::Encoding::UrlEncoded);
        EXPECT_EQ(url.to_string(), "data:text/plain,");
    }
    {
        URL url("data:,Hello%2C%20Friends%21");
        EXPECT(url.is_valid());
        EXPECT_EQ(url.scheme(), "data");
        EXPECT_EQ(url.payload().mime_type(), "text/plain");
        EXPECT_EQ(memcmp(url.payload().data().data(), "Hello, Friends!", 15), 0);
        EXPECT_EQ(url.payload().encoding(), URL::Payload::Encoding::UrlEncoded);
        EXPECT_EQ(url.to_string(), "data:text/plain,Hello%2C%20Friends%21");
    }
    {
        URL url("data:image/png;base64,SGVsbG8sIEZyaWVuZHM=");
        EXPECT(url.is_valid());
        EXPECT_EQ(url.scheme(), "data");
        EXPECT_EQ(url.payload().mime_type(), "image/png");
        EXPECT_EQ(memcmp(url.payload().data().data(), "Hello, Friends", 14), 0);
        EXPECT_EQ(url.payload().encoding(), URL::Payload::Encoding::Base64);
        EXPECT_EQ(url.to_string(), "data:image/png;base64,SGVsbG8sIEZyaWVuZHM=");
    }
    {
        URL url("data:text/html,%3Ch1%3EHello%2C%20World%3C%2Fh1%3E");
        EXPECT(url.is_valid());
        EXPECT_EQ(url.scheme(), "data");
        EXPECT_EQ(url.payload().mime_type(), "text/html");
        EXPECT_EQ(memcmp(url.payload().data().data(), "<h1>Hello, World</h1>", 21), 0);
        EXPECT_EQ(url.payload().encoding(), URL::Payload::Encoding::UrlEncoded);
        EXPECT_EQ(url.to_string(), "data:text/html,%3Ch1%3EHello%2C%20World%3C%2Fh1%3E");
    }
}

TEST_CASE(trailing_slash_with_complete_url)
{
    EXPECT_EQ(URL("http://a/b/").complete_url("c/").to_string(), "http://a/b/c/");
    EXPECT_EQ(URL("http://a/b/").complete_url("c").to_string(), "http://a/b/c");
    EXPECT_EQ(URL("http://a/b").complete_url("c/").to_string(), "http://a/c/");
    EXPECT_EQ(URL("http://a/b").complete_url("c").to_string(), "http://a/c");
}

TEST_MAIN(URL)

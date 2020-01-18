/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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
    EXPECT_EQ(URL("http://www.serenityos.org/").to_string(), "http://www.serenityos.org/");
    EXPECT_EQ(URL("http://www.serenityos.org:81/").to_string(), "http://www.serenityos.org:81/");
}

TEST_MAIN(URL)

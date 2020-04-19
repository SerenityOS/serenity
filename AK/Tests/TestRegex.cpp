/*
 * Copyright (c) 2020, Emanuel Sprung <emanuel.sprung@gmail.com>
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

#include <AK/Regex.h>
#include <AK/TestSuite.h>
#include <stdio.h>

TEST_CASE(catch_all)
{
    Pattern regex("^.*$", (u8)CompilationFlags::Extended);
    EXPECT_EQ(match("Hello World", regex), true);
}

TEST_CASE(char_utf8)
{
    Pattern regex("😀");
    MatchResult m;

    EXPECT_EQ(match("Привет, мир! 😀 γειά σου κόσμος 😀 こんにちは世界", regex, m, (u8)MatchFlags::MatchAll), true);
    EXPECT_EQ(m.match_count, 2u);
}

TEST_CASE(catch_all_newline)
{
    Pattern regex("^.*$", (u8)CompilationFlags::Extended | (u8)CompilationFlags::HandleNewLine);
    MatchResult m;
    auto lambda = [&m, &regex]() {
        String aaa = "Hello World\nTest\n1234\n";
        EXPECT_EQ(match(aaa, regex, m), true);
    };
    lambda();
    EXPECT_EQ(m.match_count, 3u);
    EXPECT_EQ(m.matches.at(0).view, "Hello World");
    EXPECT_EQ(m.matches.at(1).view, "Test");
    EXPECT_EQ(m.matches.at(2).view, "1234");
}

TEST_CASE(catch_all_newline_2)
{
    Pattern regex("^.*$", (u8)CompilationFlags::Extended);
    MatchResult m;
    EXPECT_EQ(match("Hello World\nTest\n1234\n", regex, m, (u8)MatchFlags::HandleNewLine), true);
    EXPECT_EQ(m.match_count, 3u);
    EXPECT_EQ(m.matches.at(0).view, "Hello World");
    EXPECT_EQ(m.matches.at(1).view, "Test");
    EXPECT_EQ(m.matches.at(2).view, "1234");

    EXPECT_EQ(match("Hello World\nTest\n1234\n", regex, m), true);
    EXPECT_EQ(m.match_count, 1u);
    EXPECT_EQ(m.matches.at(0).view, "Hello World\nTest\n1234\n");
}

TEST_MAIN(Regex)

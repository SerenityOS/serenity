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
}

TEST_CASE(dotdot_coalescing)
{
    EXPECT_EQ(LexicalPath("/home/user/../../not/home").string(), "/not/home");
    EXPECT_EQ(LexicalPath("/../../../../").string(), "/");
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

TEST_MAIN(LexicalPath)

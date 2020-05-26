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

// Temporarily disabled, as they were broken by commit a3e4dfdf9859a9b955bf4728328f740a47de5851
//
#if 0

TEST_CASE(relative_paths)
{
    {
        LexicalPath path("simple");
        EXPECT_EQ(path.is_valid(), true);
        EXPECT_EQ(path.string(), "./simple");
        EXPECT_EQ(path.parts().size(), 2u);
        EXPECT_EQ(path.basename(), "simple");
    }
    {
        LexicalPath path("a/relative/path");
        EXPECT_EQ(path.is_valid(), true);
        EXPECT_EQ(path.string(), "./a/relative/path");
        EXPECT_EQ(path.parts().size(), 4u);
        EXPECT_EQ(path.basename(), "path");
    }
    {
        LexicalPath path("./././foo");
        EXPECT_EQ(path.is_valid(), true);
        EXPECT_EQ(path.string(), "./foo");
        EXPECT_EQ(path.parts().size(), 2u);
        EXPECT_EQ(path.basename(), "foo");
    }

    {
        LexicalPath path(".");
        EXPECT_EQ(path.is_valid(), true);
        EXPECT_EQ(path.string(), ".");
        EXPECT_EQ(path.parts().size(), 1u);
        EXPECT_EQ(path.basename(), ".");
    }
}

TEST_CASE(has_extension)
{
    {
        FileSystemPath path("/tmp/simple.png");
        EXPECT(path.has_extension(".png"));
        EXPECT(path.has_extension(".pnG"));
        EXPECT(path.has_extension(".PNG"));
    }

    {
        FileSystemPath path("/TMP/SIMPLE.PNG");
        EXPECT(path.has_extension(".png"));
        EXPECT(path.has_extension(".pnG"));
        EXPECT(path.has_extension(".PNG"));
    }

    {
        FileSystemPath path(".png");
        EXPECT(path.has_extension(".png"));
    }

    {
        FileSystemPath path;
        EXPECT_EQ(path.has_extension(".png"), false);
    }

    {
        FileSystemPath path("png");
        EXPECT_EQ(path.has_extension(".png"), false);
    }
}

#endif

TEST_MAIN(LexicalPath)

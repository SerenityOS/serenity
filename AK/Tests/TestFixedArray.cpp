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

#include <AK/String.h>
#include <AK/FixedArray.h>

TEST_CASE(construct)
{
    EXPECT(FixedArray<int>().size() == 0);
}

TEST_CASE(ints)
{
    FixedArray<int> ints(3);
    ints[0] = 0;
    ints[1] = 1;
    ints[2] = 2;
    EXPECT_EQ(ints[0], 0);
    EXPECT_EQ(ints[1], 1);
    EXPECT_EQ(ints[2], 2);

    ints.clear();
    EXPECT_EQ(ints.size(), 0u);
}

TEST_CASE(resize)
{
    FixedArray<String> strings(2);
    strings[0] = "ABC";
    strings[1] = "DEF";

    EXPECT_EQ(strings.size(), 2u);
    EXPECT_EQ(strings[0], "ABC");
    EXPECT_EQ(strings[1], "DEF");

    strings.resize(4);

    EXPECT_EQ(strings.size(), 4u);
    EXPECT_EQ(strings[0], "ABC");
    EXPECT_EQ(strings[1], "DEF");

    EXPECT_EQ(strings[2].is_null(), true);
    EXPECT_EQ(strings[3].is_null(), true);

    strings[2] = "GHI";
    strings[3] = "JKL";

    EXPECT_EQ(strings[2], "GHI");
    EXPECT_EQ(strings[3], "JKL");

    strings.resize(1);
    EXPECT_EQ(strings.size(), 1u);
    EXPECT_EQ(strings[0], "ABC");
}

TEST_MAIN(FixedArray)

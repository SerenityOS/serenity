/*
 * Copyright (c) 2021, Brian Gianforcaro <b.gianfo@gmail.com>
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

#include <AK/EnumBits.h>
#include <AK/TestSuite.h>

enum class VideoIntro : u8 {
    None = 0x0,
    Well = 0x1,
    Hello = 0x2,
    Friends = 0x4,
    ExclimationMark = 0x8,
    CompleteIntro = Well | Hello | Friends | ExclimationMark,
};

AK_ENUM_BITWISE_OPERATORS(VideoIntro);

TEST_CASE(bitwise_or)
{
    auto intro = VideoIntro::Well | VideoIntro::Hello | VideoIntro::Friends | VideoIntro::ExclimationMark;
    EXPECT_EQ(intro, VideoIntro::CompleteIntro);
}

TEST_CASE(bitwise_and)
{
    auto intro = VideoIntro::CompleteIntro;
    EXPECT_EQ(intro & VideoIntro::Hello, VideoIntro::Hello);
}

TEST_CASE(bitwise_xor)
{
    auto intro = VideoIntro::Well | VideoIntro::Hello | VideoIntro::Friends;
    EXPECT_EQ(intro ^ VideoIntro::CompleteIntro, VideoIntro::ExclimationMark);
}

TEST_CASE(bitwise_not)
{
    auto intro = ~VideoIntro::CompleteIntro;
    EXPECT_EQ(intro & VideoIntro::CompleteIntro, VideoIntro::None);
}

TEST_CASE(bitwise_or_equal)
{
    auto intro = VideoIntro::Well | VideoIntro::Hello | VideoIntro::Friends;
    EXPECT_EQ(intro |= VideoIntro::ExclimationMark, VideoIntro::CompleteIntro);
}

TEST_CASE(bitwise_and_equal)
{
    auto intro = VideoIntro::CompleteIntro;
    EXPECT_EQ(intro &= VideoIntro::Hello, VideoIntro::Hello);
}

TEST_CASE(bitwise_xor_equal)
{
    auto intro = VideoIntro::Well | VideoIntro::Hello | VideoIntro::Friends;
    EXPECT_EQ(intro ^= VideoIntro::CompleteIntro, VideoIntro::ExclimationMark);
}

TEST_CASE(has_flag)
{
    auto intro = VideoIntro::Hello | VideoIntro::Friends;
    EXPECT(has_flag(intro, VideoIntro::Friends));
    EXPECT(!has_flag(intro, VideoIntro::Well));
}

TEST_MAIN(EnumBits)

/*
 * Copyright (c) 2020, the SerenityOS developers.
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

#include <AK/MACAddress.h>
#include <AK/Types.h>

TEST_CASE(should_default_construct)
{
    MACAddress sut {};
    EXPECT(sut.is_zero());
}

TEST_CASE(should_braces_construct)
{
    MACAddress sut { 1, 2, 3, 4, 5, 6 };
    EXPECT(!sut.is_zero());
}

TEST_CASE(should_construct_from_c_array)
{
    u8 addr[6] = { 1, 2, 3, 4, 5, 6 };
    MACAddress sut(addr);
    EXPECT(!sut.is_zero());
}

TEST_CASE(should_construct_from_6_octets)
{
    MACAddress sut(1, 2, 3, 4, 5, 6);
    EXPECT(!sut.is_zero());
}

TEST_CASE(should_provide_access_to_octet_by_index)
{
    MACAddress sut(1, 2, 3, 4, 5, 6);
    for (auto i = 0u; i < sizeof(MACAddress); ++i) {
        EXPECT_EQ(i + 1, sut[i]);
    }
}

TEST_CASE(should_equality_compare)
{
    MACAddress a(1, 2, 3, 4, 5, 6);
    MACAddress b(1, 2, 3, 42, 5, 6);
    EXPECT(a == a);
    EXPECT(a != b);
}

TEST_CASE(should_string_format)
{
    MACAddress sut(1, 2, 3, 4, 5, 6);
    EXPECT_EQ("01:02:03:04:05:06", sut.to_string());
}

TEST_MAIN(MACAddress)

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

#include <AK/Array.h>
#include <AK/TypedTransfer.h>

struct NonPrimitiveIntWrapper {
    NonPrimitiveIntWrapper(int value)
        : m_value(value)
    {
    }

    int m_value;
};

TEST_CASE(overlapping_source_and_destination_1)
{
    const Array<NonPrimitiveIntWrapper, 6> expected { 3, 4, 5, 6, 5, 6 };

    Array<NonPrimitiveIntWrapper, 6> actual { 1, 2, 3, 4, 5, 6 };
    AK::TypedTransfer<NonPrimitiveIntWrapper>::copy(actual.data(), actual.data() + 2, 4);

    for (size_t i = 0; i < 6; ++i)
        EXPECT_EQ(actual[i].m_value, expected[i].m_value);
}

TEST_CASE(overlapping_source_and_destination_2)
{
    const Array<NonPrimitiveIntWrapper, 6> expected { 1, 2, 1, 2, 3, 4 };

    Array<NonPrimitiveIntWrapper, 6> actual { 1, 2, 3, 4, 5, 6 };
    AK::TypedTransfer<NonPrimitiveIntWrapper>::copy(actual.data() + 2, actual.data(), 4);

    for (size_t i = 0; i < 6; ++i)
        EXPECT_EQ(actual[i].m_value, expected[i].m_value);
}

TEST_MAIN(TypedTransfer)

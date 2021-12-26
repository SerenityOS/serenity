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
#include <AK/WeakPtr.h>
#include <AK/Weakable.h>

#ifdef __clang__
#    pragma clang diagnostic push
#    pragma clang diagnostic ignored "-Wunused-private-field"
#endif

class SimpleWeakable : public Weakable<SimpleWeakable>
    , public RefCounted<SimpleWeakable> {
public:
    SimpleWeakable() = default;

private:
    int m_member { 123 };
};

#ifdef __clang__
#    pragma clang diagnostic pop
#endif

TEST_CASE(basic_weak)
{
    WeakPtr<SimpleWeakable> weak1;
    WeakPtr<SimpleWeakable> weak2;

    {
        auto simple = adopt(*new SimpleWeakable);
        weak1 = simple;
        weak2 = simple;
        EXPECT_EQ(weak1.is_null(), false);
        EXPECT_EQ(weak2.is_null(), false);
        EXPECT_EQ(weak1.strong_ref().ptr(), simple.ptr());
        EXPECT_EQ(weak1.strong_ref().ptr(), weak2.strong_ref().ptr());
    }

    EXPECT_EQ(weak1.is_null(), true);
    EXPECT_EQ(weak1.strong_ref().ptr(), nullptr);
    EXPECT_EQ(weak1.strong_ref().ptr(), weak2.strong_ref().ptr());
}

TEST_CASE(weakptr_move)
{
    WeakPtr<SimpleWeakable> weak1;
    WeakPtr<SimpleWeakable> weak2;

    {
        auto simple = adopt(*new SimpleWeakable);
        weak1 = simple;
        weak2 = move(weak1);
        EXPECT_EQ(weak1.is_null(), true);
        EXPECT_EQ(weak2.is_null(), false);
        EXPECT_EQ(weak2.strong_ref().ptr(), simple.ptr());
    }

    EXPECT_EQ(weak2.is_null(), true);
}

TEST_MAIN(WeakPtr)

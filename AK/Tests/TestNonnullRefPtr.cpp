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

#include <AK/NonnullRefPtr.h>
#include <AK/String.h>

struct Object : public RefCounted<Object> {
    int x;
};

TEST_CASE(basics)
{
    auto object = adopt(*new Object);
    EXPECT(object.ptr() != nullptr);
    EXPECT_EQ(object->ref_count(), 1u);
    object->ref();
    EXPECT_EQ(object->ref_count(), 2u);
    object->unref();
    EXPECT_EQ(object->ref_count(), 1u);

    {
        NonnullRefPtr another = object;
        EXPECT_EQ(object->ref_count(), 2u);
    }

    EXPECT_EQ(object->ref_count(), 1u);
}

TEST_CASE(assign_reference)
{
    auto object = adopt(*new Object);
    EXPECT_EQ(object->ref_count(), 1u);
    object = *object;
    EXPECT_EQ(object->ref_count(), 1u);
}

TEST_CASE(assign_owner_of_self)
{
    struct Object : public RefCounted<Object> {
        RefPtr<Object> parent;
    };

    auto parent = adopt(*new Object);
    auto child = adopt(*new Object);
    child->parent = move(parent);

    child = *child->parent;
    EXPECT_EQ(child->ref_count(), 1u);
}

TEST_CASE(swap_with_self)
{
    auto object = adopt(*new Object);
    swap(object, object);
    EXPECT_EQ(object->ref_count(), 1u);
}

TEST_MAIN(NonnullRefPtr)

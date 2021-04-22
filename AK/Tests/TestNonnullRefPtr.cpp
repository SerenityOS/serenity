/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
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

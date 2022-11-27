/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "LibTest/Macros.h"
#include <AK/TaggedPtr.h>
#include <LibTest/TestCase.h>

TEST_CASE(empty_tagged_ptr)
{
    TaggedPtr<unsigned, 1> tagged_ptr;
    EXPECT_EQ(tagged_ptr.ptr(), 0u);
    EXPECT_EQ(tagged_ptr.tag(), 0u);
}

TEST_CASE(happy_path_tagged_ptr)
{
    int ptr_value = 42;
    TaggedPtr<int*, 1> tagged_ptr;
    tagged_ptr.set_ptr(&ptr_value);
    tagged_ptr.set_tag(1);
    EXPECT_EQ(tagged_ptr.ptr(), &ptr_value);
    EXPECT_EQ(tagged_ptr.tag(), 1u);
}

TEST_CASE(enum_tagged_ptr)
{
    int ptr_value = 42;
    enum class Tag {
        value1,
        value2,
    };
    TaggedPtr<int*, 2, Tag> tagged_ptr;
    tagged_ptr.set_ptr(&ptr_value);
    tagged_ptr.set_tag(Tag::value1);
    EXPECT_EQ(tagged_ptr.ptr(), &ptr_value);
    EXPECT_EQ(tagged_ptr.tag(), Tag::value1);
}

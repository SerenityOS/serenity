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
#include <AK/NonnullOwnPtrVector.h>
#include <AK/OwnPtr.h>
#include <AK/Vector.h>

TEST_CASE(construct)
{
    EXPECT(Vector<int>().is_empty());
    EXPECT(Vector<int>().size() == 0);
}

TEST_CASE(ints)
{
    Vector<int> ints;
    ints.append(1);
    ints.append(2);
    ints.append(3);
    EXPECT_EQ(ints.size(), 3);
    EXPECT_EQ(ints.take_last(), 3);
    EXPECT_EQ(ints.size(), 2);
    EXPECT_EQ(ints.take_last(), 2);
    EXPECT_EQ(ints.size(), 1);
    EXPECT_EQ(ints.take_last(), 1);
    EXPECT_EQ(ints.size(), 0);

    ints.clear();
    EXPECT_EQ(ints.size(), 0);
}

TEST_CASE(strings)
{
    Vector<String> strings;
    strings.append("ABC");
    strings.append("DEF");

    int loop_counter = 0;
    for (const String& string : strings) {
        EXPECT(!string.is_null());
        EXPECT(!string.is_empty());
        ++loop_counter;
    }

    loop_counter = 0;
    for (auto& string : (const_cast<const Vector<String>&>(strings))) {
        EXPECT(!string.is_null());
        EXPECT(!string.is_empty());
        ++loop_counter;
    }
    EXPECT_EQ(loop_counter, 2);
}

TEST_CASE(strings_insert_ordered)
{
    Vector<String> strings;
    strings.append("abc");
    strings.append("def");
    strings.append("ghi");

    strings.insert_before_matching("f-g", [](auto& entry) {
        return "f-g" < entry;
    });

    EXPECT_EQ(strings[0], "abc");
    EXPECT_EQ(strings[1], "def");
    EXPECT_EQ(strings[2], "f-g");
    EXPECT_EQ(strings[3], "ghi");
}

TEST_CASE(prepend_vector)
{
    Vector<int> ints;
    ints.append(1);
    ints.append(2);
    ints.append(3);

    Vector<int> more_ints;
    more_ints.append(4);
    more_ints.append(5);
    more_ints.append(6);

    ints.prepend(move(more_ints));

    EXPECT_EQ(ints.size(), 6);
    EXPECT_EQ(more_ints.size(), 0);

    EXPECT_EQ(ints[0], 4);
    EXPECT_EQ(ints[1], 5);
    EXPECT_EQ(ints[2], 6);
    EXPECT_EQ(ints[3], 1);
    EXPECT_EQ(ints[4], 2);
    EXPECT_EQ(ints[5], 3);

    ints.prepend(move(more_ints));
    EXPECT_EQ(ints.size(), 6);
    EXPECT_EQ(more_ints.size(), 0);

    more_ints.prepend(move(ints));
    EXPECT_EQ(more_ints.size(), 6);
    EXPECT_EQ(ints.size(), 0);
}

TEST_CASE(prepend_vector_object)
{
    struct SubObject {
        SubObject(int v)
            : value(v)
        {
        }
        int value { 0 };
    };
    struct Object {
        Object(NonnullOwnPtr<SubObject>&& a_subobject)
            : subobject(move(a_subobject))
        {
        }
        OwnPtr<SubObject> subobject;
    };

    Vector<Object> objects;
    objects.empend(make<SubObject>(1));
    objects.empend(make<SubObject>(2));
    objects.empend(make<SubObject>(3));

    EXPECT_EQ(objects.size(), 3);

    Vector<Object> more_objects;
    more_objects.empend(make<SubObject>(4));
    more_objects.empend(make<SubObject>(5));
    more_objects.empend(make<SubObject>(6));
    EXPECT_EQ(more_objects.size(), 3);

    objects.prepend(move(more_objects));
    EXPECT_EQ(more_objects.size(), 0);
    EXPECT_EQ(objects.size(), 6);

    EXPECT_EQ(objects[0].subobject->value, 4);
    EXPECT_EQ(objects[1].subobject->value, 5);
    EXPECT_EQ(objects[2].subobject->value, 6);
    EXPECT_EQ(objects[3].subobject->value, 1);
    EXPECT_EQ(objects[4].subobject->value, 2);
    EXPECT_EQ(objects[5].subobject->value, 3);
}

TEST_CASE(vector_compare)
{
    Vector<int> ints;
    Vector<int> same_ints;

    for (int i = 0; i < 1000; ++i) {
        ints.append(i);
        same_ints.append(i);
    }

    EXPECT_EQ(ints.size(), 1000);
    EXPECT_EQ(ints, same_ints);

    Vector<String> strings;
    Vector<String> same_strings;

    for (int i = 0; i < 1000; ++i) {
        strings.append(String::number(i));
        same_strings.append(String::number(i));
    }

    EXPECT_EQ(strings.size(), 1000);
    EXPECT_EQ(strings, same_strings);
}

TEST_CASE(grow_past_inline_capacity)
{
    auto make_vector = [] {
        Vector<String, 16> strings;
        for (int i = 0; i < 32; ++i) {
            strings.append(String::number(i));
        }
        return strings;
    };

    auto strings = make_vector();

    EXPECT_EQ(strings.size(), 32);
    EXPECT_EQ(strings[31], "31");

    strings.clear();
    EXPECT_EQ(strings.size(), 0);
    EXPECT_EQ(strings.capacity(), 16);

    strings = make_vector();

    strings.clear_with_capacity();
    EXPECT_EQ(strings.size(), 0);
    EXPECT(strings.capacity() >= 32);
}

BENCHMARK_CASE(vector_append_trivial)
{
    // This should be super fast thanks to Vector using memmove.
    Vector<int> ints;
    for (int i = 0; i < 1000000; ++i) {
        ints.append(i);
    }
    for (int i = 0; i < 100; ++i) {
        Vector<int> tmp;
        tmp.append(ints);
        EXPECT_EQ(tmp.size(), 1000000);
    }
}

BENCHMARK_CASE(vector_remove_trivial)
{
    // This should be super fast thanks to Vector using memmove.
    Vector<int> ints;
    for (int i = 0; i < 10000; ++i) {
        ints.append(i);
    }
    while (!ints.is_empty()) {
        ints.remove(0);
    }
    EXPECT_EQ(ints.size(), 0);
}

TEST_CASE(vector_remove)
{
    Vector<int> ints;
    ints.append(1);
    ints.append(2);
    ints.append(3);
    ints.append(4);
    ints.append(5);

    ints.remove(1);
    EXPECT_EQ(ints.size(), 4);
    EXPECT_EQ(ints[0], 1);
    EXPECT_EQ(ints[1], 3);
    EXPECT_EQ(ints[2], 4);
    EXPECT_EQ(ints[3], 5);

    ints.remove(0);
    EXPECT_EQ(ints.size(), 3);
    EXPECT_EQ(ints[0], 3);
    EXPECT_EQ(ints[1], 4);
    EXPECT_EQ(ints[2], 5);

    ints.take_last();
    EXPECT_EQ(ints.size(), 2);
    EXPECT_EQ(ints[0], 3);
    EXPECT_EQ(ints[1], 4);

    ints.take_first();
    EXPECT_EQ(ints.size(), 1);
    EXPECT_EQ(ints[0], 4);
}

TEST_CASE(nonnullownptrvector)
{
    struct Object {
        String string;
    };
    NonnullOwnPtrVector<Object> objects;

    objects.append(make<Object>());
    EXPECT_EQ(objects.size(), 1);

    OwnPtr<Object> o = make<Object>();
    objects.append(o.release_nonnull());
    EXPECT(o == nullptr);
    EXPECT_EQ(objects.size(), 2);
}

TEST_CASE(insert_trivial)
{
    Vector<int> ints;
    ints.append(0);
    ints.append(10);
    ints.append(20);
    ints.append(30);
    ints.append(40);
    ints.insert(2, 15);
    EXPECT_EQ(ints.size(), 6);
    EXPECT_EQ(ints[0], 0);
    EXPECT_EQ(ints[1], 10);
    EXPECT_EQ(ints[2], 15);
    EXPECT_EQ(ints[3], 20);
    EXPECT_EQ(ints[4], 30);
    EXPECT_EQ(ints[5], 40);
}

TEST_MAIN(Vector)

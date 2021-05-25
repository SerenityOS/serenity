/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenity.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestSuite.h>

#include <AK/RefPtr.h>
#include <AK/Variant.h>

namespace {

struct Object : public RefCounted<Object> {
};

}

TEST_CASE(basic)
{
    Variant<int, String> the_value { 42 };
    EXPECT(the_value.has<int>());
    EXPECT_EQ(the_value.get<int>(), 42);
    the_value = String("42");
    EXPECT(the_value.has<String>());
    EXPECT_EQ(the_value.get<String>(), "42");
}

TEST_CASE(visit)
{
    bool correct = false;
    Variant<int, String, float> the_value { 42.0f };
    the_value.visit(
        [&](const int&) { correct = false; },
        [&](const String&) { correct = false; },
        [&](const float&) { correct = true; });
    EXPECT(correct);
}

TEST_CASE(destructor)
{
    struct DestructionChecker {
        explicit DestructionChecker(bool& was_destroyed)
            : m_was_destroyed(was_destroyed)
        {
        }

        ~DestructionChecker()
        {
            m_was_destroyed = true;
        }
        bool& m_was_destroyed;
    };

    bool was_destroyed = false;
    {
        Variant<DestructionChecker> test_variant { DestructionChecker { was_destroyed } };
    }
    EXPECT(was_destroyed);
}

TEST_CASE(move_moves)
{
    struct NoCopy {
        AK_MAKE_NONCOPYABLE(NoCopy);

    public:
        NoCopy() = default;
        NoCopy(NoCopy&&) = default;
    };

    Variant<NoCopy, int> first_variant { 42 };
    // Should not fail to compile
    first_variant = NoCopy {};

    Variant<NoCopy, int> second_variant = move(first_variant);
    EXPECT(second_variant.has<NoCopy>());
}

TEST_CASE(downcast)
{
    Variant<i8, i16, i32, i64> one_integer_to_rule_them_all { static_cast<i32>(42) };
    auto fake_integer = one_integer_to_rule_them_all.downcast<i8, i32>();
    EXPECT(fake_integer.has<i32>());
    EXPECT(one_integer_to_rule_them_all.has<i32>());
    EXPECT_EQ(fake_integer.get<i32>(), 42);
    EXPECT_EQ(one_integer_to_rule_them_all.get<i32>(), 42);

    fake_integer = static_cast<i8>(60);
    one_integer_to_rule_them_all = fake_integer.downcast<i8, i16>().downcast<i8, i32, float>().downcast<i8, i16, i32, i64>();
    EXPECT(fake_integer.has<i8>());
    EXPECT(one_integer_to_rule_them_all.has<i8>());
    EXPECT_EQ(fake_integer.get<i8>(), 60);
    EXPECT_EQ(one_integer_to_rule_them_all.get<i8>(), 60);
}

TEST_CASE(moved_from_state)
{
    // Note: This test requires that Vector's moved-from state be consistent
    //       it need not be in a specific state (though as it is currently implemented,
    //       a moved-from vector is the same as a newly-created vector)
    //       This test does not make assumptions about the state itself, but rather that
    //       it remains consistent when done on different instances.
    //       Should this assumption be broken, we should probably switch to defining a local
    //       class that has fixed semantics, but I doubt the moved-from state of Vector will
    //       change any time soon :P
    Vector<i32> bunch_of_values { 1, 2, 3, 4, 5, 6, 7, 8 };
    Variant<Vector<i32>, Empty> optionally_a_bunch_of_values { Vector<i32> { 1, 2, 3, 4, 5, 6, 7, 8 } };

    {
        [[maybe_unused]] auto devnull_0 = move(bunch_of_values);
        [[maybe_unused]] auto devnull_1 = move(optionally_a_bunch_of_values);
    }

    // The moved-from state should be the same in both cases, and the variant should still contain a moved-from vector.
    // Note: Use after move is intentional.
    EXPECT(optionally_a_bunch_of_values.has<Vector<i32>>());
    auto same_contents = __builtin_memcmp(&bunch_of_values, &optionally_a_bunch_of_values.get<Vector<i32>>(), sizeof(bunch_of_values)) == 0;
    EXPECT(same_contents);
}

TEST_CASE(duplicated_types)
{
    Variant<int, int, int, int> its_just_an_int { 42 };
    EXPECT(its_just_an_int.has<int>());
    EXPECT_EQ(its_just_an_int.get<int>(), 42);
}

TEST_CASE(return_values)
{
    using MyVariant = Variant<int, String, float>;
    {
        MyVariant the_value { 42.0f };

        float value = the_value.visit(
            [&](const int&) { return 1.0f; },
            [&](const String&) { return 2.0f; },
            [&](const float& f) { return f; });
        EXPECT_EQ(value, 42.0f);
    }
    {
        MyVariant the_value { 42 };

        int value = the_value.visit(
            [&](int& i) { return i; },
            [&](String&) { return 2; },
            [&](float&) { return 3; });
        EXPECT_EQ(value, 42);
    }
    {
        const MyVariant the_value { "str" };

        String value = the_value.visit(
            [&](const int&) { return String { "wrong" }; },
            [&](const String& s) { return s; },
            [&](const float&) { return String { "wrong" }; });
        EXPECT_EQ(value, "str");
    }
}

TEST_CASE(return_values_by_reference)
{
    auto ref = adopt_ref_if_nonnull(new Object());
    Variant<int, String, float> the_value { 42.0f };

    auto& value = the_value.visit(
        [&](const int&) -> RefPtr<Object>& { return ref; },
        [&](const String&) -> RefPtr<Object>& { return ref; },
        [&](const float&) -> RefPtr<Object>& { return ref; });

    EXPECT_EQ(ref, value);
    EXPECT_EQ(ref->ref_count(), 1u);
    EXPECT_EQ(value->ref_count(), 1u);
}

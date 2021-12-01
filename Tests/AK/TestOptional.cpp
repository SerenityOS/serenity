/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Daniel Bertalan <dani@danielbertalan.dev>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/Optional.h>
#include <AK/String.h>
#include <AK/Vector.h>

TEST_CASE(basic_optional)
{
    Optional<int> x;
    EXPECT_EQ(x.has_value(), false);
    x = 3;
    EXPECT_EQ(x.has_value(), true);
    EXPECT_EQ(x.value(), 3);
}

TEST_CASE(move_optional)
{
    Optional<int> x;
    EXPECT_EQ(x.has_value(), false);
    x = 3;
    EXPECT_EQ(x.has_value(), true);
    EXPECT_EQ(x.value(), 3);

    Optional<int> y;
    y = move(x);
    EXPECT_EQ(y.has_value(), true);
    EXPECT_EQ(y.value(), 3);
    EXPECT_EQ(x.has_value(), false);
}

TEST_CASE(optional_rvalue_ref_qualified_getters)
{
    struct DontCopyMe {
        DontCopyMe() { }
        ~DontCopyMe() = default;
        DontCopyMe(DontCopyMe&&) = default;
        DontCopyMe& operator=(DontCopyMe&&) = default;
        DontCopyMe(DontCopyMe const&) = delete;
        DontCopyMe& operator=(DontCopyMe const&) = delete;

        int x { 13 };
    };

    auto make_an_optional = []() -> Optional<DontCopyMe> {
        return DontCopyMe {};
    };

    EXPECT_EQ(make_an_optional().value().x, 13);
    EXPECT_EQ(make_an_optional().value_or(DontCopyMe {}).x, 13);
}

TEST_CASE(optional_leak_1)
{
    struct Structure {
        Optional<String> str;
    };

    // This used to leak, it does not anymore.
    Vector<Structure> vec;
    vec.append({ "foo" });
    EXPECT_EQ(vec[0].str.has_value(), true);
    EXPECT_EQ(vec[0].str.value(), "foo");
}

TEST_CASE(short_notation)
{
    Optional<StringView> value = "foo";

    EXPECT_EQ(value->length(), 3u);
    EXPECT_EQ(*value, "foo");
}

TEST_CASE(comparison_without_values)
{
    Optional<StringView> opt0;
    Optional<StringView> opt1;
    Optional<String> opt2;
    EXPECT_EQ(opt0, opt1);
    EXPECT_EQ(opt0, opt2);
}

TEST_CASE(comparison_with_values)
{
    Optional<StringView> opt0;
    Optional<StringView> opt1 = "foo";
    Optional<String> opt2 = "foo";
    Optional<StringView> opt3 = "bar";
    EXPECT_NE(opt0, opt1);
    EXPECT_EQ(opt1, opt2);
    EXPECT_NE(opt1, opt3);
}

TEST_CASE(comparison_to_underlying_types)
{
    Optional<String> opt0;
    EXPECT_NE(opt0, String());
    EXPECT_NE(opt0, "foo");

    Optional<StringView> opt1 = "foo";
    EXPECT_EQ(opt1, "foo");
    EXPECT_NE(opt1, "bar");
    EXPECT_EQ(opt1, String("foo"));
}

TEST_CASE(comparison_with_numeric_types)
{
    Optional<u8> opt0;
    EXPECT_NE(opt0, 0);
    Optional<u8> opt1 = 7;
    EXPECT_EQ(opt1, 7);
    EXPECT_EQ(opt1, 7.0);
    EXPECT_EQ(opt1, 7u);
    EXPECT_NE(opt1, -2);
}

TEST_CASE(test_copy_ctor_and_dtor_called)
{
#ifdef AK_HAVE_CONDITIONALLY_TRIVIAL
    static_assert(IsTriviallyDestructible<Optional<u8>>);
    static_assert(IsTriviallyCopyable<Optional<u8>>);
    static_assert(IsTriviallyCopyConstructible<Optional<u8>>);
    static_assert(IsTriviallyCopyAssignable<Optional<u8>>);
    // These can't be trivial as we have to clear the original object.
    static_assert(!IsTriviallyMoveConstructible<Optional<u8>>);
    static_assert(!IsTriviallyMoveAssignable<Optional<u8>>);
#endif

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

    static_assert(!IsTriviallyDestructible<Optional<DestructionChecker>>);

    bool was_destroyed = false;
    {
        Optional<DestructionChecker> test_optional = DestructionChecker { was_destroyed };
    }
    EXPECT(was_destroyed);

    struct CopyChecker {
        explicit CopyChecker(bool& was_copy_constructed)
            : m_was_copy_constructed(was_copy_constructed)
        {
        }

        CopyChecker(const CopyChecker& other)
            : m_was_copy_constructed(other.m_was_copy_constructed)
        {
            m_was_copy_constructed = true;
        }

        bool& m_was_copy_constructed;
    };

    static_assert(IsCopyConstructible<Optional<CopyChecker>>);
    static_assert(!IsTriviallyCopyConstructible<Optional<CopyChecker>>);

    bool was_copy_constructed = false;
    Optional<CopyChecker> copy1 = CopyChecker { was_copy_constructed };
    Optional<CopyChecker> copy2 = copy1;
    EXPECT(was_copy_constructed);

    struct MoveChecker {
        explicit MoveChecker(bool& was_move_constructed)
            : m_was_move_constructed(was_move_constructed)
        {
        }

        MoveChecker(const MoveChecker& other)
            : m_was_move_constructed(other.m_was_move_constructed)
        {
            EXPECT(false);
        };

        MoveChecker(MoveChecker&& other)
            : m_was_move_constructed(other.m_was_move_constructed)
        {
            m_was_move_constructed = true;
        };

        bool& m_was_move_constructed;
    };
    static_assert(IsMoveConstructible<Optional<MoveChecker>>);
    static_assert(!IsTriviallyMoveConstructible<Optional<MoveChecker>>);

    bool was_moved = false;
    Optional<MoveChecker> move1 = MoveChecker { was_moved };
    Optional<MoveChecker> move2 = move(move1);
    EXPECT(was_moved);

#ifdef AK_HAVE_CONDITIONALLY_TRIVIAL
    struct NonDestructible {
        ~NonDestructible() = delete;
    };
    static_assert(!IsDestructible<Optional<NonDestructible>>);
#endif
}

TEST_CASE(basic_optional_ref)
{
    Optional<int&> x;
    EXPECT_EQ(x.has_value(), false);
    int x_val = 3;
    x = Optional<int&> { x_val };
    EXPECT_EQ(x.has_value(), true);
    EXPECT_EQ(x.value(), 3);
    EXPECT_EQ(&(x.value()), &x_val);
}

TEST_CASE(move_optional_ref)
{
    Optional<int&> x;
    EXPECT_EQ(x.has_value(), false);
    int x_val = 3;
    x = Optional<int&> { x_val };
    EXPECT_EQ(x.has_value(), true);
    EXPECT_EQ(x.value(), 3);

    Optional<int&> y;
    y = move(x);
    EXPECT_EQ(y.has_value(), true);
    EXPECT_EQ(y.value(), 3);
    EXPECT_EQ(x.has_value(), false);
}

TEST_CASE(optional_ref_rvalue_ref_qualified_getters)
{
    int x = 42;

    auto make_an_optional = [&x]() -> Optional<int&> {
        return { x };
    };

    EXPECT_EQ(make_an_optional().value(), 42);
    EXPECT_EQ(make_an_optional().value_or(x), 42);
}

TEST_CASE(short_notation_ref)
{
    int backing = 42;
    Optional<int&> value = backing;

    EXPECT_EQ(*value, 42);
}

TEST_CASE(comparison_without_values_ref)
{
    Optional<StringView&> opt0;
    Optional<StringView&> opt1;
    Optional<String&> opt2;
    EXPECT(opt0 == opt1);
    EXPECT(opt0 == opt2);
}

TEST_CASE(comparison_with_values_ref)
{
    StringView val1 = "foo"sv;
    String val2 = "foo";
    StringView val3 = "bar"sv;

    Optional<StringView&> opt0;
    Optional<StringView&> opt1 = val1;
    Optional<String&> opt2 = val2;
    Optional<StringView&> opt3 = val3;
    EXPECT(opt0 != opt1);
    EXPECT(opt0 != opt2);
    EXPECT(opt0 != opt3);
    EXPECT(opt1 == opt2);
    EXPECT(opt1 != opt3);
    EXPECT(opt2 != opt3);
}

TEST_CASE(comparison_with_numeric_types_ref)
{
    u8 val1 = 7;
    Optional<u8&> opt0;
    Optional<u8&> opt1 = val1;

    EXPECT(opt0 != 0);
    EXPECT(opt0 != 7);
    EXPECT(opt1 == 7);
    EXPECT(opt1 == 7.0);
    EXPECT(opt1 == 7u);
    EXPECT(opt1 != 42);
    EXPECT(opt1 != -2);
}

TEST_CASE(method_call_ref)
{
    class MyFoo {
    public:
        int do_call() { return 42; }
    };
    MyFoo backing;
    Optional<MyFoo&> optional = backing;

    EXPECT_EQ(backing.do_call(), 42);
    EXPECT_EQ(optional->do_call(), 42);
}

TEST_CASE(copy_empty_ref)
{
    Optional<int&> optional1 = {};
    Optional<int> optional2 = optional1.copy();

    EXPECT(!optional1.has_value());
    EXPECT(!optional2.has_value());
}

TEST_CASE(copy_nonempty_ref)
{
    int backing = 42;
    Optional<int&> optional1 = backing;
    Optional<int> optional2 = optional1.copy();
    EXPECT_EQ(optional1.value(), 42);
    EXPECT_EQ(optional2.value(), 42);

    backing = 1337;
    EXPECT_EQ(optional1.value(), 1337);
    EXPECT_EQ(optional2.value(), 42);
}

/*
 * Copyright (c) 2020, Ben Wiederhake <BenWiederhake.GitHub@gmx.de>
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

#include <AK/Checked.h>
#include <AK/NumericLimits.h>

// These tests only check whether the usual operator semantics work.
// TODO: Add tests about the actual `Check`ing itself!

TEST_CASE(address_identity)
{
    Checked<int> a = 4;
    Checked<int> b = 5;
    EXPECT_EQ(&a == &a, true);
    EXPECT_EQ(&a == &b, false);
    EXPECT_EQ(&a != &a, false);
    EXPECT_EQ(&a != &b, true);
}

TEST_CASE(operator_identity)
{
    Checked<int> a = 4;
    EXPECT_EQ(a == 4, true);
    EXPECT_EQ(a == 5, false);
    EXPECT_EQ(a != 4, false);
    EXPECT_EQ(a != 5, true);
}

TEST_CASE(operator_incr)
{
    Checked<int> a = 4;
    EXPECT_EQ(++a, 5);
    EXPECT_EQ(++a, 6);
    EXPECT_EQ(++a, 7);
    EXPECT_EQ(a++, 7);
    EXPECT_EQ(a++, 8);
    EXPECT_EQ(a++, 9);
    EXPECT_EQ(a, 10);
    // TODO: If decrementing gets supported, test it.
}

TEST_CASE(operator_cmp)
{
    Checked<int> a = 4;
    EXPECT_EQ(a > 3, true);
    EXPECT_EQ(a < 3, false);
    EXPECT_EQ(a >= 3, true);
    EXPECT_EQ(a <= 3, false);
    EXPECT_EQ(a > 4, false);
    EXPECT_EQ(a < 4, false);
    EXPECT_EQ(a >= 4, true);
    EXPECT_EQ(a <= 4, true);
    EXPECT_EQ(a > 5, false);
    EXPECT_EQ(a < 5, true);
    EXPECT_EQ(a >= 5, false);
    EXPECT_EQ(a <= 5, true);
}

TEST_CASE(operator_arith)
{
    Checked<int> a = 12;
    Checked<int> b = 345;
    EXPECT_EQ(a + b, 357);
    EXPECT_EQ(b + a, 357);
    EXPECT_EQ(a - b, -333);
    EXPECT_EQ(b - a, 333);
    EXPECT_EQ(a * b, 4140);
    EXPECT_EQ(b * a, 4140);
    EXPECT_EQ(a / b, 0);
    EXPECT_EQ(b / a, 28);
}

TEST_CASE(detects_signed_overflow)
{
    EXPECT(!(Checked<int>(0x40000000) + Checked<int>(0x3fffffff)).has_overflow());
    EXPECT((Checked<int>(0x40000000) + Checked<int>(0x40000000)).has_overflow());
    EXPECT(!(Checked<int>(-0x40000000) + Checked<int>(-0x40000000)).has_overflow());
    EXPECT((Checked<int>(-0x40000001) + Checked<int>(-0x40000000)).has_overflow());

    EXPECT(!(Checked<int>(0x40000000) - Checked<int>(-0x3fffffff)).has_overflow());
    EXPECT((Checked<int>(0x40000000) - Checked<int>(-0x40000000)).has_overflow());
    EXPECT(!(Checked<int>(-0x40000000) - Checked<int>(0x40000000)).has_overflow());
    EXPECT((Checked<int>(-0x40000000) - Checked<int>(0x40000001)).has_overflow());

    EXPECT(!(Checked<i64>(0x4000000000000000) + Checked<i64>(0x3fffffffffffffff)).has_overflow());
    EXPECT((Checked<i64>(0x4000000000000000) + Checked<i64>(0x4000000000000000)).has_overflow());
    EXPECT(!(Checked<i64>(-0x4000000000000000) + Checked<i64>(-0x4000000000000000)).has_overflow());
    EXPECT((Checked<i64>(-0x4000000000000001) + Checked<i64>(-0x4000000000000000)).has_overflow());

    EXPECT(!(Checked<i64>(0x4000000000000000) - Checked<i64>(-0x3fffffffffffffff)).has_overflow());
    EXPECT((Checked<i64>(0x4000000000000000) - Checked<i64>(-0x4000000000000000)).has_overflow());
    EXPECT(!(Checked<i64>(-0x4000000000000000) - Checked<i64>(0x4000000000000000)).has_overflow());
    EXPECT((Checked<i64>(-0x4000000000000000) - Checked<i64>(0x4000000000000001)).has_overflow());
}

TEST_CASE(should_constexpr_default_construct)
{
    constexpr Checked<int> checked_value {};
    static_assert(!checked_value.has_overflow());
    static_assert(checked_value == int {});
}

TEST_CASE(should_constexpr_value_construct)
{
    constexpr Checked<int> checked_value { 42 };
    static_assert(!checked_value.has_overflow());
    static_assert(checked_value == 42);
}

TEST_CASE(should_constexpr_convert_construct)
{
    constexpr Checked<int> checked_value { 42u };
    static_assert(!checked_value.has_overflow());
    static_assert(checked_value == 42);
}

TEST_CASE(should_constexpr_copy_construct)
{
    constexpr auto checked_value = [] {
        const Checked<int> old_value { 42 };
        Checked<int> value(old_value);
        return value;
    }();
    static_assert(!checked_value.has_overflow());
    static_assert(checked_value == 42);
}

TEST_CASE(should_constexpr_move_construct)
{
    constexpr auto checked_value = [] {
        Checked<int> value(Checked<int> { 42 });
        return value;
    }();
    static_assert(!checked_value.has_overflow());
    static_assert(checked_value == 42);
}

TEST_CASE(should_constexpr_copy_assign)
{
    constexpr auto checked_value = [] {
        const Checked<int> old_value { 42 };
        Checked<int> value {};
        value = old_value;
        return value;
    }();
    static_assert(!checked_value.has_overflow());
    static_assert(checked_value == 42);
}

TEST_CASE(should_constexpr_move_assign)
{
    constexpr auto checked_value = [] {
        Checked<int> value {};
        value = Checked<int> { 42 };
        return value;
    }();
    static_assert(!checked_value.has_overflow());
    static_assert(checked_value == 42);
}

TEST_CASE(should_constexpr_convert_and_assign)
{
    constexpr auto checked_value = [] {
        Checked<int> value {};
        value = 42;
        return value;
    }();
    static_assert(!checked_value.has_overflow());
    static_assert(checked_value == 42);
}

TEST_CASE(should_constexpr_not_operator)
{
    constexpr Checked<int> value {};
    static_assert(!value);
}

TEST_CASE(should_constexpr_value_accessor)
{
    constexpr Checked<int> value { 42 };
    static_assert(value.value() == 42);
}

TEST_CASE(should_constexpr_add)
{
    constexpr auto checked_value = [] {
        Checked<int> value { 42 };
        value.add(3);
        return value;
    }();
    static_assert(checked_value == 45);
}

TEST_CASE(should_constexpr_sub)
{
    constexpr auto checked_value = [] {
        Checked<int> value { 42 };
        value.sub(3);
        return value;
    }();
    static_assert(checked_value == 39);
}

TEST_CASE(should_constexpr_mul)
{
    constexpr auto checked_value = [] {
        Checked<int> value { 42 };
        value.mul(2);
        return value;
    }();
    static_assert(checked_value == 84);
}

TEST_CASE(should_constexpr_div)
{
    constexpr auto checked_value = [] {
        Checked<int> value { 42 };
        value.div(3);
        return value;
    }();
    static_assert(checked_value == 14);
}

TEST_CASE(should_constexpr_assignment_by_sum)
{
    constexpr auto checked_value = [] {
        Checked<int> value { 42 };
        value += 3;
        return value;
    }();
    static_assert(checked_value == 45);
}

TEST_CASE(should_constexpr_assignment_by_diff)
{
    constexpr auto checked_value = [] {
        Checked<int> value { 42 };
        value -= 3;
        return value;
    }();
    static_assert(checked_value == 39);
}

TEST_CASE(should_constexpr_assignment_by_product)
{
    constexpr auto checked_value = [] {
        Checked<int> value { 42 };
        value *= 2;
        return value;
    }();
    static_assert(checked_value == 84);
}

TEST_CASE(should_constexpr_assignment_by_quotient)
{
    constexpr auto checked_value = [] {
        Checked<int> value { 42 };
        value /= 3;
        return value;
    }();
    static_assert(checked_value == 14);
}

TEST_CASE(should_constexpr_prefix_increment)
{
    constexpr auto checked_value = [] {
        Checked<int> value { 42 };
        ++value;
        return value;
    }();
    static_assert(checked_value == 43);
}

TEST_CASE(should_constexpr_postfix_increment)
{
    constexpr auto checked_value = [] {
        Checked<int> value { 42 };
        value++;
        return value;
    }();
    static_assert(checked_value == 43);
}

TEST_CASE(should_constexpr_check_for_overflow_addition)
{
    static_assert(Checked<int>::addition_would_overflow(NumericLimits<int>::max(), 1));
}

TEST_CASE(should_constexpr_check_for_overflow_multiplication)
{
    static_assert(Checked<int>::multiplication_would_overflow(NumericLimits<int>::max(), 2));
    static_assert(Checked<int>::multiplication_would_overflow(NumericLimits<int>::max(), 1, 2));
}

TEST_CASE(should_constexpr_add_checked_values)
{
    constexpr Checked<int> a { 42 };
    constexpr Checked<int> b { 17 };
    constexpr Checked<int> expected { 59 };
    static_assert(expected == (a + b).value());
}

TEST_CASE(should_constexpr_subtract_checked_values)
{
    constexpr Checked<int> a { 42 };
    constexpr Checked<int> b { 17 };
    constexpr Checked<int> expected { 25 };
    static_assert(expected == (a - b).value());
}

TEST_CASE(should_constexpr_multiply_checked_values)
{
    constexpr Checked<int> a { 3 };
    constexpr Checked<int> b { 5 };
    constexpr Checked<int> expected { 15 };
    static_assert(expected == (a * b).value());
}

TEST_CASE(should_constexpr_divide_checked_values)
{
    constexpr Checked<int> a { 10 };
    constexpr Checked<int> b { 2 };
    constexpr Checked<int> expected { 5 };
    static_assert(expected == (a / b).value());
}

TEST_CASE(should_constexpr_compare_checked_values_lhs)
{
    constexpr Checked<int> a { 10 };

    static_assert(a > 5);
    static_assert(a >= 10);
    static_assert(a >= 5);

    static_assert(a < 20);
    static_assert(a <= 30);
    static_assert(a <= 20);

    static_assert(a == 10);
    static_assert(a != 20);
}

TEST_CASE(should_constexpr_compare_checked_values_rhs)
{
    constexpr Checked<int> a { 10 };

    static_assert(5 < a);
    static_assert(10 <= a);
    static_assert(5 <= a);

    static_assert(20 > a);
    static_assert(30 >= a);
    static_assert(30 >= a);

    static_assert(10 == a);
    static_assert(20 != a);
}

TEST_CASE(should_constexpr_make_via_factory)
{
    [[maybe_unused]] constexpr auto value = make_checked(42);
}

TEST_MAIN(Checked)

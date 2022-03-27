/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/ArbitrarySizedEnum.h>
#include <AK/UFixedBigInt.h>

AK_MAKE_ARBITRARY_SIZED_ENUM(TestEnum, u8,
    Foo = TestEnum(1) << 0,
    Bar = TestEnum(1) << 1,
    Baz = TestEnum(1) << 2);

AK_MAKE_ARBITRARY_SIZED_ENUM(BigIntTestEnum, u128,
    Foo = BigIntTestEnum(1u) << 127u);

TEST_CASE(constructor)
{
    {
        constexpr TestEnum::Type test;
        static_assert(test.value() == 0);
    }
    {
        constexpr TestEnum::Type test { TestEnum::Foo | TestEnum::Baz };
        static_assert(test.value() == 0b101);
    }
    {
        constexpr BigIntTestEnum::Type test { BigIntTestEnum::Foo };
        static_assert(test.value() == u128(1u) << 127u);
    }
}

TEST_CASE(bitwise_or)
{
    {
        TestEnum::Type test;
        EXPECT_EQ(test.value(), 0);
        test |= TestEnum::Foo;
        EXPECT_EQ(test.value(), 0b001);
        test |= TestEnum::Bar;
        EXPECT_EQ(test.value(), 0b011);
        test |= TestEnum::Baz;
        EXPECT_EQ(test.value(), 0b111);
    }
    {
        BigIntTestEnum::Type test;
        EXPECT_EQ(test.value(), 0u);
        test |= BigIntTestEnum::Foo;
        EXPECT_EQ(test.value(), u128(1u) << 127u);
    }
}

TEST_CASE(bitwise_and)
{
    {
        TestEnum::Type test { 0b111 };
        EXPECT_EQ(test.value(), 0b111);
        test &= TestEnum::Foo;
        EXPECT_EQ(test.value(), 0b001);
    }
    {
        BigIntTestEnum::Type test { u128(1u) << 127u | u128(1u) << 126u };
        EXPECT_EQ(test.value(), u128(1u) << 127u | u128(1u) << 126u);
        test &= BigIntTestEnum::Foo;
        EXPECT_EQ(test.value(), u128(1u) << 127u);
    }
}

TEST_CASE(bitwise_xor)
{
    {
        TestEnum::Type test { 0b111 };
        EXPECT_EQ(test.value(), 0b111);
        test ^= TestEnum::Foo;
        EXPECT_EQ(test.value(), 0b110);
    }
    {
        BigIntTestEnum::Type test { u128(1u) << 127u | 1u };
        EXPECT_EQ(test.value(), u128(1u) << 127u | 1u);
        test ^= BigIntTestEnum::Foo;
        EXPECT_EQ(test.value(), 1u);
    }
}

TEST_CASE(has_flag)
{
    {
        TestEnum::Type test;
        test |= TestEnum::Foo;
        EXPECT(test.has_flag(TestEnum::Foo));
        EXPECT(!test.has_flag(TestEnum::Bar));
        EXPECT(!test.has_flag(TestEnum::Baz));
        EXPECT(!test.has_flag(TestEnum::Foo | TestEnum::Bar | TestEnum::Baz));
    }
    {
        BigIntTestEnum::Type test;
        test |= BigIntTestEnum::Foo;
        EXPECT(test.has_flag(BigIntTestEnum::Foo));
    }
}

TEST_CASE(has_any_flag)
{
    {
        TestEnum::Type test;
        test |= TestEnum::Foo;
        EXPECT(test.has_any_flag(TestEnum::Foo));
        EXPECT(!test.has_any_flag(TestEnum::Bar));
        EXPECT(!test.has_any_flag(TestEnum::Baz));
        EXPECT(test.has_any_flag(TestEnum::Foo | TestEnum::Bar | TestEnum::Baz));
    }
    {
        BigIntTestEnum::Type test;
        test |= BigIntTestEnum::Foo;
        EXPECT(test.has_any_flag(BigIntTestEnum::Foo));
    }
}

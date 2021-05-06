/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/StdLibExtras.h>
#include <AK/TypeList.h>

#define STATIC_EXPECT_EQ(lhs, rhs) \
    static_assert(IsSame<lhs, rhs>, "");

#define STATIC_EXPECT_FALSE(Expression) \
    static_assert(!Expression, "");

#define STATIC_EXPECT_TRUE(Expression) \
    static_assert(Expression, "");

#define EXPECT_TRAIT_TRUE(trait, ...)                                     \
    for_each_type<TypeList<__VA_ARGS__>>([]<typename T>(TypeWrapper<T>) { \
        STATIC_EXPECT_TRUE(trait<T>);                                     \
    })

#define EXPECT_TRAIT_FALSE(trait, ...)                                    \
    for_each_type<TypeList<__VA_ARGS__>>([]<typename T>(TypeWrapper<T>) { \
        STATIC_EXPECT_FALSE(trait<T>);                                    \
    })

#define EXPECT_EQ_WITH_TRAIT(trait, ListA, ListB)                                                   \
    for_each_type_zipped<ListA, ListB>([]<typename A, typename B>(TypeWrapper<A>, TypeWrapper<B>) { \
        STATIC_EXPECT_EQ(trait<A>, B);                                                              \
    })

struct Empty {
};

enum class Enummer : u8 {
    Dummmy,
};

TEST_CASE(FundamentalTypeClassification)
{
    EXPECT_TRAIT_TRUE(IsVoid, void);
    EXPECT_TRAIT_FALSE(IsVoid, int, Empty, std::nullptr_t);

    EXPECT_TRAIT_TRUE(IsNullPointer, std::nullptr_t);
    EXPECT_TRAIT_FALSE(IsNullPointer, void, int, Empty, decltype(0));

    EXPECT_TRAIT_TRUE(IsFloatingPoint, float, double, long double);
    EXPECT_TRAIT_FALSE(IsFloatingPoint, int, Empty, std::nullptr_t, void);

    EXPECT_TRAIT_TRUE(IsArithmetic, float, double, long double, bool, size_t);
    EXPECT_TRAIT_TRUE(IsArithmetic, char, signed char, unsigned char, char8_t, char16_t, char32_t);
    EXPECT_TRAIT_TRUE(IsArithmetic, short, int, long, long long);
    EXPECT_TRAIT_TRUE(IsArithmetic, unsigned short, unsigned int, unsigned long, unsigned long long);

    EXPECT_TRAIT_FALSE(IsArithmetic, void, std::nullptr_t, Empty);

    EXPECT_TRAIT_TRUE(IsFundamental, void, std::nullptr_t);
    EXPECT_TRAIT_TRUE(IsFundamental, float, double, long double, bool, size_t);
    EXPECT_TRAIT_TRUE(IsFundamental, char, signed char, unsigned char, char8_t, char16_t, char32_t);
    EXPECT_TRAIT_TRUE(IsFundamental, short, int, long, long long);
    EXPECT_TRAIT_TRUE(IsFundamental, unsigned short, unsigned int, unsigned long, unsigned long long);

    EXPECT_TRAIT_FALSE(IsFundamental, Empty, int*, int&);

    EXPECT_TRAIT_FALSE(IsSigned, unsigned);
    EXPECT_TRAIT_FALSE(IsSigned, unsigned short);
    EXPECT_TRAIT_FALSE(IsSigned, unsigned char);
    EXPECT_TRAIT_FALSE(IsSigned, unsigned long);
    EXPECT_TRAIT_TRUE(IsSigned, int);
    EXPECT_TRAIT_TRUE(IsSigned, short);
    EXPECT_TRAIT_TRUE(IsSigned, long);

    EXPECT_TRAIT_TRUE(IsUnsigned, unsigned);
    EXPECT_TRAIT_TRUE(IsUnsigned, unsigned short);
    EXPECT_TRAIT_TRUE(IsUnsigned, unsigned char);
    EXPECT_TRAIT_TRUE(IsUnsigned, unsigned long);
    EXPECT_TRAIT_FALSE(IsUnsigned, int);
    EXPECT_TRAIT_FALSE(IsUnsigned, short);
    EXPECT_TRAIT_FALSE(IsUnsigned, long);

    EXPECT_TRAIT_TRUE(IsEnum, Enummer);
    EXPECT_TRAIT_FALSE(IsEnum, Empty);
    EXPECT_TRAIT_FALSE(IsEnum, int);
    EXPECT_TRAIT_FALSE(IsEnum, void);
    EXPECT_TRAIT_FALSE(IsEnum, std::nullptr_t);
}

TEST_CASE(AddConst)
{
    // clang-format off
    using NoConstList =  TypeList<int,       const int, Empty,       const Empty>;
    using YesConstList = TypeList<const int, const int, const Empty, const Empty>;
    // clang-format on

    EXPECT_EQ_WITH_TRAIT(AddConst, NoConstList, YesConstList);
}

TEST_CASE(UnderlyingType)
{
    using Type = UnderlyingType<Enummer>;

    STATIC_EXPECT_EQ(Type, u8);
}

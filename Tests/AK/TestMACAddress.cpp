/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/MACAddress.h>
#include <AK/Types.h>

TEST_CASE(should_default_construct)
{
    constexpr MACAddress sut {};
    static_assert(sut.is_zero());
    EXPECT(sut.is_zero());
}

TEST_CASE(should_braces_construct)
{
    constexpr MACAddress sut { 1, 2, 3, 4, 5, 6 };
    static_assert(!sut.is_zero());
    EXPECT(!sut.is_zero());
}

TEST_CASE(should_construct_from_6_octets)
{
    constexpr MACAddress sut(1, 2, 3, 4, 5, 6);
    static_assert(!sut.is_zero());
    EXPECT(!sut.is_zero());
}

TEST_CASE(should_provide_read_access_to_octet_by_index)
{
    constexpr auto is_all_expected = [](auto& sut) {
        for (auto i = 0u; i < sizeof(MACAddress); ++i) {
            if (sut[i] != i + 1) {
                return false;
            }
        }
        return true;
    };

    constexpr MACAddress sut(1, 2, 3, 4, 5, 6);

    static_assert(is_all_expected(sut));

    for (auto i = 0u; i < sizeof(MACAddress); ++i) {
        EXPECT_EQ(i + 1, sut[i]);
    }
}

TEST_CASE(should_provide_write_access_to_octet_by_index)
{
    constexpr auto sut = [] {
        MACAddress m {};
        for (auto i = 0u; i < sizeof(MACAddress); ++i) {
            m[i] = i + 1;
        }
        return m;
    }();

    constexpr MACAddress expected(1, 2, 3, 4, 5, 6);

    static_assert(expected == sut);
}

TEST_CASE(should_equality_compare)
{
    constexpr MACAddress a(1, 2, 3, 4, 5, 6);
    constexpr MACAddress b(1, 2, 3, 42, 5, 6);

    static_assert(a == a);
    static_assert(a != b);

    EXPECT(a == a);
    EXPECT(a != b);
}

TEST_CASE(should_string_format)
{
    MACAddress sut(1, 2, 3, 4, 5, 6);
    EXPECT_EQ("01:02:03:04:05:06", sut.to_byte_string());
}

TEST_CASE(should_make_mac_address_from_string_numbers)
{
    auto const sut = MACAddress::from_string("01:02:03:04:05:06"sv);

    EXPECT(sut.has_value());
    EXPECT_EQ(1, sut.value()[0]);
    EXPECT_EQ(2, sut.value()[1]);
    EXPECT_EQ(3, sut.value()[2]);
    EXPECT_EQ(4, sut.value()[3]);
    EXPECT_EQ(5, sut.value()[4]);
    EXPECT_EQ(6, sut.value()[5]);
}

TEST_CASE(should_make_mac_address_from_string_letters)
{
    auto const sut = MACAddress::from_string("de:ad:be:ee:ee:ef"sv);

    EXPECT(sut.has_value());
    EXPECT_EQ(u8 { 0xDE }, sut.value()[0]);
    EXPECT_EQ(u8 { 0xAD }, sut.value()[1]);
    EXPECT_EQ(u8 { 0xBE }, sut.value()[2]);
    EXPECT_EQ(u8 { 0xEE }, sut.value()[3]);
    EXPECT_EQ(u8 { 0xEE }, sut.value()[4]);
    EXPECT_EQ(u8 { 0xEF }, sut.value()[5]);
}

TEST_CASE(should_make_empty_optional_from_bad_string)
{
    auto const sut = MACAddress::from_string("bad string"sv);

    EXPECT(!sut.has_value());
}

TEST_CASE(should_make_empty_optional_from_out_of_range_values)
{
    auto const sut = MACAddress::from_string("de:ad:be:ee:ee:fz"sv);

    EXPECT(!sut.has_value());
}

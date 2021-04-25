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
    EXPECT_EQ("01:02:03:04:05:06", sut.to_string());
}

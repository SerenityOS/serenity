/*
 * Copyright (c) 2020, the SerenityOS developers.
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

#include <AK/Endian.h>
#include <AK/IPv4Address.h>

TEST_CASE(should_default_contructor_with_0s)
{
    constexpr IPv4Address addr {};

    static_assert(addr.is_zero());

    EXPECT(addr.is_zero());
}

TEST_CASE(should_construct_from_c_array)
{
    constexpr auto addr = [] {
        const u8 a[4] = { 1, 2, 3, 4 };
        return IPv4Address(a);
    }();

    static_assert(!addr.is_zero());

    EXPECT(!addr.is_zero());
}

TEST_CASE(should_construct_from_u32)
{
    constexpr auto addr = [] {
        const NetworkOrdered<u32> a = 0x11'22'33'44;
        return IPv4Address(a);
    }();

    static_assert(!addr.is_zero());

    EXPECT(!addr.is_zero());
}

TEST_CASE(should_get_octets_by_byte_offset)
{
    constexpr IPv4Address addr(1, 25, 39, 42);

    static_assert(1 == addr[0]);
    static_assert(25 == addr[1]);
    static_assert(39 == addr[2]);
    static_assert(42 == addr[3]);

    EXPECT_EQ(1, addr[0]);
    EXPECT_EQ(25, addr[1]);
    EXPECT_EQ(39, addr[2]);
    EXPECT_EQ(42, addr[3]);
}

TEST_CASE(should_convert_to_string)
{
    constexpr IPv4Address addr(1, 25, 39, 42);

    EXPECT_EQ("1.25.39.42", addr.to_string());
}

TEST_CASE(should_make_ipv4_address_from_string)
{
    const auto addr = IPv4Address::from_string("192.168.0.1");

    EXPECT(addr.has_value());
    EXPECT_EQ(192, addr.value()[0]);
    EXPECT_EQ(168, addr.value()[1]);
    EXPECT_EQ(0, addr.value()[2]);
    EXPECT_EQ(1, addr.value()[3]);
}

TEST_CASE(should_make_empty_optional_from_bad_string)
{
    const auto addr = IPv4Address::from_string("bad string");

    EXPECT(!addr.has_value());
}

TEST_CASE(should_make_empty_optional_from_out_of_range_values)
{
    const auto addr = IPv4Address::from_string("192.168.0.500");

    EXPECT(!addr.has_value());
}

TEST_CASE(should_fill_d_octet_from_1_part)
{
    const auto addr = IPv4Address::from_string("1");

    EXPECT(addr.has_value());
    EXPECT_EQ(0, addr.value()[0]);
    EXPECT_EQ(0, addr.value()[1]);
    EXPECT_EQ(0, addr.value()[2]);
    EXPECT_EQ(1, addr.value()[3]);
}

TEST_CASE(should_fill_a_and_d_octets_from_2_parts)
{
    const auto addr = IPv4Address::from_string("192.1");

    EXPECT(addr.has_value());
    EXPECT_EQ(192, addr.value()[0]);
    EXPECT_EQ(0, addr.value()[1]);
    EXPECT_EQ(0, addr.value()[2]);
    EXPECT_EQ(1, addr.value()[3]);
}

TEST_CASE(should_fill_a_b_d_octets_from_3_parts)
{
    const auto addr = IPv4Address::from_string("192.168.1");

    EXPECT(addr.has_value());
    EXPECT_EQ(192, addr.value()[0]);
    EXPECT_EQ(168, addr.value()[1]);
    EXPECT_EQ(0, addr.value()[2]);
    EXPECT_EQ(1, addr.value()[3]);
}

TEST_CASE(should_convert_to_in_addr_t)
{
    constexpr IPv4Address addr(1, 2, 3, 4);

    static_assert(0x04'03'02'01u == addr.to_in_addr_t());

    EXPECT_EQ(0x04'03'02'01u, addr.to_in_addr_t());
}

TEST_CASE(should_convert_to_u32)
{
    constexpr IPv4Address addr(1, 2, 3, 4);

    static_assert(0x04'03'02'01u == addr.to_in_addr_t());

    EXPECT_EQ(0x04'03'02'01u, addr.to_u32());
}

TEST_CASE(should_compare)
{
    constexpr IPv4Address addr_a(1, 2, 3, 4);
    constexpr IPv4Address addr_b(1, 2, 3, 5);

    static_assert(addr_a != addr_b);
    static_assert(addr_a == addr_a);

    EXPECT(addr_a != addr_b);
    EXPECT(addr_a == addr_a);
}

TEST_MAIN(IPv4Address)

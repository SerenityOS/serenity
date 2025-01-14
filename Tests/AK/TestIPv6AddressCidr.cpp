/*
 * Copyright (c) 2024, famfo <famfo@famfo.xyz>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/IPv6Address.h>
#include <AK/IpAddressCidr.h>

TEST_CASE(sanity_check)
{
    auto address_result = IPv6AddressCidr::create(IPv6Address({ 0x20, 0x01, 0x0d, 0xb8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 }), 128);

    IPv6AddressCidr address = TRY_OR_FAIL(address_result);

    EXPECT_EQ(address.length(), 128l);
    EXPECT_EQ(address.ip_address(), IPv6Address({ 0x20, 0x01, 0x0d, 0xb8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 }));
    EXPECT_EQ(address.first_address_of_subnet(), IPv6Address({ 0x20, 0x01, 0x0d, 0xb8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 }));
    EXPECT_EQ(address.last_address_of_subnet(), IPv6Address({ 0x20, 0x01, 0x0d, 0xb8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 }));
    EXPECT(address.contains(IPv6Address({ 0x20, 0x01, 0x0d, 0xb8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 })));
}
TEST_CASE(should_fail_on_invalid_length)
{
    auto address_result = IPv6AddressCidr::create(IPv6Address({ 0x20, 0x01, 0x0d, 0xb8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 }), 129);
    EXPECT(address_result.is_error());
    EXPECT_EQ(address_result.error(), AK::Details::IPAddressCidrError::CidrTooLong);
}

TEST_CASE(should_find_first_in_subnet)
{
    IPv6AddressCidr address = IPv6AddressCidr::create(IPv6Address({ 0x20, 0x01, 0x0d, 0xb8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 }), 48).release_value();
    EXPECT_EQ(address.first_address_of_subnet(), IPv6Address({ 0x20, 0x01, 0x0d, 0xb8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }));
}

TEST_CASE(should_find_last_in_subnet)
{
    IPv6AddressCidr address = IPv6AddressCidr::create(IPv6Address({ 0x20, 0x01, 0x0d, 0xb8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 }), 48).release_value();
    EXPECT_EQ(address.last_address_of_subnet(), IPv6Address({ 0x20, 0x01, 0x0d, 0xb8, 0, 0, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff }));
}

TEST_CASE(should_contain_other)
{
    IPv6AddressCidr address = IPv6AddressCidr::create(IPv6Address({ 0x20, 0x01, 0x0d, 0xb8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 }), 48).release_value();
    EXPECT(address.contains(IPv6Address({ 0x20, 0x01, 0x0d, 0xb8, 0, 0, 0xff, 0xff, 0, 0, 0, 0, 0, 0, 0, 1 })));
}

TEST_CASE(should_set_address)
{
    IPv6AddressCidr address = IPv6AddressCidr::create(IPv6Address({ 0x20, 0x01, 0x0d, 0xb8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 }), 48).release_value();
    EXPECT_EQ(address.ip_address(), IPv6Address({ 0x20, 0x01, 0x0d, 0xb8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 }));

    address.set_ip_address(IPv6Address({ 0x20, 0x01, 0x0d, 0xb8, 0, 0, 0xff, 0xff, 0, 0, 0, 0, 0, 0, 0, 1 }));
    EXPECT_EQ(address.ip_address(), IPv6Address({ 0x20, 0x01, 0x0d, 0xb8, 0, 0, 0xff, 0xff, 0, 0, 0, 0, 0, 0, 0, 1 }));
}

TEST_CASE(should_set_length)
{
    IPv6AddressCidr address = IPv6AddressCidr::create(IPv6Address({ 0x20, 0x01, 0x0d, 0xb8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 }), 48).release_value();
    EXPECT_EQ(address.length(), 48l);

    EXPECT(!address.set_length(64).is_error());
    EXPECT_EQ(address.length(), 64l);
}

TEST_CASE(should_not_set_invalid_length)
{
    IPv6AddressCidr address = IPv6AddressCidr::create(IPv6Address({ 0x20, 0x01, 0x0d, 0xb8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 }), 48).release_value();
    EXPECT(address.set_length(129).is_error());
}

TEST_CASE(should_not_contain_other)
{
    IPv6AddressCidr address = IPv6AddressCidr::create(IPv6Address({ 0x20, 0x01, 0x0d, 0xb8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 }), 48).release_value();
    EXPECT(!address.contains(IPv6Address({ 0x20, 0x01, 0x0d, 0xb8, 0xff, 0xff, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 })));
}

TEST_CASE(should_contain_this)
{
    IPv6AddressCidr address = IPv6AddressCidr::create(IPv6Address({ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }), 0).release_value();
    EXPECT(address.contains(IPv6Address({ 0x20, 0x01, 0x0d, 0xb8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 })));
}

TEST_CASE(should_parse_cidr_string)
{
    auto address = IPv6AddressCidr::from_string("2001:db8::1/48"sv);
    EXPECT(!address.is_error());
    EXPECT_EQ(address.release_value(), IPv6AddressCidr::create(IPv6Address({ 0x20, 0x01, 0x0d, 0xb8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 }), 48).release_value());
}

TEST_CASE(should_not_parse_invalid_address)
{
    auto address = IPv6AddressCidr::from_string("200f:db8:::1/48"sv);
    EXPECT(address.is_error());
    EXPECT_EQ(address.error(), AK::Details::IPAddressCidrError::StringParsingFailed);
}

TEST_CASE(should_not_parse_invalid_length)
{
    auto address = IPv6AddressCidr::from_string("2001:db8::1/129"sv);
    EXPECT(address.is_error());
    EXPECT_EQ(address.error(), AK::Details::IPAddressCidrError::CidrTooLong);
}

TEST_CASE(should_not_parse_invalid_cidr_format)
{
    auto address = IPv6AddressCidr::from_string("2001:db8::1"sv);
    EXPECT(address.is_error());
    EXPECT_EQ(address.error(), AK::Details::IPAddressCidrError::StringParsingFailed);
}

TEST_CASE(should_format_cidr)
{
    auto address = IPv6AddressCidr::create(IPv6Address({ 0x20, 0x01, 0x0d, 0xb8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 }), 48).release_value().to_string().release_value();
    EXPECT_EQ(address.bytes_as_string_view(), "2001:db8::1/48"sv);
}

TEST_CASE(unaligned_mask)
{
    auto address = IPv6AddressCidr::from_string("2001:db8:0:80::1/57"sv).release_value();
    EXPECT_EQ(address.first_address_of_subnet(), IPv6Address::from_string("2001:db8:0:80::"sv).release_value());
    EXPECT_EQ(address.last_address_of_subnet(), IPv6Address::from_string("2001:db8:0:ff:ffff:ffff:ffff:ffff"sv).release_value());
}

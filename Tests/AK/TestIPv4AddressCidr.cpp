/*
 * Copyright (c) 2024, famfo <famfo@famfo.xyz>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/IPv4Address.h>
#include <AK/IpAddressCidr.h>

TEST_CASE(sanity_check)
{
    auto address_result = IPv4AddressCidr::create(IPv4Address(192, 0, 2, 1), 32);

    IPv4AddressCidr address = TRY_OR_FAIL(address_result);

    EXPECT_EQ(address.length(), 32l);
    EXPECT_EQ(address.ip_address(), IPv4Address(192, 0, 2, 1));
    EXPECT_EQ(address.first_address_of_subnet(), IPv4Address(192, 0, 2, 1));
    EXPECT_EQ(address.last_address_of_subnet(), IPv4Address(192, 0, 2, 1));
    EXPECT_EQ(address.netmask(), IPv4Address(255, 255, 255, 255));
    EXPECT(address.contains(IPv4Address(192, 0, 2, 1)));
}

TEST_CASE(should_fail_on_invalid_length)
{
    auto address_result = IPv4AddressCidr::create(IPv4Address(192, 0, 2, 1), 33);
    EXPECT(address_result.is_error());
    EXPECT_EQ(address_result.error(), AK::Details::IPAddressCidrError::CidrTooLong);
}

TEST_CASE(should_find_first_in_subnet)
{
    IPv4AddressCidr address = IPv4AddressCidr::create(IPv4Address(192, 0, 2, 1), 24).release_value();
    EXPECT_EQ(address.first_address_of_subnet(), IPv4Address(192, 0, 2, 0));
}

TEST_CASE(should_find_last_in_subnet)
{
    IPv4AddressCidr address = IPv4AddressCidr::create(IPv4Address(192, 0, 2, 1), 24).release_value();
    EXPECT_EQ(address.last_address_of_subnet(), IPv4Address(192, 0, 2, 255));
}

TEST_CASE(should_return_matching_netmask)
{
    IPv4AddressCidr address = IPv4AddressCidr::create(IPv4Address(192, 0, 2, 1), 24).release_value();
    EXPECT_EQ(address.netmask(), IPv4Address(255, 255, 255, 0));
}

TEST_CASE(should_contain_other)
{
    IPv4AddressCidr address = IPv4AddressCidr::create(IPv4Address(192, 0, 2, 1), 24).release_value();
    EXPECT(address.contains(IPv4Address(192, 0, 2, 100)));
}

TEST_CASE(should_set_address)
{
    IPv4AddressCidr address = IPv4AddressCidr::create(IPv4Address(192, 0, 2, 1), 8).release_value();
    EXPECT_EQ(address.ip_address(), IPv4Address(192, 0, 2, 1));

    address.set_ip_address(IPv4Address(198, 51, 100, 1));
    EXPECT_EQ(address.ip_address(), IPv4Address(198, 51, 100, 1));
}

TEST_CASE(should_set_length)
{
    IPv4AddressCidr address = IPv4AddressCidr::create(IPv4Address(192, 0, 2, 1), 32).release_value();
    EXPECT_EQ(address.length(), 32l);

    EXPECT(!address.set_length(24).is_error());
    EXPECT_EQ(address.length(), 24l);
}

TEST_CASE(should_not_set_invalid_length)
{
    IPv4AddressCidr address = IPv4AddressCidr::create(IPv4Address(192, 0, 2, 1), 32).release_value();
    EXPECT(address.set_length(33).is_error());
}

TEST_CASE(should_not_contain_other)
{
    IPv4AddressCidr address = IPv4AddressCidr::create(IPv4Address(192, 0, 2, 1), 24).release_value();
    EXPECT(!address.contains(IPv4Address(198, 51, 100, 1)));
}

TEST_CASE(should_contain_this)
{
    IPv4AddressCidr address = IPv4AddressCidr::create(IPv4Address(0, 0, 0, 0), 0).release_value();
    EXPECT(address.contains(IPv4Address(192, 0, 2, 1)));
}

TEST_CASE(should_parse_cidr_string)
{
    auto address = IPv4AddressCidr::from_string("192.0.2.1/24"sv);
    EXPECT(!address.is_error());
    EXPECT_EQ(address.release_value(), IPv4AddressCidr::create(IPv4Address(192, 0, 2, 1), 24).release_value());
}

TEST_CASE(should_not_parse_invalid_address)
{
    auto address = IPv4AddressCidr::from_string("256.0.0.1/24"sv);
    EXPECT(address.is_error());
    EXPECT_EQ(address.error(), AK::Details::IPAddressCidrError::StringParsingFailed);
}

TEST_CASE(should_not_parse_invalid_length)
{
    auto address = IPv4AddressCidr::from_string("192.0.2.1/33"sv);
    EXPECT(address.is_error());
    EXPECT_EQ(address.error(), AK::Details::IPAddressCidrError::CidrTooLong);
}

TEST_CASE(should_not_parse_invalid_cidr_format)
{
    auto address = IPv4AddressCidr::from_string("192.0.2.1"sv);
    EXPECT(address.is_error());
    EXPECT_EQ(address.error(), AK::Details::IPAddressCidrError::StringParsingFailed);
}

TEST_CASE(should_format_cidr)
{
    auto address = IPv4AddressCidr(IPv4Address(192, 0, 2, 1), 24).to_string().release_value();
    EXPECT_EQ(address.bytes_as_string_view(), "192.0.2.1/24"sv);
}

TEST_CASE(unaligned_mask)
{
    auto address = IPv4AddressCidr::create(IPv4Address(192, 0, 0, 42), 27).release_value();
    EXPECT_EQ(address.first_address_of_subnet(), IPv4Address(192, 0, 0, 32));
    EXPECT_EQ(address.last_address_of_subnet(), IPv4Address(192, 0, 0, 63));
}

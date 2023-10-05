/*
 * Copyright (c) 2023, Tim Ledbetter  <timledbetter@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTLS/Certificate.h>
#include <LibTest/TestCase.h>

TEST_CASE(certificate_with_malformed_tbscertificate_should_fail_gracefully)
{
    Array<u8, 4> invalid_certificate_data { 0xB0, 0x02, 0x70, 0x00 };
    auto parse_result = TLS::Certificate::parse_certificate(invalid_certificate_data);
    EXPECT(parse_result.is_error());
}

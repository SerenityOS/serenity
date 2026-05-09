/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibSSH/IdentificationString.h>
#include <LibTest/TestCase.h>

TEST_CASE(valid)
{
    EXPECT(!SSH::validate_identification_string(SSH::PROTOCOL_STRING.bytes()).is_error());
    EXPECT(!SSH::validate_identification_string("SSH-2.0-OpenSSH_10.0p2 Ubuntu-5ubuntu5\r\n"sv.bytes()).is_error());

    EXPECT(!SSH::validate_identification_string("SSH-2.0-billsSSH_3.6.3q3\r\n"sv.bytes()).is_error());
}

TEST_CASE(invalid)
{
    // Empty / too short
    EXPECT(SSH::validate_identification_string(""sv.bytes()).is_error());
    EXPECT(SSH::validate_identification_string("SSH-2.0"sv.bytes()).is_error());

    // Missing CR/LF
    EXPECT(SSH::validate_identification_string("SSH-2.0-OpenSSH_10.0p2"sv.bytes()).is_error());
    EXPECT(SSH::validate_identification_string("SSH-2.0-OpenSSH_10.0p2\n"sv.bytes()).is_error());
    EXPECT(SSH::validate_identification_string("SSH-2.0-OpenSSH_10.0p2\r"sv.bytes()).is_error());

    // Wrong prefix
    EXPECT(SSH::validate_identification_string("SSHH-2.0-OpenSSH_10.0p2\r\n"sv.bytes()).is_error());
    EXPECT(SSH::validate_identification_string("SSH-3.0-OpenSSH_10.0p2\r\n"sv.bytes()).is_error());

    // Missing hyphen separators
    EXPECT(SSH::validate_identification_string("SSH2.0-OpenSSH_10.0p2\r\n"sv.bytes()).is_error());
    EXPECT(SSH::validate_identification_string("SSH-2.0OpenSSH_10.0p2\r\n"sv.bytes()).is_error());

    // Missing software version
    EXPECT(SSH::validate_identification_string("SSH-2.0-\r\n"sv.bytes()).is_error());

    // Excessively long line (>255 bytes)
    auto many_a = ByteString::repeated('A', 250);
    auto final_message = ByteString::formatted("SSH-2.0-{}\r\n"sv, many_a);
    EXPECT(SSH::validate_identification_string(final_message.bytes()).is_error());
}

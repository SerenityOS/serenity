/*
 * Copyright (c) 2023, Valtteri Koskivuori <vkoskiv@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <LibIMAP/MessageHeaderEncoding.h>
#include <LibTest/TestCase.h>

TEST_CASE(test_decode)
{
    auto decode_equal = [](StringView input, StringView expected) {
        auto decoded = MUST(IMAP::decode_rfc2047_encoded_words(input));
        EXPECT_EQ(StringView(decoded), StringView(expected));
    };

    // Underscores should end up as spaces
    decode_equal("=?utf-8?Q?Spaces_should_be_spaces_!?="sv, "Spaces should be spaces !"sv);

    // RFC 2047 Section 8 "Examples", https://datatracker.ietf.org/doc/html/rfc2047#section-8

    decode_equal("=?ISO-8859-1?Q?a?="sv, "a"sv);

    decode_equal("=?ISO-8859-1?Q?a?= b"sv, "a b"sv);

    // White space between adjacent 'encoded-word's is not displayed.
    decode_equal("=?ISO-8859-1?Q?a?= =?ISO-8859-1?Q?b?="sv, "ab"sv);

    // Even multiple SPACEs between 'encoded-word's are ignored for the purpose of display.
    decode_equal("=?ISO-8859-1?Q?a?=  =?ISO-8859-1?Q?b?="sv, "ab"sv);
    decode_equal("=?ISO-8859-1?Q?a?=        =?ISO-8859-1?Q?b?=    =?ISO-8859-1?Q?c?==?ISO-8859-1?Q?d?="sv, "abcd"sv);

    // Any amount of linear-space-white between 'encoded-word's, even if it includes a CRLF followed by one or more SPACEs, is ignored for the purposes of display.
    decode_equal("=?utf-8?Q?a?=\r\n=?utf-8?Q?b?=    \r\n=?utf-8?Q?c?=\r\n      =?utf-8?Q?d?="sv, "abcd"sv);

    // In order to cause a SPACE to be displayed within a portion of encoded text, the SPACE MUST be encoded as part of the 'encoded-word'.
    decode_equal("=?ISO-8859-1?Q?a_b?="sv, "a b"sv);

    // In order to cause a SPACE to be displayed between two strings of encoded text, the SPACE MAY be encoded as part of one of the 'encoded-word's.
    decode_equal("=?ISO-8859-1?Q?a?= =?ISO-8859-2?Q?_b?="sv, "a b"sv);

    // More examples from the RFC document, a nice mix of different charsets & encodings.
    auto long_input = "From: =?US-ASCII?Q?Keith_Moore?= <moore@cs.utk.edu>"
                      "To: =?ISO-8859-1?Q?Keld_J=F8rn_Simonsen?= <keld@dkuug.dk>"
                      "CC: =?ISO-8859-1?Q?Andr=E9?= Pirard <PIRARD@vm1.ulg.ac.be>"
                      "Subject: =?ISO-8859-1?B?SWYgeW91IGNhbiByZWFkIHRoaXMgeW8=?="
                      "=?ISO-8859-2?B?dSB1bmRlcnN0YW5kIHRoZSBleGFtcGxlLg==?="sv;

    auto long_expected = "From: Keith Moore <moore@cs.utk.edu>"
                         "To: Keld Jørn Simonsen <keld@dkuug.dk>"
                         "CC: André Pirard <PIRARD@vm1.ulg.ac.be>"
                         "Subject: If you can read this you understand the example."sv;
    decode_equal(long_input, long_expected);
}

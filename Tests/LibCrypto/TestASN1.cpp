/*
 * Copyright (c) 2022, Ben Wiederhake <BenWiederhake.GitHub@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <LibCrypto/ASN1/ASN1.h>
#include <LibTest/TestCase.h>

#define EXPECT_DATETIME(sv, y, mo, d, h, mi, s) \
    EXPECT_EQ(Crypto::ASN1::parse_utc_time(sv).value(), Core::DateTime::create(y, mo, d, h, mi, s))

TEST_CASE(test_utc_boring)
{
    // YYMMDDhhmm[ss]Z
    EXPECT_DATETIME("010101010101Z"sv, 2001, 1, 1, 1, 1, 1);
    EXPECT_DATETIME("010203040506Z"sv, 2001, 2, 3, 4, 5, 6);
    EXPECT_DATETIME("020406081012Z"sv, 2002, 4, 6, 8, 10, 12);
    EXPECT_DATETIME("0204060810Z"sv, 2002, 4, 6, 8, 10, 0);
    EXPECT_DATETIME("220911220000Z"sv, 2022, 9, 11, 22, 0, 0);
}

TEST_CASE(test_utc_year_rollover)
{
    // YYMMDDhhmm[ss]Z
    EXPECT_DATETIME("000101010101Z"sv, 2000, 1, 1, 1, 1, 1);
    EXPECT_DATETIME("010101010101Z"sv, 2001, 1, 1, 1, 1, 1);
    EXPECT_DATETIME("020101010101Z"sv, 2002, 1, 1, 1, 1, 1);
    // ...
    EXPECT_DATETIME("480101010101Z"sv, 2048, 1, 1, 1, 1, 1);
    EXPECT_DATETIME("490101010101Z"sv, 2049, 1, 1, 1, 1, 1);
    // This Y2050-problem is hardcoded in the spec. Oh no.
    EXPECT_DATETIME("500101010101Z"sv, 1950, 1, 1, 1, 1, 1);
    EXPECT_DATETIME("510101010101Z"sv, 1951, 1, 1, 1, 1, 1);
    // ...
    EXPECT_DATETIME("970101010101Z"sv, 1997, 1, 1, 1, 1, 1);
    EXPECT_DATETIME("980101010101Z"sv, 1998, 1, 1, 1, 1, 1);
    EXPECT_DATETIME("990101010101Z"sv, 1999, 1, 1, 1, 1, 1);
}

TEST_CASE(test_utc_offset)
{
    // YYMMDDhhmm[ss](+|-)hhmm
    // We don't yet support storing the offset anywhere and instead just assume that the offset is just +0000.
    EXPECT_DATETIME("010101010101+0000"sv, 2001, 1, 1, 1, 1, 1);
    EXPECT_DATETIME("010203040506+0000"sv, 2001, 2, 3, 4, 5, 6);
    EXPECT_DATETIME("020406081012+0000"sv, 2002, 4, 6, 8, 10, 12);
    EXPECT_DATETIME("0204060810+0000"sv, 2002, 4, 6, 8, 10, 0);
    EXPECT_DATETIME("220911220000+0000"sv, 2022, 9, 11, 22, 0, 0);
    // Designed to fail once we support offsets:
    EXPECT_DATETIME("220911220000+0600"sv, 2022, 9, 11, 22, 0, 0);
}

TEST_CASE(test_utc_missing_z)
{
    // YYMMDDhhmm[ss]
    // We don't actually need to parse this correctly; rejecting these inputs is fine.
    // This test just makes sure that we don't crash.
    (void)Crypto::ASN1::parse_utc_time("010101010101"sv);
    (void)Crypto::ASN1::parse_utc_time("010203040506"sv);
    (void)Crypto::ASN1::parse_utc_time("020406081012"sv);
    (void)Crypto::ASN1::parse_utc_time("0204060810"sv);
    (void)Crypto::ASN1::parse_utc_time("220911220000"sv);
}

/*
 * Copyright (c) 2023, Tim Schumacher <timschumi@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/Error.h>
#include <AK/ErrorPayloadWithEnum.h>
#include <AK/Format.h>

enum TestEnumA {
    EntryA1,
};

enum TestEnumB {
    EntryB1,
    EntryB2,
};

template<Enum T>
struct AK::Formatter<T> : AK::Formatter<AK::FormatString> {
    ErrorOr<void> format(AK::FormatBuilder& builder, T value)
    {
        return Formatter<FormatString>::format(builder, "{}"sv, to_underlying(value));
    }
};

TEST_CASE(custom_error_basic)
{
    auto error_a1 = Error::from_error_payload(ErrorPayloadWithEnum<TestEnumA>(TestEnumA::EntryA1));
    auto error_b1 = Error::from_error_payload(ErrorPayloadWithEnum<TestEnumB>(TestEnumB::EntryB1));
    auto error_b2 = Error::from_error_payload(ErrorPayloadWithEnum<TestEnumB>(TestEnumB::EntryB2));
    auto error_b2_v2 = Error::from_error_payload(ErrorPayloadWithEnum<TestEnumB>(TestEnumB::EntryB2));

    // Check that everything is convertible to `CustomErrorBase`.
    // This is needed for any type-agnostic functions to access virtual methods.
    EXPECT(error_a1.error_payload<ErrorPayload>().has_value());
    EXPECT(error_b1.error_payload<ErrorPayload>().has_value());
    EXPECT(error_b2.error_payload<ErrorPayload>().has_value());

    // Check that the error contents are only convertible to their respective types.
    EXPECT(error_a1.error_payload<ErrorPayloadWithEnum<TestEnumA>>().has_value());
    EXPECT(!error_a1.error_payload<ErrorPayloadWithEnum<TestEnumB>>().has_value());
    EXPECT(error_b1.error_payload<ErrorPayloadWithEnum<TestEnumB>>().has_value());
    EXPECT(!error_b1.error_payload<ErrorPayloadWithEnum<TestEnumA>>().has_value());

    // Check that the error codes get through the conversion unscathed.
    EXPECT_EQ(error_a1.error_payload<ErrorPayloadWithEnum<TestEnumA>>(), TestEnumA::EntryA1);
    EXPECT_EQ(error_b1.error_payload<ErrorPayloadWithEnum<TestEnumB>>(), TestEnumB::EntryB1);
    EXPECT_EQ(error_b2.error_payload<ErrorPayloadWithEnum<TestEnumB>>(), TestEnumB::EntryB2);

    // Ensure that comparing against values from a different error type counts as non-matching.
    EXPECT_NE(error_a1.error_payload<ErrorPayloadWithEnum<TestEnumB>>(), TestEnumB::EntryB1);

    // Ensure that comparisons of the overarching error type works as expected.
    // Note: Errors don't allow being copied (which the `EXPECT_*` macros do), so we have to compare them manually.
    if (error_a1 == error_b1)
        FAIL("error_a1 and error_b1 are equal according to the comparison function");
    if (error_b1 == error_b2)
        FAIL("error_b1 and error_b2 are equal according to the comparison function");
    if (error_b2 != error_b2_v2)
        FAIL("error_b2 and error_b2_v2 are not equal according to the comparison function");
}

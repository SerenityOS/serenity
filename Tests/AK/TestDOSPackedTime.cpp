/*
 * Copyright (c) 2025, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/DOSPackedTime.h>
#include <AK/Time.h>

TEST_CASE(test_date_serialization)
{
    auto matches = [](i32 year, u8 month, u8 day) -> bool {
        DOSPackedDate date = {};
        date.year = year - 1980;
        date.month = month;
        date.day = day;

        return MUST(to_packed_dos_date(UnixDateTime::from_unix_time_parts(year, month, day, 0, 0, 0, 0))).value == date.value;
    };

    EXPECT(matches(1980, 1, 1));
    EXPECT(matches(2000, 1, 1));
    EXPECT(matches(2016, 2, 29));
    EXPECT(matches(2016, 3, 1));
    EXPECT(matches(2017, 2, 28));
    EXPECT(matches(2017, 3, 1));
    EXPECT(matches(2018, 10, 10));
    EXPECT(matches(2025, 4, 26));
}

TEST_CASE(test_time_serialization)
{
    auto matches = [](u8 hour, u8 minute, u8 second) -> bool {
        DOSPackedTime time = {};
        time.hour = hour;
        time.minute = minute;
        // This can only handle 2-second intervals, since it's stored in only 5 bits.
        time.second = second / 2;

        return MUST(to_packed_dos_time(UnixDateTime::from_unix_time_parts(2025, 1, 1, hour, minute, second, 0))).value == time.value;
    };

    auto test_hour = [&matches](u8 hour) -> bool {
        for (size_t minute = 0; minute < 60; ++minute) {
            for (size_t second = 0; second < 60; ++second) {
                if (!matches(hour, minute, second))
                    return false;
            }
        }
        return true;
    };

    EXPECT(test_hour(0));
    EXPECT(test_hour(23));
}

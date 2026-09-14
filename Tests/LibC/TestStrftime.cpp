/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>
#include <time.h>

#pragma GCC diagnostic ignored "-Wformat-y2k"

TEST_CASE(strftime)
{
    char str[200];
    tm t;
    t.tm_sec = 7;
    t.tm_min = 53;
    t.tm_hour = 11;
    t.tm_mday = 10;
    t.tm_mon = 9;
    t.tm_year = 118;
    t.tm_wday = 3;
    t.tm_yday = 282;
    t.tm_isdst = -1;

    EXPECT(strftime(str, sizeof str, "The short name of the day of the week is %a.", &t));
    EXPECT_EQ("The short name of the day of the week is Wed."sv, str);

    EXPECT(strftime(str, sizeof str, "The full name of the day of the week is %A.", &t));
    EXPECT_EQ("The full name of the day of the week is Wednesday."sv, str);

    EXPECT(strftime(str, sizeof str, "The short name of the month is %b.", &t));
    EXPECT_EQ("The short name of the month is Oct."sv, str);

    EXPECT(strftime(str, sizeof str, "The full name of the month is %B.", &t));
    EXPECT_EQ("The full name of the month is October."sv, str);

    EXPECT(strftime(str, sizeof str, "The century is %C.", &t));
    EXPECT_EQ("The century is 20."sv, str);

    EXPECT(strftime(str, sizeof str, "The day of the month is %d.", &t));
    EXPECT_EQ("The day of the month is 10."sv, str);

    EXPECT(strftime(str, sizeof str, "The date is %D.", &t));
    EXPECT_EQ("The date is 10/10/18."sv, str);

    EXPECT(strftime(str, sizeof str, "The day of the month is %e.", &t));
    EXPECT_EQ("The day of the month is 10."sv, str);

    EXPECT(strftime(str, sizeof str, "The short name of the month is %h.", &t));
    EXPECT_EQ("The short name of the month is Oct."sv, str);

    EXPECT(strftime(str, sizeof str, "The hour is %H.", &t));
    EXPECT_EQ("The hour is 11."sv, str);

    EXPECT(strftime(str, sizeof str, "The hour is %I.", &t));
    EXPECT_EQ("The hour is 11."sv, str);

    EXPECT(strftime(str, sizeof str, "The day of the year is %j.", &t));
    EXPECT_EQ("The day of the year is 283."sv, str);

    EXPECT(strftime(str, sizeof str, "The number of the month is %m.", &t));
    EXPECT_EQ("The number of the month is 10."sv, str);

    EXPECT(strftime(str, sizeof str, "The minute is %M.", &t));
    EXPECT_EQ("The minute is 53."sv, str);

    EXPECT(strftime(str, sizeof str, "This should be followed by a new line: %n.", &t));
    EXPECT_EQ("This should be followed by a new line: \n."sv, str);

    EXPECT(strftime(str, sizeof str, "The period of the day is %p.", &t));
    EXPECT_EQ("The period of the day is AM."sv, str);

    EXPECT(strftime(str, sizeof str, "The time is %r.", &t));
    EXPECT_EQ("The time is 11:53:07 AM."sv, str);

    EXPECT(strftime(str, sizeof str, "The time is %R.", &t));
    EXPECT_EQ("The time is 11:53."sv, str);

    EXPECT(strftime(str, sizeof str, "The second is %S.", &t));
    EXPECT_EQ("The second is 07."sv, str);

    EXPECT(strftime(str, sizeof str, "This should be followed by a tab: %t.", &t));
    EXPECT_EQ("This should be followed by a tab: \t."sv, str);

    EXPECT(strftime(str, sizeof str, "The time is %T.", &t));
    EXPECT_EQ("The time is 11:53:07."sv, str);

    EXPECT(strftime(str, sizeof str, "The day of the week is %u.", &t));
    EXPECT_EQ("The day of the week is 3."sv, str);

    EXPECT(strftime(str, sizeof str, "The week of the year is %U.", &t));
    EXPECT_EQ("The week of the year is 40."sv, str);

    EXPECT(strftime(str, sizeof str, "The ISO week of the year is %V.", &t));
    EXPECT_EQ("The ISO week of the year is 41."sv, str);

    EXPECT(strftime(str, sizeof str, "The day of the week is %w.", &t));
    EXPECT_EQ("The day of the week is 3."sv, str);

    EXPECT(strftime(str, sizeof str, "The week of the year is %W.", &t));
    EXPECT_EQ("The week of the year is 41."sv, str);

    EXPECT(strftime(str, sizeof str, "The year is %y.", &t));
    EXPECT_EQ("The year is 18."sv, str);

    EXPECT(strftime(str, sizeof str, "The year is %Y.", &t));
    EXPECT_EQ("The year is 2018."sv, str);
}

TEST_CASE(strftime_with_modifiers)
{
    char str[200];
    tm t;
    t.tm_sec = 7;
    t.tm_min = 53;
    t.tm_hour = 11;
    t.tm_mday = 10;
    t.tm_mon = 9;
    t.tm_year = 118;
    t.tm_wday = 3;
    t.tm_yday = 282;
    t.tm_isdst = -1;

    EXPECT(strftime(str, sizeof str, "The century in the alternate representation is %EC.", &t));
    EXPECT_EQ("The century in the alternate representation is 20."sv, str);

    EXPECT(strftime(str, sizeof str, "The date in the alternate representation is %Ex.", &t));
    EXPECT_EQ("The date in the alternate representation is 10/10/18."sv, str);

    EXPECT(strftime(str, sizeof str, "The time in the alternate representation is %EX.", &t));
    EXPECT_EQ("The time in the alternate representation is 11:53:07."sv, str);

    EXPECT(strftime(str, sizeof str, "The year offset in the alternate representation is %Ey.", &t));
    EXPECT_EQ("The year offset in the alternate representation is 18."sv, str);

    EXPECT(strftime(str, sizeof str, "The year in the alternate representation is %EY.", &t));
    EXPECT_EQ("The year in the alternate representation is 2018."sv, str);

    EXPECT(strftime(str, sizeof str, "The day of the month in the alternate representation is %Od.", &t));
    EXPECT_EQ("The day of the month in the alternate representation is 10."sv, str);

    EXPECT(strftime(str, sizeof str, "The day of the month in the alternate representation is %Oe.", &t));
    EXPECT_EQ("The day of the month in the alternate representation is 10."sv, str);

    EXPECT(strftime(str, sizeof str, "The hour in the alternate representation is %OH.", &t));
    EXPECT_EQ("The hour in the alternate representation is 11."sv, str);

    EXPECT(strftime(str, sizeof str, "The hour in the alternate representation is %OI.", &t));
    EXPECT_EQ("The hour in the alternate representation is 11."sv, str);

    EXPECT(strftime(str, sizeof str, "The number of the month in the alternate representation is %Om.", &t));
    EXPECT_EQ("The number of the month in the alternate representation is 10."sv, str);

    EXPECT(strftime(str, sizeof str, "The minute in the alternate representation is %OM.", &t));
    EXPECT_EQ("The minute in the alternate representation is 53."sv, str);

    EXPECT(strftime(str, sizeof str, "The second in the alternate representation is %OS.", &t));
    EXPECT_EQ("The second in the alternate representation is 07."sv, str);

    EXPECT(strftime(str, sizeof str, "The day of the week in the alternate representation is %Ou.", &t));
    EXPECT_EQ("The day of the week in the alternate representation is 3."sv, str);

    EXPECT(strftime(str, sizeof str, "The week of the year in the alternate representation is %OU.", &t));
    EXPECT_EQ("The week of the year in the alternate representation is 40."sv, str);

    EXPECT(strftime(str, sizeof str, "The ISO week of the year in the alternate representation is %OV.", &t));
    EXPECT_EQ("The ISO week of the year in the alternate representation is 41."sv, str);

    EXPECT(strftime(str, sizeof str, "The day of the week in the alternate representation is %Ow.", &t));
    EXPECT_EQ("The day of the week in the alternate representation is 3."sv, str);

    EXPECT(strftime(str, sizeof str, "The week of the year in the alternate representation is %OW.", &t));
    EXPECT_EQ("The week of the year in the alternate representation is 41."sv, str);

    EXPECT(strftime(str, sizeof str, "The year in the alternate representation is %Oy.", &t));
    EXPECT_EQ("The year in the alternate representation is 18."sv, str);
}

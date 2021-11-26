/*
 * Copyright (c) 2021, Ben Wiederhake <BenWiederhake.GitHub@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/Time.h>
#include <sys/time.h>

#define EXPECT_TIME(t, s, ns)        \
    do {                             \
        auto ts = (t).to_timespec(); \
        EXPECT_EQ(ts.tv_sec, (s));   \
        EXPECT_EQ(ts.tv_nsec, (ns)); \
    } while (0)

TEST_CASE(is_sane)
{
    auto t0 = Time::from_seconds(0);
    auto t2 = Time::from_seconds(2);
    auto t5 = Time::from_seconds(5);
    auto tn3 = Time::from_seconds(-3);
    EXPECT(t0 == t0);
    EXPECT(t2 == t2);
    EXPECT(t5 == t5);
    EXPECT(t0 != t2);
    EXPECT(t2 != tn3);
    EXPECT(t2 != t5);
    EXPECT_TIME(t0, 0, 0);
    EXPECT_TIME(t2, 2, 0);
    EXPECT_TIME(t5, 5, 0);
    EXPECT_TIME(t2 + t5, 7, 0);
    EXPECT_TIME(tn3 + t2, -1, 0);
    EXPECT_TIME(tn3 + t5, 2, 0);
}

TEST_CASE(limits)
{
    EXPECT_TIME(Time::min(), (i64)-0x8000'0000'0000'0000, 0);
    EXPECT_TIME(Time::max(), 0x7fff'ffff'ffff'ffff, 999'999'999);
}

TEST_CASE(seconds_parsing)
{
    EXPECT_TIME(Time::from_seconds(0), 0, 0);
    EXPECT_TIME(Time::from_seconds(42), 42, 0);
    EXPECT_TIME(Time::from_seconds(-1), -1, 0);

    // "6.4.4.1.5: The type of an integer constant is the first of the corresponding list in which its value can be represented."
    // In the case of "0x8000'0000", the list is "int, unsigned int, …", and unsigned int (u32) matches.
    // Then the unary minus: On unsigned 32-bit integers, -0x8000'0000 == 0x8000'0000, which only then is made signed again.
    // So we would pass a medium-large *positive* number to 'from_seconds', which is not what we want to test here.
    // That's why this is the only place that needs an "LL" suffix.
    EXPECT_TIME(Time::from_seconds(-0x8000'0000LL), -0x8000'0000LL, 0);
    EXPECT_TIME(Time::from_seconds(-0x8000'0000'0000'0000), (i64)-0x8000'0000'0000'0000, 0);
    EXPECT_TIME(Time::from_seconds(0x7fff'ffff'ffff'ffff), 0x7fff'ffff'ffff'ffff, 0);
}

TEST_CASE(timespec_parsing)
{
    EXPECT_TIME(Time::from_timespec(timespec { 2, 4 }), 2, 4);
    EXPECT_TIME(Time::from_timespec(timespec { 1234, 5678 }), 1234, 5678);

    EXPECT_TIME(Time::from_timespec(timespec { 0, 1'000'000'000 }), 1, 0);
    EXPECT_TIME(Time::from_timespec(timespec { 8, 2'000'000'000 }), 10, 0);
    EXPECT_TIME(Time::from_timespec(timespec { 0, 2'147'483'647 }), 2, 147'483'647);

    EXPECT_TIME(Time::from_timespec(timespec { 1, -1 }), 0, 999'999'999);
    EXPECT_TIME(Time::from_timespec(timespec { 0, -1 }), -1, 999'999'999);
    EXPECT_TIME(Time::from_timespec(timespec { -1, 0 }), -1, 0);
    EXPECT_TIME(Time::from_timespec(timespec { -1, 1'000'000'001 }), 0, 1);
    EXPECT_TIME(Time::from_timespec(timespec { -2, 2'000'000'003 }), 0, 3);
    EXPECT_TIME(Time::from_timespec(timespec { -2, 1'999'999'999 }), -1, 999'999'999);

    EXPECT_TIME(Time::from_timespec(timespec { 0x7fff'ffff'ffff'fffe, 999'999'998 }), 0x7fff'ffff'ffff'fffe, 999'999'998);
    EXPECT_TIME(Time::from_timespec(timespec { 0x7fff'ffff'ffff'fffe, 1'999'999'998 }), 0x7fff'ffff'ffff'ffff, 999'999'998);
    EXPECT_TIME(Time::from_timespec(timespec { 0x7fff'ffff'ffff'fffe, 1'999'999'999 }), 0x7fff'ffff'ffff'ffff, 999'999'999);
    EXPECT_TIME(Time::from_timespec(timespec { 0x7fff'ffff'ffff'fffe, 2'000'000'000 }), 0x7fff'ffff'ffff'ffff, 999'999'999);

    EXPECT_TIME(Time::from_timespec(timespec { -0x7fff'ffff'ffff'fffe, -1 }), -0x7fff'ffff'ffff'ffff, 999'999'999);
    EXPECT_TIME(Time::from_timespec(timespec { -0x7fff'ffff'ffff'fffe, -999'999'999 }), -0x7fff'ffff'ffff'ffff, 1);
    EXPECT_TIME(Time::from_timespec(timespec { -0x7fff'ffff'ffff'fffe, -1'999'999'999 }), (i64)-0x8000'0000'0000'0000, 1);
    EXPECT_TIME(Time::from_timespec(timespec { -0x7fff'ffff'ffff'fffe, -2'000'000'000 }), (i64)-0x8000'0000'0000'0000, 0);
    EXPECT_TIME(Time::from_timespec(timespec { -0x7fff'ffff'ffff'fffe, -2'000'000'001 }), (i64)-0x8000'0000'0000'0000, 0);
}

TEST_CASE(timeval_parsing)
{
    EXPECT_TIME(Time::from_timeval(timeval { 2, 4 }), 2, 4'000);
    EXPECT_TIME(Time::from_timeval(timeval { 1234, 5'678 }), 1234, 5'678'000);
    EXPECT_TIME(Time::from_timeval(timeval { -123, -45'678 }), -124, 954'322'000);

    EXPECT_TIME(Time::from_timeval(timeval { 0, 1'000'000 }), 1, 0);
    EXPECT_TIME(Time::from_timeval(timeval { 0, 1'000'000'000 }), 1'000, 0);
    EXPECT_TIME(Time::from_timeval(timeval { 8, 2'000'000 }), 10, 0);
    EXPECT_TIME(Time::from_timeval(timeval { 0, 2'147'483'647 }), 2'147, 483'647'000);

    EXPECT_TIME(Time::from_timeval(timeval { 1, -1 }), 0, 999'999'000);
    EXPECT_TIME(Time::from_timeval(timeval { 0, -1 }), -1, 999'999'000);
    EXPECT_TIME(Time::from_timeval(timeval { -1, 0 }), -1, 0);
    EXPECT_TIME(Time::from_timeval(timeval { -1, 1'000'001 }), 0, 1'000);
    EXPECT_TIME(Time::from_timeval(timeval { -2, 2'000'003 }), 0, 3'000);
    EXPECT_TIME(Time::from_timeval(timeval { -2, 1'999'999 }), -1, 999'999'000);

    EXPECT_TIME(Time::from_timeval(timeval { 0x7fff'ffff'ffff'fffe, 999'998 }), 0x7fff'ffff'ffff'fffe, 999'998'000);
    EXPECT_TIME(Time::from_timeval(timeval { 0x7fff'ffff'ffff'fffe, 1'999'998 }), 0x7fff'ffff'ffff'ffff, 999'998'000);
    EXPECT_TIME(Time::from_timeval(timeval { 0x7fff'ffff'ffff'fffe, 1'999'999 }), 0x7fff'ffff'ffff'ffff, 999'999'000);
    EXPECT_TIME(Time::from_timeval(timeval { 0x7fff'ffff'ffff'fffe, 2'000'000 }), 0x7fff'ffff'ffff'ffff, 999'999'999);

    EXPECT_TIME(Time::from_timeval(timeval { -0x7fff'ffff'ffff'fffe, -1 }), -0x7fff'ffff'ffff'ffff, 999'999'000);
    EXPECT_TIME(Time::from_timeval(timeval { -0x7fff'ffff'ffff'fffe, -999'999 }), -0x7fff'ffff'ffff'ffff, 1'000);
    EXPECT_TIME(Time::from_timeval(timeval { -0x7fff'ffff'ffff'fffe, -1'999'999 }), (i64)-0x8000'0000'0000'0000, 1'000);
    EXPECT_TIME(Time::from_timeval(timeval { -0x7fff'ffff'ffff'fffe, -2'000'000 }), (i64)-0x8000'0000'0000'0000, 0);
    EXPECT_TIME(Time::from_timeval(timeval { -0x7fff'ffff'ffff'fffe, -2'000'001 }), (i64)-0x8000'0000'0000'0000, 0);
}

#define TIME(s, ns) \
    Time::from_timespec(timespec { (s), (ns) })

TEST_CASE(addition)
{
#define EXPECT_ADDITION(s1, ns1, s2, ns2, sr, nsr)           \
    do {                                                     \
        EXPECT_TIME(TIME(s1, ns1) + TIME(s2, ns2), sr, nsr); \
        EXPECT_TIME(TIME(s2, ns2) + TIME(s1, ns1), sr, nsr); \
        auto t = TIME(s1, ns1);                              \
        t += TIME(s2, ns2);                                  \
        EXPECT_TIME(t, sr, nsr);                             \
    } while (0)

    EXPECT_ADDITION(11, 123'456'789, 22, 900'000'000, 34, 23'456'789);

    EXPECT_ADDITION(0, 0, 9223372036854775807LL, 999'999'998, 0x7fff'ffff'ffff'ffff, 999'999'998);
    EXPECT_ADDITION(0, 1, 9223372036854775807LL, 999'999'998, 0x7fff'ffff'ffff'ffff, 999'999'999);
    EXPECT_ADDITION(0, 2, 9223372036854775807LL, 999'999'998, 0x7fff'ffff'ffff'ffff, 999'999'999);

    EXPECT_ADDITION(0x80, 40, 0x7fff'ffff'ffff'ff7f, 999'999'958, 0x7fff'ffff'ffff'ffff, 999'999'998);
    EXPECT_ADDITION(0x80, 41, 0x7fff'ffff'ffff'ff7f, 999'999'958, 0x7fff'ffff'ffff'ffff, 999'999'999);
    EXPECT_ADDITION(0x80, 42, 0x7fff'ffff'ffff'ff7f, 999'999'958, 0x7fff'ffff'ffff'ffff, 999'999'999);

    EXPECT_ADDITION(-2, 5, -3, 7, -5, 12);
    EXPECT_ADDITION(-2, 999'999'995, -3, 999'999'997, -4, 999'999'992);

    EXPECT_ADDITION(-0x7fff'ffff'ffff'ffff, 999'999'995, -1, 6, -0x7fff'ffff'ffff'ffff, 1);
    EXPECT_ADDITION(-0x7fff'ffff'ffff'ffff, 999'999'995, -2, 6, (i64)-0x8000'0000'0000'0000, 1);
    EXPECT_ADDITION(-0x7fff'ffff'ffff'ffff, 999'999'995, -2, 5, (i64)-0x8000'0000'0000'0000, 0);
    EXPECT_ADDITION(-0x7fff'ffff'ffff'ffff, 999'999'995, -2, 4, (i64)-0x8000'0000'0000'0000, 0);

    EXPECT_ADDITION((i64)-0x8000'0000'0000'0000, 999'999'995, 0x7fff'ffff'ffff'ffff, 4, -1, 999'999'999);
    EXPECT_ADDITION((i64)-0x8000'0000'0000'0000, 999'999'995, 0x7fff'ffff'ffff'ffff, 5, 0, 0);
    EXPECT_ADDITION((i64)-0x8000'0000'0000'0000, 999'999'995, 0x7fff'ffff'ffff'ffff, 6, 0, 1);
#undef EXPECT_ADDITION
}

TEST_CASE(subtraction)
{
#define EXPECT_SUBTRACTION(s1, ns1, s2, ns2, sr, nsr)        \
    do {                                                     \
        EXPECT_TIME(TIME(s1, ns1) - TIME(s2, ns2), sr, nsr); \
        auto t = TIME(s1, ns1);                              \
        t -= TIME(s2, ns2);                                  \
        EXPECT_TIME(t, sr, nsr);                             \
    } while (0)

    EXPECT_SUBTRACTION(5, 0, 3, 0, 2, 0);
    EXPECT_SUBTRACTION(0, 0, 0, 0, 0, 0);
    EXPECT_SUBTRACTION(0, 5, 0, 3, 0, 2);
    EXPECT_SUBTRACTION(0x7fff'ffff'ffff'ffff, 999'999'999, 8, 123, 0x7fff'ffff'ffff'fff7, 999'999'876);

    EXPECT_SUBTRACTION(1, 0, 0, 999'999'999, 0, 1);
    EXPECT_SUBTRACTION(0x7fff'ffff'ffff'ffff, 0, 1, 999'999'999, 0x7fff'ffff'ffff'fffd, 1);

    EXPECT_SUBTRACTION(3, 0, 5, 0, -2, 0);
    EXPECT_SUBTRACTION(0, 3, 0, 5, -1, 999'999'998);
    EXPECT_SUBTRACTION(0, 0, 0x7fff'ffff'ffff'ffff, 999'999'999, (i64)-0x8000'0000'0000'0000, 1);
    EXPECT_SUBTRACTION(0, 0, (i64)-0x8000'0000'0000'0000, 0, 0x7fff'ffff'ffff'ffff, 999'999'999);
    EXPECT_SUBTRACTION(-1, 999'999'999, (i64)-0x8000'0000'0000'0000, 0, 0x7fff'ffff'ffff'ffff, 999'999'999);
    EXPECT_SUBTRACTION(-1, 999'999'998, (i64)-0x8000'0000'0000'0000, 0, 0x7fff'ffff'ffff'ffff, 999'999'998);

    EXPECT_SUBTRACTION(123, 456, 123, 455, 0, 1);
    EXPECT_SUBTRACTION(123, 456, 123, 456, 0, 0);
    EXPECT_SUBTRACTION(123, 456, 123, 457, -1, 999'999'999);

    EXPECT_SUBTRACTION(124, 456, 123, 455, 1, 1);
    EXPECT_SUBTRACTION(124, 456, 123, 456, 1, 0);
    EXPECT_SUBTRACTION(124, 456, 123, 457, 0, 999'999'999);

    EXPECT_SUBTRACTION(-0x7fff'ffff'ffff'ffff, 999'999'995, 1, 999'999'994, (i64)-0x8000'0000'0000'0000, 1);
    EXPECT_SUBTRACTION(-0x7fff'ffff'ffff'ffff, 999'999'995, 1, 999'999'995, (i64)-0x8000'0000'0000'0000, 0);
    EXPECT_SUBTRACTION(-0x7fff'ffff'ffff'ffff, 999'999'995, 1, 999'999'996, (i64)-0x8000'0000'0000'0000, 0);
}

TEST_CASE(rounding)
{
    EXPECT_EQ(TIME(2, 800'800'800).to_seconds(), 3);
    EXPECT_EQ(TIME(2, 800'800'800).to_milliseconds(), 2'801);
    EXPECT_EQ(TIME(2, 800'800'800).to_microseconds(), 2'800'801);
    EXPECT_EQ(TIME(2, 800'800'800).to_nanoseconds(), 2'800'800'800);
    EXPECT_EQ(TIME(-2, 800'800'800).to_seconds(), -2);
    EXPECT_EQ(TIME(-2, 800'800'800).to_milliseconds(), -1'200);
    EXPECT_EQ(TIME(-2, 800'800'800).to_microseconds(), -1'199'200);
    EXPECT_EQ(TIME(-2, 800'800'800).to_nanoseconds(), -1'199'199'200);

    EXPECT_EQ(TIME(0, 0).to_seconds(), 0);
    EXPECT_EQ(TIME(0, 0).to_milliseconds(), 0);
    EXPECT_EQ(TIME(0, 0).to_microseconds(), 0);
    EXPECT_EQ(TIME(0, 0).to_nanoseconds(), 0);

    EXPECT_EQ(TIME(0, 1).to_seconds(), 1);
    EXPECT_EQ(TIME(0, 1).to_milliseconds(), 1);
    EXPECT_EQ(TIME(0, 1).to_microseconds(), 1);
    EXPECT_EQ(TIME(0, 1).to_nanoseconds(), 1);
    EXPECT_EQ(TIME(0, -1).to_seconds(), -1);
    EXPECT_EQ(TIME(0, -1).to_milliseconds(), -1);
    EXPECT_EQ(TIME(0, -1).to_microseconds(), -1);
    EXPECT_EQ(TIME(0, -1).to_nanoseconds(), -1);

    EXPECT_EQ(TIME(-9223372037, 145'224'191).to_nanoseconds(), (i64)-0x8000'0000'0000'0000);
    EXPECT_EQ(TIME(-9223372037, 145'224'192).to_nanoseconds(), (i64)-0x8000'0000'0000'0000);
    EXPECT_EQ(TIME(-9223372037, 145'224'193).to_nanoseconds(), -0x7fff'ffff'ffff'ffff);
    EXPECT_EQ(TIME(9223372036, 854'775'806).to_nanoseconds(), 0x7fff'ffff'ffff'fffe);
    EXPECT_EQ(TIME(9223372036, 854'775'807).to_nanoseconds(), 0x7fff'ffff'ffff'ffff);
    EXPECT_EQ(TIME(9223372036, 854'775'808).to_nanoseconds(), 0x7fff'ffff'ffff'ffff);
}

TEST_CASE(truncation)
{
    // Sanity
    EXPECT_EQ(TIME(2, 0).to_truncated_seconds(), 2);
    EXPECT_EQ(TIME(-2, 0).to_truncated_seconds(), -2);
    EXPECT_EQ(TIME(2, 800'800'800).to_truncated_seconds(), 2);
    EXPECT_EQ(TIME(2, 800'800'800).to_truncated_milliseconds(), 2'800);
    EXPECT_EQ(TIME(2, 800'800'800).to_truncated_microseconds(), 2'800'800);
    EXPECT_EQ(TIME(-2, -800'800'800).to_truncated_seconds(), -2);
    EXPECT_EQ(TIME(-2, -800'800'800).to_truncated_milliseconds(), -2'800);
    EXPECT_EQ(TIME(-2, -800'800'800).to_truncated_microseconds(), -2'800'800);

    // Overflow, seconds
    EXPECT_EQ(Time::min().to_truncated_seconds(), (i64)-0x8000'0000'0000'0000);
    EXPECT_EQ(Time::max().to_truncated_seconds(), 0x7fff'ffff'ffff'ffff);

    // Overflow, milliseconds
    EXPECT_EQ(TIME(-9223372036854776, 191'000'000).to_truncated_milliseconds(), (i64)-0x8000'0000'0000'0000);
    EXPECT_EQ(TIME(-9223372036854776, 192'000'000).to_truncated_milliseconds(), (i64)-0x8000'0000'0000'0000);
    EXPECT_EQ(TIME(-9223372036854776, 192'000'001).to_truncated_milliseconds(), -0x7fff'ffff'ffff'ffff);
    EXPECT_EQ(TIME(-9223372036854776, 193'000'000).to_truncated_milliseconds(), -0x7fff'ffff'ffff'ffff);
    EXPECT_EQ(TIME(9223372036854775, 806'000'000).to_truncated_milliseconds(), 0x7fff'ffff'ffff'fffe);
    EXPECT_EQ(TIME(9223372036854775, 806'999'999).to_truncated_milliseconds(), 0x7fff'ffff'ffff'fffe);
    EXPECT_EQ(TIME(9223372036854775, 807'000'000).to_truncated_milliseconds(), 0x7fff'ffff'ffff'ffff);
    EXPECT_EQ(TIME(9223372036854775, 808'000'000).to_truncated_milliseconds(), 0x7fff'ffff'ffff'ffff);

    // Overflow, microseconds
    EXPECT_EQ(TIME(-9223372036855, 224'191'000).to_truncated_microseconds(), (i64)-0x8000'0000'0000'0000);
    EXPECT_EQ(TIME(-9223372036855, 224'192'000).to_truncated_microseconds(), (i64)-0x8000'0000'0000'0000);
    EXPECT_EQ(TIME(-9223372036855, 224'192'001).to_truncated_microseconds(), (i64)-0x7fff'ffff'ffff'ffff);
    EXPECT_EQ(TIME(-9223372036855, 224'193'000).to_truncated_microseconds(), (i64)-0x7fff'ffff'ffff'ffff);
    EXPECT_EQ(TIME(9223372036854, 775'806'000).to_truncated_microseconds(), 0x7fff'ffff'ffff'fffe);
    EXPECT_EQ(TIME(9223372036854, 775'806'999).to_truncated_microseconds(), 0x7fff'ffff'ffff'fffe);
    EXPECT_EQ(TIME(9223372036854, 775'807'000).to_truncated_microseconds(), 0x7fff'ffff'ffff'ffff);
    EXPECT_EQ(TIME(9223372036854, 775'808'000).to_truncated_microseconds(), 0x7fff'ffff'ffff'ffff);
}

TEST_CASE(is_negative)
{
    auto small = Time::from_nanoseconds(10);
    auto large = Time::from_nanoseconds(15);
    auto result = small - large;
    EXPECT_EQ(result.to_nanoseconds(), -5);
    EXPECT(result.is_negative());

    result = large - small;
    EXPECT_EQ(result.to_nanoseconds(), 5);
    EXPECT(!result.is_negative());
}

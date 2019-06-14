#pragma once

#include <stdio.h>
#include <AK/AKString.h>

#define LOG_FAIL(cond) \
    fprintf(stderr, "\033[31;1mFAIL\033[0m: " #cond "\n")

#define LOG_PASS(cond) \
    fprintf(stderr, "\033[32;1mPASS\033[0m: " #cond "\n")

#define LOG_FAIL_EQ(cond, expected_value, actual_value) \
    fprintf(stderr, "\033[31;1mFAIL\033[0m: " #cond " should be " #expected_value ", got "); \
    stringify_for_test(actual_value); \
    fprintf(stderr, "\n")

#define LOG_PASS_EQ(cond, expected_value) \
    fprintf(stderr, "\033[32;1mPASS\033[0m: " #cond " should be " #expected_value " and it is\n")

#define EXPECT_EQ(expr, expected_value) \
    do { \
        auto result = (expr); \
        if (!(result == expected_value)) { \
            LOG_FAIL_EQ(expr, expected_value, result); \
        } else { \
            LOG_PASS_EQ(expr, expected_value); \
        } \
    } while(0)

#define EXPECT(cond) \
    do { \
        if (!(cond)) { \
            LOG_FAIL(cond); \
        } else { \
            LOG_PASS(cond); \
        } \
    } while(0)

inline void stringify_for_test(int value)
{
    fprintf(stderr, "%d", value);
}

inline void stringify_for_test(unsigned value)
{
    fprintf(stderr, "%u", value);
}

inline void stringify_for_test(const char* value)
{
    fprintf(stderr, "%s", value);
}

inline void stringify_for_test(char value)
{
    fprintf(stderr, "%c", value);
}

inline void stringify_for_test(const AK::String& string)
{
    stringify_for_test(string.characters());
}

inline void stringify_for_test(const AK::StringImpl& string)
{
    stringify_for_test(string.characters());
}


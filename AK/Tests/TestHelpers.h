#pragma once

#include <stdio.h>

#define LOG_FAIL(cond) \
    fprintf(stderr, "\033[31;1mFAIL\033[0m: " #cond "\n")

#define LOG_PASS(cond) \
    fprintf(stderr, "\033[32;1mPASS\033[0m: " #cond "\n")

#define EXPECT(cond) \
    do { \
        if (!(cond)) { \
            LOG_FAIL(cond); \
        } else { \
            LOG_PASS(cond); \
        } \
    } while(0)


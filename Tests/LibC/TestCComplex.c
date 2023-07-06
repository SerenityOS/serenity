/*
 * Copyright (c) 2023, Peter Elliott <pelliott@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Platform.h>
#include <complex.h>
#include <math.h>
#include <stdio.h>

// This file has to be C. C++ compilers will not compile c99 complex numbers.

#if defined(AK_COMPILER_CLANG)
#    pragma clang optimize off
#else
#    pragma GCC optimize("O0")
#endif

#define EXPECT_EQ(a, b)                                                                 \
    {                                                                                   \
        typeof(a) lhs = a;                                                              \
        typeof(b) rhs = b;                                                              \
        if (lhs != rhs) {                                                               \
            fprintf(stderr, "\033[31;1mFAIL\033[0m: %s:%i: EXPECT_EQ(%s, %s) failed\n", \
                __FILE__, __LINE__, #a, #b);                                            \
            fail_counter++;                                                             \
        }                                                                               \
    }

#define EXPECT_APPROXIMATE_WITH_ERROR(a, b, err)                                                 \
    {                                                                                            \
        typeof(a) lhs = a;                                                                       \
        typeof(b) rhs = b;                                                                       \
        if (fabsl(creall(lhs) - creall(rhs)) > err || fabsl(cimagl(lhs) - cimagl(rhs)) > err) {  \
            fprintf(stderr, "\033[31;1mFAIL\033[0m: %s:%i: EXPECT_APPROXIMATE(%s, %s) failed\n", \
                __FILE__, __LINE__, #a, #b);                                                     \
            fail_counter++;                                                                      \
        }                                                                                        \
    }

#define EXPECT_APPROXIMATE(a, b) EXPECT_APPROXIMATE_WITH_ERROR(a, b, 0.0000005)

int main()
{
    int fail_counter = 0;

    // cabsl tests
    EXPECT_EQ(cabsl(0), 0);
    EXPECT_EQ(cabsl(1.1), 1.1);
    EXPECT_EQ(cabsl(1.3i), 1.3);
    EXPECT_EQ(cabsl(-0.8), 0.8);
    EXPECT_EQ(cabsl(-9i), 9);
    EXPECT_EQ(cabsl(3 - 4i), 5);
    EXPECT_APPROXIMATE(cabsl(-9 - 0.00001i), 9);

    // csqrtl tests
    EXPECT_EQ(csqrtl(1), 1 + 0i);
    EXPECT_EQ(csqrtl(-1), 0 + 1i);
    EXPECT_APPROXIMATE(csqrtl(2), 1.41421356237 + 0i);
    EXPECT_APPROXIMATE(csqrtl(3i), 1.22474487 + 1.22474487i);
    EXPECT_APPROXIMATE(csqrtl(-9i), 2.12132034 - 2.12132034i);
    EXPECT_APPROXIMATE(csqrtl(-7 + 24i), 3 + 4i);
    EXPECT_APPROXIMATE(csqrtl(7 + 24i), 4 + 3i);
    EXPECT_APPROXIMATE_WITH_ERROR(csqrtl(-9 + 0.000001i), 0 + 3i, 0.000005);
    EXPECT_APPROXIMATE_WITH_ERROR(csqrtl(-9 - 0.000001i), 0 - 3i, 0.000005);
    EXPECT_APPROXIMATE(csqrtl(1e-4950L), 6.03755e-2476L + 0i);

    return fail_counter;
}

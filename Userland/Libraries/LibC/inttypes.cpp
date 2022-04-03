/*
 * Copyright (c) 2021, Mițca Dumitru <dumitru0mitca@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NumericLimits.h>
#include <errno.h>
#include <inttypes.h>
#include <stdlib.h>

extern "C" {

imaxdiv_t imaxdiv(intmax_t numerator, intmax_t denominator)
{
    imaxdiv_t result;
    result.quot = numerator / denominator;
    result.rem = numerator % denominator;

    if (numerator >= 0 && result.rem < 0) {
        result.quot++;
        result.rem -= denominator;
    }

    return result;
}

intmax_t strtoimax(char const* str, char** endptr, int base)
{
    long long_value = strtoll(str, endptr, base);

    intmax_t max_int_value = NumericLimits<intmax_t>::max();
    intmax_t min_int_value = NumericLimits<intmax_t>::min();
    if (long_value > max_int_value) {
        errno = -ERANGE;
        return max_int_value;
    } else if (long_value < min_int_value) {
        errno = -ERANGE;
        return min_int_value;
    }

    return long_value;
}

uintmax_t strtoumax(char const* str, char** endptr, int base)
{
    unsigned long ulong_value = strtoull(str, endptr, base);

    uintmax_t max_uint_value = NumericLimits<uintmax_t>::max();
    if (ulong_value > max_uint_value) {
        errno = -ERANGE;
        return max_uint_value;
    }

    return ulong_value;
}
}

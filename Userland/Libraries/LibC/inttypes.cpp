/*
 * Copyright (c) 2021, Mițca Dumitru <dumitru0mitca@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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

intmax_t strtoimax(const char* str, char** endptr, int base)
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

uintmax_t strtoumax(const char* str, char** endptr, int base)
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

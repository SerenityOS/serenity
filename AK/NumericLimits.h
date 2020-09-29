/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#pragma once

#include <AK/Types.h>

namespace AK {

template<typename T>
struct NumericLimits {
};

template<>
struct NumericLimits<bool> {
    static constexpr char min() { return false; }
    static constexpr char max() { return true; }
    static constexpr bool is_signed() { return false; }
};

template<>
struct NumericLimits<signed char> {
    static constexpr signed char min() { return -__SCHAR_MAX__ - 1; }
    static constexpr signed char max() { return __SCHAR_MAX__; }
    static constexpr bool is_signed() { return true; }
};

template<>
struct NumericLimits<char> {
    static constexpr char min() { return -__SCHAR_MAX__ - 1; }
    static constexpr char max() { return __SCHAR_MAX__; }
    static constexpr bool is_signed() { return true; }
};

template<>
struct NumericLimits<short> {
    static constexpr short min() { return -__SHRT_MAX__ - 1; }
    static constexpr short max() { return __SHRT_MAX__; }
    static constexpr bool is_signed() { return true; }
};

template<>
struct NumericLimits<int> {
    static constexpr int min() { return -__INT_MAX__ - 1; }
    static constexpr int max() { return __INT_MAX__; }
    static constexpr bool is_signed() { return true; }
};

template<>
struct NumericLimits<long> {
    static constexpr long min() { return -__LONG_MAX__ - 1; }
    static constexpr long max() { return __LONG_MAX__; }
    static constexpr bool is_signed() { return true; }
};

template<>
struct NumericLimits<long long> {
    static constexpr long long min() { return -__LONG_LONG_MAX__ - 1; }
    static constexpr long long max() { return __LONG_LONG_MAX__; }
    static constexpr bool is_signed() { return true; }
};

template<>
struct NumericLimits<unsigned char> {
    static constexpr unsigned char min() { return 0; }
    static constexpr unsigned char max() { return __SCHAR_MAX__ * 2u + 1; }
    static constexpr bool is_signed() { return false; }
};

template<>
struct NumericLimits<unsigned short> {
    static constexpr unsigned short min() { return 0; }
    static constexpr unsigned short max() { return __SHRT_MAX__ * 2u + 1; }
    static constexpr bool is_signed() { return false; }
};

template<>
struct NumericLimits<unsigned> {
    static constexpr unsigned min() { return 0; }
    static constexpr unsigned max() { return __INT_MAX__ * 2u + 1; }
    static constexpr bool is_signed() { return false; }
};

template<>
struct NumericLimits<unsigned long> {
    static constexpr unsigned long min() { return 0; }
    static constexpr unsigned long max() { return __LONG_MAX__ * 2ul + 1; }
    static constexpr bool is_signed() { return false; }
};

template<>
struct NumericLimits<unsigned long long> {
    static constexpr unsigned long long min() { return 0; }
    static constexpr unsigned long long max() { return __LONG_LONG_MAX__ * 2ull + 1; }
    static constexpr bool is_signed() { return false; }
};

#ifndef KERNEL
template<>
struct NumericLimits<float> {
    static constexpr float min() { return __FLT_MIN__; }
    static constexpr float max() { return __FLT_MAX__; }
    static constexpr bool is_signed() { return true; }
};

template<>
struct NumericLimits<double> {
    static constexpr double min() { return __DBL_MIN__; }
    static constexpr double max() { return __DBL_MAX__; }
    static constexpr bool is_signed() { return true; }
};

template<>
struct NumericLimits<long double> {
    static constexpr long double min() { return __LDBL_MIN__; }
    static constexpr long double max() { return __LDBL_MAX__; }
    static constexpr bool is_signed() { return true; }
};
#endif

}

using AK::NumericLimits;

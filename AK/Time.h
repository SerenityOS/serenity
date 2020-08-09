/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

namespace AK {

template<typename TimevalType>
inline void timeval_sub(const TimevalType& a, const TimevalType& b, TimevalType& result)
{
    result.tv_sec = a.tv_sec - b.tv_sec;
    result.tv_usec = a.tv_usec - b.tv_usec;
    if (result.tv_usec < 0) {
        --result.tv_sec;
        result.tv_usec += 1'000'000;
    }
}

template<typename TimevalType>
inline void timeval_add(const TimevalType& a, const TimevalType& b, TimevalType& result)
{
    result.tv_sec = a.tv_sec + b.tv_sec;
    result.tv_usec = a.tv_usec + b.tv_usec;
    if (result.tv_usec >= 1'000'000) {
        ++result.tv_sec;
        result.tv_usec -= 1'000'000;
    }
}

template<typename TimespecType>
inline void timespec_sub(const TimespecType& a, const TimespecType& b, TimespecType& result)
{
    result.tv_sec = a.tv_sec - b.tv_sec;
    result.tv_nsec = a.tv_nsec - b.tv_nsec;
    if (result.tv_nsec < 0) {
        --result.tv_sec;
        result.tv_nsec += 1'000'000'000;
    }
}

template<typename TimespecType, typename TimevalType>
inline void timespec_sub_timeval(const TimespecType& a, const TimevalType& b, TimespecType& result)
{
    result.tv_sec = a.tv_sec - b.tv_sec;
    result.tv_nsec = a.tv_nsec - b.tv_usec * 1000;
    if (result.tv_nsec < 0) {
        --result.tv_sec;
        result.tv_nsec += 1'000'000'000;
    }
}

template<typename TimespecType>
inline void timespec_add(const TimespecType& a, const TimespecType& b, TimespecType& result)
{
    result.tv_sec = a.tv_sec + b.tv_sec;
    result.tv_nsec = a.tv_nsec + b.tv_nsec;
    if (result.tv_nsec >= 1000'000'000) {
        ++result.tv_sec;
        result.tv_nsec -= 1000'000'000;
    }
}

template<typename TimespecType, typename TimevalType>
inline void timespec_add_timeval(const TimespecType& a, const TimevalType& b, TimespecType& result)
{
    result.tv_sec = a.tv_sec + b.tv_sec;
    result.tv_nsec = a.tv_nsec + b.tv_usec * 1000;
    if (result.tv_nsec >= 1000'000'000) {
        ++result.tv_sec;
        result.tv_nsec -= 1000'000'000;
    }
}

template<typename TimevalType, typename TimespecType>
inline void timeval_to_timespec(const TimevalType& tv, TimespecType& ts)
{
   ts.tv_sec = tv.tv_sec;
   ts.tv_nsec = tv.tv_usec * 1000;
}

template<typename TimespecType, typename TimevalType>
inline void timespec_to_timeval(const TimespecType& ts, TimevalType& tv)
{
    tv.tv_sec = ts.tv_sec;
    tv.tv_usec = ts.tv_nsec / 1000;
}

template<typename TimespecType>
inline bool operator>=(const TimespecType& a, const TimespecType& b)
{
    return a.tv_sec > b.tv_sec || (a.tv_sec == b.tv_sec && a.tv_nsec >= b.tv_nsec);
}

template<typename TimespecType>
inline bool operator>(const TimespecType& a, const TimespecType& b)
{
    return a.tv_sec > b.tv_sec || (a.tv_sec == b.tv_sec && a.tv_nsec > b.tv_nsec);
}

template<typename TimespecType>
inline bool operator<(const TimespecType& a, const TimespecType& b)
{
    return a.tv_sec < b.tv_sec || (a.tv_sec == b.tv_sec && a.tv_nsec < b.tv_nsec);
}

template<typename TimespecType>
inline bool operator<=(const TimespecType& a, const TimespecType& b)
{
    return a.tv_sec < b.tv_sec || (a.tv_sec == b.tv_sec && a.tv_nsec <= b.tv_nsec);
}

template<typename TimespecType>
inline bool operator==(const TimespecType& a, const TimespecType& b)
{
    return a.tv_sec == b.tv_sec && a.tv_nsec == b.tv_nsec;
}

template<typename TimespecType>
inline bool operator!=(const TimespecType& a, const TimespecType& b)
{
    return a.tv_sec != b.tv_sec || a.tv_nsec != b.tv_nsec;
}

}

using AK::timespec_add;
using AK::timespec_add_timeval;
using AK::timespec_sub;
using AK::timespec_sub_timeval;
using AK::timespec_to_timeval;
using AK::timeval_add;
using AK::timeval_sub;
using AK::timeval_to_timespec;
using AK::operator<=;
using AK::operator<;
using AK::operator>;
using AK::operator>=;
using AK::operator==;
using AK::operator!=;

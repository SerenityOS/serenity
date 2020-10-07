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

#include <AK/Assertions.h>
#include <AK/LogStream.h>
#include <AK/StdLibExtras.h>
#include <AK/StringBuilder.h>
#include <AK/Types.h>
#include <stdarg.h>

namespace PrintfImplementation {

static constexpr const char* printf_hex_digits_lower = "0123456789abcdef";
static constexpr const char* printf_hex_digits_upper = "0123456789ABCDEF";

#ifdef __serenity__
extern "C" size_t strlen(const char*);
#else
#    include <string.h>
#endif

template<typename PutChFunc, typename T>
ALWAYS_INLINE int print_hex(PutChFunc putch, char*& bufptr, T number, bool upper_case, bool alternate_form, bool left_pad, bool zero_pad, u8 field_width)
{
    int ret = 0;

    int digits = 0;
    for (T n = number; n > 0; n >>= 4)
        ++digits;
    if (digits == 0)
        digits = 1;

    if (left_pad) {
        int stop_at = field_width - digits;
        if (alternate_form)
            stop_at -= 2;

        while (ret < stop_at) {
            putch(bufptr, ' ');
            ++ret;
        }
    }

    if (alternate_form) {
        putch(bufptr, '0');
        putch(bufptr, 'x');
        ret += 2;
        field_width += 2;
    }

    if (zero_pad) {
        while (ret < field_width - digits) {
            putch(bufptr, '0');
            ++ret;
        }
    }

    if (number == 0) {
        putch(bufptr, '0');
        ++ret;
    } else {
        u8 shift_count = digits * 4;
        while (shift_count) {
            shift_count -= 4;
            putch(bufptr,
                upper_case
                    ? printf_hex_digits_upper[(number >> shift_count) & 0x0f]
                    : printf_hex_digits_lower[(number >> shift_count) & 0x0f]);
            ++ret;
        }
    }

    return ret;
}

template<typename PutChFunc>
ALWAYS_INLINE int print_number(PutChFunc putch, char*& bufptr, u32 number, bool left_pad, bool zero_pad, u32 field_width)
{
    u32 divisor = 1000000000;
    char ch;
    char padding = 1;
    char buf[16];
    char* p = buf;

    for (;;) {
        ch = '0' + (number / divisor);
        number %= divisor;
        if (ch != '0')
            padding = 0;
        if (!padding || divisor == 1)
            *(p++) = ch;
        if (divisor == 1)
            break;
        divisor /= 10;
    }

    size_t numlen = p - buf;
    if (!field_width || field_width < numlen)
        field_width = numlen;
    if (!left_pad) {
        for (unsigned i = 0; i < field_width - numlen; ++i) {
            putch(bufptr, zero_pad ? '0' : ' ');
        }
    }
    for (unsigned i = 0; i < numlen; ++i) {
        putch(bufptr, buf[i]);
    }
    if (left_pad) {
        for (unsigned i = 0; i < field_width - numlen; ++i) {
            putch(bufptr, ' ');
        }
    }

    return field_width;
}

template<typename PutChFunc>
ALWAYS_INLINE int print_u64(PutChFunc putch, char*& bufptr, u64 number, bool left_pad, bool zero_pad, u32 field_width)
{
    u64 divisor = 10000000000000000000LLU;
    char ch;
    char padding = 1;
    char buf[16];
    char* p = buf;

    for (;;) {
        ch = '0' + (number / divisor);
        number %= divisor;
        if (ch != '0')
            padding = 0;
        if (!padding || divisor == 1)
            *(p++) = ch;
        if (divisor == 1)
            break;
        divisor /= 10;
    }

    size_t numlen = p - buf;
    if (!field_width || field_width < numlen)
        field_width = numlen;
    if (!left_pad) {
        for (unsigned i = 0; i < field_width - numlen; ++i) {
            putch(bufptr, zero_pad ? '0' : ' ');
        }
    }
    for (unsigned i = 0; i < numlen; ++i) {
        putch(bufptr, buf[i]);
    }
    if (left_pad) {
        for (unsigned i = 0; i < field_width - numlen; ++i) {
            putch(bufptr, ' ');
        }
    }

    return field_width;
}

template<typename PutChFunc>
ALWAYS_INLINE int print_double(PutChFunc putch, char*& bufptr, double number, bool left_pad, bool zero_pad, u32 field_width, u32 fraction_length)
{
    int length = 0;

    if (number < 0) {
        putch(bufptr, '-');
        length++;
        number = 0 - number;
    }

    length = print_u64(putch, bufptr, (i64)number, left_pad, zero_pad, field_width);
    putch(bufptr, '.');
    length++;
    double fraction = number - (i64)number;

    for (u32 i = 0; i < fraction_length; ++i)
        fraction = fraction * 10;

    return length + print_u64(putch, bufptr, (i64)fraction, false, true, fraction_length);
}

template<typename PutChFunc>
ALWAYS_INLINE int print_i64(PutChFunc putch, char*& bufptr, i64 number, bool left_pad, bool zero_pad, u32 field_width)
{
    // FIXME: This won't work if there is padding. '  -17' becomes '-  17'.
    if (number < 0) {
        putch(bufptr, '-');
        return print_u64(putch, bufptr, 0 - number, left_pad, zero_pad, field_width) + 1;
    }
    return print_u64(putch, bufptr, number, left_pad, zero_pad, field_width);
}

template<typename PutChFunc>
ALWAYS_INLINE int print_octal_number(PutChFunc putch, char*& bufptr, u32 number, bool left_pad, bool zero_pad, u32 field_width)
{
    u32 divisor = 134217728;
    char ch;
    char padding = 1;
    char buf[32];
    char* p = buf;

    for (;;) {
        ch = '0' + (number / divisor);
        number %= divisor;
        if (ch != '0')
            padding = 0;
        if (!padding || divisor == 1)
            *(p++) = ch;
        if (divisor == 1)
            break;
        divisor /= 8;
    }

    size_t numlen = p - buf;
    if (!field_width || field_width < numlen)
        field_width = numlen;
    if (!left_pad) {
        for (unsigned i = 0; i < field_width - numlen; ++i) {
            putch(bufptr, zero_pad ? '0' : ' ');
        }
    }
    for (unsigned i = 0; i < numlen; ++i) {
        putch(bufptr, buf[i]);
    }
    if (left_pad) {
        for (unsigned i = 0; i < field_width - numlen; ++i) {
            putch(bufptr, ' ');
        }
    }

    return field_width;
}

template<typename PutChFunc>
ALWAYS_INLINE int print_string(PutChFunc putch, char*& bufptr, const char* str, size_t len, bool left_pad, size_t field_width, bool dot)
{
    if (!dot && (!field_width || field_width < len))
        field_width = len;
    size_t pad_amount = field_width > len ? field_width - len : 0;

    if (!left_pad) {
        for (size_t i = 0; i < pad_amount; ++i)
            putch(bufptr, ' ');
    }
    for (size_t i = 0; i < min(len, field_width); ++i) {
        putch(bufptr, str[i]);
    }
    if (left_pad) {
        for (size_t i = 0; i < pad_amount; ++i)
            putch(bufptr, ' ');
    }
    return field_width;
}

template<typename PutChFunc>
ALWAYS_INLINE int print_signed_number(PutChFunc putch, char*& bufptr, int number, bool left_pad, bool zero_pad, u32 field_width, bool always_sign)
{
    if (number < 0) {
        putch(bufptr, '-');
        return print_number(putch, bufptr, 0 - number, left_pad, zero_pad, field_width) + 1;
    }
    if (always_sign)
        putch(bufptr, '+');
    return print_number(putch, bufptr, number, left_pad, zero_pad, field_width);
}

struct ModifierState {
    bool left_pad { false };
    bool zero_pad { false };
    bool dot { false };
    unsigned field_width { 0 };
    bool has_fraction_length { false };
    unsigned fraction_length { 6 };
    unsigned long_qualifiers { 0 };
    bool size_qualifier { false };
    bool alternate_form { 0 };
    bool always_sign { false };
};

template<typename PutChFunc, typename ArgumentListRefT, template<typename T, typename U = ArgumentListRefT> typename NextArgument>
struct PrintfImpl {
    ALWAYS_INLINE PrintfImpl(PutChFunc& putch, char*& bufptr, const int& nwritten)
        : m_bufptr(bufptr)
        , m_nwritten(nwritten)
        , m_putch(putch)
    {
    }

    ALWAYS_INLINE int format_s(const ModifierState& state, ArgumentListRefT ap) const
    {
        const char* sp = NextArgument<const char*>()(ap);
        if (!sp)
            sp = "(null)";
        return print_string(m_putch, m_bufptr, sp, strlen(sp), state.left_pad, state.field_width, state.dot);
    }
    ALWAYS_INLINE int format_d(const ModifierState& state, ArgumentListRefT ap) const
    {
        if (state.long_qualifiers >= 2)
            return print_i64(m_putch, m_bufptr, NextArgument<i64>()(ap), state.left_pad, state.zero_pad, state.field_width);

        return print_signed_number(m_putch, m_bufptr, NextArgument<int>()(ap), state.left_pad, state.zero_pad, state.field_width, state.always_sign);
    }
    ALWAYS_INLINE int format_i(const ModifierState& state, ArgumentListRefT ap) const
    {
        return format_d(state, ap);
    }
    ALWAYS_INLINE int format_u(const ModifierState& state, ArgumentListRefT ap) const
    {
        if (state.long_qualifiers >= 2)
            return print_u64(m_putch, m_bufptr, NextArgument<u64>()(ap), state.left_pad, state.zero_pad, state.field_width);
        return print_number(m_putch, m_bufptr, NextArgument<u32>()(ap), state.left_pad, state.zero_pad, state.field_width);
    }
    ALWAYS_INLINE int format_Q(const ModifierState& state, ArgumentListRefT ap) const
    {
        return print_u64(m_putch, m_bufptr, NextArgument<u64>()(ap), state.left_pad, state.zero_pad, state.field_width);
    }
    ALWAYS_INLINE int format_q(const ModifierState& state, ArgumentListRefT ap) const
    {
        return print_hex(m_putch, m_bufptr, NextArgument<u64>()(ap), false, false, state.left_pad, state.zero_pad, 16);
    }
    ALWAYS_INLINE int format_g(const ModifierState& state, ArgumentListRefT ap) const
    {
        return format_f(state, ap);
    }
    ALWAYS_INLINE int format_f(const ModifierState& state, ArgumentListRefT ap) const
    {
        return print_double(m_putch, m_bufptr, NextArgument<double>()(ap), state.left_pad, state.zero_pad, state.field_width, state.fraction_length);
    }
    ALWAYS_INLINE int format_o(const ModifierState& state, ArgumentListRefT ap) const
    {
        if (state.alternate_form)
            m_putch(m_bufptr, '0');

        return (state.alternate_form ? 1 : 0) + print_octal_number(m_putch, m_bufptr, NextArgument<u32>()(ap), state.left_pad, state.zero_pad, state.field_width);
    }
    ALWAYS_INLINE int format_x(const ModifierState& state, ArgumentListRefT ap) const
    {
        if (state.long_qualifiers >= 2)
            return print_hex(m_putch, m_bufptr, NextArgument<u64>()(ap), false, state.alternate_form, state.left_pad, state.zero_pad, state.field_width);
        return print_hex(m_putch, m_bufptr, NextArgument<u32>()(ap), false, state.alternate_form, state.left_pad, state.zero_pad, state.field_width);
    }
    ALWAYS_INLINE int format_X(const ModifierState& state, ArgumentListRefT ap) const
    {
        if (state.long_qualifiers >= 2)
            return print_hex(m_putch, m_bufptr, NextArgument<u64>()(ap), true, state.alternate_form, state.left_pad, state.zero_pad, state.field_width);
        return print_hex(m_putch, m_bufptr, NextArgument<u32>()(ap), true, state.alternate_form, state.left_pad, state.zero_pad, state.field_width);
    }
    ALWAYS_INLINE int format_n(const ModifierState&, ArgumentListRefT ap) const
    {
        *NextArgument<int*>()(ap) = m_nwritten;
        return 0;
    }
    ALWAYS_INLINE int format_p(const ModifierState&, ArgumentListRefT ap) const
    {
        return print_hex(m_putch, m_bufptr, NextArgument<u32>()(ap), false, true, false, true, 8);
    }
    ALWAYS_INLINE int format_P(const ModifierState&, ArgumentListRefT ap) const
    {
        return print_hex(m_putch, m_bufptr, NextArgument<u32>()(ap), true, true, false, true, 8);
    }
    ALWAYS_INLINE int format_percent(const ModifierState&, ArgumentListRefT) const
    {
        m_putch(m_bufptr, '%');
        return 1;
    }
    ALWAYS_INLINE int format_w(const ModifierState& state, ArgumentListRefT ap) const
    {
        return print_hex(m_putch, m_bufptr, NextArgument<int>()(ap), false, state.alternate_form, false, true, 4);
    }
    ALWAYS_INLINE int format_b(const ModifierState& state, ArgumentListRefT ap) const
    {
        return print_hex(m_putch, m_bufptr, NextArgument<int>()(ap), false, state.alternate_form, false, true, 2);
    }
    ALWAYS_INLINE int format_c(const ModifierState& state, ArgumentListRefT ap) const
    {
        char c = NextArgument<int>()(ap);
        return print_string(m_putch, m_bufptr, &c, 1, state.left_pad, state.field_width, state.dot);
    }
    ALWAYS_INLINE int format_unrecognized(char format_op, const char* fmt, const ModifierState&, ArgumentListRefT) const
    {
        dbgln("printf_internal: Unimplemented format specifier {} (fmt: {})", format_op, fmt);
        return 0;
    }

protected:
    char*& m_bufptr;
    const int& m_nwritten;
    PutChFunc& m_putch;
};

template<typename T, typename V>
struct VaArgNextArgument {
    ALWAYS_INLINE T operator()(V ap) const
    {
        return va_arg(ap, T);
    }
};

#define PRINTF_IMPL_DELEGATE_TO_IMPL(c)    \
    case* #c:                              \
        ret += impl.format_##c(state, ap); \
        break;

template<typename PutChFunc, template<typename T, typename U, template<typename X, typename Y> typename V> typename Impl = PrintfImpl, typename ArgumentListT = va_list, template<typename T, typename V = decltype(declval<ArgumentListT&>())> typename NextArgument = VaArgNextArgument>
ALWAYS_INLINE int printf_internal(PutChFunc putch, char* buffer, const char*& fmt, ArgumentListT ap)
{
    int ret = 0;
    char* bufptr = buffer;

    Impl<PutChFunc, ArgumentListT&, NextArgument> impl { putch, bufptr, ret };

    for (const char* p = fmt; *p; ++p) {
        ModifierState state;
        if (*p == '%' && *(p + 1)) {
        one_more:
            ++p;
            if (*p == '.') {
                state.dot = true;
                if (*(p + 1))
                    goto one_more;
            }
            if (*p == '-') {
                state.left_pad = true;
                if (*(p + 1))
                    goto one_more;
            }
            if (*p == '+') {
                state.always_sign = true;
                if (*(p + 1))
                    goto one_more;
            }
            if (!state.zero_pad && !state.field_width && *p == '0') {
                state.zero_pad = true;
                if (*(p + 1))
                    goto one_more;
            }
            if (*p >= '0' && *p <= '9') {
                if (!state.dot) {
                    state.field_width *= 10;
                    state.field_width += *p - '0';
                    if (*(p + 1))
                        goto one_more;
                } else {
                    if (!state.has_fraction_length) {
                        state.has_fraction_length = true;
                        state.fraction_length = 0;
                    }
                    state.fraction_length *= 10;
                    state.fraction_length += *p - '0';
                    if (*(p + 1))
                        goto one_more;
                }
            }
            if (*p == '*') {
                state.field_width = NextArgument<int>()(ap);
                if (*(p + 1))
                    goto one_more;
            }
            if (*p == 'l') {
                ++state.long_qualifiers;
                if (*(p + 1))
                    goto one_more;
            }
            if (*p == 'z') {
                state.size_qualifier = true;
                if (*(p + 1))
                    goto one_more;
            }
            if (*p == '#') {
                state.alternate_form = true;
                if (*(p + 1))
                    goto one_more;
            }
            switch (*p) {
            case '%':
                ret += impl.format_percent(state, ap);
                break;

                PRINTF_IMPL_DELEGATE_TO_IMPL(P);
                PRINTF_IMPL_DELEGATE_TO_IMPL(Q);
                PRINTF_IMPL_DELEGATE_TO_IMPL(X);
                PRINTF_IMPL_DELEGATE_TO_IMPL(b);
                PRINTF_IMPL_DELEGATE_TO_IMPL(c);
                PRINTF_IMPL_DELEGATE_TO_IMPL(d);
#ifndef KERNEL
                PRINTF_IMPL_DELEGATE_TO_IMPL(f);
                PRINTF_IMPL_DELEGATE_TO_IMPL(g);
#endif
                PRINTF_IMPL_DELEGATE_TO_IMPL(i);
                PRINTF_IMPL_DELEGATE_TO_IMPL(n);
                PRINTF_IMPL_DELEGATE_TO_IMPL(o);
                PRINTF_IMPL_DELEGATE_TO_IMPL(p);
                PRINTF_IMPL_DELEGATE_TO_IMPL(q);
                PRINTF_IMPL_DELEGATE_TO_IMPL(s);
                PRINTF_IMPL_DELEGATE_TO_IMPL(u);
                PRINTF_IMPL_DELEGATE_TO_IMPL(w);
                PRINTF_IMPL_DELEGATE_TO_IMPL(x);
            default:
                ret += impl.format_unrecognized(*p, fmt, state, ap);
                break;
            }
        } else {
            putch(bufptr, *p);
            ++ret;
        }
    }
    return ret;
}

#undef PRINTF_IMPL_DELEGATE_TO_IMPL

}

using PrintfImplementation::printf_internal;

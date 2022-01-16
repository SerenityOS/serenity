/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>
#include <AK/StdLibExtras.h>
#include <AK/Types.h>
#include <stdarg.h>

#ifdef __serenity__
extern "C" size_t strlen(const char*);
#else
#    include <string.h>
#endif

namespace PrintfImplementation {

template<typename PutChFunc, typename T, typename CharType>
ALWAYS_INLINE int print_hex(PutChFunc putch, CharType*& bufptr, T number, bool upper_case, bool alternate_form, bool left_pad, bool zero_pad, u8 field_width)
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
            constexpr const char* printf_hex_digits_lower = "0123456789abcdef";
            constexpr const char* printf_hex_digits_upper = "0123456789ABCDEF";

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

static constexpr auto powers_of_10 = [] {
    Array<u64, 8> table {};
    u64 last_power_of_10 = 1;
    for (size_t i = 0; i < 8; ++i) {
        auto power = i + 1;
        auto current_power_of_10 = last_power_of_10;
        while (last_power_of_10 < (1ull << (power * 8 - 1))) {
            current_power_of_10 = last_power_of_10;
            last_power_of_10 *= 10ull;
        }

        table[i] = current_power_of_10;
    }
    return table;
}();

template<typename PutChFunc, Unsigned Number, typename CharType>
ALWAYS_INLINE int print_number(PutChFunc putch, CharType*& bufptr, Number number, bool left_pad, bool zero_pad, u32 field_width)
{
    Number divisor = powers_of_10[sizeof(Number) - 1];
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

template<typename PutChFunc, typename CharType>
ALWAYS_INLINE int print_double(PutChFunc putch, CharType*& bufptr, double number, bool left_pad, bool zero_pad, u32 field_width, u32 fraction_length)
{
    int length = 0;

    if (number < 0) {
        putch(bufptr, '-');
        length++;
        number = 0 - number;
    }

    auto number_as_integer = static_cast<u64>(number);
    length = print_number(putch, bufptr, number_as_integer, left_pad, zero_pad, field_width);
    putch(bufptr, '.');
    length++;
    double fraction = number - number_as_integer;

    for (u32 i = 0; i < fraction_length; ++i)
        fraction = fraction * 10;

    return length + print_number(putch, bufptr, (u64)fraction, false, true, fraction_length);
}

template<typename PutChFunc, typename CharType>
ALWAYS_INLINE int print_octal_number(PutChFunc putch, CharType*& bufptr, u32 number, bool left_pad, bool zero_pad, u32 field_width)
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

template<typename PutChFunc, typename CharType>
ALWAYS_INLINE int print_string(PutChFunc putch, CharType*& bufptr, const char* str, size_t len, bool left_pad, size_t field_width, bool dot, size_t fraction_length, bool has_fraction)
{
    if (has_fraction)
        len = min(len, fraction_length);

    if (!dot && (!field_width || field_width < len))
        field_width = len;

    if (has_fraction && !field_width)
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

template<typename PutChFunc, Signed Number, typename CharType>
ALWAYS_INLINE int print_signed_number(PutChFunc putch, CharType*& bufptr, Number number, bool left_pad, bool zero_pad, u32 field_width, bool always_sign)
{
    if (number < 0) {
        putch(bufptr, '-');
        return print_number(putch, bufptr, static_cast<MakeUnsigned<Number>>(0 - number), left_pad, zero_pad, field_width) + 1;
    }
    if (always_sign)
        putch(bufptr, '+');
    return print_number(putch, bufptr, static_cast<MakeUnsigned<Number>>(0 - number), left_pad, zero_pad, field_width) + always_sign;
}

struct ModifierState {
    bool left_pad { false };
    bool zero_pad { false };
    bool dot { false };
    unsigned field_width { 0 };
    bool has_fraction_length { false };
    unsigned fraction_length { 6 };
    enum SizeQualifier {
        Default,
        Char,
        Short,
        Int,
        Long,
        LongLong,
        Size,
        Ptr,
        LongDouble,
    } size_qualifier { Default };
    bool alternate_form { 0 };
    bool always_sign { false };
};

template<typename PutChFunc, typename ArgumentListRefT, template<typename T, typename U = ArgumentListRefT> typename NextArgument, typename CharType = char>
struct PrintfImpl {
    ALWAYS_INLINE PrintfImpl(PutChFunc& putch, CharType*& bufptr, const int& nwritten)
        : m_bufptr(bufptr)
        , m_nwritten(nwritten)
        , m_putch(putch)
    {
    }

#define FN(name) [this](auto&&... args) { return name(args...); }

    template<auto DefaultQualifier, template<typename> typename TransformIfNeeded, auto... InvalidQualifiers, typename Func, typename... Args>
    ALWAYS_INLINE auto apply_with_specified_type(ModifierState::SizeQualifier qualifier, ArgumentListRefT ap, Func&& fn, Args&&... args) const
    {
        if (qualifier == ModifierState::Default)
            qualifier = DefaultQualifier;

        constexpr auto is_valid = [](auto q) { return ((q != InvalidQualifiers) && ...); };

        switch (qualifier) {
        case ModifierState::Default:
            VERIFY_NOT_REACHED();
        case ModifierState::Char:
            // Note: char is promoted to int through varargs.
            if constexpr (is_valid(ModifierState::Char))
                return fn(m_putch, m_bufptr, TransformIfNeeded<u8>(NextArgument<TransformIfNeeded<unsigned>>()(ap)), forward<Args>(args)...);
            break;
        case ModifierState::Short:
            // Note: short is promoted to int through varargs.
            if constexpr (is_valid(ModifierState::Short))
                return fn(m_putch, m_bufptr, TransformIfNeeded<u16>(NextArgument<TransformIfNeeded<unsigned>>()(ap)), forward<Args>(args)...);
            break;
        case ModifierState::Int:
            if constexpr (is_valid(ModifierState::Int))
                return fn(m_putch, m_bufptr, NextArgument<TransformIfNeeded<u32>>()(ap), forward<Args>(args)...);
            break;
        case ModifierState::Long:
            if constexpr (is_valid(ModifierState::Long))
                return fn(m_putch, m_bufptr, NextArgument<TransformIfNeeded<u32>>()(ap), forward<Args>(args)...);
            break;
        case ModifierState::LongLong:
            if constexpr (is_valid(ModifierState::LongLong))
                return fn(m_putch, m_bufptr, NextArgument<TransformIfNeeded<u64>>()(ap), forward<Args>(args)...);
            break;
        case ModifierState::Size:
            if constexpr (is_valid(ModifierState::Size))
                return fn(m_putch, m_bufptr, NextArgument<TransformIfNeeded<size_t>>()(ap), forward<Args>(args)...);
            break;
        case ModifierState::Ptr:
            if constexpr (is_valid(ModifierState::Ptr))
                return fn(m_putch, m_bufptr, NextArgument<TransformIfNeeded<ptrdiff_t>>()(ap), forward<Args>(args)...);
            break;
        case ModifierState::LongDouble:
            if constexpr (is_valid(ModifierState::LongDouble))
                return fn(m_putch, m_bufptr, NextArgument<long double>()(ap), forward<Args>(args)...);
            break;
        }

        dbgln("PrintfImplementation: A given qualifier did not apply to its format type");
        using ReturnType = decltype(fn(m_putch, m_bufptr, declval<TransformIfNeeded<int>>(), forward<Args>(args)...));
        if constexpr (IsVoid<ReturnType>)
            return;
        else
            return ReturnType {};
    }

    ALWAYS_INLINE int format_s(const ModifierState& state, ArgumentListRefT ap) const
    {
        const char* sp = NextArgument<const char*>()(ap);
        if (!sp)
            sp = "(null)";
        return print_string(m_putch, m_bufptr, sp, strlen(sp), state.left_pad, state.field_width, state.dot, state.fraction_length, state.has_fraction_length);
    }
    ALWAYS_INLINE int format_d(const ModifierState& state, ArgumentListRefT ap) const
    {
        return apply_with_specified_type<ModifierState::Int, MakeSigned, ModifierState::Ptr, ModifierState::LongDouble>(
            state.size_qualifier, ap, FN(print_signed_number), state.left_pad, state.zero_pad, state.field_width, state.always_sign);
    }
    ALWAYS_INLINE int format_i(const ModifierState& state, ArgumentListRefT ap) const
    {
        return format_d(state, ap);
    }
    ALWAYS_INLINE int format_u(const ModifierState& state, ArgumentListRefT ap) const
    {
        return apply_with_specified_type<ModifierState::Int, MakeUnsigned, ModifierState::Ptr, ModifierState::LongDouble>(
            state.size_qualifier, ap, FN(print_number), state.left_pad, state.zero_pad, state.field_width);
    }
    ALWAYS_INLINE int format_Q(const ModifierState& state, ArgumentListRefT ap) const
    {
        return print_number(m_putch, m_bufptr, NextArgument<u64>()(ap), state.left_pad, state.zero_pad, state.field_width);
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
        // FIXME: This needs to be implemented!
        if (state.size_qualifier == ModifierState::LongDouble)
            dbgln("PrintfImplementation: Unimplemented long double size modifier - ignoring!");
        return print_double(m_putch, m_bufptr, NextArgument<double>()(ap), state.left_pad, state.zero_pad, state.field_width, state.fraction_length);
    }
    ALWAYS_INLINE int format_o(const ModifierState& state, ArgumentListRefT ap) const
    {
        if (state.alternate_form)
            m_putch(m_bufptr, '0');

        auto result = apply_with_specified_type<ModifierState::Int, MakeUnsigned, ModifierState::Ptr, ModifierState::LongDouble>(
            state.size_qualifier, ap, FN(print_octal_number), state.left_pad, state.zero_pad, state.field_width);

        return (state.alternate_form ? 1 : 0) + result;
    }
    ALWAYS_INLINE int format_x(const ModifierState& state, ArgumentListRefT ap) const
    {
        return apply_with_specified_type<ModifierState::Int, MakeUnsigned, ModifierState::Ptr, ModifierState::LongDouble>(
            state.size_qualifier, ap, FN(print_hex), false, state.alternate_form, state.left_pad, state.zero_pad, state.field_width);
    }
    ALWAYS_INLINE int format_X(const ModifierState& state, ArgumentListRefT ap) const
    {
        return apply_with_specified_type<ModifierState::Int, MakeUnsigned, ModifierState::Ptr, ModifierState::LongDouble>(
            state.size_qualifier, ap, FN(print_hex), true, state.alternate_form, state.left_pad, state.zero_pad, state.field_width);
    }
    ALWAYS_INLINE int format_n(const ModifierState& state, ArgumentListRefT ap) const
    {
        auto fn = [](auto&, auto&, auto* ptr, auto nwritten) {
            *ptr = nwritten;
        };

        apply_with_specified_type<ModifierState::Int, RawPtr, ModifierState::LongDouble>(
            state.size_qualifier, ap, fn, m_nwritten);

        return 0;
    }
    ALWAYS_INLINE int format_p(const ModifierState&, ArgumentListRefT ap) const
    {
        return print_hex(m_putch, m_bufptr, NextArgument<FlatPtr>()(ap), false, true, false, true, 8);
    }
    ALWAYS_INLINE int format_P(const ModifierState&, ArgumentListRefT ap) const
    {
        return print_hex(m_putch, m_bufptr, NextArgument<FlatPtr>()(ap), true, true, false, true, 8);
    }
    ALWAYS_INLINE int format_percent(const ModifierState&, ArgumentListRefT) const
    {
        m_putch(m_bufptr, '%');
        return 1;
    }
    ALWAYS_INLINE int format_c(const ModifierState& state, ArgumentListRefT ap) const
    {
        char c = NextArgument<int>()(ap);
        return print_string(m_putch, m_bufptr, &c, 1, state.left_pad, state.field_width, state.dot, state.fraction_length, state.has_fraction_length);
    }
    ALWAYS_INLINE int format_unrecognized(CharType format_op, const CharType* fmt, const ModifierState&, ArgumentListRefT) const
    {
        dbgln("printf_internal: Unimplemented format specifier {} (fmt: {})", format_op, fmt);
        return 0;
    }

#undef FN

protected:
    CharType*& m_bufptr;
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

template<typename PutChFunc, template<typename T, typename U, template<typename X, typename Y> typename V, typename C = char> typename Impl = PrintfImpl, typename ArgumentListT = va_list, template<typename T, typename V = decltype(declval<ArgumentListT&>())> typename NextArgument = VaArgNextArgument, typename CharType = char>
ALWAYS_INLINE int printf_internal(PutChFunc putch, IdentityType<CharType>* buffer, const CharType*& fmt, ArgumentListT ap)
{
    int ret = 0;
    CharType* bufptr = buffer;

    Impl<PutChFunc, ArgumentListT&, NextArgument, CharType> impl { putch, bufptr, ret };

    for (const CharType* p = fmt; *p; ++p) {
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
            if (!state.zero_pad && !state.field_width && !state.has_fraction_length && *p == '0') {
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
                        state.zero_pad = true;
                        state.fraction_length = 0;
                    }
                    state.fraction_length *= 10;
                    state.fraction_length += *p - '0';
                    if (*(p + 1))
                        goto one_more;
                }
            }
            if (*p == '*') {
                if (state.dot) {
                    state.has_fraction_length = true;
                    state.zero_pad = true;
                    state.fraction_length = NextArgument<int>()(ap);
                } else {
                    state.field_width = NextArgument<int>()(ap);
                }

                if (*(p + 1))
                    goto one_more;
            }
            if (*p == 'l') {
                if (state.size_qualifier == ModifierState::Long)
                    state.size_qualifier = ModifierState::LongLong;
                else if (state.size_qualifier == ModifierState::Default)
                    state.size_qualifier = ModifierState::Long;

                if (*(p + 1))
                    goto one_more;
            }
            if (*p == 'L') {
                state.size_qualifier = ModifierState::LongDouble;
                if (*(p + 1))
                    goto one_more;
            }
            if (*p == 'h') {
                state.size_qualifier = ModifierState::Short;
                if (*(p + 1) == 'h') {
                    ++p;
                    state.size_qualifier = ModifierState::Char;
                }
                if (*(p + 1))
                    goto one_more;
            }
            if (*p == 'z') {
                state.size_qualifier = ModifierState::Size;
                if (*(p + 1))
                    goto one_more;
            }
            if (*p == 't') {
                state.size_qualifier = ModifierState::Ptr;
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

/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Peter Elliott <pelliott@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

// Referencs to the C11 spec in this file are based on draft N1548
// https://www.open-std.org/jtc1/sc22/wg14/www/docs/n1548.pdf

#pragma once

#include <AK/Format.h>
#include <AK/Types.h>
#include <stdarg.h>
#include <stdint.h>

#ifndef KERNEL
#    include <math.h>
#    include <wchar.h>
#endif

#ifdef AK_OS_SERENITY
extern "C" size_t strlen(char const*);
#else
#    include <string.h>
#endif

#ifdef KERNEL
ALWAYS_INLINE int tolower(int c)
{
    if (c >= 'A' && c <= 'Z')
        return c | 0x20;
    return c;
}

ALWAYS_INLINE int isupper(int c)
{
    return (c >= 'A' && c <= 'Z');
}

ALWAYS_INLINE int isdigit(int c)
{
    return (c >= '0' && c <= '9');
}
#else
#    include <ctype.h>
#endif

namespace PrintfImplementation {

#ifdef AK_HAS_FLOAT_128
using fraction_t = unsigned __int128;
#else
using fraction_t = uint64_t;
#endif

ALWAYS_INLINE int number_length(fraction_t number, int base)
{
    int digits = 0;
    for (size_t n = number; n != 0; n /= base)
        ++digits;

    return digits;
}

ALWAYS_INLINE fraction_t number_divisor(fraction_t number, int base)
{
    fraction_t divisor = 1;
    for (size_t n = number / base; n != 0; n /= base)
        divisor *= base;

    return divisor;
}

#ifndef KERNEL
ALWAYS_INLINE void get_fraction(fraction_t& integral, fraction_t& fraction, long double absnumber, int base, int precision)
{
    integral = (fraction_t)absnumber;
    long double f = absnumber - integral;

    fraction_t carry = 1;
    for (int i = 0; i < precision; ++i) {
        f *= base;
        carry *= base;
    }
    // FIXME: Use roundl when it works.
    fraction = ((fraction_t)f) + ((f - (fraction_t)f) >= 0.5);
    // carry == 0 when we are using max_precision, where we wouldn't round anyway.
    if (fraction == carry && carry != 0) {
        fraction = 0;
        integral++;
    }
}

ALWAYS_INLINE fraction_t normalize_fraction(fraction_t fraction, int base)
{
    if (fraction == 0)
        return 0;

    while (fraction % base == 0) {
        fraction /= base;
    }

    return fraction;
}
#endif

template<typename PutChFunc, typename CharType>
ALWAYS_INLINE int print_digits(PutChFunc putch, CharType*& bufptr, fraction_t number, int base, bool uppercase = false, bool trailing_zeros = true)
{
    constexpr char const* printf_hex_digits_lower = "0123456789abcdef";
    constexpr char const* printf_hex_digits_upper = "0123456789ABCDEF";

    if (number == 0)
        return 0;

    int n = 0;
    for (fraction_t d = number_divisor(number, base); d > 0; d /= base) {

        putch(bufptr, (uppercase ? printf_hex_digits_upper : printf_hex_digits_lower)[(number / d) % base]);
        ++n;

        if (!trailing_zeros && number % d == 0)
            break;
    }
    return n;
}

enum class LengthModifier {
    Default,
    Char,
    Short,
    Long,
    LongLong,
    IntMax,
    Size,
    PtrDiff,
    LongDouble,
};

constexpr int asterix_field = -1;

// C11 7.21.6.1p4
struct FormatSpecifier {
    // C11 7.21.6.1p6
    struct {
        bool left_justify { false };
        bool always_sign { false };
        bool space_no_sign { false };
        bool alternate_form { false };
        bool zero_pad { false };
        bool uppercase { false }; // Not a true flag, calculated from case of conversion specifier.
    } flags;
    int field_width { 0 };
    Optional<int> precision {};
    char conversion_specifier { 0 };
    LengthModifier length_modifier { LengthModifier::Default };
};

ALWAYS_INLINE bool is_float_specifier(char c)
{
    c = tolower(c);
    return c == 'e' || c == 'f' || c == 'g' || c == 'a';
}

ALWAYS_INLINE bool is_signed_specifier(char c)
{
    c = tolower(c);
    return c == 'd' || c == 'i';
}

ALWAYS_INLINE bool is_unsigned_specifier(char c)
{
    c = tolower(c);
    return c == 'o' || c == 'u' || c == 'x' || c == 'p';
}

ALWAYS_INLINE bool is_integer_specifier(char c)
{
    return is_unsigned_specifier(c) || is_signed_specifier(c);
}

template<typename T, typename V>
struct VaArgNextArgument {
    ALWAYS_INLINE T operator()(V ap) const
    {
#ifdef AK_OS_WINDOWS
        // GCC on msys2 complains about the type of ap,
        // so let's force the compiler to believe it's a
        // va_list.
        return va_arg((va_list&)ap, T);
#else
        return va_arg(ap, T);
#endif
    }
};

template<typename ArgumentListRefT, template<typename T, typename V = ArgumentListRefT> typename NextArgument>
ALWAYS_INLINE void normalize_format_specifier(FormatSpecifier& fmt, ArgumentListRefT ap)
{
    if (isupper(fmt.conversion_specifier)) {
        fmt.flags.uppercase = true;
        fmt.conversion_specifier = tolower(fmt.conversion_specifier);
    }

    // C11 7.21.6.1p6: "If the space and + flags both appear, the space flag is ignored."
    if (fmt.flags.always_sign) {
        fmt.flags.space_no_sign = false;
    }

    // C11 7.21.6.1p5: "The arguments specifying field width, or precision, or both, shall appear (in that order) before the argument (if any) to be converted"
    if (fmt.field_width == asterix_field) {
        fmt.field_width = NextArgument<int>()(ap);
    }

    // C11 7.21.6.1p5: "The arguments specifying field width, or precision, or both, shall appear (in that order) before the argument (if any) to be converted"
    if (fmt.precision == asterix_field) {
        fmt.precision = NextArgument<int>()(ap);
    }

    // C11 7.21.6.1p6: "For d, i, o, u, x, and X conversions, if a precision is specified, the 0 flag is ignored."
    if (fmt.precision.has_value() && is_integer_specifier(fmt.conversion_specifier)) {
        fmt.flags.zero_pad = false;
    }

    if (is_unsigned_specifier(fmt.conversion_specifier)) {
        // C11 7.21.6.1p6: "The result of a *signed* conversion always begins with a plus or minus sign."
        fmt.flags.always_sign = false;
        // C11 7.21.6.1p6: "If the first character of a *signed* conversion is not a sign, or if a *signed* conversion results in no characters, a space is prefixed to the result"
        fmt.flags.space_no_sign = false;
    }
}

template<typename PutChFunc, typename CharType, typename ArgumentListRefT, template<typename T, typename V = ArgumentListRefT> typename NextArgument>
ALWAYS_INLINE int do_format(PutChFunc putch, CharType*& bufptr, FormatSpecifier const& fmt, ArgumentListRefT ap)
{
    int n_emitted = 0;
    int expected_len = 0;
    int base = 0;

    // Helper functions.
    auto expect_chars = [&](int nchars) {
        expected_len += nchars;
    };

    auto expect_chars_if = [&](bool pred, int nchars) {
        if (pred)
            expect_chars(nchars);
    };

    auto emit_char = [&](char ch) {
        putch(bufptr, ch);
        ++n_emitted;
    };

    auto emit_sign = [&](bool neg) {
        if (neg) {
            emit_char('-');
            return 1;
        } else if (fmt.flags.always_sign) {
            emit_char('+');
            return 1;
        } else if (fmt.flags.space_no_sign) {
            emit_char(' ');
            return 1;
        }

        return 0;
    };

    auto emit_pad = [&](char pad) {
        if (expected_len >= fmt.field_width)
            return 0;

        int n = fmt.field_width - expected_len;
        for (int i = 0; i < n; ++i) {
            emit_char(pad);
        }
        return n;
    };

    auto emit_precision_pad = [&](int n) {
        for (int i = 0; i < n; ++i) {
            emit_char('0');
        }
        return n;
    };

    auto emit_digits = [&](fraction_t number, int min_digits = 0, bool trailing_zeros = true) {
        int digits = number_length(number, base);
        if ((number != 0 || trailing_zeros) && digits < min_digits)
            emit_precision_pad(min_digits - digits);
        n_emitted += print_digits(putch, bufptr, number, base, fmt.flags.uppercase, trailing_zeros);
    };

    auto emit_str = [&](auto str, int maxlen = -1) {
        for (; *str != 0; ++str) {
            if (maxlen == 0)
                break;

            emit_char(*str);
            --maxlen;
        }
    };

    // Formatting.
    if (fmt.conversion_specifier == 's') {
        // FIXME: Narrow characters should be converted to wide characters on the fly and vice versa.
        // FIXME: NULL values

#ifndef KERNEL
        if (fmt.length_modifier == LengthModifier::Long) {
            // C11 7.21.6.1p8: "If an l length modifier is present, the argument shall be a pointer to the initial element of an array of wchar_t type."
            wchar_t const* str = NextArgument<wchar_t const*>()(ap);

            int slen = wcslen(str);
            // C11 7.21.6.1p8: "If the precision is specified, no more than that many bytes are written"
            int maxlen = (fmt.precision.has_value()) ? min(slen, fmt.precision.value()) : slen;
            expect_chars(maxlen);

            if (fmt.flags.left_justify) {
                emit_str(str, maxlen);
                emit_pad(' ');
            } else {
                emit_pad(' ');
                emit_str(str, maxlen);
            }
        } else
#endif
        {
            char const* str = NextArgument<char const*>()(ap);

            int slen = strlen(str);
            // C11 7.21.6.1p8: "If the precision is specified, no more than that many bytes are written"
            int maxlen = (fmt.precision.has_value()) ? min(slen, fmt.precision.value()) : slen;
            expect_chars(maxlen);

            if (fmt.flags.left_justify) {
                emit_str(str, maxlen);
                emit_pad(' ');
            } else {
                emit_pad(' ');
                emit_str(str, maxlen);
            }
        }
    } else if (fmt.conversion_specifier == 'c') {
        // FIXME: wide character support
        // C11 7.21.6.1p8: "If no l length modifier is present, the int argument is converted to an unsigned char"
        unsigned char c = NextArgument<int>()(ap);
        expect_chars(1);

        if (fmt.flags.left_justify) {
            emit_char(c);
            emit_pad(' ');
        } else {
            emit_pad(' ');
            emit_char(c);
        }
    } else
#ifndef KERNEL
        if (is_float_specifier(fmt.conversion_specifier)) {

        long double value;
        if (fmt.length_modifier == LengthModifier::LongDouble) {
            // C11 7.21.6.1p7: "Specifies that a following a, A, e, E, f, F, g, or G conversion specifier applies to a long double argument."
            value = NextArgument<long double>()(ap);
        } else {
            value = NextArgument<double>()(ap);
        }

        bool neg = signbit(value);
        value = (neg) ? -value : value;

        bool nan = isnan(value);
        bool inf = isinf(value);

        base = 10;
        // C11 7.21.6.1p8: "A double argument representing a floating-point number is converted in the style [−]0xh.hhhhp±d"
        if (fmt.conversion_specifier == 'a')
            base = 16;

#    ifdef AK_HAS_FLOAT_128
        // max_precision = ceil(mantissa_bits / log_2(base))
        int max_precision = (base == 10) ? 34 : 28; // space for 112 bits of mantissa
#    else
        int max_precision = (base == 10) ? 19 : 16; // Space for 63 bits of mantissa.
#    endif

        // C11 7.21.6.1p8: "If the precision is missing, it is taken as 6"
        int precision = 6;
        // C11 7.21.6.1p8: "if the precision is missing and FLT_RADIX is a power of 2, then the precision is sufficient for an exact representation of the value;"
        if (fmt.conversion_specifier == 'a')
            precision = max_precision;

        if (fmt.precision.has_value())
            precision = fmt.precision.value();

        bool exponential = fmt.conversion_specifier == 'e' || fmt.conversion_specifier == 'g' || fmt.conversion_specifier == 'a';

        // C11 7.21.6.1p8: "The exponent always contains at least one digit, and only as many more digits as necessary to represent the decimal exponent of 2"
        int exponent_base = (fmt.conversion_specifier == 'a') ? 2 : 10;
        long double saved_value = value;
        int exponent = 0;
        if (exponential && !inf) {
            while (value >= base) {
                value /= exponent_base;
                ++exponent;
            }
            while (value < 1 && value != 0) {
                value *= exponent_base;
                --exponent;
            }
        }

        if (fmt.conversion_specifier == 'g') {
            // C11 7.21.6.1p8: "if P > X ≥ −4, the conversion is with style f (or F) and precision P − (X + 1)."
            if (exponent >= -4 && max(precision, 1) > exponent) {
                exponential = false;
                value = saved_value;
                precision -= exponent + 1;
            } else {
                // C11 7.21.6.1p8: "otherwise, the conversion is with style e (or E) and precision P − 1."
                precision -= 1;
            }
        }

        // If the precision is higher than is representable, pad with trailing zeros.
        int unused_precision = 0;
        if (precision > max_precision) {
            unused_precision = precision - max_precision;
            precision = max_precision;
        }

        fraction_t integral, fraction;
        get_fraction(integral, fraction, value, base, precision);

        // C11 7.21.6.1p8: "unless the # flag is used, any trailing zeros are removed from the fractional portion of the result and the decimal-point character is removed if there is no fractional portion remaining."
        // C11 7.21.6.1p8: "except that trailing zeros may be omitted"
        bool full_precision = !((fmt.conversion_specifier == 'g' || fmt.conversion_specifier == 'a') && !fmt.flags.alternate_form);

        // Calculate length.
        expect_chars_if(neg || fmt.flags.always_sign || fmt.flags.space_no_sign, 1);
        if (nan || inf) {
            expect_chars(3);
        } else {
            int digits = number_length(integral, base);
            expect_chars(max(digits, 1));
            expect_chars_if((full_precision && precision != 0) || fmt.flags.alternate_form || fraction != 0, 1);
            expect_chars((full_precision) ? precision : number_length(normalize_fraction(fraction, base), base));
            expect_chars_if(full_precision, unused_precision);
            if (exponential) {
                expect_chars(2);                                                     // e+
                expect_chars_if(fmt.conversion_specifier == 'a' && !nan && !inf, 2); // 0x
                expect_chars(max((fmt.conversion_specifier == 'a') ? 1 : 2, number_length(exponent, base)));
            }
        }

        // Output Characters.
        auto emit_digits_or_nan = [&](fraction_t integral, fraction_t fraction) {
            if (nan) {
                // C11 7.21.6.1p8: "A double argument representing a NaN is converted in one of the styles [-]nan or [-]nan(n-char-sequence)"
                emit_str((fmt.flags.uppercase) ? "NAN" : "nan");
            } else if (inf) {
                // C11 7.21.6.1p8: "A double argument representing an infinity is converted in one of the styles [-]inf or [-]infinity"
                emit_str((fmt.flags.uppercase) ? "INF" : "inf");
            } else {
                emit_digits(integral, 1);
                // C11 7.21.6.1p8: "if the precision is zero and the # flag is not specified, no decimal-point character appears."
                if ((full_precision && precision != 0) || fmt.flags.alternate_form || fraction != 0) {
                    emit_char('.');
                    emit_digits(fraction, precision, full_precision);
                    if (full_precision)
                        emit_precision_pad(unused_precision);
                }

                if (exponential) {
                    if (fmt.conversion_specifier == 'a') {
                        emit_char((fmt.flags.uppercase) ? 'P' : 'p');
                        base = 10;
                    } else {
                        emit_char((fmt.flags.uppercase) ? 'E' : 'e');
                    }

                    if (exponent < 0) {
                        emit_char('-');
                        exponent = -exponent;
                    } else {
                        emit_char('+');
                    }

                    emit_digits(exponent, (fmt.conversion_specifier == 'a') ? 1 : 2);
                }
            }
        };

        if (fmt.flags.left_justify) {
            emit_sign(neg);
            if (fmt.conversion_specifier == 'a' && !nan && !inf)
                emit_str((fmt.flags.uppercase) ? "0X" : "0x");
            emit_digits_or_nan(integral, fraction);
            emit_pad(' ');
        } else if (fmt.flags.zero_pad && !(nan || inf)) {
            emit_sign(neg);
            if (fmt.conversion_specifier == 'a' && !nan && !inf)
                emit_str((fmt.flags.uppercase) ? "0X" : "0x");
            emit_pad('0');
            emit_digits_or_nan(integral, fraction);
        } else {
            emit_pad(' ');
            emit_sign(neg);
            if (fmt.conversion_specifier == 'a' && !nan && !inf)
                emit_str((fmt.flags.uppercase) ? "0X" : "0x");
            emit_digits_or_nan(integral, fraction);
        }
    } else
#endif
        if (is_integer_specifier(fmt.conversion_specifier)) {
        fraction_t value;
        bool neg = false;
        if (is_signed_specifier(fmt.conversion_specifier)) {
            intmax_t svalue;
            switch (fmt.length_modifier) {
            case LengthModifier::Char:
                // C11 7.21.6.1p7: "the argument will have been promoted according to the integer promotions, but its value shall be converted to signed char or unsigned char before printing"
                svalue = (signed char)NextArgument<int>()(ap);
                break;
            case LengthModifier::Short:
                // C11 7.21.6.1p7: "the argument will have been promoted according to the integer promotions, but its value shall be converted to signed short int or unsigned short int before printing"
                svalue = (short)NextArgument<int>()(ap);
                break;
            case LengthModifier::Long:
                svalue = NextArgument<long>()(ap);
                break;
            case LengthModifier::LongLong:
                svalue = NextArgument<long long>()(ap);
                break;
            case LengthModifier::IntMax:
                svalue = NextArgument<intmax_t>()(ap);
                break;
            case LengthModifier::Size:
                svalue = NextArgument<ssize_t>()(ap);
                break;
            case LengthModifier::PtrDiff:
                svalue = NextArgument<AK::Detail::MakeSigned<ptrdiff_t>>()(ap);
                break;
            default:
                svalue = NextArgument<int>()(ap);
            }

            neg = svalue < 0;
            // FIXME: `- number` overflows if we are trying to negate the smallest possible value.
            value = (neg) ? -svalue : svalue;
        } else {
            if (fmt.conversion_specifier == 'p') {
                value = NextArgument<uintptr_t>()(ap);
            } else
                switch (fmt.length_modifier) {
                case LengthModifier::Char:
                    // C11: 7.21.6.1p7: "the argument will have been promoted according to the integer promotions, but its value shall be converted to signed char or unsigned char before printing"
                    value = (unsigned char)NextArgument<unsigned int>()(ap);
                    break;
                case LengthModifier::Short:
                    // C11: 7.21.6.1p7: "the argument will have been promoted according to the integer promotions, but its value shall be converted to signed short int or unsigned short int before printing"
                    value = (unsigned short)NextArgument<unsigned int>()(ap);
                    break;
                case LengthModifier::Long:
                    value = NextArgument<unsigned long>()(ap);
                    break;
                case LengthModifier::LongLong:
                    value = NextArgument<unsigned long long>()(ap);
                    break;
                case LengthModifier::IntMax:
                    value = NextArgument<uintmax_t>()(ap);
                    break;
                case LengthModifier::Size:
                    value = NextArgument<size_t>()(ap);
                    break;
                case LengthModifier::PtrDiff:
                    value = NextArgument<ptrdiff_t>()(ap);
                    break;
                default:
                    value = NextArgument<unsigned int>()(ap);
                }
        }

        switch (fmt.conversion_specifier) {
        case 'd':
        case 'i':
        case 'u':
            base = 10;
            break;
        case 'o':
            base = 8;
            break;
        case 'x':
        case 'p':
            base = 16;
            break;
        }

        // C11 7.21.6.1p8: "The default precision is 1."
        int precision = 1;
        if (fmt.precision.has_value())
            precision = fmt.precision.value();

        if (fmt.conversion_specifier == 'p')
            precision = max(1, precision);

        // Calculate length.
        expect_chars_if(neg || fmt.flags.always_sign || fmt.flags.space_no_sign, 1);

        int digits = number_length(value, base);
        if (fmt.conversion_specifier == 'p' && !value) {
            // (null)
            expect_chars(6);
        } else {
            expect_chars(max(digits, precision));
        }

        expect_chars_if(fmt.conversion_specifier == 'o' && fmt.flags.alternate_form && digits >= precision, 1);
        expect_chars_if(fmt.conversion_specifier == 'x' && digits && fmt.flags.alternate_form, 2);
        expect_chars_if(fmt.conversion_specifier == 'p' && value, 2);

        // Output characters.
        auto alternate_forms = [&]() {
            // C11 7.21.6.1p6: "For x (or X) conversion, a nonzero result has 0x (or 0X) prefixed to it"
            if (digits && ((fmt.conversion_specifier == 'x' && fmt.flags.alternate_form) || (fmt.conversion_specifier == 'p' && value)))
                emit_str((fmt.flags.uppercase) ? "0X" : "0x");

            // C11 7.21.6.1p6: "For o conversion, it increases the precision, if and only if necessary, to force the first digit of the result to be a zero (if the value and precision are both 0, a single 0 is printed)"
            if (fmt.conversion_specifier == 'o' && fmt.flags.alternate_form && digits >= precision)
                emit_str((fmt.flags.uppercase) ? "0" : "0");
        };

        auto emit_digits_or_null = [&](uintmax_t value) {
            if (fmt.conversion_specifier == 'p' && !value) {
                emit_str("(null)");
            } else {
                emit_digits(value, precision);
            }
        };

        if (fmt.flags.left_justify) {
            emit_sign(neg);
            alternate_forms();
            emit_digits_or_null(value);
            emit_pad(' ');
        } else if (fmt.flags.zero_pad) {
            emit_sign(neg);
            alternate_forms();
            emit_pad('0');
            emit_digits_or_null(value);
        } else {
            emit_pad(' ');
            emit_sign(neg);
            alternate_forms();
            emit_digits_or_null(value);
        }
    } else {
        dbgln("printf_internal: Unimplemented conversion specifier {}", fmt.conversion_specifier);
        return 0;
    }

    return n_emitted;
}

template<typename PutChFunc, typename ArgumentListRefT, template<typename T, typename U = ArgumentListRefT> typename NextArgument, typename CharType = char>
struct PrintfImpl {
    ALWAYS_INLINE PrintfImpl(PutChFunc& putch, CharType*& bufptr, int const& nwritten)
        : m_bufptr(bufptr)
        , m_nwritten(nwritten)
        , m_putch(putch)
    {
    }

    ALWAYS_INLINE int format_n(FormatSpecifier const&, ArgumentListRefT ap) const
    {
        // C11 7.21.6.1p8: "The argument shall be a pointer to signed integer into which is written the number of characters written to the output stream so far"
        *NextArgument<int*>()(ap) = m_nwritten;
        return 0;
    }

#define DEFINE_FORMAT(letter)                                                                              \
    ALWAYS_INLINE int format_##letter(FormatSpecifier const& fmt, ArgumentListRefT ap) const               \
    {                                                                                                      \
        return do_format<PutChFunc, CharType, ArgumentListRefT, NextArgument>(m_putch, m_bufptr, fmt, ap); \
    }

    DEFINE_FORMAT(s);
    DEFINE_FORMAT(c);
    DEFINE_FORMAT(d);
    DEFINE_FORMAT(i);
    DEFINE_FORMAT(u);
#ifndef KERNEL
    DEFINE_FORMAT(g);
    DEFINE_FORMAT(f);
    DEFINE_FORMAT(e);
    DEFINE_FORMAT(a);
#endif
    DEFINE_FORMAT(o);
    DEFINE_FORMAT(x);
    DEFINE_FORMAT(p);

#undef DEFINE_FORMAT_NUMBER

    ALWAYS_INLINE int format_unrecognized(FormatSpecifier const& fmt, ArgumentListRefT) const
    {
        dbgln("printf_internal: Unimplemented conversion specifier {}", fmt.conversion_specifier);
        return 0;
    }

protected:
    CharType*& m_bufptr;
    int const& m_nwritten;
    PutChFunc& m_putch;
};

#define PRINTF_IMPL_DELEGATE_TO_IMPL(c)        \
    case *#c:                                  \
        ret += impl.format_##c(specifier, ap); \
        break;

template<typename PutChFunc, template<typename T, typename U, template<typename X, typename Y> typename V, typename C = char> typename Impl = PrintfImpl, typename ArgumentListT = va_list, template<typename T, typename V = decltype(declval<ArgumentListT&>())> typename NextArgument = VaArgNextArgument, typename CharType = char>
ALWAYS_INLINE int printf_internal(PutChFunc putch, IdentityType<CharType>* buffer, CharType const*& fmt, ArgumentListT ap)
{
    int ret = 0;
    CharType* bufptr = buffer;

    Impl<PutChFunc, ArgumentListT&, NextArgument, CharType> impl { putch, bufptr, ret };

    FormatSpecifier specifier;

#define NEXT() ({ ++p; if (!*p) goto handle_format; *p; })

    for (CharType const* p = fmt; *p; ++p) {
        // Clear format specifier.
        specifier = FormatSpecifier();

        // C11 7.21.6.1p4: "Each conversion specification is introduced by the character %"
        if (*p == '%') {
            bool brk = false;

            NEXT();
            if (*p == '%') {
                // C11 7.21.6.1: "%: A % character is written. No argument is converted. The complete conversion specification shall be %%."
                putch(bufptr, '%');
                ++ret;
                continue;
            }

            // C11 7.21.6.1p4: "Zero or more flags (in any order) that modify the meaning of the conversion specification."
            for (;;) {
                switch (*p) {
                case '-':
                    specifier.flags.left_justify = true;
                    break;
                case '+':
                    specifier.flags.always_sign = true;
                    break;
                case ' ':
                    specifier.flags.space_no_sign = true;
                    break;
                case '#':
                    specifier.flags.alternate_form = true;
                    break;
                case '0':
                    specifier.flags.zero_pad = true;
                    break;
                default:
                    brk = true;
                }
                if (brk)
                    break;
                NEXT();
            }

            // C11 7.21.6.1p4: "an optional minimum field width. [...] The field width takes the form of an asterisk * (described later) or a nonnegative decimal integer."
            if (*p == '*') {
                specifier.field_width = asterix_field;
                NEXT();
            } else {
                for (; isdigit(*p); NEXT()) {
                    specifier.field_width *= 10;
                    specifier.field_width += *p - '0';
                }
            }

            // C11 7.21.6.1p4: "an optional precision [...] The precision takes the form of a period (.) followed either by an asterisk * (described later) or by an optional decimal integer"
            if (*p == '.') {
                specifier.precision = 0;
                NEXT();
                if (*p == '*') {
                    specifier.precision = asterix_field;
                    NEXT();
                } else {
                    for (; isdigit(*p); NEXT()) {
                        specifier.precision = specifier.precision.value() * 10;
                        specifier.precision = specifier.precision.value() + (*p - '0');
                    }
                }
            }

            // C11 7.21.6.1p4: "an optional length modifier that specifies the size of the argument."
            switch (*p) {
            case 'h':
                // C11 7.21.6.1p7: "Specifies that a following d, i, o, u, x, or X conversion specifier applies to a short int or unsigned short int argument"
                specifier.length_modifier = LengthModifier::Short;
                NEXT();
                break;
            case 'l':
                // C11 7.21.6.1p7: "Specifies that a following d, i, o, u, x, or X conversion specifier applies to a long int or unsigned long int argument;"
                specifier.length_modifier = LengthModifier::Long;
                NEXT();
                break;
            case 'j':
                // C11 7.21.6.1p7: "Specifies that a following d, i, o, u, x, or X conversion specifier applies to an intmax_t or uintmax_t argument;"
                specifier.length_modifier = LengthModifier::IntMax;
                NEXT();
                break;
            case 'z':
                // C11 7.21.6.1p7: "Specifies that a following d, i, o, u, x, or X conversion specifier applies to a size_t or the corresponding signed integer type argument;"
                specifier.length_modifier = LengthModifier::Size;
                NEXT();
                break;
            case 't':
                // C11 7.21.6.1p7: "Specifies that a following d, i, o, u, x, or X conversion specifier applies to a ptrdiff_t or the corresponding unsigned integer type argument;"
                specifier.length_modifier = LengthModifier::PtrDiff;
                NEXT();
                break;
            case 'L':
                // C11 7.21.6.1p7: "Specifies that a following a, A, e, E, f, F, g, or G conversion specifier applies to a long double argument."
                specifier.length_modifier = LengthModifier::LongDouble;
                NEXT();
                break;
            }

            if (specifier.length_modifier == LengthModifier::Short && *p == 'h') {
                // C11 7.21.6.1p7: "Specifies that a following d, i, o, u, x, or X conversion specifier applies to a signed char or unsigned char argument"
                specifier.length_modifier = LengthModifier::Char;
                NEXT();
            } else if (specifier.length_modifier == LengthModifier::Long && *p == 'l') {
                // C11 7.21.6.1p7: "Specifies that a following d, i, o, u, x, or X conversion specifier applies to a long long int or unsigned long long int argument;"
                specifier.length_modifier = LengthModifier::LongLong;
                NEXT();
            }

            // C11 7.21.6.1p4: "A conversion specifier character that specifies the type of conversion to be applied."
            specifier.conversion_specifier = *p;

        handle_format:
            if (specifier.conversion_specifier) {
                normalize_format_specifier<ArgumentListT&, NextArgument>(specifier, ap);
                switch (specifier.conversion_specifier) {
                    PRINTF_IMPL_DELEGATE_TO_IMPL(c);
                    PRINTF_IMPL_DELEGATE_TO_IMPL(d);
#ifndef KERNEL
                    PRINTF_IMPL_DELEGATE_TO_IMPL(f);
                    PRINTF_IMPL_DELEGATE_TO_IMPL(g);
                    PRINTF_IMPL_DELEGATE_TO_IMPL(e);
                    PRINTF_IMPL_DELEGATE_TO_IMPL(a);
#endif
                    PRINTF_IMPL_DELEGATE_TO_IMPL(i);
                    PRINTF_IMPL_DELEGATE_TO_IMPL(n);
                    PRINTF_IMPL_DELEGATE_TO_IMPL(o);
                    PRINTF_IMPL_DELEGATE_TO_IMPL(p);
                    PRINTF_IMPL_DELEGATE_TO_IMPL(s);
                    PRINTF_IMPL_DELEGATE_TO_IMPL(u);
                    PRINTF_IMPL_DELEGATE_TO_IMPL(x);
                default:
                    ret += impl.format_unrecognized(specifier, ap);
                    break;
                }
            }
        } else {
            putch(bufptr, *p);
            ++ret;
        }
    }

#undef NEXT
    return ret;
}

#undef PRINTF_IMPL_DELEGATE_TO_IMPL

}

using PrintfImplementation::printf_internal;

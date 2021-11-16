/*
 * Copyright (c) 2021, Leon Albrecht <leon2002.la@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Checked.h>
#include <AK/Concepts.h>
#include <AK/Format.h>
#include <AK/NumericLimits.h>
#include <AK/StdLibExtraDetails.h>
#include <AK/StdLibExtras.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>

namespace AK {

template<typename T>
requires(sizeof(T) >= sizeof(u64) && IsUnsigned<T>) class UFixedBigInt;

// FIXME: This breaks formatting
// template<typename T>
// constexpr inline bool Detail::IsIntegral<UFixedBigInt<T>> = true;

template<typename T>
constexpr inline bool Detail::IsUnsigned<UFixedBigInt<T>> = true;
template<typename T>
constexpr inline bool Detail::IsSigned<UFixedBigInt<T>> = false;

template<typename T>
struct NumericLimits<UFixedBigInt<T>> {
    static constexpr UFixedBigInt<T> min() { return 0; }
    static constexpr UFixedBigInt<T> max() { return { NumericLimits<T>::max(), NumericLimits<T>::max() }; }
    static constexpr bool is_signed() { return false; }
};

template<typename T>
requires(sizeof(T) >= sizeof(u64) && IsUnsigned<T>) class UFixedBigInt {
public:
    using R = UFixedBigInt<T>;

    constexpr UFixedBigInt() = default;
    template<Unsigned U>
    requires(sizeof(T) >= sizeof(U)) constexpr UFixedBigInt(U low)
        : m_low(low)
        , m_high(0u)
    {
    }
    template<Unsigned U, Unsigned U2>
    requires(sizeof(T) >= sizeof(U) && sizeof(T) >= sizeof(U2)) constexpr UFixedBigInt(U low, U2 high)
        : m_low(low)
        , m_high(high)
    {
    }
    constexpr T& low()
    {
        return m_low;
    }
    constexpr const T& low() const
    {
        return m_low;
    }
    constexpr T& high()
    {
        return m_high;
    }
    constexpr const T& high() const
    {
        return m_high;
    }

    Span<u8> bytes()
    {
        return Span<u8>(reinterpret_cast<u8*>(this), sizeof(R));
    }
    Span<const u8> bytes() const
    {
        return Span<const u8>(reinterpret_cast<const u8*>(this), sizeof(R));
    }

    template<Unsigned U>
    requires(sizeof(T) >= sizeof(U)) explicit operator U() const
    {
        return static_cast<U>(m_low);
    }

    // Utils
    constexpr size_t clz() const requires(IsSame<T, u64>)
    {
        if (m_high)
            return __builtin_clzll(m_high);
        else
            return sizeof(T) * 8 + __builtin_clzll(m_low);
    }
    constexpr size_t clz() const requires(!IsSame<T, u64>)
    {
        if (m_high)
            return m_high.clz();
        else
            return sizeof(T) * 8 + m_low.clz();
    }
    constexpr size_t ctz() const requires(IsSame<T, u64>)
    {
        if (m_low)
            return __builtin_ctzll(m_low);
        else
            return sizeof(T) * 8 + __builtin_ctzll(m_high);
    }
    constexpr size_t ctz() const requires(!IsSame<T, u64>)
    {
        if (m_low)
            return m_low.ctz();
        else
            return sizeof(T) * 8 + m_high.ctz();
    }
    constexpr size_t popcnt() const requires(IsSame<T, u64>)
    {
        return __builtin_popcntll(m_low) + __builtin_popcntll(m_high);
    }
    constexpr size_t popcnt() const requires(!IsSame<T, u64>)
    {
        return m_low.popcnt() + m_high.popcnt();
    }

    // Comparison Operations
    constexpr bool operator!() const
    {
        return !m_low && !m_high;
    }
    constexpr explicit operator bool() const
    {
        return m_low || m_high;
    }
    template<Unsigned U>
    requires(sizeof(T) >= sizeof(U)) constexpr bool operator==(const T& other) const
    {
        return !m_high && m_low == other;
    }
    template<Unsigned U>
    requires(sizeof(T) >= sizeof(U)) constexpr bool operator!=(const T& other) const
    {
        return m_high || m_low != other;
    }
    template<Unsigned U>
    requires(sizeof(T) >= sizeof(U)) constexpr bool operator>(const T& other) const
    {
        return m_high || m_low > other;
    }
    template<Unsigned U>
    requires(sizeof(T) >= sizeof(U)) constexpr bool operator<(const T& other) const
    {
        return !m_high && m_low < other;
    }
    template<Unsigned U>
    requires(sizeof(T) >= sizeof(U)) constexpr bool operator>=(const T& other) const
    {
        return *this == other || *this > other;
    }
    template<Unsigned U>
    requires(sizeof(T) >= sizeof(U)) constexpr bool operator<=(const T& other) const
    {
        return *this == other || *this < other;
    }

    constexpr bool operator==(const R& other) const
    {
        return m_low == other.low() && m_high == other.high();
    }
    constexpr bool operator!=(const R& other) const
    {
        return m_low != other.low() || m_high != other.high();
    }
    constexpr bool operator>(const R& other) const
    {
        return m_high > other.high()
            || (m_high == other.high() && m_low > other.low());
    }
    constexpr bool operator<(const R& other) const
    {
        return m_high < other.high()
            || (m_high == other.high() && m_low < other.low());
    }
    constexpr bool operator>=(const R& other) const
    {
        return *this == other || *this > other;
    }
    constexpr bool operator<=(const R& other) const
    {
        return *this == other || *this < other;
    }

    // Bitwise operations
    constexpr R operator~() const
    {
        return { ~m_low, ~m_high };
    }
    template<Unsigned U>
    requires(sizeof(T) >= sizeof(U)) constexpr U operator&(const U& other) const
    {
        return static_cast<const U>(m_low) & other;
    }
    template<Unsigned U>
    requires(sizeof(T) >= sizeof(U)) constexpr R operator|(const U& other) const
    {
        return { m_low | other, m_high };
    }
    template<Unsigned U>
    requires(sizeof(T) >= sizeof(U)) constexpr R operator^(const U& other) const
    {
        return { m_low ^ other, m_high };
    }
    template<Unsigned U>
    constexpr R operator<<(const U& shift) const
    {
        if (shift >= sizeof(R) * 8u)
            return 0u;
        if (shift >= sizeof(T) * 8u)
            return R { 0u, m_low << (shift - sizeof(T) * 8u) };
        if (!shift)
            return *this;

        T overflow = m_low >> (sizeof(T) * 8u - shift);
        return R { m_low << shift, (m_high << shift) | overflow };
    }
    template<Unsigned U>
    constexpr R operator>>(const U& shift) const
    {
        if (shift >= sizeof(R) * 8u)
            return 0u;
        if (shift >= sizeof(T) * 8u)
            return m_high >> (shift - sizeof(T) * 8u);
        if (!shift)
            return *this;

        T underflow = m_high << (sizeof(T) * 8u - shift);
        return R { (m_low >> shift) | underflow, m_high >> shift };
    }
    template<Unsigned U>
    constexpr R rol(const U& shift) const
    {
        return (*this >> sizeof(T) * 8u - shift) | (*this << shift);
    }
    template<Unsigned U>
    constexpr R ror(const U& shift) const
    {
        return (*this << sizeof(T) * 8u - shift) | (*this >> shift);
    }

    constexpr R operator&(const R& other) const
    {
        return { m_low & other.low(), m_high & other.high() };
    }
    constexpr R operator|(const R& other) const
    {
        return { m_low | other.low(), m_high | other.high() };
    }
    constexpr R operator^(const R& other) const
    {
        return { m_low ^ other.low(), m_high ^ other.high() };
    }

    // Bitwise assignment
    template<Unsigned U>
    requires(sizeof(T) >= sizeof(U)) constexpr R& operator&=(const U& other)
    {
        m_high = 0u;
        m_low &= other;
        return *this;
    }
    template<Unsigned U>
    requires(sizeof(T) >= sizeof(U)) constexpr R& operator|=(const U& other)
    {
        m_low |= other;
        return *this;
    }
    template<Unsigned U>
    requires(sizeof(T) >= sizeof(U)) constexpr R& operator^=(const U& other)
    {
        m_low ^= other;
        return *this;
    }
    template<Unsigned U>
    constexpr R& operator>>=(const U& other)
    {
        *this = *this >> other;
        return *this;
    }
    template<Unsigned U>
    constexpr R& operator<<=(const U& other)
    {
        *this = *this << other;
        return *this;
    }

    constexpr R& operator&=(const R& other)
    {
        m_high &= other.high();
        m_low &= other.low();
        return *this;
    }
    constexpr R& operator|=(const R& other)
    {
        m_high |= other.high();
        m_low |= other.low();
        return *this;
    }
    constexpr R& operator^=(const R& other)
    {
        m_high ^= other.high();
        m_low ^= other.low();
        return *this;
    }

    static constexpr size_t my_size()
    {
        return sizeof(R);
    }

    // Arithmetic

    // implies size of less than u64, so passing references isn't useful
    template<Unsigned U>
    requires(sizeof(T) >= sizeof(U) && IsSame<T, u64>) constexpr R addc(const U other, bool& carry) const
    {
        bool low_carry = Checked<T>::addition_would_overflow(m_low, other);
        low_carry |= Checked<T>::addition_would_overflow(m_low, carry);
        bool high_carry = Checked<T>::addition_would_overflow(m_high, low_carry);

        T lower = m_low + other + carry;
        T higher = m_high + low_carry;

        carry = high_carry;

        return {
            lower,
            higher
        };
    }
    template<Unsigned U>
    requires(my_size() > sizeof(U) && sizeof(T) > sizeof(u64)) constexpr R addc(const U& other, bool& carry) const
    {
        T lower = m_low.addc(other, carry);
        T higher = m_high.addc(0u, carry);

        return {
            lower,
            higher
        };
    }
    template<Unsigned U>
    requires(IsSame<R, U>&& IsSame<T, u64>) constexpr R addc(const U& other, bool& carry) const
    {
        bool low_carry = Checked<T>::addition_would_overflow(m_low, other.low());
        bool high_carry = Checked<T>::addition_would_overflow(m_high, other.high());

        T lower = m_low + other.low();
        T higher = m_high + other.high();
        low_carry |= Checked<T>::addition_would_overflow(lower, carry);
        high_carry |= Checked<T>::addition_would_overflow(higher, low_carry);

        lower += carry;
        higher += low_carry;
        carry = high_carry;

        return {
            lower,
            higher
        };
    }
    template<Unsigned U>
    requires(IsSame<R, U> && sizeof(T) > sizeof(u64)) constexpr R addc(const U& other, bool& carry) const
    {
        T lower = m_low.addc(other.low(), carry);
        T higher = m_high.addc(other.high(), carry);

        return {
            lower,
            higher
        };
    }
    template<Unsigned U>
    requires(my_size() < sizeof(U)) constexpr U addc(const U& other, bool& carry) const
    {
        return other.addc(*this, carry);
    }

    // FIXME: subc for sizeof(T) < sizeof(U)
    template<Unsigned U>
    requires(sizeof(T) >= sizeof(U)) constexpr R subc(const U& other, bool& carry) const
    {
        bool low_carry = (!m_low && carry) || (m_low - carry) < other;
        bool high_carry = !m_high && low_carry;

        T lower = m_low - other - carry;
        T higher = m_high - low_carry;
        carry = high_carry;

        return { lower, higher };
    }
    constexpr R subc(const R& other, bool& carry) const
    {
        bool low_carry = (!m_low && carry) || (m_low - carry) < other.low();
        bool high_carry = (!m_high && low_carry) || (m_high - low_carry) < other.high();

        T lower = m_low - other.low() - carry;
        T higher = m_high - other.high() - low_carry;
        carry = high_carry;

        return { lower, higher };
    }

    constexpr R operator+(const bool& other) const
    {
        bool carry = false; // unused
        return addc((u8)other, carry);
    }
    template<Unsigned U>
    constexpr R operator+(const U& other) const
    {
        bool carry = false; // unused
        return addc(other, carry);
    }

    constexpr R operator-(const bool& other) const
    {
        bool carry = false; // unused
        return subc((u8)other, carry);
    }

    template<Unsigned U>
    constexpr R operator-(const U& other) const
    {
        bool carry = false; // unused
        return subc(other, carry);
    }

    template<Unsigned U>
    constexpr R& operator+=(const U& other)
    {
        *this = *this + other;
        return *this;
    }
    template<Unsigned U>
    constexpr R& operator-=(const U& other)
    {
        *this = *this - other;
        return *this;
    }

    constexpr R operator++()
    {
        // x++
        auto old = *this;
        *this += 1;
        return old;
    }
    constexpr R& operator++(int)
    {
        // ++x
        *this += 1;
        return *this;
    }
    constexpr R operator--()
    {
        // x--
        auto old = *this;
        *this -= 1;
        return old;
    }
    constexpr R& operator--(int)
    {
        // --x
        *this -= 1;
        return *this;
    }

    // FIXME: no restraints on this
    template<Unsigned U>
    requires(my_size() >= sizeof(U)) constexpr R div_mod(const U& divisor, U& remainder) const
    {
        // FIXME: Is there a better way to raise a division by 0?
        //        Maybe as a compiletime warning?
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wdiv-by-zero"
        if (!divisor) {
            volatile int x = 1;
            volatile int y = 0;
            [[maybe_unused]] volatile int z = x / y;
        }
#pragma GCC diagnostic pop

        // fastpaths
        if (*this < divisor) {
            remainder = static_cast<U>(*this);
            return 0u;
        }
        if (*this == divisor) {
            remainder = 0u;
            return 1u;
        }
        if (divisor == 1u) {
            remainder = 0u;
            return *this;
        }

        remainder = 0u;
        R quotient = 0u;

        for (ssize_t i = sizeof(R) * 8 - clz() - 1; i >= 0; --i) {
            remainder <<= 1u;
            remainder |= (*this >> (size_t)i) & 1u;
            if (remainder >= divisor) {
                remainder -= divisor;
                quotient |= R { 1u } << (size_t)i;
            }
        }

        return quotient;
    }

    template<Unsigned U>
    constexpr R operator*(U other) const
    {
        R res = 0u;
        R that = *this;
        for (; other != 0u; other >>= 1u) {
            if (other & 1u)
                res += that;
            that <<= 1u;
        }
        return res;
    }

    template<Unsigned U>
    constexpr R operator/(const U& other) const
    {
        U mod { 0u }; // unused
        return div_mod(other, mod);
    }
    template<Unsigned U>
    constexpr U operator%(const U& other) const
    {
        R res { 0u };
        div_mod(other, res);
        return res;
    }

    template<Unsigned U>
    constexpr R& operator*=(const U& other)
    {
        *this = *this * other;
        return *this;
    }
    template<Unsigned U>
    constexpr R& operator/=(const U& other)
    {
        *this = *this / other;
        return *this;
    }
    template<Unsigned U>
    constexpr R& operator%=(const U& other)
    {
        *this = *this % other;
        return *this;
    }

    constexpr R sqrt() const
    {
        // Bitwise method: https://en.wikipedia.org/wiki/Integer_square_root#Using_bitwise_operations
        // the bitwise method seems to be way faster then Newtons:
        // https://quick-bench.com/q/eXZwW1DVhZxLE0llumeCXkfOK3Q
        if (*this == 1u)
            return 1u;

        ssize_t shift = (sizeof(R) * 8 - clz()) & ~1ULL;
        // should be equivalent to:
        // long shift = 2;
        // while ((val >> shift) != 0)
        //   shift += 2;

        R res = 0u;
        while (shift >= 0) {
            res = res << 1u;
            R large_cand = (res | 1u);
            if (*this >> (size_t)shift >= large_cand * large_cand)
                res = large_cand;
            shift -= 2;
        }
        return res;
    }

    constexpr R pow(u64 exp)
    {
        // Montgomery's Ladder Technique
        // https://en.wikipedia.org/wiki/Exponentiation_by_squaring#Montgomery's_ladder_technique
        R x1 = *this;
        R x2 = *this * *this;
        u64 exp_copy = exp;
        for (ssize_t i = sizeof(u64) * 8 - __builtin_clzll(exp) - 2; i >= 0; --i) {
            if (exp_copy & 1u) {
                x2 *= x1;
                x1 *= x1;
            } else {
                x1 *= x2;
                x2 *= x2;
            }
            exp_copy >>= 1u;
        }
        return x1;
    }
    template<Unsigned U>
    requires(sizeof(U) > sizeof(u64)) constexpr R pow(U exp)
    {
        // Montgomery's Ladder Technique
        // https://en.wikipedia.org/wiki/Exponentiation_by_squaring#Montgomery's_ladder_technique
        R x1 = *this;
        R x2 = *this * *this;
        U exp_copy = exp;
        for (ssize_t i = sizeof(U) * 8 - exp().clz() - 2; i >= 0; --i) {
            if (exp_copy & 1u) {
                x2 *= x1;
                x1 *= x1;
            } else {
                x1 *= x2;
                x2 *= x2;
            }
            exp_copy >>= 1u;
        }
        return x1;
    }

    template<Unsigned U>
    constexpr U pow_mod(u64 exp, U mod)
    {
        // Left to right binary method:
        // https://en.wikipedia.org/wiki/Modular_exponentiation#Left-to-right_binary_method
        // FIXME: this is not sidechanel proof
        if (!mod)
            return 0u;

        U res = 1;
        u64 exp_copy = exp;
        for (size_t i = sizeof(u64) - __builtin_clzll(exp) - 1u; i < exp; ++i) {
            res *= res;
            res %= mod;
            if (exp_copy & 1u) {
                res = (*this * res) % mod;
            }
            exp_copy >>= 1u;
        }
        return res;
    }
    template<Unsigned ExpT, Unsigned U>
    requires(sizeof(ExpT) > sizeof(u64)) constexpr U pow_mod(ExpT exp, U mod)
    {
        // Left to right binary method:
        // https://en.wikipedia.org/wiki/Modular_exponentiation#Left-to-right_binary_method
        // FIXME: this is not side channel proof
        if (!mod)
            return 0u;

        U res = 1;
        ExpT exp_copy = exp;
        for (size_t i = sizeof(ExpT) - exp.clz() - 1u; i < exp; ++i) {
            res *= res;
            res %= mod;
            if (exp_copy & 1u) {
                res = (*this * res) % mod;
            }
            exp_copy >>= 1u;
        }
        return res;
    }

    constexpr size_t log2()
    {
        // FIXME: proper rounding
        return sizeof(R) - clz();
    }
    constexpr size_t logn(u64 base)
    {
        // FIXME: proper rounding
        return log2() / (sizeof(u64) - __builtin_clzll(base));
    }
    template<Unsigned U>
    requires(sizeof(U) > sizeof(u64)) constexpr size_t logn(U base)
    {
        // FIXME: proper rounding
        return log2() / base.log2();
    }

private:
    T m_low;
    T m_high;
};

// reverse operators
template<Unsigned U, Unsigned T>
requires(sizeof(U) < sizeof(T) * 2) constexpr bool operator<(const U a, const UFixedBigInt<T>& b) { return b >= a; }
template<Unsigned U, Unsigned T>
requires(sizeof(U) < sizeof(T) * 2) constexpr bool operator>(const U a, const UFixedBigInt<T>& b) { return b <= a; }
template<Unsigned U, Unsigned T>
requires(sizeof(U) < sizeof(T) * 2) constexpr bool operator<=(const U a, const UFixedBigInt<T>& b) { return b > a; }
template<Unsigned U, Unsigned T>
requires(sizeof(U) < sizeof(T) * 2) constexpr bool operator>=(const U a, const UFixedBigInt<T>& b) { return b < a; }

template<Unsigned T>
struct Formatter<UFixedBigInt<T>> : StandardFormatter {
    Formatter() = default;
    explicit Formatter(StandardFormatter formatter)
        : StandardFormatter(formatter)
    {
    }

    ErrorOr<void> format(FormatBuilder& builder, UFixedBigInt<T> value)
    {
        if (m_precision.has_value())
            VERIFY_NOT_REACHED();

        if (m_mode == Mode::Pointer) {
            // these are way to big for a pointer
            VERIFY_NOT_REACHED();
        }
        if (m_mode == Mode::Default)
            m_mode = Mode::Hexadecimal;

        if (!value.high()) {
            Formatter<T> formatter { *this };
            return formatter.format(builder, value.low());
        }

        u8 base = 0;
        if (m_mode == Mode::Binary) {
            base = 2;
        } else if (m_mode == Mode::BinaryUppercase) {
            base = 2;
        } else if (m_mode == Mode::Octal) {
            TODO();
        } else if (m_mode == Mode::Decimal) {
            TODO();
        } else if (m_mode == Mode::Hexadecimal) {
            base = 16;
        } else if (m_mode == Mode::HexadecimalUppercase) {
            base = 16;
        } else {
            VERIFY_NOT_REACHED();
        }
        ssize_t width = m_width.value_or(0);
        ssize_t lower_length = ceil_div(sizeof(T) * 8, (ssize_t)base);
        Formatter<T> formatter { *this };
        formatter.m_width = max(width - lower_length, (ssize_t)0);
        TRY(formatter.format(builder, value.high()));
        TRY(builder.put_literal("'"sv));
        formatter.m_zero_pad = true;
        formatter.m_alternative_form = false;
        formatter.m_width = lower_length;
        TRY(formatter.format(builder, value.low()));
        return {};
    }
};
}

// Nit: Doing these as custom classes might be faster, especially when writing
//      then in SSE, but this would cause a lot of Code duplication and due to
//      the nature of constexprs and the intelligence of the compiler they might
//      be using SSE/MMX either way

// these sizes should suffice for most usecases
using u128 = AK::UFixedBigInt<u64>;
using u256 = AK::UFixedBigInt<u128>;
using u512 = AK::UFixedBigInt<u256>;
using u1024 = AK::UFixedBigInt<u512>;
using u2048 = AK::UFixedBigInt<u1024>;
using u4096 = AK::UFixedBigInt<u2048>;

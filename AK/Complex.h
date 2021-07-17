/*
 * Copyright (c) 2021, Cesar Torres <shortanemoia@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Concepts.h>
#include <AK/Math.h>

#ifdef __cplusplus
#    if __cplusplus >= 201103L
#        define COMPLEX_NOEXCEPT noexcept
#    endif
namespace AK {

template<AK::Concepts::Arithmetic T>
class [[gnu::packed]] Complex {
public:
    constexpr Complex()
        : m_real(0)
        , m_imag(0)
    {
    }

    constexpr Complex(T real)
        : m_real(real)
        , m_imag((T)0)
    {
    }

    constexpr Complex(T real, T imaginary)
        : m_real(real)
        , m_imag(imaginary)
    {
    }

    constexpr T real() const COMPLEX_NOEXCEPT { return m_real; }

    constexpr T imag() const COMPLEX_NOEXCEPT { return m_imag; }

    constexpr T magnitude_squared() const COMPLEX_NOEXCEPT { return m_real * m_real + m_imag * m_imag; }

    constexpr T magnitude() const COMPLEX_NOEXCEPT
    {
        return hypot(m_real, m_imag);
    }

    constexpr T phase() const COMPLEX_NOEXCEPT
    {
        return atan2(m_imag, m_real);
    }

    template<AK::Concepts::Arithmetic U, AK::Concepts::Arithmetic V>
    static constexpr Complex<T> from_polar(U magnitude, V phase)
    {
        return Complex<T>(magnitude * cos(phase), magnitude * sin(phase));
    }

    template<AK::Concepts::Arithmetic U>
    constexpr Complex<T>& operator=(const Complex<U>& other)
    {
        m_real = other.real();
        m_imag = other.imag();
        return *this;
    }

    template<AK::Concepts::Arithmetic U>
    constexpr Complex<T>& operator=(const U& x)
    {
        m_real = x;
        m_imag = 0;
        return *this;
    }

    template<AK::Concepts::Arithmetic U>
    constexpr Complex<T> operator+=(const Complex<U>& x)
    {
        m_real += x.real();
        m_imag += x.imag();
        return *this;
    }

    template<AK::Concepts::Arithmetic U>
    constexpr Complex<T> operator+=(const U& x)
    {
        m_real += x.real();
        return *this;
    }

    template<AK::Concepts::Arithmetic U>
    constexpr Complex<T> operator-=(const Complex<U>& x)
    {
        m_real -= x.real();
        m_imag -= x.imag();
        return *this;
    }

    template<AK::Concepts::Arithmetic U>
    constexpr Complex<T> operator-=(const U& x)
    {
        m_real -= x.real();
        return *this;
    }

    template<AK::Concepts::Arithmetic U>
    constexpr Complex<T> operator*=(const Complex<U>& x)
    {
        const T real = m_real;
        m_real = real * x.real() - m_imag * x.imag();
        m_imag = real * x.imag() + m_imag * x.real();
        return *this;
    }

    template<AK::Concepts::Arithmetic U>
    constexpr Complex<T> operator*=(const U& x)
    {
        m_real *= x;
        m_imag *= x;
        return *this;
    }

    template<AK::Concepts::Arithmetic U>
    constexpr Complex<T> operator/=(const Complex<U>& x)
    {
        const T real = m_real;
        const T divisor = x.real() * x.real() + x.imag() * x.imag();
        m_real = (real * x.real() + m_imag * x.imag()) / divisor;
        m_imag = (m_imag * x.real() - x.real() * x.imag()) / divisor;
        return *this;
    }

    template<AK::Concepts::Arithmetic U>
    constexpr Complex<T> operator/=(const U& x)
    {
        m_real /= x;
        m_imag /= x;
        return *this;
    }

    template<AK::Concepts::Arithmetic U>
    constexpr Complex<T> operator+(const Complex<U>& a)
    {
        Complex<T> x = *this;
        x += a;
        return x;
    }

    template<AK::Concepts::Arithmetic U>
    constexpr Complex<T> operator+(const U& a)
    {
        Complex<T> x = *this;
        x += a;
        return x;
    }

    template<AK::Concepts::Arithmetic U>
    constexpr Complex<T> operator-(const Complex<U>& a)
    {
        Complex<T> x = *this;
        x -= a;
        return x;
    }

    template<AK::Concepts::Arithmetic U>
    constexpr Complex<T> operator-(const U& a)
    {
        Complex<T> x = *this;
        x -= a;
        return x;
    }

    template<AK::Concepts::Arithmetic U>
    constexpr Complex<T> operator*(const Complex<U>& a)
    {
        Complex<T> x = *this;
        x *= a;
        return x;
    }

    template<AK::Concepts::Arithmetic U>
    constexpr Complex<T> operator*(const U& a)
    {
        Complex<T> x = *this;
        x *= a;
        return x;
    }

    template<AK::Concepts::Arithmetic U>
    constexpr Complex<T> operator/(const Complex<U>& a)
    {
        Complex<T> x = *this;
        x /= a;
        return x;
    }

    template<AK::Concepts::Arithmetic U>
    constexpr Complex<T> operator/(const U& a)
    {
        Complex<T> x = *this;
        x /= a;
        return x;
    }

    template<AK::Concepts::Arithmetic U>
    constexpr bool operator==(const Complex<U>& a) const
    {
        return (this->real() == a.real()) && (this->imag() == a.imag());
    }

    template<AK::Concepts::Arithmetic U>
    constexpr bool operator!=(const Complex<U>& a) const
    {
        return !(*this == a);
    }

    constexpr Complex<T> operator+()
    {
        return *this;
    }

    constexpr Complex<T> operator-()
    {
        return Complex<T>(-m_real, -m_imag);
    }

private:
    T m_real;
    T m_imag;
};

// reverse associativity operators for scalars
template<AK::Concepts::Arithmetic T, AK::Concepts::Arithmetic U>
constexpr Complex<T> operator+(const U& b, const Complex<T>& a)
{
    Complex<T> x = a;
    x += b;
    return x;
}

template<AK::Concepts::Arithmetic T, AK::Concepts::Arithmetic U>
constexpr Complex<T> operator-(const U& b, const Complex<T>& a)
{
    Complex<T> x = a;
    x -= b;
    return x;
}

template<AK::Concepts::Arithmetic T, AK::Concepts::Arithmetic U>
constexpr Complex<T> operator*(const U& b, const Complex<T>& a)
{
    Complex<T> x = a;
    x *= b;
    return x;
}

template<AK::Concepts::Arithmetic T, AK::Concepts::Arithmetic U>
constexpr Complex<T> operator/(const U& b, const Complex<T>& a)
{
    Complex<T> x = a;
    x /= b;
    return x;
}

// some identities
template<AK::Concepts::Arithmetic T>
static constinit Complex<T> complex_real_unit = Complex<T>((T)1, (T)0);
template<AK::Concepts::Arithmetic T>
static constinit Complex<T> complex_imag_unit = Complex<T>((T)0, (T)1);

template<AK::Concepts::Arithmetic T, AK::Concepts::Arithmetic U>
static constexpr bool approx_eq(const Complex<T>& a, const Complex<U>& b, const double margin = 0.000001)
{
    const auto x = const_cast<Complex<T>&>(a) - const_cast<Complex<U>&>(b);
    return x.magnitude() <= margin;
}

// complex version of exp()
template<AK::Concepts::Arithmetic T>
static constexpr Complex<T> cexp(const Complex<T>& a)
{
    // FIXME: this can probably be faster and not use so many "expensive" trigonometric functions
    return exp(a.real()) * Complex<T>(cos(a.imag()), sin(a.imag()));
}
}

using AK::approx_eq;
using AK::cexp;
using AK::Complex;
using AK::complex_imag_unit;
using AK::complex_real_unit;

#endif

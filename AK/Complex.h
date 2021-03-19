/*
 * Copyright (c) 2021, Cesar Torres <shortanemoia@protonmail.com>
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

#include <AK/Concepts.h>
#if __has_include(<math.h>)
#    define AKCOMPLEX_CAN_USE_MATH_H
#    include <math.h>
#endif

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

#    ifdef AKCOMPLEX_CAN_USE_MATH_H
    constexpr T magnitude() const COMPLEX_NOEXCEPT
    {
        // for numbers 32 or under bit long we don't need the extra precision of sqrt
        // although it may return values with a considerable error if real and imag are too big?
        if constexpr (sizeof(T) <= sizeof(float)) {
            return sqrtf(m_real * m_real + m_imag * m_imag);
        } else if constexpr (sizeof(T) <= sizeof(double)) {
            return sqrt(m_real * m_real + m_imag * m_imag);
        } else {
            return sqrtl(m_real * m_real + m_imag * m_imag);
        }
    }

    constexpr T phase() const COMPLEX_NOEXCEPT
    {
        return atan2(m_imag, m_real);
    }

    template<AK::Concepts::Arithmetic U, AK::Concepts::Arithmetic V>
    static constexpr Complex<T> from_polar(U magnitude, V phase)
    {
        if constexpr (sizeof(T) <= sizeof(float)) {
            return Complex<T>(magnitude * cosf(phase), magnitude * sinf(phase));
        } else if constexpr (sizeof(T) <= sizeof(double)) {
            return Complex<T>(magnitude * cos(phase), magnitude * sin(phase));
        } else {
            return Complex<T>(magnitude * cosl(phase), magnitude * sinl(phase));
        }
    }
#    endif

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
    constexpr bool operator==(const Complex<U>& a)
    {
        return (this->real() == a.real()) && (this->imag() == a.imag());
    }

    template<AK::Concepts::Arithmetic U>
    constexpr bool operator!=(const Complex<U>& a)
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

#    ifdef AKCOMPLEX_CAN_USE_MATH_H
template<AK::Concepts::Arithmetic T, AK::Concepts::Arithmetic U>
static constexpr bool approx_eq(const Complex<T>& a, const Complex<U>& b, const double margin = 0.000001)
{
    const auto x = const_cast<Complex<T>&>(a) - const_cast<Complex<U>&>(b);
    return x.magnitude() <= margin;
}

//complex version of exp()
template<AK::Concepts::Arithmetic T>
static constexpr Complex<T> cexp(const Complex<T>& a)
{
    //FIXME: this can probably be faster and not use so many expensive trigonometric functions
    if constexpr (sizeof(T) <= sizeof(float)) {
        return expf(a.real()) * Complex<T>(cosf(a.imag()), sinf(a.imag()));
    } else if constexpr (sizeof(T) <= sizeof(double)) {
        return exp(a.real()) * Complex<T>(cos(a.imag()), sin(a.imag()));
    } else {
        return expl(a.real()) * Complex<T>(cosl(a.imag()), sinl(a.imag()));
    }
}
}
#    endif

using AK::Complex;
using AK::complex_imag_unit;
using AK::complex_real_unit;
#    ifdef AKCOMPLEX_CAN_USE_MATH_H
using AK::approx_eq;
using AK::cexp;
#    endif
#endif

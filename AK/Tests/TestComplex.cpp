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

#include <AK/Complex.h>
#include <AK/TestSuite.h>

TEST_CASE(Complex)
{
    auto a = Complex<float> { 1.f, 1.f };
    auto b = complex_real_unit<double> + Complex<double> { 0, 1 } * 1;
    EXPECT_APPROXIMATE(a.real(), b.real());
    EXPECT_APPROXIMATE(a.imag(), b.imag());

#ifdef AKCOMPLEX_CAN_USE_MATH_H
    EXPECT_APPROXIMATE((complex_imag_unit<float> - complex_imag_unit<float>).magnitude(), 0);
    EXPECT_APPROXIMATE((complex_imag_unit<float> + complex_real_unit<float>).magnitude(), sqrt(2));

    auto c = Complex<double> { 0., 1. };
    auto d = Complex<double>::from_polar(1., M_PI / 2.);
    EXPECT_APPROXIMATE(c.real(), d.real());
    EXPECT_APPROXIMATE(c.imag(), d.imag());

    c = Complex<double> { -1., 1. };
    d = Complex<double>::from_polar(sqrt(2.), 3. * M_PI / 4.);
    EXPECT_APPROXIMATE(c.real(), d.real());
    EXPECT_APPROXIMATE(c.imag(), d.imag());
    EXPECT_APPROXIMATE(d.phase(), 3. * M_PI / 4.);
    EXPECT_APPROXIMATE(c.magnitude(), d.magnitude());
    EXPECT_APPROXIMATE(c.magnitude(), sqrt(2.));
#endif
    EXPECT_EQ((complex_imag_unit<double> * complex_imag_unit<double>).real(), -1.);
    EXPECT_EQ((complex_imag_unit<double> / complex_imag_unit<double>).real(), 1.);

    EXPECT_EQ(Complex(1., 10.) == (Complex<double>(1., 0.) + Complex(0., 10.)), true);
    EXPECT_EQ(Complex(1., 10.) != (Complex<double>(1., 1.) + Complex(0., 10.)), true);
#ifdef AKCOMPLEX_CAN_USE_MATH_H
    EXPECT_EQ(approx_eq(Complex<int>(1), Complex<float>(1.0000004f)), true);
    EXPECT_APPROXIMATE(cexp(Complex<double>(0., 1.) * M_PI).real(), -1.);
#endif
}

TEST_MAIN(Complex)

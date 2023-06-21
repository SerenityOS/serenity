/*
 * Copyright (c) 2021, Cesar Torres <shortanemoia@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/Complex.h>

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

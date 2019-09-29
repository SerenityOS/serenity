#include <LibC/assert.h>
#include <LibM/math.h>
#include <limits>
#include <stdint.h>
#include <stdlib.h>

template<size_t> constexpr double e_to_power();
template<> constexpr double e_to_power<0>() { return 1; }
template<size_t exponent> constexpr double e_to_power() { return M_E * e_to_power<exponent - 1>(); }

template<size_t> constexpr size_t factorial();
template<> constexpr size_t factorial<0>() { return 1; }
template<size_t value> constexpr size_t factorial() { return value * factorial<value - 1>(); }

extern "C" {
double trunc(double x)
{
    return (int64_t)x;
}

double cos(double angle)
{
    return sin(angle + M_PI_2);
}

double ampsin(double angle)
{
    double looped_angle = fmod(M_PI + angle, M_TAU) - M_PI;
    double looped_angle_squared = looped_angle * looped_angle;

    double quadratic_term;
    if (looped_angle > 0) {
        quadratic_term = -looped_angle_squared;
    } else {
        quadratic_term = looped_angle_squared;
    }

    double linear_term = M_PI * looped_angle;

    return quadratic_term + linear_term;
}

double sin(double angle)
{
    double vertical_scaling = M_PI_2 * M_PI_2;
    return ampsin(angle) / vertical_scaling;
}

double pow(double x, double y)
{
    (void)x;
    (void)y;
    ASSERT_NOT_REACHED();
    return 0;
}

double ldexp(double, int exp)
{
    (void)exp;
    ASSERT_NOT_REACHED();
    return 0;
}

double tanh(double x)
{
    if (x > 0) {
        double exponentiated = exp(2 * x);
        return (exponentiated - 1) / (exponentiated + 1);
    }
    double plusX = exp(x);
    double minusX = exp(-x);
    return (plusX - minusX) / (plusX + minusX);
}

double tan(double angle)
{
    return ampsin(angle) / ampsin(M_PI_2 + angle);
}

double sqrt(double x)
{
    double res;
    __asm__("fsqrt" : "=t"(res) : "0"(x));
    return res;
}

double sinh(double x)
{
    if (x > 0) {
        double exponentiated = exp(x);
        return (exponentiated * exponentiated - 1) / 2 / exponentiated;
    }
    return (exp(x) - exp(-x)) / 2;
}

double log10(double)
{
    ASSERT_NOT_REACHED();
    return 0;
}

double log(double)
{
    ASSERT_NOT_REACHED();
    return 0;
}

double fmod(double index, double period)
{
    return index - trunc(index / period) * period;
}

double exp(double exponent)
{
    double result = 1;
    if (exponent >= 1) {
        size_t integer_part = (size_t)exponent;
        if (integer_part & 1) result *= e_to_power<1>();
        if (integer_part & 2) result *= e_to_power<2>();
        if (integer_part > 3) {
            if (integer_part & 4) result *= e_to_power<4>();
            if (integer_part & 8) result *= e_to_power<8>();
            if (integer_part & 16) result *= e_to_power<16>();
            if (integer_part & 32) result *= e_to_power<32>();
            if (integer_part >= 64) return std::numeric_limits<double>::infinity();
        }
        exponent -= integer_part;
    } else if (exponent < 0)
        return 1 / exp(-exponent);
    double taylor_series_result = 1 + exponent;
    double taylor_series_numerator = exponent * exponent;
    taylor_series_result += taylor_series_numerator / factorial<2>();
    taylor_series_numerator *= exponent;
    taylor_series_result += taylor_series_numerator / factorial<3>();
    taylor_series_numerator *= exponent;
    taylor_series_result += taylor_series_numerator / factorial<4>();
    taylor_series_numerator *= exponent;
    taylor_series_result += taylor_series_numerator / factorial<5>();
    return result * taylor_series_result;
}

double cosh(double x)
{
    if (x < 0) {
        double exponentiated = exp(-x);
        return (1 + exponentiated * exponentiated) / 2 / exponentiated;
    }
    return (exp(x) + exp(-x)) / 2;
}

double atan2(double, double)
{
    ASSERT_NOT_REACHED();
    return 0;
}

double atan(double)
{
    ASSERT_NOT_REACHED();
    return 0;
}

double asin(double)
{
    ASSERT_NOT_REACHED();
    return 0;
}

double acos(double)
{
    ASSERT_NOT_REACHED();
    return 0;
}

double fabs(double value)
{
    return value < 0 ? -value : value;
}

double log2(double)
{
    ASSERT_NOT_REACHED();
    return 0;
}

float log2f(float)
{
    ASSERT_NOT_REACHED();
    return 0;
}

long double log2l(long double)
{
    ASSERT_NOT_REACHED();
    return 0;
}

double frexp(double, int*)
{
    ASSERT_NOT_REACHED();
    return 0;
}

float frexpf(float, int*)
{
    ASSERT_NOT_REACHED();
    return 0;
}

long double frexpl(long double, int*)
{
    ASSERT_NOT_REACHED();
    return 0;
}

}

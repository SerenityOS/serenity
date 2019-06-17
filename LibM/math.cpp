#include <LibC/assert.h>
#include <LibM/math.h>

extern "C" {
double trunc(double x)
{
    return (int)x;
}

double cos(double angle)
{
    return sin(angle + M_PI_2);
}

static double sin_helper(double a)
{
    double a3 = a * a * a;
    double a5 = a3 * a * a;
    return a - a3 / 6.0 + a5 / 120.0;
}

static double cos_helper(double a)
{
    double a2 = a * a;
    double a4 = a2 * a2;
    return 1 - a2 / 2.0 + a4 / 24.0;
}

double sin(double angle)
{
    double a = fmod(angle, M_TAU);
    if (a < 0)
        a += M_TAU;
    if (a <= M_PI_2 * 0.5)
        return sin_helper(a);
    if (a <= M_PI_2)
        return cos_helper(M_PI_2 - a);
    if (a <= M_PI_2 * 1.5)
        return cos_helper(a - M_PI_2);
    if (a <= M_PI)
        return sin_helper(M_PI - a);
    return -sin(angle - M_PI);
}

double pow(double x, double y)
{
    (void)x;
    (void)y;
    ASSERT_NOT_REACHED();
}

double ldexp(double, int exp)
{
    (void)exp;
    ASSERT_NOT_REACHED();
}

double tanh(double)
{
    ASSERT_NOT_REACHED();
}

double tan(double angle)
{
    return sin(angle) / cos(angle);
}

double sqrt(double)
{
    ASSERT_NOT_REACHED();
}

double sinh(double)
{
    ASSERT_NOT_REACHED();
}

double log10(double)
{
    ASSERT_NOT_REACHED();
}

double log(double)
{
    ASSERT_NOT_REACHED();
}

double fmod(double index, double period)
{
    return index - trunc(index / period) * period;
}

double exp(double)
{
    ASSERT_NOT_REACHED();
}

double cosh(double)
{
    ASSERT_NOT_REACHED();
}

double atan2(double, double)
{
    ASSERT_NOT_REACHED();
}

double atan(double)
{
    ASSERT_NOT_REACHED();
}

double asin(double)
{
    ASSERT_NOT_REACHED();
}

double acos(double)
{
    ASSERT_NOT_REACHED();
}

double fabs(double value)
{
    return value < 0 ? -value : value;
}
double log2(double)
{
    ASSERT_NOT_REACHED();
}

float log2f(float)
{
    ASSERT_NOT_REACHED();
}

long double log2l(long double)
{
    ASSERT_NOT_REACHED();
}

double frexp(double, int*)
{
    ASSERT_NOT_REACHED();
}

float frexpf(float, int*)
{
    ASSERT_NOT_REACHED();
}

long double frexpl(long double, int*)
{
    ASSERT_NOT_REACHED();
}
}

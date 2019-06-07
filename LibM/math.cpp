#include <LibC/assert.h>
#include <LibM/math.h>

extern "C" {

double cos(double)
{
    ASSERT_NOT_REACHED();
}

double sin(double)
{
    ASSERT_NOT_REACHED();
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

double tan(double)
{
    ASSERT_NOT_REACHED();
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

double fmod(double, double)
{
    ASSERT_NOT_REACHED();
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

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
    return ampsin(angle) / ampsin(M_PI_2 + angle);
}

double sqrt(double x)
{
    double res;
    __asm__("fsqrt" : "=t"(res) : "0"(x));
    return res;
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

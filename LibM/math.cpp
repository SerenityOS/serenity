#include <LibC/assert.h>
#include <LibM/math.h>

extern "C" {

double cos(double)
{
    assert(false);
}

double sin(double)
{
    assert(false);
}

double pow(double x, double y)
{
    (void)x;
    (void)y;
    assert(false);
}

double ldexp(double, int exp)
{
    (void)exp;
    assert(false);
}

}

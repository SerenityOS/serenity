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

}

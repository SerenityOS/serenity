#include <math.h>
#include <assert.h>

extern "C" {

double pow(double x, double y)
{
    (void) x;
    (void) y;
    assert(false);
}

}


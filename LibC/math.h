#pragma once

#include <sys/cdefs.h>

__BEGIN_DECLS

#define HUGE_VAL 1e10000

double acos(double);
float acosf(float);
double asin(double);
float asinf(float);
double atan(double);
float atanf(float);
double atan2(double, double);
float atan2f(float, float);
double cos(double);
float cosf(float);
double cosh(double);
float coshf(float);
double sin(double);
float sinf(float);
double sinh(double);
float sinhf(float);
double tan(double);
float tanf(float);
double tanh(double);
float tanhf(float);
double ceil(double);
float ceilf(float);
double floor(double);
float floorf(float);
double round(double);
float roundf(float);
double fabs(double);
float fabsf(float);
double fmod(double, double);
float fmodf(float, float);
double exp(double);
float expf(float);
double frexp(double, int* exp);
float frexpf(float, int* exp);
double log(double);
float logf(float);
double log10(double);
float log10f(float);
double sqrt(double);
float sqrtf(float);
double modf(double, double*);
float modff(float, float*);
double ldexp(double, int exp);
float ldexpf(float, int exp);

double pow(double x, double y);

__END_DECLS


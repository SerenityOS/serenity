#include "assert.h"
#include "stdlib.h"
#include "stdio.h"

extern "C" {

extern void __assertion_failed(const char* msg, const char* file, unsigned line, const char* func)
{
    printf("ASSERTION FAILED: %s\n%s:%u in %s\n", msg, file, line, func);
    abort();
}

}


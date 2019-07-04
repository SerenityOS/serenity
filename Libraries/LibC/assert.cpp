#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

extern "C" {

#ifdef DEBUG
void __assertion_failed(const char* msg, const char* file, unsigned line, const char* func)
{
    dbgprintf("USERSPACE(%d) ASSERTION FAILED: %s\n%s:%u in %s\n", getpid(), msg, file, line, func);
    fprintf(stderr, "ASSERTION FAILED: %s\n%s:%u in %s\n", msg, file, line, func);
    abort();
    for (;;)
        ;
}
#endif
}

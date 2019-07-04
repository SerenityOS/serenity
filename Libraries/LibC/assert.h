#pragma once

#include <sys/cdefs.h>

__BEGIN_DECLS

#ifdef DEBUG
__attribute__((noreturn)) void __assertion_failed(const char* msg, const char* file, unsigned line, const char* func);
#    define assert(expr) ((expr) ? (void)0 : __assertion_failed(#    expr, __FILE__, __LINE__, __PRETTY_FUNCTION__))
#    define ASSERT_NOT_REACHED() assert(false)
#else
#    define assert(expr)
#    define ASSERT_NOT_REACHED() CRASH()
#endif

#define CRASH()              \
    do {                     \
        asm volatile("ud2"); \
    } while (0)
#define ASSERT assert
#define RELEASE_ASSERT assert

__END_DECLS

#pragma once

#include <sys/cdefs.h>

__BEGIN_DECLS

void __assertion_failed(const char* msg, const char* file, unsigned line, const char* func) __NORETURN;

#define assert(expr) ((expr) ? (void)0 : __assertion_failed(#expr, __FILE__, __LINE__, __PRETTY_FUNCTION__))
#define CRASH() do { asm volatile("ud2"); } while(0)
#define ASSERT assert
#define RELEASE_ASSERT assert
#define ASSERT_NOT_REACHED() assert(false)

__END_DECLS


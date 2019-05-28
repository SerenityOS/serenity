#pragma once

#include <Kernel/i386.h>
#include <Kernel/kstdio.h>

#ifdef DEBUG
[[noreturn]] void __assertion_failed(const char* msg, const char* file, unsigned line, const char* func);
#    define ASSERT(expr) (static_cast<bool>(expr) ? (void)0 : __assertion_failed(#    expr, __FILE__, __LINE__, __PRETTY_FUNCTION__))
#    define ASSERT_NOT_REACHED() ASSERT(false)
#else
#    define ASSERT(expr)
#    define ASSERT_NOT_REACHED() CRASH()
#endif
#define CRASH()              \
    do {                     \
        asm volatile("ud2"); \
    } while (0)
#define RELEASE_ASSERT(x) \
    do {                  \
        if (!(x))         \
            CRASH();      \
    } while (0)
#define ASSERT_INTERRUPTS_DISABLED() ASSERT(!(cpu_flags() & 0x200))
#define ASSERT_INTERRUPTS_ENABLED() ASSERT(cpu_flags() & 0x200)

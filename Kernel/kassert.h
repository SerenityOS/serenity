#pragma once

#include "kprintf.h"
#include "i386.h"

#define CRASH() do { asm volatile("ud2"); } while(0)
#define ASSERT(x) do { if (!(x)) { asm volatile("cli"); kprintf("ASSERTION FAILED: " #x "\n%s:%u in %s\n", __FILE__, __LINE__, __PRETTY_FUNCTION__); CRASH(); } } while(0)
#define RELEASE_ASSERT(x) do { if (!(x)) CRASH(); } while(0)
#define ASSERT_NOT_REACHED() ASSERT(false)
#define ASSERT_INTERRUPTS_DISABLED() ASSERT(!(cpuFlags() & 0x200))
#define ASSERT_INTERRUPTS_ENABLED() ASSERT(cpuFlags() & 0x200)

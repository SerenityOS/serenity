#pragma once

#include "VGA.h"

#define CRASH() do { asm volatile("ud2"); } while(0)
#define ASSERT(x) do { if (!(x)) { kprintf("ASSERTION FAILED: " #x "\n%s:%u in %s\n", __FILE__, __LINE__, __PRETTY_FUNCTION__); CRASH(); } } while(0)
#define RELEASE_ASSERT(x) do { if (!(x)) CRASH(); } while(0)
#define ASSERT_NOT_REACHED() ASSERT(false)

#pragma once

#ifdef SERENITY
#ifdef KERNEL
#include <Kernel/kassert.h>
#else
#include <LibC/assert.h>
#endif
#else
#include <assert.h>
#define ASSERT(x) assert(x)
#define ASSERT_NOT_REACHED() assert(false)
#endif

namespace AK {

inline void notImplemented() { ASSERT(false); }

}

using AK::notImplemented;


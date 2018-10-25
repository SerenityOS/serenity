#pragma once

#ifdef SERENITY
#include <Kernel/kassert.h>
#else
#include <assert.h>
#define ASSERT(x) assert(x)
#define ASSERT_NOT_REACHED() assert(false)
#endif

namespace AK {

inline void notImplemented() { ASSERT(false); }

}

using AK::notImplemented;


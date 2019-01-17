#pragma once

#ifdef KERNEL
#include <Kernel/kassert.h>
#else
#include <LibC/assert.h>
#endif

namespace AK {

inline void notImplemented() { ASSERT(false); }

}

using AK::notImplemented;


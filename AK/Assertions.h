#pragma once

#ifdef KERNEL
#include <Kernel/Assertions.h>
#else
#include <LibC/assert.h>
#endif

namespace AK {

inline void not_implemented() { ASSERT(false); }

}

using AK::not_implemented;


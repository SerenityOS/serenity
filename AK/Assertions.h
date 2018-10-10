#pragma once

#include <assert.h>

#define ASSERT(x) assert(x)
#define ASSERT_NOT_REACHED() assert(false)

namespace AK {

inline void notImplemented() { assert(false); }

}

using AK::notImplemented;


#pragma once

#include "UnixTypes.h"

#ifdef SERENITY_KERNEL
inline static const Unix::off_t maxFileOffset = 9223372036854775807LL;
#else
#include <limits>
inline static const Unix::off_t maxFileOffset = std::numeric_limits<Unix::off_t>::max();
#endif

static const Unix::size_t GoodBufferSize = 4096;



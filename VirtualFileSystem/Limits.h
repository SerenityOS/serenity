#pragma once

#include "UnixTypes.h"

#ifdef SERENITY
inline static const Unix::off_t maxFileOffset = 2147483647;
#else
#include <limits>
inline static const Unix::off_t maxFileOffset = std::numeric_limits<Unix::off_t>::max();
#endif

static const Unix::size_t GoodBufferSize = 4096;



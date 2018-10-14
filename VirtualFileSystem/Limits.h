#pragma once

#include <limits>
#include "UnixTypes.h"

static const Unix::size_t GoodBufferSize = 4096;

inline static const Unix::off_t maxFileOffset = std::numeric_limits<Unix::off_t>::max();


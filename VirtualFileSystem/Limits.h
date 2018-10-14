#pragma once

#include <limits>

typedef dword size_t;
typedef signed_dword ssize_t;

static const size_t GoodBufferSize = 4096;

typedef int64_t FileOffset;
inline static const FileOffset maxFileOffset = std::numeric_limits<FileOffset>::max();


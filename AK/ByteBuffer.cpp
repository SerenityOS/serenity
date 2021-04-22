/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteBuffer.h>

namespace AK {

bool ByteBuffer::operator==(const ByteBuffer& other) const
{
    if (is_empty() != other.is_empty())
        return false;
    if (is_empty())
        return true;
    if (size() != other.size())
        return false;

    // So they both have data, and the same length.
    return !__builtin_memcmp(data(), other.data(), size());
}

}

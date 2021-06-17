/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <string.h>

namespace SQL {

template<typename T>
void deserialize_from(ByteBuffer& buffer, size_t& at_offset, T& t)
{
    auto ptr = buffer.offset_pointer((int)at_offset);
    memcpy(&t, ptr, sizeof(T));
    at_offset += sizeof(T);
}

template<typename T>
void serialize_to(ByteBuffer& buffer, T const& t)
{
    buffer.append(&t, sizeof(T));
}

}

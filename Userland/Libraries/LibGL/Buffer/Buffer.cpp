/*
 * Copyright (c) 2022, cflip <cflip@cflip.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Buffer.h"

namespace GL {

ErrorOr<void> Buffer::set_data(void const* data, size_t size)
{
    if (!data) {
        m_data = TRY(ByteBuffer::create_uninitialized(size));
        return {};
    }
    m_data = TRY(ByteBuffer::copy(data, size));
    return {};
}

void Buffer::replace_data(void const* data, size_t offset, size_t size)
{
    m_data.overwrite(offset, data, size);
}

size_t Buffer::size()
{
    return m_data.size();
}

void* Buffer::data()
{
    return m_data.data();
}

void* Buffer::offset_data(size_t offset)
{
    return m_data.offset_pointer(offset);
}

}

/*
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/ArrayBuffer.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

ArrayBuffer* ArrayBuffer::create(GlobalObject& global_object, size_t byte_size)
{
    return global_object.heap().allocate<ArrayBuffer>(global_object, byte_size, *global_object.array_buffer_prototype());
}

ArrayBuffer* ArrayBuffer::create(GlobalObject& global_object, ByteBuffer& buffer)
{
    return global_object.heap().allocate<ArrayBuffer>(global_object, buffer, *global_object.array_buffer_prototype());
}

ArrayBuffer* ArrayBuffer::create(GlobalObject& global_object, ByteBuffer* buffer)
{
    return global_object.heap().allocate<ArrayBuffer>(global_object, buffer, *global_object.array_buffer_prototype());
}

ArrayBuffer::ArrayBuffer(size_t byte_size, Object& prototype)
    : Object(prototype)
    , m_buffer(ByteBuffer::create_zeroed(byte_size))
{
}

ArrayBuffer::ArrayBuffer(ByteBuffer& buffer, Object& prototype)
    : Object(prototype)
    , m_buffer(buffer)
{
}

ArrayBuffer::ArrayBuffer(ByteBuffer* buffer, Object& prototype)
    : Object(prototype)
    , m_buffer(buffer)
{
}

ArrayBuffer::~ArrayBuffer()
{
}

}

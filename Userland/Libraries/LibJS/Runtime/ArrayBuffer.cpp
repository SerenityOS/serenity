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

ArrayBuffer* ArrayBuffer::create(GlobalObject& global_object, ByteBuffer* buffer)
{
    return global_object.heap().allocate<ArrayBuffer>(global_object, buffer, *global_object.array_buffer_prototype());
}

ArrayBuffer::ArrayBuffer(size_t byte_size, Object& prototype)
    : Object(prototype)
    , m_buffer(ByteBuffer::create_zeroed(byte_size).release_value()) // FIXME: Handle this possible OOM failure.
    , m_detach_key(js_undefined())
{
}

ArrayBuffer::ArrayBuffer(ByteBuffer* buffer, Object& prototype)
    : Object(prototype)
    , m_buffer(buffer)
    , m_detach_key(js_undefined())
{
}

ArrayBuffer::~ArrayBuffer()
{
}

void ArrayBuffer::visit_edges(Cell::Visitor& visitor)
{
    Object::visit_edges(visitor);
    visitor.visit(m_detach_key);
}

}

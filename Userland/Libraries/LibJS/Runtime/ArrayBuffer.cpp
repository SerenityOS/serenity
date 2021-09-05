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
    auto buffer = ByteBuffer::create_zeroed(byte_size);
    if (!buffer.has_value()) {
        global_object.vm().throw_exception<RangeError>(global_object, ErrorType::NotEnoughMemoryToAllocate, byte_size);
        return nullptr;
    }
    return global_object.heap().allocate<ArrayBuffer>(global_object, buffer.release_value(), *global_object.array_buffer_prototype());
}

ArrayBuffer* ArrayBuffer::create(GlobalObject& global_object, ByteBuffer* buffer)
{
    return global_object.heap().allocate<ArrayBuffer>(global_object, buffer, *global_object.array_buffer_prototype());
}

ArrayBuffer::ArrayBuffer(ByteBuffer buffer, Object& prototype)
    : Object(prototype)
    , m_buffer(move(buffer))
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

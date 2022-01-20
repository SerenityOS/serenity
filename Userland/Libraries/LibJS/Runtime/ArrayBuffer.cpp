/*
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/ArrayBuffer.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

ArrayBuffer* ArrayBuffer::create(GlobalObject& global_object, size_t byte_length)
{
    auto buffer = ByteBuffer::create_zeroed(byte_length);
    if (buffer.is_error()) {
        global_object.vm().throw_exception<RangeError>(global_object, ErrorType::NotEnoughMemoryToAllocate, byte_length);
        return nullptr;
    }
    return global_object.heap().allocate<ArrayBuffer>(global_object, buffer.release_value(), *global_object.array_buffer_prototype());
}

ArrayBuffer* ArrayBuffer::create(GlobalObject& global_object, ByteBuffer buffer)
{
    return global_object.heap().allocate<ArrayBuffer>(global_object, move(buffer), *global_object.array_buffer_prototype());
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
    Base::visit_edges(visitor);
    visitor.visit(m_detach_key);
}

// 25.1.2.1 AllocateArrayBuffer ( constructor, byteLength ), https://tc39.es/ecma262/#sec-allocatearraybuffer
ThrowCompletionOr<ArrayBuffer*> allocate_array_buffer(GlobalObject& global_object, FunctionObject& constructor, size_t byte_length)
{
    // 1. Let obj be ? OrdinaryCreateFromConstructor(constructor, "%ArrayBuffer.prototype%", « [[ArrayBufferData]], [[ArrayBufferByteLength]], [[ArrayBufferDetachKey]] »).
    auto* obj = TRY(ordinary_create_from_constructor<ArrayBuffer>(global_object, constructor, &GlobalObject::array_buffer_prototype, nullptr));

    // 2. Let block be ? CreateByteDataBlock(byteLength).
    auto block = ByteBuffer::create_zeroed(byte_length);
    if (block.is_error())
        return global_object.vm().throw_completion<RangeError>(global_object, ErrorType::NotEnoughMemoryToAllocate, byte_length);

    // 3. Set obj.[[ArrayBufferData]] to block.
    obj->set_buffer(block.release_value());

    // 4. Set obj.[[ArrayBufferByteLength]] to byteLength.

    // 5. Return obj.
    return obj;
}

}

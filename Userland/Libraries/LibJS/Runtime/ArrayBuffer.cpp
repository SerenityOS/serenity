/*
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/ArrayBuffer.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

ThrowCompletionOr<ArrayBuffer*> ArrayBuffer::create(GlobalObject& global_object, size_t byte_length)
{
    auto buffer = ByteBuffer::create_zeroed(byte_length);
    if (buffer.is_error())
        return global_object.vm().throw_completion<RangeError>(global_object, ErrorType::NotEnoughMemoryToAllocate, byte_length);

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

// 25.1.2.4 CloneArrayBuffer ( srcBuffer, srcByteOffset, srcLength, cloneConstructor ), https://tc39.es/ecma262/#sec-clonearraybuffer
ThrowCompletionOr<ArrayBuffer*> clone_array_buffer(GlobalObject& global_object, ArrayBuffer& source_buffer, size_t source_byte_offset, size_t source_length, FunctionObject& clone_constructor)
{
    auto& vm = global_object.vm();

    // 1. Let targetBuffer be ? AllocateArrayBuffer(cloneConstructor, srcLength).
    auto* target_buffer = TRY(allocate_array_buffer(global_object, clone_constructor, source_length));

    // 2. If IsDetachedBuffer(srcBuffer) is true, throw a TypeError exception.
    if (source_buffer.is_detached())
        return vm.throw_completion<TypeError>(global_object, ErrorType::DetachedArrayBuffer);

    // 3. Let srcBlock be srcBuffer.[[ArrayBufferData]].
    auto& source_block = source_buffer.buffer();

    // 4. Let targetBlock be targetBuffer.[[ArrayBufferData]].
    auto& target_block = target_buffer->buffer();

    // 5. Perform CopyDataBlockBytes(targetBlock, 0, srcBlock, srcByteOffset, srcLength).
    // FIXME: This is only correct for ArrayBuffers, once SharedArrayBuffer is implemented, the AO has to be implemented
    target_block.overwrite(0, source_block.offset_pointer(source_byte_offset), source_length);

    // 6. Return targetBuffer.
    return target_buffer;
}

}

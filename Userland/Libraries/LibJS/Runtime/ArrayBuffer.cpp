/*
 * Copyright (c) 2020-2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/ArrayBuffer.h>
#include <LibJS/Runtime/ArrayBufferConstructor.h>
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

// 1.1.5 IsResizableArrayBuffer ( arrayBuffer ), https://tc39.es/proposal-resizablearraybuffer/#sec-isresizablearraybuffer
bool ArrayBuffer::is_resizable_array_buffer() const
{
    // 1. Assert: Type(arrayBuffer) is Object and arrayBuffer has an [[ArrayBufferData]] internal slot.

    // 2. If buffer has an [[ArrayBufferMaxByteLength]] internal slot, return true.
    // 3. Return false.
    return m_max_byte_length.has_value();
}

void ArrayBuffer::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_detach_key);
}

// 25.1.2.1 AllocateArrayBuffer ( constructor, byteLength ), https://tc39.es/ecma262/#sec-allocatearraybuffer
// 1.1.2 AllocateArrayBuffer ( constructor, byteLength [, maxByteLength ] ), https://tc39.es/proposal-resizablearraybuffer/#sec-allocatearraybuffer
ThrowCompletionOr<ArrayBuffer*> allocate_array_buffer(GlobalObject& global_object, FunctionObject& constructor, size_t byte_length, Optional<size_t> max_byte_length)
{
    // 1. Let slots be « [[ArrayBufferData]], [[ArrayBufferByteLength]], [[ArrayBufferDetachKey]] ».
    // 2. If maxByteLength is present, append [[ArrayBufferMaxByteLength]] to slots.

    // 3. Let obj be ? OrdinaryCreateFromConstructor(constructor, "%ArrayBuffer.prototype%", slots).
    auto* obj = TRY(ordinary_create_from_constructor<ArrayBuffer>(global_object, constructor, &GlobalObject::array_buffer_prototype, nullptr));

    // 4. Let block be ? CreateByteDataBlock(byteLength).
    auto block = ByteBuffer::create_zeroed(byte_length);
    if (block.is_error())
        return global_object.vm().throw_completion<RangeError>(global_object, ErrorType::NotEnoughMemoryToAllocate, byte_length);

    // 5. Set obj.[[ArrayBufferData]] to block.
    obj->set_buffer(block.release_value());

    // 6. Set obj.[[ArrayBufferByteLength]] to byteLength.

    // 7. If maxByteLength is present, then
    if (max_byte_length.has_value()) {
        // a. Assert: byteLength ≤ maxByteLength.
        VERIFY(byte_length <= *max_byte_length);

        // b. If it is not possible to create a Data Block block consisting of maxByteLength bytes, throw a RangeError exception.
        // c. NOTE: Resizable ArrayBuffers are designed to be implementable with in-place growth. Implementations reserve the right to throw if, for example, virtual memory cannot be reserved up front.

        // d. Set obj.[[ArrayBufferMaxByteLength]] to maxByteLength.
        obj->set_max_byte_length(*max_byte_length);
    }

    // 8. Return obj.
    return obj;
}

// 25.1.2.3 DetachArrayBuffer ( arrayBuffer [ , key ] ), https://tc39.es/ecma262/#sec-detacharraybuffer
ThrowCompletionOr<void> detach_array_buffer(GlobalObject& global_object, ArrayBuffer& array_buffer, Optional<Value> key)
{
    auto& vm = global_object.vm();

    // 1. Assert: IsSharedArrayBuffer(arrayBuffer) is false.
    // FIXME: Check for shared buffer

    // 2. If key is not present, set key to undefined.
    if (!key.has_value())
        key = js_undefined();

    // 3. If SameValue(arrayBuffer.[[ArrayBufferDetachKey]], key) is false, throw a TypeError exception.
    if (!same_value(array_buffer.detach_key(), *key))
        return vm.throw_completion<TypeError>(global_object, ErrorType::DetachKeyMismatch, *key, array_buffer.detach_key());

    // 4. Set arrayBuffer.[[ArrayBufferData]] to null.
    // 5. Set arrayBuffer.[[ArrayBufferByteLength]] to 0.
    array_buffer.detach_buffer();

    // 6. Return unused.
    return {};
}

// 25.1.2.4 CloneArrayBuffer ( srcBuffer, srcByteOffset, srcLength, cloneConstructor ), https://tc39.es/ecma262/#sec-clonearraybuffer
ThrowCompletionOr<ArrayBuffer*> clone_array_buffer(GlobalObject& global_object, ArrayBuffer& source_buffer, size_t source_byte_offset, size_t source_length)
{
    // 1. Assert: IsDetachedBuffer(srcBuffer) is false.
    VERIFY(!source_buffer.is_detached());

    // 2. Let targetBuffer be ? AllocateArrayBuffer(%ArrayBuffer%, srcLength).
    auto* target_buffer = TRY(allocate_array_buffer(global_object, *global_object.array_buffer_constructor(), source_length));

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

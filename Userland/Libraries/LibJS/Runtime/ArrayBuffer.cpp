/*
 * Copyright (c) 2020-2023, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/ArrayBuffer.h>
#include <LibJS/Runtime/ArrayBufferConstructor.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

ThrowCompletionOr<NonnullGCPtr<ArrayBuffer>> ArrayBuffer::create(Realm& realm, size_t byte_length)
{
    auto buffer = ByteBuffer::create_zeroed(byte_length);
    if (buffer.is_error())
        return realm.vm().throw_completion<RangeError>(ErrorType::NotEnoughMemoryToAllocate, byte_length);

    return MUST_OR_THROW_OOM(realm.heap().allocate<ArrayBuffer>(realm, buffer.release_value(), realm.intrinsics().array_buffer_prototype()));
}

NonnullGCPtr<ArrayBuffer> ArrayBuffer::create(Realm& realm, ByteBuffer buffer)
{
    return realm.heap().allocate<ArrayBuffer>(realm, move(buffer), realm.intrinsics().array_buffer_prototype()).release_allocated_value_but_fixme_should_propagate_errors();
}

NonnullGCPtr<ArrayBuffer> ArrayBuffer::create(Realm& realm, ByteBuffer* buffer)
{
    return realm.heap().allocate<ArrayBuffer>(realm, buffer, realm.intrinsics().array_buffer_prototype()).release_allocated_value_but_fixme_should_propagate_errors();
}

ArrayBuffer::ArrayBuffer(ByteBuffer buffer, Object& prototype)
    : Object(ConstructWithPrototypeTag::Tag, prototype)
    , m_buffer(move(buffer))
    , m_detach_key(js_undefined())
{
}

ArrayBuffer::ArrayBuffer(ByteBuffer* buffer, Object& prototype)
    : Object(ConstructWithPrototypeTag::Tag, prototype)
    , m_buffer(buffer)
    , m_detach_key(js_undefined())
{
}

void ArrayBuffer::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_detach_key);
}

// 6.2.9.1 CreateByteDataBlock ( size ), https://tc39.es/ecma262/#sec-createbytedatablock
static ThrowCompletionOr<ByteBuffer> create_byte_data_block(VM& vm, size_t size)
{
    // 1. If size > 2^53 - 1, throw a RangeError exception.
    if (size > MAX_ARRAY_LIKE_INDEX)
        return vm.throw_completion<RangeError>(ErrorType::InvalidLength, "array buffer");

    // 2. Let db be a new Data Block value consisting of size bytes. If it is impossible to create such a Data Block, throw a RangeError exception.
    // 3. Set all of the bytes of db to 0.
    auto data_block = ByteBuffer::create_zeroed(size);
    if (data_block.is_error())
        return vm.throw_completion<RangeError>(ErrorType::NotEnoughMemoryToAllocate, size);

    // 4. Return db.
    return data_block.release_value();
}

// 25.1.2.1 AllocateArrayBuffer ( constructor, byteLength ), https://tc39.es/ecma262/#sec-allocatearraybuffer
ThrowCompletionOr<ArrayBuffer*> allocate_array_buffer(VM& vm, FunctionObject& constructor, size_t byte_length)
{
    // 1. Let obj be ? OrdinaryCreateFromConstructor(constructor, "%ArrayBuffer.prototype%", « [[ArrayBufferData]], [[ArrayBufferByteLength]], [[ArrayBufferDetachKey]] »).
    auto obj = TRY(ordinary_create_from_constructor<ArrayBuffer>(vm, constructor, &Intrinsics::array_buffer_prototype, nullptr));

    // 2. Let block be ? CreateByteDataBlock(byteLength).
    auto block = TRY(create_byte_data_block(vm, byte_length));

    // 3. Set obj.[[ArrayBufferData]] to block.
    obj->set_buffer(move(block));

    // 4. Set obj.[[ArrayBufferByteLength]] to byteLength.

    // 5. Return obj.
    return obj.ptr();
}

// 25.1.2.3 DetachArrayBuffer ( arrayBuffer [ , key ] ), https://tc39.es/ecma262/#sec-detacharraybuffer
ThrowCompletionOr<void> detach_array_buffer(VM& vm, ArrayBuffer& array_buffer, Optional<Value> key)
{
    // 1. Assert: IsSharedArrayBuffer(arrayBuffer) is false.
    // FIXME: Check for shared buffer

    // 2. If key is not present, set key to undefined.
    if (!key.has_value())
        key = js_undefined();

    // 3. If SameValue(arrayBuffer.[[ArrayBufferDetachKey]], key) is false, throw a TypeError exception.
    if (!same_value(array_buffer.detach_key(), *key))
        return vm.throw_completion<TypeError>(ErrorType::DetachKeyMismatch, *key, array_buffer.detach_key());

    // 4. Set arrayBuffer.[[ArrayBufferData]] to null.
    // 5. Set arrayBuffer.[[ArrayBufferByteLength]] to 0.
    array_buffer.detach_buffer();

    // 6. Return unused.
    return {};
}

// 25.1.2.4 CloneArrayBuffer ( srcBuffer, srcByteOffset, srcLength, cloneConstructor ), https://tc39.es/ecma262/#sec-clonearraybuffer
ThrowCompletionOr<ArrayBuffer*> clone_array_buffer(VM& vm, ArrayBuffer& source_buffer, size_t source_byte_offset, size_t source_length)
{
    auto& realm = *vm.current_realm();

    // 1. Assert: IsDetachedBuffer(srcBuffer) is false.
    VERIFY(!source_buffer.is_detached());

    // 2. Let targetBuffer be ? AllocateArrayBuffer(%ArrayBuffer%, srcLength).
    auto* target_buffer = TRY(allocate_array_buffer(vm, realm.intrinsics().array_buffer_constructor(), source_length));

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

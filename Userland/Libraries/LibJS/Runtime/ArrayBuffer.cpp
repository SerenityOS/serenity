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

    return realm.heap().allocate<ArrayBuffer>(realm, buffer.release_value(), realm.intrinsics().array_buffer_prototype());
}

NonnullGCPtr<ArrayBuffer> ArrayBuffer::create(Realm& realm, ByteBuffer buffer)
{
    return realm.heap().allocate<ArrayBuffer>(realm, move(buffer), realm.intrinsics().array_buffer_prototype());
}

NonnullGCPtr<ArrayBuffer> ArrayBuffer::create(Realm& realm, ByteBuffer* buffer)
{
    return realm.heap().allocate<ArrayBuffer>(realm, buffer, realm.intrinsics().array_buffer_prototype());
}

ArrayBuffer::ArrayBuffer(ByteBuffer buffer, Object& prototype)
    : Object(ConstructWithPrototypeTag::Tag, prototype)
    , m_data_block(DataBlock { move(buffer), DataBlock::Shared::No })
    , m_detach_key(js_undefined())
{
}

ArrayBuffer::ArrayBuffer(ByteBuffer* buffer, Object& prototype)
    : Object(ConstructWithPrototypeTag::Tag, prototype)
    , m_data_block(DataBlock { buffer, DataBlock::Shared::No })
    , m_detach_key(js_undefined())
{
}

void ArrayBuffer::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_detach_key);
}

// 6.2.9.1 CreateByteDataBlock ( size ), https://tc39.es/ecma262/#sec-createbytedatablock
ThrowCompletionOr<DataBlock> create_byte_data_block(VM& vm, size_t size)
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
    return DataBlock { data_block.release_value(), DataBlock::Shared::No };
}

// FIXME: The returned DataBlock is not shared in the sense that the standard specifies it.
// 6.2.9.2 CreateSharedByteDataBlock ( size ), https://tc39.es/ecma262/#sec-createsharedbytedatablock
static ThrowCompletionOr<DataBlock> create_shared_byte_data_block(VM& vm, size_t size)
{
    // 1. Let db be a new Shared Data Block value consisting of size bytes. If it is impossible to create such a Shared Data Block, throw a RangeError exception.
    auto data_block = ByteBuffer::create_zeroed(size);
    if (data_block.is_error())
        return vm.throw_completion<RangeError>(ErrorType::NotEnoughMemoryToAllocate, size);

    // 2. Let execution be the [[CandidateExecution]] field of the surrounding agent's Agent Record.
    // 3. Let eventsRecord be the Agent Events Record of execution.[[EventsRecords]] whose [[AgentSignifier]] is AgentSignifier().
    // 4. Let zero be « 0 ».
    // 5. For each index i of db, do
    // a. Append WriteSharedMemory { [[Order]]: init, [[NoTear]]: true, [[Block]]: db, [[ByteIndex]]: i, [[ElementSize]]: 1, [[Payload]]: zero } to eventsRecord.[[EventList]].
    // 6. Return db.
    return DataBlock { data_block.release_value(), DataBlock::Shared::Yes };
}

// 6.2.9.3 CopyDataBlockBytes ( toBlock, toIndex, fromBlock, fromIndex, count ), https://tc39.es/ecma262/#sec-copydatablockbytes
void copy_data_block_bytes(ByteBuffer& to_block, u64 to_index, ByteBuffer const& from_block, u64 from_index, u64 count)
{
    // 1. Assert: fromBlock and toBlock are distinct values.
    VERIFY(&to_block != &from_block);

    // 2. Let fromSize be the number of bytes in fromBlock.
    auto from_size = from_block.size();

    // 3. Assert: fromIndex + count ≤ fromSize.
    VERIFY(from_index + count <= from_size);

    // 4. Let toSize be the number of bytes in toBlock.
    auto to_size = to_block.size();

    // 5. Assert: toIndex + count ≤ toSize.
    VERIFY(to_index + count <= to_size);

    // 6. Repeat, while count > 0,
    while (count > 0) {
        // FIXME: a. If fromBlock is a Shared Data Block, then
        // FIXME:    i. Let execution be the [[CandidateExecution]] field of the surrounding agent's Agent Record.
        // FIXME:    ii. Let eventsRecord be the Agent Events Record of execution.[[EventsRecords]] whose [[AgentSignifier]] is AgentSignifier().
        // FIXME:    iii. Let bytes be a List whose sole element is a nondeterministically chosen byte value.
        // FIXME:    iv. NOTE: In implementations, bytes is the result of a non-atomic read instruction on the underlying hardware. The nondeterminism is a semantic prescription of the memory model to describe observable behaviour of hardware with weak consistency.
        // FIXME:    v. Let readEvent be ReadSharedMemory { [[Order]]: Unordered, [[NoTear]]: true, [[Block]]: fromBlock, [[ByteIndex]]: fromIndex, [[ElementSize]]: 1 }.
        // FIXME:    vi. Append readEvent to eventsRecord.[[EventList]].
        // FIXME:    vii. Append Chosen Value Record { [[Event]]: readEvent, [[ChosenValue]]: bytes } to execution.[[ChosenValues]].
        // FIXME:    viii. If toBlock is a Shared Data Block, then
        // FIXME:       1. Append WriteSharedMemory { [[Order]]: Unordered, [[NoTear]]: true, [[Block]]: toBlock, [[ByteIndex]]: toIndex, [[ElementSize]]: 1, [[Payload]]: bytes } to eventsRecord.[[EventList]].
        // FIXME:    ix. Else,
        // FIXME:       1. Set toBlock[toIndex] to bytes[0].
        // FIXME: b. Else,
        // FIXME:    i. Assert: toBlock is not a Shared Data Block.

        // ii. Set toBlock[toIndex] to fromBlock[fromIndex].
        to_block[to_index] = from_block[from_index];

        // c. Set toIndex to toIndex + 1.
        ++to_index;

        // d. Set fromIndex to fromIndex + 1.
        ++from_index;

        // e. Set count to count - 1.
        --count;
    }

    // 7. Return unused.
}

// 25.1.2.1 AllocateArrayBuffer ( constructor, byteLength ), https://tc39.es/ecma262/#sec-allocatearraybuffer
ThrowCompletionOr<ArrayBuffer*> allocate_array_buffer(VM& vm, FunctionObject& constructor, size_t byte_length)
{
    // 1. Let obj be ? OrdinaryCreateFromConstructor(constructor, "%ArrayBuffer.prototype%", « [[ArrayBufferData]], [[ArrayBufferByteLength]], [[ArrayBufferDetachKey]] »).
    auto obj = TRY(ordinary_create_from_constructor<ArrayBuffer>(vm, constructor, &Intrinsics::array_buffer_prototype, nullptr));

    // 2. Let block be ? CreateByteDataBlock(byteLength).
    auto block = TRY(create_byte_data_block(vm, byte_length));

    // 3. Set obj.[[ArrayBufferData]] to block.
    obj->set_data_block(move(block));

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
    copy_data_block_bytes(target_block, 0, source_block, source_byte_offset, source_length);

    // 6. Return targetBuffer.
    return target_buffer;
}

// 25.1.2.14 ArrayBufferCopyAndDetach ( arrayBuffer, newLength, preserveResizability ), https://tc39.es/proposal-arraybuffer-transfer/#sec-arraybuffer.prototype.transfertofixedlength
ThrowCompletionOr<ArrayBuffer*> array_buffer_copy_and_detach(VM& vm, ArrayBuffer& array_buffer, Value new_length, PreserveResizability)
{
    auto& realm = *vm.current_realm();

    // 1. Perform ? RequireInternalSlot(arrayBuffer, [[ArrayBufferData]]).

    // FIXME: 2. If IsSharedArrayBuffer(arrayBuffer) is true, throw a TypeError exception.

    // 3. If newLength is undefined, then
    // a. Let newByteLength be arrayBuffer.[[ArrayBufferByteLength]].
    // 4. Else,
    // a. Let newByteLength be ? ToIndex(newLength).
    auto new_byte_length = new_length.is_undefined() ? array_buffer.byte_length() : TRY(new_length.to_index(vm));

    // 5. If IsDetachedBuffer(arrayBuffer) is true, throw a TypeError exception.
    if (array_buffer.is_detached())
        return vm.throw_completion<TypeError>(ErrorType::DetachedArrayBuffer);

    // FIXME: 6. If preserveResizability is preserve-resizability and IsResizableArrayBuffer(arrayBuffer) is true, then
    // a. Let newMaxByteLength be arrayBuffer.[[ArrayBufferMaxByteLength]].
    // 7. Else,
    // a. Let newMaxByteLength be empty.

    // 8. If arrayBuffer.[[ArrayBufferDetachKey]] is not undefined, throw a TypeError exception.
    if (!array_buffer.detach_key().is_undefined())
        return vm.throw_completion<TypeError>(ErrorType::DetachKeyMismatch, array_buffer.detach_key(), js_undefined());

    // 9. Let newBuffer be ? AllocateArrayBuffer(%ArrayBuffer%, newByteLength, FIXME: newMaxByteLength).
    auto* new_buffer = TRY(allocate_array_buffer(vm, realm.intrinsics().array_buffer_constructor(), new_byte_length));

    // 10. Let copyLength be min(newByteLength, arrayBuffer.[[ArrayBufferByteLength]]).
    auto copy_length = min(new_byte_length, array_buffer.byte_length());

    // 11. Let fromBlock be arrayBuffer.[[ArrayBufferData]].
    // 12. Let toBlock be newBuffer.[[ArrayBufferData]].
    // 13. Perform CopyDataBlockBytes(toBlock, 0, fromBlock, 0, copyLength).
    // 14. NOTE: Neither creation of the new Data Block nor copying from the old Data Block are observable. Implementations may implement this method as a zero-copy move or a realloc.
    copy_data_block_bytes(new_buffer->buffer(), 0, array_buffer.buffer(), 0, copy_length);

    // 15. Perform ! DetachArrayBuffer(arrayBuffer).
    TRY(detach_array_buffer(vm, array_buffer));

    // 16. Return newBuffer.
    return new_buffer;
}

// 25.2.1.1 AllocateSharedArrayBuffer ( constructor, byteLength ), https://tc39.es/ecma262/#sec-allocatesharedarraybuffer
ThrowCompletionOr<NonnullGCPtr<ArrayBuffer>> allocate_shared_array_buffer(VM& vm, FunctionObject& constructor, size_t byte_length)
{
    // 1. Let obj be ? OrdinaryCreateFromConstructor(constructor, "%SharedArrayBuffer.prototype%", « [[ArrayBufferData]], [[ArrayBufferByteLength]] »).
    auto obj = TRY(ordinary_create_from_constructor<ArrayBuffer>(vm, constructor, &Intrinsics::shared_array_buffer_prototype, nullptr));

    // 2. Let block be ? CreateSharedByteDataBlock(byteLength).
    auto block = TRY(create_shared_byte_data_block(vm, byte_length));

    // 3. Set obj.[[ArrayBufferData]] to block.
    // 4. Set obj.[[ArrayBufferByteLength]] to byteLength.
    obj->set_data_block(move(block));

    // 5. Return obj.
    return obj;
}

}

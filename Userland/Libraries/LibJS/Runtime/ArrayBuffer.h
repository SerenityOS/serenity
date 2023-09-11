/*
 * Copyright (c) 2020-2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Function.h>
#include <AK/Variant.h>
#include <LibJS/Runtime/BigInt.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Object.h>

namespace JS {

struct ClampedU8 {
};

// 25.1.1 Notation (read-modify-write modification function), https://tc39.es/ecma262/#sec-arraybuffer-notation
using ReadWriteModifyFunction = Function<ByteBuffer(ByteBuffer, ByteBuffer)>;

enum class PreserveResizability {
    FixedLength,
    PreserveResizability
};

// 6.2.9 Data Blocks, https://tc39.es/ecma262/#sec-data-blocks
struct DataBlock {
    enum class Shared {
        No,
        Yes,
    };

    ByteBuffer& buffer()
    {
        ByteBuffer* ptr { nullptr };
        byte_buffer.visit([&](Empty) { VERIFY_NOT_REACHED(); }, [&](auto* pointer) { ptr = pointer; }, [&](auto& value) { ptr = &value; });
        return *ptr;
    }
    ByteBuffer const& buffer() const { return const_cast<DataBlock*>(this)->buffer(); }

    Variant<Empty, ByteBuffer, ByteBuffer*> byte_buffer;
    Shared is_shared = { Shared::No };
};

class ArrayBuffer : public Object {
    JS_OBJECT(ArrayBuffer, Object);

public:
    static ThrowCompletionOr<NonnullGCPtr<ArrayBuffer>> create(Realm&, size_t);
    static NonnullGCPtr<ArrayBuffer> create(Realm&, ByteBuffer);
    static NonnullGCPtr<ArrayBuffer> create(Realm&, ByteBuffer*);

    virtual ~ArrayBuffer() override = default;

    size_t byte_length() const
    {
        if (is_detached())
            return 0;

        return m_data_block.buffer().size();
    }

    // [[ArrayBufferData]]
    ByteBuffer& buffer() { return m_data_block.buffer(); }
    ByteBuffer const& buffer() const { return m_data_block.buffer(); }

    // Used by allocate_array_buffer() to attach the data block after construction
    void set_data_block(DataBlock block) { m_data_block = move(block); }

    Value detach_key() const { return m_detach_key; }
    void set_detach_key(Value detach_key) { m_detach_key = detach_key; }

    void detach_buffer() { m_data_block.byte_buffer = Empty {}; }

    // 25.1.2.2 IsDetachedBuffer ( arrayBuffer ), https://tc39.es/ecma262/#sec-isdetachedbuffer
    bool is_detached() const
    {
        // 1. If arrayBuffer.[[ArrayBufferData]] is null, return true.
        if (m_data_block.byte_buffer.has<Empty>())
            return true;
        // 2. Return false.
        return false;
    }

    // 25.2.1.2 IsSharedArrayBuffer ( obj ), https://tc39.es/ecma262/#sec-issharedarraybuffer
    bool is_shared_array_buffer() const
    {
        // 1. Let bufferData be obj.[[ArrayBufferData]].
        // 2. If bufferData is null, return false.
        if (m_data_block.byte_buffer.has<Empty>())
            return false;
        // 3. If bufferData is a Data Block, return false.
        if (m_data_block.is_shared == DataBlock::Shared::No)
            return false;
        // 4. Assert: bufferData is a Shared Data Block.
        VERIFY(m_data_block.is_shared == DataBlock::Shared::Yes);
        // 5. Return true.
        return true;
    }

    enum Order {
        SeqCst,
        Unordered
    };
    template<typename type>
    ThrowCompletionOr<Value> get_value(size_t byte_index, bool is_typed_array, Order, bool is_little_endian = true);
    template<typename type>
    ThrowCompletionOr<void> set_value(size_t byte_index, Value value, bool is_typed_array, Order, bool is_little_endian = true);
    template<typename T>
    ThrowCompletionOr<Value> get_modify_set_value(size_t byte_index, Value value, ReadWriteModifyFunction operation, bool is_little_endian = true);

private:
    ArrayBuffer(ByteBuffer buffer, Object& prototype);
    ArrayBuffer(ByteBuffer* buffer, Object& prototype);

    virtual void visit_edges(Visitor&) override;

    DataBlock m_data_block;
    // The various detach related members of ArrayBuffer are not used by any ECMA262 functionality,
    // but are required to be available for the use of various harnesses like the Test262 test runner.
    Value m_detach_key;
};

ThrowCompletionOr<DataBlock> create_byte_data_block(VM& vm, size_t size);
void copy_data_block_bytes(ByteBuffer& to_block, u64 to_index, ByteBuffer const& from_block, u64 from_index, u64 count);
ThrowCompletionOr<ArrayBuffer*> allocate_array_buffer(VM&, FunctionObject& constructor, size_t byte_length);
ThrowCompletionOr<void> detach_array_buffer(VM&, ArrayBuffer& array_buffer, Optional<Value> key = {});
ThrowCompletionOr<ArrayBuffer*> clone_array_buffer(VM&, ArrayBuffer& source_buffer, size_t source_byte_offset, size_t source_length);
ThrowCompletionOr<ArrayBuffer*> array_buffer_copy_and_detach(VM&, ArrayBuffer& array_buffer, Value new_length, PreserveResizability preserve_resizability);
ThrowCompletionOr<NonnullGCPtr<ArrayBuffer>> allocate_shared_array_buffer(VM&, FunctionObject& constructor, size_t byte_length);

// 25.1.2.9 RawBytesToNumeric ( type, rawBytes, isLittleEndian ), https://tc39.es/ecma262/#sec-rawbytestonumeric
template<typename T>
static Value raw_bytes_to_numeric(VM& vm, ByteBuffer raw_value, bool is_little_endian)
{
    // 1. Let elementSize be the Element Size value specified in Table 70 for Element Type type.
    //    NOTE: Used in step 6, but not needed with our implementation of that step.

    // 2. If isLittleEndian is false, reverse the order of the elements of rawBytes.
    if (!is_little_endian) {
        VERIFY(raw_value.size() % 2 == 0);
        for (size_t i = 0; i < raw_value.size() / 2; ++i)
            swap(raw_value[i], raw_value[raw_value.size() - 1 - i]);
    }

    // 3. If type is Float32, then
    using UnderlyingBufferDataType = Conditional<IsSame<ClampedU8, T>, u8, T>;
    if constexpr (IsSame<UnderlyingBufferDataType, float>) {
        // a. Let value be the byte elements of rawBytes concatenated and interpreted as a little-endian bit string encoding of an IEEE 754-2019 binary32 value.
        float value;
        raw_value.span().copy_to({ &value, sizeof(float) });

        // b. If value is an IEEE 754-2019 binary32 NaN value, return the NaN Number value.
        if (isnan(value))
            return js_nan();

        // c. Return the Number value that corresponds to value.
        return Value(value);
    }

    // 4. If type is Float64, then
    if constexpr (IsSame<UnderlyingBufferDataType, double>) {
        // a. Let value be the byte elements of rawBytes concatenated and interpreted as a little-endian bit string encoding of an IEEE 754-2019 binary64 value.
        double value;
        raw_value.span().copy_to({ &value, sizeof(double) });

        // b. If value is an IEEE 754-2019 binary64 NaN value, return the NaN Number value.
        if (isnan(value))
            return js_nan();

        // c. Return the Number value that corresponds to value.
        return Value(value);
    }

    // NOTE: Not in spec, sanity check for steps below.
    if constexpr (!IsIntegral<UnderlyingBufferDataType>)
        VERIFY_NOT_REACHED();

    // 5. If IsUnsignedElementType(type) is true, then
    //     a. Let intValue be the byte elements of rawBytes concatenated and interpreted as a bit string encoding of an unsigned little-endian binary number.
    // 6. Else,
    //     a. Let intValue be the byte elements of rawBytes concatenated and interpreted as a bit string encoding of a binary little-endian two's complement number of bit length elementSize × 8.
    //
    // NOTE: The signed/unsigned logic above is implemented in step 7 by the IsSigned<> check, and in step 8 by JS::Value constructor overloads.
    UnderlyingBufferDataType int_value = 0;
    raw_value.span().copy_to({ &int_value, sizeof(UnderlyingBufferDataType) });

    // 7. If IsBigIntElementType(type) is true, return the BigInt value that corresponds to intValue.
    if constexpr (sizeof(UnderlyingBufferDataType) == 8) {
        if constexpr (IsSigned<UnderlyingBufferDataType>) {
            static_assert(IsSame<UnderlyingBufferDataType, i64>);
            return BigInt::create(vm, Crypto::SignedBigInteger { int_value });
        } else {
            static_assert(IsOneOf<UnderlyingBufferDataType, u64, double>);
            return BigInt::create(vm, Crypto::SignedBigInteger { Crypto::UnsignedBigInteger { int_value } });
        }
    }
    // 8. Otherwise, return the Number value that corresponds to intValue.
    else {
        return Value(int_value);
    }
}

// Implementation for 25.1.2.10 GetValueFromBuffer, used in TypedArray<T>::get_value_from_buffer(), https://tc39.es/ecma262/#sec-getvaluefrombuffer
template<typename T>
ThrowCompletionOr<Value> ArrayBuffer::get_value(size_t byte_index, [[maybe_unused]] bool is_typed_array, Order, bool is_little_endian)
{
    auto& vm = this->vm();
    // 1. Assert: IsDetachedBuffer(arrayBuffer) is false.
    VERIFY(!is_detached());

    // 2. Assert: There are sufficient bytes in arrayBuffer starting at byteIndex to represent a value of type.
    VERIFY(m_data_block.buffer().bytes().slice(byte_index).size() >= sizeof(T));

    // 3. Let block be arrayBuffer.[[ArrayBufferData]].
    auto& block = m_data_block.buffer();

    // 4. Let elementSize be the Element Size value specified in Table 70 for Element Type type.
    auto element_size = sizeof(T);

    ByteBuffer raw_value;

    // FIXME: 5. If IsSharedArrayBuffer(arrayBuffer) is true, then
    if (false) {
        // FIXME: a. Let execution be the [[CandidateExecution]] field of the surrounding agent's Agent Record.
        // FIXME: b. Let eventsRecord be the Agent Events Record of execution.[[EventsRecords]] whose [[AgentSignifier]] is AgentSignifier().
        // FIXME: c. If isTypedArray is true and IsNoTearConfiguration(type, order) is true, let noTear be true; otherwise let noTear be false.
        // FIXME: d. Let rawValue be a List of length elementSize whose elements are nondeterministically chosen byte values.
        // FIXME: e. NOTE: In implementations, rawValue is the result of a non-atomic or atomic read instruction on the underlying hardware. The nondeterminism is a semantic prescription of the memory model to describe observable behaviour of hardware with weak consistency.
        // FIXME: f. Let readEvent be ReadSharedMemory { [[Order]]: order, [[NoTear]]: noTear, [[Block]]: block, [[ByteIndex]]: byteIndex, [[ElementSize]]: elementSize }.
        // FIXME: g. Append readEvent to eventsRecord.[[EventList]].
        // FIXME: h. Append Chosen Value Record { [[Event]]: readEvent, [[ChosenValue]]: rawValue } to execution.[[ChosenValues]].
    }
    // 6. Else,
    else {
        // a. Let rawValue be a List whose elements are bytes from block at indices in the interval from byteIndex (inclusive) to byteIndex + elementSize (exclusive).
        raw_value = TRY_OR_THROW_OOM(vm, block.slice(byte_index, element_size));
    }

    // 7. Assert: The number of elements in rawValue is elementSize.
    VERIFY(raw_value.size() == element_size);

    // 8. If isLittleEndian is not present, set isLittleEndian to the value of the [[LittleEndian]] field of the surrounding agent's Agent Record.
    //    NOTE: Done by default parameter at declaration of this function.

    // 9. Return RawBytesToNumeric(type, rawValue, isLittleEndian).
    return raw_bytes_to_numeric<T>(vm, move(raw_value), is_little_endian);
}

// 25.1.2.11 NumericToRawBytes ( type, value, isLittleEndian ), https://tc39.es/ecma262/#sec-numerictorawbytes
template<typename T>
static ThrowCompletionOr<ByteBuffer> numeric_to_raw_bytes(VM& vm, Value value, bool is_little_endian)
{
    VERIFY(value.is_number() || value.is_bigint());
    using UnderlyingBufferDataType = Conditional<IsSame<ClampedU8, T>, u8, T>;
    ByteBuffer raw_bytes = TRY_OR_THROW_OOM(vm, ByteBuffer::create_uninitialized(sizeof(UnderlyingBufferDataType)));
    auto flip_if_needed = [&]() {
        if (is_little_endian)
            return;
        VERIFY(sizeof(UnderlyingBufferDataType) % 2 == 0);
        for (size_t i = 0; i < sizeof(UnderlyingBufferDataType) / 2; ++i)
            swap(raw_bytes[i], raw_bytes[sizeof(UnderlyingBufferDataType) - 1 - i]);
    };
    if constexpr (IsSame<UnderlyingBufferDataType, float>) {
        float raw_value = MUST(value.to_double(vm));
        ReadonlyBytes { &raw_value, sizeof(float) }.copy_to(raw_bytes);
        flip_if_needed();
        return raw_bytes;
    }
    if constexpr (IsSame<UnderlyingBufferDataType, double>) {
        double raw_value = MUST(value.to_double(vm));
        ReadonlyBytes { &raw_value, sizeof(double) }.copy_to(raw_bytes);
        flip_if_needed();
        return raw_bytes;
    }
    if constexpr (!IsIntegral<UnderlyingBufferDataType>)
        VERIFY_NOT_REACHED();
    if constexpr (sizeof(UnderlyingBufferDataType) == 8) {
        UnderlyingBufferDataType int_value;

        if constexpr (IsSigned<UnderlyingBufferDataType>)
            int_value = MUST(value.to_bigint_int64(vm));
        else
            int_value = MUST(value.to_bigint_uint64(vm));

        ReadonlyBytes { &int_value, sizeof(UnderlyingBufferDataType) }.copy_to(raw_bytes);
        flip_if_needed();
        return raw_bytes;
    } else {
        UnderlyingBufferDataType int_value;
        if constexpr (IsSigned<UnderlyingBufferDataType>) {
            if constexpr (sizeof(UnderlyingBufferDataType) == 4)
                int_value = MUST(value.to_i32(vm));
            else if constexpr (sizeof(UnderlyingBufferDataType) == 2)
                int_value = MUST(value.to_i16(vm));
            else
                int_value = MUST(value.to_i8(vm));
        } else {
            if constexpr (sizeof(UnderlyingBufferDataType) == 4)
                int_value = MUST(value.to_u32(vm));
            else if constexpr (sizeof(UnderlyingBufferDataType) == 2)
                int_value = MUST(value.to_u16(vm));
            else if constexpr (!IsSame<T, ClampedU8>)
                int_value = MUST(value.to_u8(vm));
            else
                int_value = MUST(value.to_u8_clamp(vm));
        }
        ReadonlyBytes { &int_value, sizeof(UnderlyingBufferDataType) }.copy_to(raw_bytes);
        if constexpr (sizeof(UnderlyingBufferDataType) % 2 == 0)
            flip_if_needed();
        return raw_bytes;
    }
}

// 25.1.2.12 SetValueInBuffer ( arrayBuffer, byteIndex, type, value, isTypedArray, order [ , isLittleEndian ] ), https://tc39.es/ecma262/#sec-setvalueinbuffer
template<typename T>
ThrowCompletionOr<void> ArrayBuffer::set_value(size_t byte_index, Value value, [[maybe_unused]] bool is_typed_array, Order, bool is_little_endian)
{
    auto& vm = this->vm();

    // 1. Assert: IsDetachedBuffer(arrayBuffer) is false.
    VERIFY(!is_detached());

    // 2. Assert: There are sufficient bytes in arrayBuffer starting at byteIndex to represent a value of type.
    VERIFY(m_data_block.buffer().bytes().slice(byte_index).size() >= sizeof(T));

    // 3. Assert: value is a BigInt if IsBigIntElementType(type) is true; otherwise, value is a Number.
    if constexpr (IsIntegral<T> && sizeof(T) == 8)
        VERIFY(value.is_bigint());
    else
        VERIFY(value.is_number());

    // 4. Let block be arrayBuffer.[[ArrayBufferData]].
    auto& block = m_data_block.buffer();

    // FIXME: 5. Let elementSize be the Element Size value specified in Table 70 for Element Type type.

    // 6. If isLittleEndian is not present, set isLittleEndian to the value of the [[LittleEndian]] field of the surrounding agent's Agent Record.
    //    NOTE: Done by default parameter at declaration of this function.

    // 7. Let rawBytes be NumericToRawBytes(type, value, isLittleEndian).
    auto raw_bytes = MUST_OR_THROW_OOM(numeric_to_raw_bytes<T>(vm, value, is_little_endian));

    // FIXME 8. If IsSharedArrayBuffer(arrayBuffer) is true, then
    if (false) {
        // FIXME: a. Let execution be the [[CandidateExecution]] field of the surrounding agent's Agent Record.
        // FIXME: b. Let eventsRecord be the Agent Events Record of execution.[[EventsRecords]] whose [[AgentSignifier]] is AgentSignifier().
        // FIXME: c. If isTypedArray is true and IsNoTearConfiguration(type, order) is true, let noTear be true; otherwise let noTear be false.
        // FIXME: d. Append WriteSharedMemory { [[Order]]: order, [[NoTear]]: noTear, [[Block]]: block, [[ByteIndex]]: byteIndex, [[ElementSize]]: elementSize, [[Payload]]: rawBytes } to eventsRecord.[[EventList]].
    }
    // 9. Else,
    else {
        // a. Store the individual bytes of rawBytes into block, starting at block[byteIndex].
        raw_bytes.span().copy_to(block.span().slice(byte_index));
    }

    // 10. Return unused.
    return {};
}

// 25.1.2.13 GetModifySetValueInBuffer ( arrayBuffer, byteIndex, type, value, op [ , isLittleEndian ] ), https://tc39.es/ecma262/#sec-getmodifysetvalueinbuffer
template<typename T>
ThrowCompletionOr<Value> ArrayBuffer::get_modify_set_value(size_t byte_index, Value value, ReadWriteModifyFunction operation, bool is_little_endian)
{
    auto& vm = this->vm();

    auto raw_bytes = MUST_OR_THROW_OOM(numeric_to_raw_bytes<T>(vm, value, is_little_endian));

    // FIXME: Check for shared buffer

    auto raw_bytes_read = TRY_OR_THROW_OOM(vm, m_data_block.buffer().slice(byte_index, sizeof(T)));
    auto raw_bytes_modified = operation(raw_bytes_read, raw_bytes);
    raw_bytes_modified.span().copy_to(m_data_block.buffer().span().slice(byte_index));

    return raw_bytes_to_numeric<T>(vm, raw_bytes_read, is_little_endian);
}

}

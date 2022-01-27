/*
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Function.h>
#include <AK/Variant.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Object.h>

namespace JS {

struct ClampedU8 {
};

// 25.1.1 Notation (read-modify-write modification function), https://tc39.es/ecma262/#sec-arraybuffer-notation
using ReadWriteModifyFunction = Function<ByteBuffer(ByteBuffer, ByteBuffer)>;

class ArrayBuffer : public Object {
    JS_OBJECT(ArrayBuffer, Object);

public:
    static ArrayBuffer* create(GlobalObject&, size_t);
    static ArrayBuffer* create(GlobalObject&, ByteBuffer);
    static ArrayBuffer* create(GlobalObject&, ByteBuffer*);

    ArrayBuffer(ByteBuffer buffer, Object& prototype);
    ArrayBuffer(ByteBuffer* buffer, Object& prototype);
    virtual ~ArrayBuffer() override;

    size_t byte_length() const { return buffer_impl().size(); }
    ByteBuffer& buffer() { return buffer_impl(); }
    const ByteBuffer& buffer() const { return buffer_impl(); }

    // Used by allocate_array_buffer() to attach the data block after construction
    void set_buffer(ByteBuffer buffer) { m_buffer = move(buffer); }

    Value detach_key() const { return m_detach_key; }
    void set_detach_key(Value detach_key) { m_detach_key = detach_key; }

    void detach_buffer() { m_buffer = Empty {}; }
    bool is_detached() const { return m_buffer.has<Empty>(); }

    enum Order {
        SeqCst,
        Unordered
    };
    template<typename type>
    Value get_value(size_t byte_index, bool is_typed_array, Order, bool is_little_endian = true);
    template<typename type>
    Value set_value(size_t byte_index, Value value, bool is_typed_array, Order, bool is_little_endian = true);
    template<typename T>
    Value get_modify_set_value(size_t byte_index, Value value, ReadWriteModifyFunction operation, bool is_little_endian = true);

private:
    virtual void visit_edges(Visitor&) override;

    ByteBuffer& buffer_impl()
    {
        ByteBuffer* ptr { nullptr };
        m_buffer.visit([&](Empty) { VERIFY_NOT_REACHED(); }, [&](auto* pointer) { ptr = pointer; }, [&](auto& value) { ptr = &value; });
        return *ptr;
    }

    const ByteBuffer& buffer_impl() const { return const_cast<ArrayBuffer*>(this)->buffer_impl(); }

    Variant<Empty, ByteBuffer, ByteBuffer*> m_buffer;
    // The various detach related members of ArrayBuffer are not used by any ECMA262 functionality,
    // but are required to be available for the use of various harnesses like the Test262 test runner.
    Value m_detach_key;
};

ThrowCompletionOr<ArrayBuffer*> allocate_array_buffer(GlobalObject&, FunctionObject& constructor, size_t byte_length);

// 25.1.2.9 RawBytesToNumeric ( type, rawBytes, isLittleEndian ), https://tc39.es/ecma262/#sec-rawbytestonumeric
template<typename T>
static Value raw_bytes_to_numeric(GlobalObject& global_object, ByteBuffer raw_value, bool is_little_endian)
{
    if (!is_little_endian) {
        VERIFY(raw_value.size() % 2 == 0);
        for (size_t i = 0; i < raw_value.size() / 2; ++i)
            swap(raw_value[i], raw_value[raw_value.size() - 1 - i]);
    }
    using UnderlyingBufferDataType = Conditional<IsSame<ClampedU8, T>, u8, T>;
    if constexpr (IsSame<UnderlyingBufferDataType, float>) {
        float value;
        raw_value.span().copy_to({ &value, sizeof(float) });
        if (isnan(value))
            return js_nan();
        return Value(value);
    }
    if constexpr (IsSame<UnderlyingBufferDataType, double>) {
        double value;
        raw_value.span().copy_to({ &value, sizeof(double) });
        if (isnan(value))
            return js_nan();
        return Value(value);
    }
    if constexpr (!IsIntegral<UnderlyingBufferDataType>)
        VERIFY_NOT_REACHED();
    UnderlyingBufferDataType int_value = 0;
    raw_value.span().copy_to({ &int_value, sizeof(UnderlyingBufferDataType) });
    if constexpr (sizeof(UnderlyingBufferDataType) == 8) {
        if constexpr (IsSigned<UnderlyingBufferDataType>)
            return js_bigint(global_object.heap(), Crypto::SignedBigInteger::create_from(int_value));
        else
            return js_bigint(global_object.heap(), Crypto::SignedBigInteger { Crypto::UnsignedBigInteger::create_from(int_value) });
    } else {
        return Value(int_value);
    }
}

// Implementation for 25.1.2.10 GetValueFromBuffer, used in TypedArray<T>::get_value_from_buffer().
template<typename T>
Value ArrayBuffer::get_value(size_t byte_index, [[maybe_unused]] bool is_typed_array, Order, bool is_little_endian)
{
    auto element_size = sizeof(T);

    // FIXME: Check for shared buffer

    auto raw_value = buffer_impl().slice(byte_index, element_size);
    return raw_bytes_to_numeric<T>(global_object(), move(raw_value), is_little_endian);
}

// 25.1.2.11 NumericToRawBytes ( type, value, isLittleEndian ), https://tc39.es/ecma262/#sec-numerictorawbytes
template<typename T>
static ByteBuffer numeric_to_raw_bytes(GlobalObject& global_object, Value value, bool is_little_endian)
{
    VERIFY(value.is_number() || value.is_bigint());
    using UnderlyingBufferDataType = Conditional<IsSame<ClampedU8, T>, u8, T>;
    ByteBuffer raw_bytes = ByteBuffer::create_uninitialized(sizeof(UnderlyingBufferDataType)).release_value_but_fixme_should_propagate_errors(); // FIXME: Handle possible OOM situation.
    auto flip_if_needed = [&]() {
        if (is_little_endian)
            return;
        VERIFY(sizeof(UnderlyingBufferDataType) % 2 == 0);
        for (size_t i = 0; i < sizeof(UnderlyingBufferDataType) / 2; ++i)
            swap(raw_bytes[i], raw_bytes[sizeof(UnderlyingBufferDataType) - 1 - i]);
    };
    if constexpr (IsSame<UnderlyingBufferDataType, float>) {
        float raw_value = MUST(value.to_double(global_object));
        ReadonlyBytes { &raw_value, sizeof(float) }.copy_to(raw_bytes);
        flip_if_needed();
        return raw_bytes;
    }
    if constexpr (IsSame<UnderlyingBufferDataType, double>) {
        double raw_value = MUST(value.to_double(global_object));
        ReadonlyBytes { &raw_value, sizeof(double) }.copy_to(raw_bytes);
        flip_if_needed();
        return raw_bytes;
    }
    if constexpr (!IsIntegral<UnderlyingBufferDataType>)
        VERIFY_NOT_REACHED();
    if constexpr (sizeof(UnderlyingBufferDataType) == 8) {
        UnderlyingBufferDataType int_value;

        if constexpr (IsSigned<UnderlyingBufferDataType>)
            int_value = MUST(value.to_bigint_int64(global_object));
        else
            int_value = MUST(value.to_bigint_uint64(global_object));

        ReadonlyBytes { &int_value, sizeof(UnderlyingBufferDataType) }.copy_to(raw_bytes);
        flip_if_needed();
        return raw_bytes;
    } else {
        UnderlyingBufferDataType int_value;
        if constexpr (IsSigned<UnderlyingBufferDataType>) {
            if constexpr (sizeof(UnderlyingBufferDataType) == 4)
                int_value = MUST(value.to_i32(global_object));
            else if constexpr (sizeof(UnderlyingBufferDataType) == 2)
                int_value = MUST(value.to_i16(global_object));
            else
                int_value = MUST(value.to_i8(global_object));
        } else {
            if constexpr (sizeof(UnderlyingBufferDataType) == 4)
                int_value = MUST(value.to_u32(global_object));
            else if constexpr (sizeof(UnderlyingBufferDataType) == 2)
                int_value = MUST(value.to_u16(global_object));
            else if constexpr (!IsSame<T, ClampedU8>)
                int_value = MUST(value.to_u8(global_object));
            else
                int_value = MUST(value.to_u8_clamp(global_object));
        }
        ReadonlyBytes { &int_value, sizeof(UnderlyingBufferDataType) }.copy_to(raw_bytes);
        if constexpr (sizeof(UnderlyingBufferDataType) % 2 == 0)
            flip_if_needed();
        return raw_bytes;
    }
}

// 25.1.2.12 SetValueInBuffer ( arrayBuffer, byteIndex, type, value, isTypedArray, order [ , isLittleEndian ] ), https://tc39.es/ecma262/#sec-setvalueinbuffer
template<typename T>
Value ArrayBuffer::set_value(size_t byte_index, Value value, [[maybe_unused]] bool is_typed_array, Order, bool is_little_endian)
{
    auto raw_bytes = numeric_to_raw_bytes<T>(global_object(), value, is_little_endian);

    // FIXME: Check for shared buffer

    raw_bytes.span().copy_to(buffer_impl().span().slice(byte_index));
    return js_undefined();
}

// 25.1.2.13 GetModifySetValueInBuffer ( arrayBuffer, byteIndex, type, value, op [ , isLittleEndian ] ), https://tc39.es/ecma262/#sec-getmodifysetvalueinbuffer
template<typename T>
Value ArrayBuffer::get_modify_set_value(size_t byte_index, Value value, ReadWriteModifyFunction operation, bool is_little_endian)
{
    auto raw_bytes = numeric_to_raw_bytes<T>(global_object(), value, is_little_endian);

    // FIXME: Check for shared buffer

    auto raw_bytes_read = buffer_impl().slice(byte_index, sizeof(T));
    auto raw_bytes_modified = operation(raw_bytes_read, raw_bytes);
    raw_bytes_modified.span().copy_to(buffer_impl().span().slice(byte_index));

    return raw_bytes_to_numeric<T>(global_object(), raw_bytes_read, is_little_endian);
}

}

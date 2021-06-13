/*
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Variant.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Object.h>

namespace JS {

class ArrayBuffer : public Object {
    JS_OBJECT(ArrayBuffer, Object);

public:
    static ArrayBuffer* create(GlobalObject&, size_t);
    static ArrayBuffer* create(GlobalObject&, ByteBuffer*);

    ArrayBuffer(size_t, Object& prototype);
    ArrayBuffer(ByteBuffer* buffer, Object& prototype);
    virtual ~ArrayBuffer() override;

    size_t byte_length() const { return buffer_impl().size(); }
    ByteBuffer& buffer() { return buffer_impl(); }
    const ByteBuffer& buffer() const { return buffer_impl(); }

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

// 25.1.2.9 RawBytesToNumeric ( type, rawBytes, isLittleEndian ), https://tc39.es/ecma262/#sec-rawbytestonumeric
template<typename T>
static Value raw_bytes_to_numeric(GlobalObject& global_object, ByteBuffer raw_value, bool is_little_endian)
{
    if (!is_little_endian) {
        VERIFY(raw_value.size() % 2 == 0);
        for (size_t i = 0; i < raw_value.size() / 2; ++i)
            swap(raw_value[i], raw_value[raw_value.size() - 1 - i]);
    }
    if constexpr (IsSame<T, float>) {
        float value;
        raw_value.span().copy_to({ &value, sizeof(float) });
        if (isnan(value))
            return js_nan();
        return Value(value);
    }
    if constexpr (IsSame<T, double>) {
        double value;
        raw_value.span().copy_to({ &value, sizeof(double) });
        if (isnan(value))
            return js_nan();
        return Value(value);
    }
    if constexpr (!IsIntegral<T>)
        VERIFY_NOT_REACHED();
    T int_value = 0;
    raw_value.span().copy_to({ &int_value, sizeof(T) });
    if constexpr (sizeof(T) == 8) {
        if constexpr (IsSigned<T>)
            return js_bigint(global_object.heap(), Crypto::SignedBigInteger::create_from(int_value));
        else
            return js_bigint(global_object.heap(), Crypto::SignedBigInteger { Crypto::UnsignedBigInteger::create_from(int_value) });
    } else {
        return Value(int_value);
    }
}

// 25.1.2.10 GetValueFromBuffer ( arrayBuffer, byteIndex, type, isTypedArray, order [ , isLittleEndian ] ), https://tc39.es/ecma262/#sec-getvaluefrombuffer
template<typename T>
Value ArrayBuffer::get_value(size_t byte_index, [[maybe_unused]] bool is_typed_array, Order, bool is_little_endian)
{
    auto element_size = sizeof(T);

    // FIXME: Check for shared buffer

    auto raw_value = buffer_impl().slice(byte_index, element_size);
    return raw_bytes_to_numeric<T>(global_object(), move(raw_value), is_little_endian);
}

}

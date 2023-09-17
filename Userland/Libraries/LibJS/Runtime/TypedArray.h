/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2023, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/ArrayBuffer.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/PropertyDescriptor.h>
#include <LibJS/Runtime/PropertyKey.h>
#include <LibJS/Runtime/TypedArrayConstructor.h>
#include <LibJS/Runtime/VM.h>

namespace JS {

class TypedArrayBase;

ThrowCompletionOr<TypedArrayBase*> typed_array_from(VM&, Value);
ThrowCompletionOr<void> validate_typed_array(VM&, TypedArrayBase&);

class TypedArrayBase : public Object {
    JS_OBJECT(TypedArrayBase, Object);

public:
    enum class ContentType {
        BigInt,
        Number,
    };

    using IntrinsicConstructor = NonnullGCPtr<TypedArrayConstructor> (Intrinsics::*)();

    u32 array_length() const { return m_array_length; }
    u32 byte_length() const { return m_byte_length; }
    u32 byte_offset() const { return m_byte_offset; }
    ContentType content_type() const { return m_content_type; }
    ArrayBuffer* viewed_array_buffer() const { return m_viewed_array_buffer; }
    IntrinsicConstructor intrinsic_constructor() const { return m_intrinsic_constructor; }

    void set_array_length(u32 length) { m_array_length = length; }
    void set_byte_length(u32 length) { m_byte_length = length; }
    void set_byte_offset(u32 offset) { m_byte_offset = offset; }
    void set_viewed_array_buffer(ArrayBuffer* array_buffer) { m_viewed_array_buffer = array_buffer; }

    virtual size_t element_size() const = 0;
    virtual DeprecatedFlyString const& element_name() const = 0;

    // 25.1.2.6 IsUnclampedIntegerElementType ( type ), https://tc39.es/ecma262/#sec-isunclampedintegerelementtype
    virtual bool is_unclamped_integer_element_type() const = 0;
    // 25.1.2.7 IsBigIntElementType ( type ), https://tc39.es/ecma262/#sec-isbigintelementtype
    virtual bool is_bigint_element_type() const = 0;
    // 25.1.2.10 GetValueFromBuffer ( arrayBuffer, byteIndex, type, isTypedArray, order [ , isLittleEndian ] ), https://tc39.es/ecma262/#sec-getvaluefrombuffer
    virtual ThrowCompletionOr<Value> get_value_from_buffer(size_t byte_index, ArrayBuffer::Order, bool is_little_endian = true) const = 0;
    // 25.1.2.12 SetValueInBuffer ( arrayBuffer, byteIndex, type, value, isTypedArray, order [ , isLittleEndian ] ), https://tc39.es/ecma262/#sec-setvalueinbuffer
    virtual ThrowCompletionOr<void> set_value_in_buffer(size_t byte_index, Value, ArrayBuffer::Order, bool is_little_endian = true) = 0;
    // 25.1.2.13 GetModifySetValueInBuffer ( arrayBuffer, byteIndex, type, value, op [ , isLittleEndian ] ), https://tc39.es/ecma262/#sec-getmodifysetvalueinbuffer
    virtual ThrowCompletionOr<Value> get_modify_set_value_in_buffer(size_t byte_index, Value value, ReadWriteModifyFunction operation, bool is_little_endian = true) = 0;

protected:
    TypedArrayBase(Object& prototype, IntrinsicConstructor intrinsic_constructor)
        : Object(ConstructWithPrototypeTag::Tag, prototype)
        , m_intrinsic_constructor(intrinsic_constructor)
    {
    }

    u32 m_array_length { 0 };
    u32 m_byte_length { 0 };
    u32 m_byte_offset { 0 };
    ContentType m_content_type { ContentType::Number };
    GCPtr<ArrayBuffer> m_viewed_array_buffer;
    IntrinsicConstructor m_intrinsic_constructor { nullptr };

private:
    virtual void visit_edges(Visitor&) override;
};

// 10.4.5.9 IsValidIntegerIndex ( O, index ), https://tc39.es/ecma262/#sec-isvalidintegerindex
inline bool is_valid_integer_index(TypedArrayBase const& typed_array, CanonicalIndex property_index)
{
    // 1. If IsDetachedBuffer(O.[[ViewedArrayBuffer]]) is true, return false.
    if (typed_array.viewed_array_buffer()->is_detached())
        return false;

    // 2. If IsIntegralNumber(index) is false, return false.
    // 3. If index is -0𝔽, return false.
    if (!property_index.is_index())
        return false;

    // 4. If ℝ(index) < 0 or ℝ(index) ≥ O.[[ArrayLength]], return false.
    if (property_index.as_index() >= typed_array.array_length())
        return false;

    // 5. Return true.
    return true;
}

// 10.4.5.10 IntegerIndexedElementGet ( O, index ), https://tc39.es/ecma262/#sec-integerindexedelementget
template<typename T>
inline ThrowCompletionOr<Value> integer_indexed_element_get(TypedArrayBase const& typed_array, CanonicalIndex property_index)
{
    // 1. If IsValidIntegerIndex(O, index) is false, return undefined.
    if (!is_valid_integer_index(typed_array, property_index))
        return js_undefined();

    // 2. Let offset be O.[[ByteOffset]].
    auto offset = typed_array.byte_offset();

    // 3. Let elementSize be TypedArrayElementSize(O).
    // 4. Let indexedPosition be (ℝ(index) × elementSize) + offset.
    Checked<size_t> indexed_position = property_index.as_index();
    indexed_position *= typed_array.element_size();
    indexed_position += offset;
    // FIXME: Not exactly sure what we should do when overflow occurs.
    //        Just return as if it's an invalid index for now.
    if (indexed_position.has_overflow()) {
        dbgln("integer_indexed_element_get(): indexed_position overflowed, returning as if it's an invalid index.");
        return js_undefined();
    }

    // 5. Let elementType be TypedArrayElementType(O).
    // 6. Return GetValueFromBuffer(O.[[ViewedArrayBuffer]], indexedPosition, elementType, true, Unordered).
    return typed_array.viewed_array_buffer()->template get_value<T>(indexed_position.value(), true, ArrayBuffer::Order::Unordered);
}

// 10.4.5.11 IntegerIndexedElementSet ( O, index, value ), https://tc39.es/ecma262/#sec-integerindexedelementset
// NOTE: In error cases, the function will return as if it succeeded.
template<typename T>
inline ThrowCompletionOr<void> integer_indexed_element_set(TypedArrayBase& typed_array, CanonicalIndex property_index, Value value)
{
    VERIFY(!value.is_empty());
    auto& vm = typed_array.vm();

    Value num_value;

    // 1. If O.[[ContentType]] is BigInt, let numValue be ? ToBigInt(value).
    if (typed_array.content_type() == TypedArrayBase::ContentType::BigInt)
        num_value = TRY(value.to_bigint(vm));
    // 2. Otherwise, let numValue be ? ToNumber(value).
    else
        num_value = TRY(value.to_number(vm));

    // 3. If IsValidIntegerIndex(O, index) is true, then
    // NOTE: Inverted for flattened logic.
    if (!is_valid_integer_index(typed_array, property_index))
        return {};

    // a. Let offset be O.[[ByteOffset]].
    auto offset = typed_array.byte_offset();

    // b. Let elementSize be TypedArrayElementSize(O).
    // c. Let indexedPosition be (ℝ(index) × elementSize) + offset.
    Checked<size_t> indexed_position = property_index.as_index();
    indexed_position *= typed_array.element_size();
    indexed_position += offset;
    // FIXME: Not exactly sure what we should do when overflow occurs.
    //        Just return as if it succeeded for now.
    if (indexed_position.has_overflow()) {
        dbgln("integer_indexed_element_set(): indexed_position overflowed, returning as if succeeded.");
        return {};
    }

    // d. Let elementType be TypedArrayElementType(O).
    // e. Perform SetValueInBuffer(O.[[ViewedArrayBuffer]], indexedPosition, elementType, numValue, true, Unordered).
    MUST_OR_THROW_OOM(typed_array.viewed_array_buffer()->template set_value<T>(indexed_position.value(), num_value, true, ArrayBuffer::Order::Unordered));

    // 4. Return unused.
    return {};
}

template<typename T>
class TypedArray : public TypedArrayBase {
    JS_OBJECT(TypedArray, TypedArrayBase);

    using UnderlyingBufferDataType = Conditional<IsSame<ClampedU8, T>, u8, T>;

public:
    // 10.4.5.1 [[GetOwnProperty]] ( P ), https://tc39.es/ecma262/#sec-integer-indexed-exotic-objects-getownproperty-p
    virtual ThrowCompletionOr<Optional<PropertyDescriptor>> internal_get_own_property(PropertyKey const& property_key) const override
    {
        VERIFY(property_key.is_valid());

        // NOTE: If the property name is a number type (An implementation-defined optimized
        // property key type), it can be treated as a string property that will transparently be
        // converted into a canonical numeric index.

        // 1. If Type(P) is String, then
        // NOTE: This includes an implementation-defined optimization, see note above!
        if (property_key.is_string() || property_key.is_number()) {
            // a. Let numericIndex be CanonicalNumericIndexString(P).
            auto numeric_index = canonical_numeric_index_string(property_key, CanonicalIndexMode::DetectNumericRoundtrip);
            // b. If numericIndex is not undefined, then
            if (!numeric_index.is_undefined()) {
                // i. Let value be IntegerIndexedElementGet(O, numericIndex).
                auto value = MUST_OR_THROW_OOM(integer_indexed_element_get<T>(*this, numeric_index));

                // ii. If value is undefined, return undefined.
                if (value.is_undefined())
                    return Optional<PropertyDescriptor> {};

                // iii. Return the PropertyDescriptor { [[Value]]: value, [[Writable]]: true, [[Enumerable]]: true, [[Configurable]]: true }.
                return PropertyDescriptor {
                    .value = value,
                    .writable = true,
                    .enumerable = true,
                    .configurable = true,
                };
            }
        }

        // 2. Return OrdinaryGetOwnProperty(O, P).
        return Object::internal_get_own_property(property_key);
    }

    // 10.4.5.2 [[HasProperty]] ( P ), https://tc39.es/ecma262/#sec-integer-indexed-exotic-objects-hasproperty-p
    virtual ThrowCompletionOr<bool> internal_has_property(PropertyKey const& property_key) const override
    {
        VERIFY(property_key.is_valid());

        // NOTE: If the property name is a number type (An implementation-defined optimized
        // property key type), it can be treated as a string property that will transparently be
        // converted into a canonical numeric index.

        // 1. If Type(P) is String, then
        // NOTE: This includes an implementation-defined optimization, see note above!
        if (property_key.is_string() || property_key.is_number()) {
            // a. Let numericIndex be CanonicalNumericIndexString(P).
            auto numeric_index = canonical_numeric_index_string(property_key, CanonicalIndexMode::DetectNumericRoundtrip);
            // b. If numericIndex is not undefined, return IsValidIntegerIndex(O, numericIndex).
            if (!numeric_index.is_undefined())
                return is_valid_integer_index(*this, numeric_index);
        }

        // 2. Return ? OrdinaryHasProperty(O, P).
        return Object::internal_has_property(property_key);
    }

    // 10.4.5.3 [[DefineOwnProperty]] ( P, Desc ), https://tc39.es/ecma262/#sec-integer-indexed-exotic-objects-defineownproperty-p-desc
    virtual ThrowCompletionOr<bool> internal_define_own_property(PropertyKey const& property_key, PropertyDescriptor const& property_descriptor) override
    {
        VERIFY(property_key.is_valid());

        // NOTE: If the property name is a number type (An implementation-defined optimized
        // property key type), it can be treated as a string property that will transparently be
        // converted into a canonical numeric index.

        // 1. If Type(P) is String, then
        // NOTE: This includes an implementation-defined optimization, see note above!
        if (property_key.is_string() || property_key.is_number()) {
            // a. Let numericIndex be CanonicalNumericIndexString(P).
            auto numeric_index = canonical_numeric_index_string(property_key, CanonicalIndexMode::DetectNumericRoundtrip);
            // b. If numericIndex is not undefined, then
            if (!numeric_index.is_undefined()) {
                // i. If IsValidIntegerIndex(O, numericIndex) is false, return false.
                if (!is_valid_integer_index(*this, numeric_index))
                    return false;

                // ii. If Desc has a [[Configurable]] field and if Desc.[[Configurable]] is false, return false.
                if (property_descriptor.configurable.has_value() && !*property_descriptor.configurable)
                    return false;

                // iii. If Desc has an [[Enumerable]] field and if Desc.[[Enumerable]] is false, return false.
                if (property_descriptor.enumerable.has_value() && !*property_descriptor.enumerable)
                    return false;

                // iv. If IsAccessorDescriptor(Desc) is true, return false.
                if (property_descriptor.is_accessor_descriptor())
                    return false;

                // v. If Desc has a [[Writable]] field and if Desc.[[Writable]] is false, return false.
                if (property_descriptor.writable.has_value() && !*property_descriptor.writable)
                    return false;

                // vi. If Desc has a [[Value]] field, perform ? IntegerIndexedElementSet(O, numericIndex, Desc.[[Value]]).
                if (property_descriptor.value.has_value())
                    TRY(integer_indexed_element_set<T>(*this, numeric_index, *property_descriptor.value));

                // vii. Return true.
                return true;
            }
        }

        // 2. Return ! OrdinaryDefineOwnProperty(O, P, Desc).
        return Object::internal_define_own_property(property_key, property_descriptor);
    }

    // 10.4.5.4 [[Get]] ( P, Receiver ), 10.4.5.4 [[Get]] ( P, Receiver )
    virtual ThrowCompletionOr<Value> internal_get(PropertyKey const& property_key, Value receiver, CacheablePropertyMetadata* cacheable_metadata) const override
    {
        VERIFY(!receiver.is_empty());

        VERIFY(property_key.is_valid());
        // NOTE: If the property name is a number type (An implementation-defined optimized
        // property key type), it can be treated as a string property that will transparently be
        // converted into a canonical numeric index.

        // 1. If Type(P) is String, then
        // NOTE: This includes an implementation-defined optimization, see note above!
        if (property_key.is_string() || property_key.is_number()) {
            // a. Let numericIndex be CanonicalNumericIndexString(P).
            auto numeric_index = canonical_numeric_index_string(property_key, CanonicalIndexMode::DetectNumericRoundtrip);
            // b. If numericIndex is not undefined, then
            if (!numeric_index.is_undefined()) {
                // i. Return IntegerIndexedElementGet(O, numericIndex).
                return integer_indexed_element_get<T>(*this, numeric_index);
            }
        }

        // 2. Return ? OrdinaryGet(O, P, Receiver).
        return Object::internal_get(property_key, receiver, cacheable_metadata);
    }

    // 10.4.5.5 [[Set]] ( P, V, Receiver ), https://tc39.es/ecma262/#sec-integer-indexed-exotic-objects-set-p-v-receiver
    virtual ThrowCompletionOr<bool> internal_set(PropertyKey const& property_key, Value value, Value receiver) override
    {
        VERIFY(!value.is_empty());
        VERIFY(!receiver.is_empty());

        VERIFY(property_key.is_valid());
        // NOTE: If the property name is a number type (An implementation-defined optimized
        // property key type), it can be treated as a string property that will transparently be
        // converted into a canonical numeric index.

        // 1. If Type(P) is String, then
        // NOTE: This includes an implementation-defined optimization, see note above!
        if (property_key.is_string() || property_key.is_number()) {
            // a. Let numericIndex be CanonicalNumericIndexString(P).
            auto numeric_index = canonical_numeric_index_string(property_key, CanonicalIndexMode::DetectNumericRoundtrip);
            // b. If numericIndex is not undefined, then
            if (!numeric_index.is_undefined()) {
                // i. If SameValue(O, Receiver) is true, then
                if (same_value(this, receiver)) {
                    // 1. Perform ? IntegerIndexedElementSet(O, numericIndex, V).
                    TRY(integer_indexed_element_set<T>(*this, numeric_index, value));

                    // 2. Return true.
                    return true;
                }

                // ii. If IsValidIntegerIndex(O, numericIndex) is false, return true.
                if (!is_valid_integer_index(*this, numeric_index))
                    return true;
            }
        }

        // 2. Return ? OrdinarySet(O, P, V, Receiver).
        return Object::internal_set(property_key, value, receiver);
    }

    // 10.4.5.6 [[Delete]] ( P ), https://tc39.es/ecma262/#sec-integer-indexed-exotic-objects-delete-p
    virtual ThrowCompletionOr<bool> internal_delete(PropertyKey const& property_key) override
    {
        VERIFY(property_key.is_valid());

        // NOTE: If the property name is a number type (An implementation-defined optimized
        // property key type), it can be treated as a string property that will transparently be
        // converted into a canonical numeric index.

        // 1. If Type(P) is String, then
        // NOTE: This includes an implementation-defined optimization, see note above!
        if (property_key.is_string() || property_key.is_number()) {
            // a. Let numericIndex be CanonicalNumericIndexString(P).
            auto numeric_index = canonical_numeric_index_string(property_key, CanonicalIndexMode::DetectNumericRoundtrip);
            // b. If numericIndex is not undefined, then
            if (!numeric_index.is_undefined()) {
                // i. If IsValidIntegerIndex(O, numericIndex) is false, return true; else return false.
                if (!is_valid_integer_index(*this, numeric_index))
                    return true;
                return false;
            }
        }

        // 2. Return ? OrdinaryDelete(O, P).
        return Object::internal_delete(property_key);
    }

    // 10.4.5.7 [[OwnPropertyKeys]] ( ), https://tc39.es/ecma262/#sec-integer-indexed-exotic-objects-ownpropertykeys
    virtual ThrowCompletionOr<MarkedVector<Value>> internal_own_property_keys() const override
    {
        auto& vm = this->vm();

        // 1. Let keys be a new empty List.
        auto keys = MarkedVector<Value> { heap() };

        // 2. If IsDetachedBuffer(O.[[ViewedArrayBuffer]]) is false, then
        if (!m_viewed_array_buffer->is_detached()) {
            // a. For each integer i starting with 0 such that i < O.[[ArrayLength]], in ascending order, do
            for (size_t i = 0; i < m_array_length; ++i) {
                // i. Add ! ToString(𝔽(i)) as the last element of keys.
                keys.append(PrimitiveString::create(vm, DeprecatedString::number(i)));
            }
        }

        // 3. For each own property key P of O such that Type(P) is String and P is not an integer index, in ascending chronological order of property creation, do
        for (auto& it : shape().property_table()) {
            if (it.key.is_string()) {
                // a. Add P as the last element of keys.
                keys.append(it.key.to_value(vm));
            }
        }

        // 4. For each own property key P of O such that Type(P) is Symbol, in ascending chronological order of property creation, do
        for (auto& it : shape().property_table()) {
            if (it.key.is_symbol()) {
                // a. Add P as the last element of keys.
                keys.append(it.key.to_value(vm));
            }
        }

        // 5. Return keys.
        return { move(keys) };
    }

    ReadonlySpan<UnderlyingBufferDataType> data() const
    {
        return { reinterpret_cast<UnderlyingBufferDataType const*>(m_viewed_array_buffer->buffer().data() + m_byte_offset), m_array_length };
    }
    Span<UnderlyingBufferDataType> data()
    {
        return { reinterpret_cast<UnderlyingBufferDataType*>(m_viewed_array_buffer->buffer().data() + m_byte_offset), m_array_length };
    }

    virtual size_t element_size() const override { return sizeof(UnderlyingBufferDataType); }

    bool is_unclamped_integer_element_type() const override
    {
        constexpr bool is_unclamped_integer = IsSame<T, i8> || IsSame<T, u8> || IsSame<T, i16> || IsSame<T, u16> || IsSame<T, i32> || IsSame<T, u32>;
        return is_unclamped_integer;
    }

    bool is_bigint_element_type() const override
    {
        constexpr bool is_bigint = IsSame<T, i64> || IsSame<T, u64>;
        return is_bigint;
    }

    ThrowCompletionOr<Value> get_value_from_buffer(size_t byte_index, ArrayBuffer::Order order, bool is_little_endian = true) const override { return viewed_array_buffer()->template get_value<T>(byte_index, true, order, is_little_endian); }
    ThrowCompletionOr<void> set_value_in_buffer(size_t byte_index, Value value, ArrayBuffer::Order order, bool is_little_endian = true) override { return viewed_array_buffer()->template set_value<T>(byte_index, value, true, order, is_little_endian); }
    ThrowCompletionOr<Value> get_modify_set_value_in_buffer(size_t byte_index, Value value, ReadWriteModifyFunction operation, bool is_little_endian = true) override { return viewed_array_buffer()->template get_modify_set_value<T>(byte_index, value, move(operation), is_little_endian); }

protected:
    TypedArray(Object& prototype, IntrinsicConstructor intrinsic_constructor, u32 array_length, ArrayBuffer& array_buffer)
        : TypedArrayBase(prototype, intrinsic_constructor)
    {
        VERIFY(!Checked<u32>::multiplication_would_overflow(array_length, sizeof(UnderlyingBufferDataType)));
        m_viewed_array_buffer = &array_buffer;
        if (array_length)
            VERIFY(!data().is_null());
        m_array_length = array_length;
        m_byte_length = m_viewed_array_buffer->byte_length();
    }

private:
    virtual bool is_typed_array() const final { return true; }
};

ThrowCompletionOr<TypedArrayBase*> typed_array_create(VM&, FunctionObject& constructor, MarkedVector<Value> arguments);
ThrowCompletionOr<TypedArrayBase*> typed_array_create_same_type(VM&, TypedArrayBase const& exemplar, MarkedVector<Value> arguments);
ThrowCompletionOr<double> compare_typed_array_elements(VM&, Value x, Value y, FunctionObject* comparefn);

#define JS_DECLARE_TYPED_ARRAY(ClassName, snake_name, PrototypeName, ConstructorName, Type)                       \
    class ClassName : public TypedArray<Type> {                                                                   \
        JS_OBJECT(ClassName, TypedArray);                                                                         \
                                                                                                                  \
    public:                                                                                                       \
        virtual ~ClassName();                                                                                     \
        static ThrowCompletionOr<NonnullGCPtr<ClassName>> create(Realm&, u32 length, FunctionObject& new_target); \
        static ThrowCompletionOr<NonnullGCPtr<ClassName>> create(Realm&, u32 length);                             \
        static NonnullGCPtr<ClassName> create(Realm&, u32 length, ArrayBuffer& buffer);                           \
        virtual DeprecatedFlyString const& element_name() const override;                                         \
                                                                                                                  \
    protected:                                                                                                    \
        ClassName(Object& prototype, u32 length, ArrayBuffer& array_buffer);                                      \
    };                                                                                                            \
    class PrototypeName final : public Object {                                                                   \
        JS_OBJECT(PrototypeName, Object);                                                                         \
                                                                                                                  \
    public:                                                                                                       \
        virtual void initialize(Realm&) override;                                                                 \
        virtual ~PrototypeName() override;                                                                        \
                                                                                                                  \
    private:                                                                                                      \
        PrototypeName(Object& prototype);                                                                         \
    };                                                                                                            \
    class ConstructorName final : public TypedArrayConstructor {                                                  \
        JS_OBJECT(ConstructorName, TypedArrayConstructor);                                                        \
                                                                                                                  \
    public:                                                                                                       \
        virtual void initialize(Realm&) override;                                                                 \
        virtual ~ConstructorName() override;                                                                      \
                                                                                                                  \
        virtual ThrowCompletionOr<Value> call() override;                                                         \
        virtual ThrowCompletionOr<NonnullGCPtr<Object>> construct(FunctionObject& new_target) override;           \
                                                                                                                  \
    private:                                                                                                      \
        explicit ConstructorName(Realm&, Object& prototype);                                                      \
        virtual bool has_constructor() const override                                                             \
        {                                                                                                         \
            return true;                                                                                          \
        }                                                                                                         \
    };

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, Type) \
    JS_DECLARE_TYPED_ARRAY(ClassName, snake_name, PrototypeName, ConstructorName, Type);
JS_ENUMERATE_TYPED_ARRAYS
#undef __JS_ENUMERATE

}

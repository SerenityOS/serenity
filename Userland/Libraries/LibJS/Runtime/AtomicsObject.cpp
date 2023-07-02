/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Atomic.h>
#include <AK/ByteBuffer.h>
#include <AK/Endian.h>
#include <AK/TypeCasts.h>
#include <LibJS/Runtime/AtomicsObject.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/TypedArray.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

// 25.4.2.1 ValidateIntegerTypedArray ( typedArray [ , waitable ] ), https://tc39.es/ecma262/#sec-validateintegertypedarray
static ThrowCompletionOr<ArrayBuffer*> validate_integer_typed_array(VM& vm, TypedArrayBase& typed_array, bool waitable = false)
{
    // 1. If waitable is not present, set waitable to false.

    // 2. Perform ? ValidateTypedArray(typedArray).
    TRY(validate_typed_array(vm, typed_array));

    // 3. Let buffer be typedArray.[[ViewedArrayBuffer]].
    auto* buffer = typed_array.viewed_array_buffer();

    auto const& type_name = typed_array.element_name();

    // 4. If waitable is true, then
    if (waitable) {
        // a. If typedArray.[[TypedArrayName]] is not "Int32Array" or "BigInt64Array", throw a TypeError exception.
        if ((type_name != vm.names.Int32Array.as_string()) && (type_name != vm.names.BigInt64Array.as_string()))
            return vm.throw_completion<TypeError>(ErrorType::TypedArrayTypeIsNot, type_name, "Int32 or BigInt64"sv);
    }
    // 5. Else,
    else {
        // a. Let type be TypedArrayElementType(typedArray).
        // b. If IsUnclampedIntegerElementType(type) is false and IsBigIntElementType(type) is false, throw a TypeError exception.
        if (!typed_array.is_unclamped_integer_element_type() && !typed_array.is_bigint_element_type())
            return vm.throw_completion<TypeError>(ErrorType::TypedArrayTypeIsNot, type_name, "an unclamped integer or BigInt"sv);
    }

    // 6. Return buffer.
    return buffer;
}

// 25.4.2.2 ValidateAtomicAccess ( typedArray, requestIndex ), https://tc39.es/ecma262/#sec-validateatomicaccess
// 6.2.2 ValidateAtomicAccess ( typedArray, requestIndex ), https://tc39.es/proposal-resizablearraybuffer/#sec-validateatomicaccess
static ThrowCompletionOr<size_t> validate_atomic_access(VM& vm, TypedArrayBase& typed_array, Value request_index)
{
    // 1. Let getBufferByteLength be MakeIdempotentArrayBufferByteLengthGetter(Unordered).
    auto get_buffer_byte_length = make_idempotent_array_buffer_byte_length_getter(ArrayBuffer::Order::Unordered);

    // 2. Let length be IntegerIndexedObjectLength(typedArray, getBufferByteLength).
    auto length = integer_indexed_object_length(vm, typed_array, get_buffer_byte_length);

    // 3. Assert: length is not out-of-bounds.
    VERIFY(length.has_value());

    // 4. Let accessIndex be ? ToIndex(requestIndex).
    auto access_index = TRY(request_index.to_index(vm));

    // 5. Assert: accessIndex ≥ 0.

    // 6. If accessIndex ≥ length, throw a RangeError exception.
    if (access_index >= length.value())
        return vm.throw_completion<RangeError>(ErrorType::IndexOutOfRange, access_index, length.value());

    // 7. Let elementSize be TypedArrayElementSize(typedArray).
    auto element_size = typed_array.element_size();

    // 8. Let offset be typedArray.[[ByteOffset]].
    auto offset = typed_array.byte_offset();

    // 9. Return (accessIndex × elementSize) + offset.
    return (access_index * element_size) + offset;
}

// 25.4.2.11 AtomicReadModifyWrite ( typedArray, index, value, op ), https://tc39.es/ecma262/#sec-atomicreadmodifywrite
static ThrowCompletionOr<Value> atomic_read_modify_write(VM& vm, TypedArrayBase& typed_array, Value index, Value value, ReadWriteModifyFunction operation)
{
    // 1. Let buffer be ? ValidateIntegerTypedArray(typedArray).
    [[maybe_unused]] auto* buffer = TRY(validate_integer_typed_array(vm, typed_array));

    // 2. Let indexedPosition be ? ValidateAtomicAccess(typedArray, index).
    auto indexed_position = TRY(validate_atomic_access(vm, typed_array, index));

    Value value_to_set;

    // 3. If typedArray.[[ContentType]] is BigInt, let v be ? ToBigInt(value).
    if (typed_array.content_type() == TypedArrayBase::ContentType::BigInt)
        value_to_set = TRY(value.to_bigint(vm));
    // 4. Otherwise, let v be 𝔽(? ToIntegerOrInfinity(value)).
    else
        value_to_set = Value(TRY(value.to_integer_or_infinity(vm)));

    // 5. Let getBufferByteLength be MakeIdempotentArrayBufferByteLengthGetter(Unordered).
    auto get_buffer_byte_length = make_idempotent_array_buffer_byte_length_getter(ArrayBuffer::Order::Unordered);

    // FIXME: 6. NOTE: Bounds checking is not a synchronizing operation when typedArray's backing buffer is a growable SharedArrayBuffer.

    // NOTE: This below steps (7 and 8) seems to be a spec bug as we want byte length not the object length as validate_atomic_access returns a byte index
    // 7. Let length be IntegerIndexedObjectLength(typedArray, getBufferByteLength).
    auto length = integer_indexed_object_byte_length(vm, typed_array, get_buffer_byte_length);

    // 8. If length is out-of-bounds, throw a TypeError exception.

    // 9. If indexedPosition ≥ length, throw a RangeError exception.
    if (indexed_position >= length)
        return vm.throw_completion<RangeError>(ErrorType::IndexOutOfRange, indexed_position, length);

    // 10. NOTE: The above checks are not redundant with the checks in ValidateIntegerTypedArray and ValidateAtomicAccess because the call to ToBigInt or ToIntegerOrInfinity on the preceding lines can have arbitrary side effects, which could cause the buffer to become detached. These checks are only needed when typedArray's backing buffer is an ArrayBuffer.

    // 11. Let elementType be TypedArrayElementType(typedArray).
    // 12. Return GetModifySetValueInBuffer(buffer, indexedPosition, elementType, v, op).
    return typed_array.get_modify_set_value_in_buffer(indexed_position, value_to_set, move(operation));
}

template<typename T, typename AtomicFunction>
static ThrowCompletionOr<Value> perform_atomic_operation(VM& vm, TypedArrayBase& typed_array, AtomicFunction&& operation)
{
    auto index = vm.argument(1);
    auto value = vm.argument(2);

    auto operation_wrapper = [&, operation = forward<AtomicFunction>(operation)](ByteBuffer x_bytes, ByteBuffer y_bytes) -> ByteBuffer {
        if constexpr (IsFloatingPoint<T>) {
            (void)operation;
            VERIFY_NOT_REACHED();
        } else {
            using U = Conditional<IsSame<ClampedU8, T>, u8, T>;

            auto* x = reinterpret_cast<U*>(x_bytes.data());
            auto* y = reinterpret_cast<U*>(y_bytes.data());
            operation(x, *y);

            return x_bytes;
        }
    };

    return atomic_read_modify_write(vm, typed_array, index, value, move(operation_wrapper));
}

AtomicsObject::AtomicsObject(Realm& realm)
    : Object(ConstructWithPrototypeTag::Tag, realm.intrinsics().object_prototype())
{
}

ThrowCompletionOr<void> AtomicsObject::initialize(Realm& realm)
{
    MUST_OR_THROW_OOM(Base::initialize(realm));
    auto& vm = this->vm();

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(realm, vm.names.add, add, 3, attr);
    define_native_function(realm, vm.names.and_, and_, 3, attr);
    define_native_function(realm, vm.names.compareExchange, compare_exchange, 4, attr);
    define_native_function(realm, vm.names.exchange, exchange, 3, attr);
    define_native_function(realm, vm.names.isLockFree, is_lock_free, 1, attr);
    define_native_function(realm, vm.names.load, load, 2, attr);
    define_native_function(realm, vm.names.or_, or_, 3, attr);
    define_native_function(realm, vm.names.store, store, 3, attr);
    define_native_function(realm, vm.names.sub, sub, 3, attr);
    define_native_function(realm, vm.names.xor_, xor_, 3, attr);

    // 25.4.15 Atomics [ @@toStringTag ], https://tc39.es/ecma262/#sec-atomics-@@tostringtag
    define_direct_property(vm.well_known_symbol_to_string_tag(), MUST_OR_THROW_OOM(PrimitiveString::create(vm, "Atomics"sv)), Attribute::Configurable);

    return {};
}

// 25.4.3 Atomics.add ( typedArray, index, value ), https://tc39.es/ecma262/#sec-atomics.add
JS_DEFINE_NATIVE_FUNCTION(AtomicsObject::add)
{
    auto* typed_array = TRY(typed_array_from(vm, vm.argument(0)));

    auto atomic_add = [](auto* storage, auto value) { return AK::atomic_fetch_add(storage, value); };

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, Type) \
    if (is<ClassName>(typed_array))                                                 \
        return TRY(perform_atomic_operation<Type>(vm, *typed_array, move(atomic_add)));
    JS_ENUMERATE_TYPED_ARRAYS
#undef __JS_ENUMERATE

    VERIFY_NOT_REACHED();
}

// 25.4.4 Atomics.and ( typedArray, index, value ), https://tc39.es/ecma262/#sec-atomics.and
JS_DEFINE_NATIVE_FUNCTION(AtomicsObject::and_)
{
    auto* typed_array = TRY(typed_array_from(vm, vm.argument(0)));

    auto atomic_and = [](auto* storage, auto value) { return AK::atomic_fetch_and(storage, value); };

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, Type) \
    if (is<ClassName>(typed_array))                                                 \
        return TRY(perform_atomic_operation<Type>(vm, *typed_array, move(atomic_and)));
    JS_ENUMERATE_TYPED_ARRAYS
#undef __JS_ENUMERATE

    VERIFY_NOT_REACHED();
}

// 25.4.5 Atomics.compareExchange ( typedArray, index, expectedValue, replacementValue ), https://tc39.es/ecma262/#sec-atomics.compareexchange
// 6.1.1 Atomics.compareExchange ( typedArray, index, expectedValue, replacementValue ), https://tc39.es/proposal-resizablearraybuffer/#sec-atomics.compareexchange
template<typename T>
static ThrowCompletionOr<Value> atomic_compare_exchange_impl(VM& vm, TypedArrayBase& typed_array)
{
    // 1. Let buffer be ? ValidateIntegerTypedArray(typedArray).
    auto* buffer = TRY(validate_integer_typed_array(vm, typed_array));

    // 2. Let block be buffer.[[ArrayBufferData]].
    auto& block = buffer->buffer();

    // 3. Let indexedPosition be ? ValidateAtomicAccess(typedArray, index).
    auto indexed_position = TRY(validate_atomic_access(vm, typed_array, vm.argument(1)));

    Value expected;
    Value replacement;

    // 4. If typedArray.[[ContentType]] is BigInt, then
    if (typed_array.content_type() == TypedArrayBase::ContentType::BigInt) {
        // a. Let expected be ? ToBigInt(expectedValue).
        expected = TRY(vm.argument(2).to_bigint(vm));

        // b. Let replacement be ? ToBigInt(replacementValue).
        replacement = TRY(vm.argument(3).to_bigint(vm));
    }
    // 5. Else,
    else {
        // a. Let expected be 𝔽(? ToIntegerOrInfinity(expectedValue)).
        expected = Value(TRY(vm.argument(2).to_integer_or_infinity(vm)));

        // b. Let replacement be 𝔽(? ToIntegerOrInfinity(replacementValue)).
        replacement = Value(TRY(vm.argument(3).to_integer_or_infinity(vm)));
    }

    // 6. Let getBufferByteLength be MakeIdempotentArrayBufferByteLengthGetter(Unordered).
    auto get_buffer_byte_length = make_idempotent_array_buffer_byte_length_getter(ArrayBuffer::Order::Unordered);

    // FIXME: 7. NOTE: Bounds checking is not a synchronizing operation when typedArray's backing buffer is a growable SharedArrayBuffer.

    // NOTE: This below steps (8 and 9) seems to be a spec bug as we want byte length not the object length as validate_atomic_access returns a byte index
    // 8. Let length be IntegerIndexedObjectLength(typedArray, getBufferByteLength).
    auto length = integer_indexed_object_byte_length(vm, typed_array, get_buffer_byte_length);

    // 9. If length is out-of-bounds, throw a TypeError exception.

    // 10. If indexedPosition ≥ length, throw a RangeError exception.
    if (indexed_position >= length)
        return vm.throw_completion<TypeError>(ErrorType::IndexOutOfRange, indexed_position, length);

    // 11. NOTE: The above checks are not redundant with the checks in ValidateIntegerTypedArray and ValidateAtomicAccess because the call to ToBigInt or ToIntegerOrInfinity on the preceding lines can have arbitrary side effects, which could cause the buffer to become detached.
    // These checks are only needed when typedArray's backing buffer is an ArrayBuffer

    // 12. Let elementType be TypedArrayElementType(typedArray).
    // 13. Let elementSize be TypedArrayElementSize(typedArray).

    // 14. Let isLittleEndian be the value of the [[LittleEndian]] field of the surrounding agent's Agent Record.
    constexpr bool is_little_endian = __BYTE_ORDER__ == __ORDER_LITTLE_ENDIAN__;

    // 15. Let expectedBytes be NumericToRawBytes(elementType, expected, isLittleEndian).
    auto expected_bytes = numeric_to_raw_bytes<T>(vm, expected, is_little_endian);

    // 16. Let replacementBytes be NumericToRawBytes(elementType, replacement, isLittleEndian).
    auto replacement_bytes = numeric_to_raw_bytes<T>(vm, replacement, is_little_endian);

    // FIXME: Implement SharedArrayBuffer case.
    // 17. If IsSharedArrayBuffer(buffer) is true, then
    //     a-i.
    // 18. Else,

    // a. Let rawBytesRead be a List of length elementSize whose elements are the sequence of elementSize bytes starting with block[indexedPosition].
    // FIXME: Propagate errors.
    auto raw_bytes_read = MUST(block.slice(indexed_position, sizeof(T)));

    // b. If ByteListEqual(rawBytesRead, expectedBytes) is true, then
    //    i. Store the individual bytes of replacementBytes into block, starting at block[indexedPosition].
    if constexpr (IsFloatingPoint<T>) {
        VERIFY_NOT_REACHED();
    } else {
        using U = Conditional<IsSame<ClampedU8, T>, u8, T>;

        auto* v = reinterpret_cast<U*>(block.span().slice(indexed_position).data());
        auto* e = reinterpret_cast<U*>(expected_bytes.data());
        auto* r = reinterpret_cast<U*>(replacement_bytes.data());
        (void)AK::atomic_compare_exchange_strong(v, *e, *r);
    }

    // 19. Return RawBytesToNumeric(elementType, rawBytesRead, isLittleEndian).
    return raw_bytes_to_numeric<T>(vm, raw_bytes_read, is_little_endian);
}

// 25.4.5 Atomics.compareExchange ( typedArray, index, expectedValue, replacementValue ), https://tc39.es/ecma262/#sec-atomics.compareexchange
JS_DEFINE_NATIVE_FUNCTION(AtomicsObject::compare_exchange)
{
    auto* typed_array = TRY(typed_array_from(vm, vm.argument(0)));

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, Type) \
    if (is<ClassName>(typed_array))                                                 \
        return TRY(atomic_compare_exchange_impl<Type>(vm, *typed_array));
    JS_ENUMERATE_TYPED_ARRAYS
#undef __JS_ENUMERATE

    VERIFY_NOT_REACHED();
}

// 25.4.6 Atomics.exchange ( typedArray, index, value ), https://tc39.es/ecma262/#sec-atomics.exchange
JS_DEFINE_NATIVE_FUNCTION(AtomicsObject::exchange)
{
    auto* typed_array = TRY(typed_array_from(vm, vm.argument(0)));

    auto atomic_exchange = [](auto* storage, auto value) { return AK::atomic_exchange(storage, value); };

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, Type) \
    if (is<ClassName>(typed_array))                                                 \
        return TRY(perform_atomic_operation<Type>(vm, *typed_array, move(atomic_exchange)));
    JS_ENUMERATE_TYPED_ARRAYS
#undef __JS_ENUMERATE

    VERIFY_NOT_REACHED();
}

// 25.4.7 Atomics.isLockFree ( size ), https://tc39.es/ecma262/#sec-atomics.islockfree
JS_DEFINE_NATIVE_FUNCTION(AtomicsObject::is_lock_free)
{
    auto size = TRY(vm.argument(0).to_integer_or_infinity(vm));
    if (size == 1)
        return Value(AK::atomic_is_lock_free<u8>());
    if (size == 2)
        return Value(AK::atomic_is_lock_free<u16>());
    if (size == 4)
        return Value(true);
    if (size == 8)
        return Value(AK::atomic_is_lock_free<u64>());
    return Value(false);
}

// 25.4.8 Atomics.load ( typedArray, index ), https://tc39.es/ecma262/#sec-atomics.load
JS_DEFINE_NATIVE_FUNCTION(AtomicsObject::load)
{
    // 1. Let buffer be ? ValidateIntegerTypedArray(typedArray).
    auto* typed_array = TRY(typed_array_from(vm, vm.argument(0)));
    TRY(validate_integer_typed_array(vm, *typed_array));

    // 2. Let indexedPosition be ? ValidateAtomicAccess(typedArray, index).
    auto indexed_position = TRY(validate_atomic_access(vm, *typed_array, vm.argument(1)));

    // 3. If IsDetachedBuffer(buffer) is true, throw a TypeError exception.
    if (typed_array->viewed_array_buffer()->is_detached())
        return vm.throw_completion<TypeError>(ErrorType::DetachedArrayBuffer);

    // 4. NOTE: The above check is not redundant with the check in ValidateIntegerTypedArray because the call to ValidateAtomicAccess on the preceding line can have arbitrary side effects, which could cause the buffer to become detached.

    // 5. Let elementType be TypedArrayElementType(typedArray).
    // 6. Return GetValueFromBuffer(buffer, indexedPosition, elementType, true, SeqCst).
    return typed_array->get_value_from_buffer(indexed_position, ArrayBuffer::Order::SeqCst, true);
}

// 25.4.9 Atomics.or ( typedArray, index, value ), https://tc39.es/ecma262/#sec-atomics.or
JS_DEFINE_NATIVE_FUNCTION(AtomicsObject::or_)
{
    auto* typed_array = TRY(typed_array_from(vm, vm.argument(0)));

    auto atomic_or = [](auto* storage, auto value) { return AK::atomic_fetch_or(storage, value); };

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, Type) \
    if (is<ClassName>(typed_array))                                                 \
        return TRY(perform_atomic_operation<Type>(vm, *typed_array, move(atomic_or)));
    JS_ENUMERATE_TYPED_ARRAYS
#undef __JS_ENUMERATE

    VERIFY_NOT_REACHED();
}

// 25.4.10 Atomics.store ( typedArray, index, value ), https://tc39.es/ecma262/#sec-atomics.store
// 6.1.2 Atomics.store ( typedArray, index, value ), https://tc39.es/proposal-resizablearraybuffer/#sec-atomics.store
JS_DEFINE_NATIVE_FUNCTION(AtomicsObject::store)
{
    // 1. Let buffer be ? ValidateIntegerTypedArray(typedArray).
    auto* typed_array = TRY(typed_array_from(vm, vm.argument(0)));
    TRY(validate_integer_typed_array(vm, *typed_array));

    // 2. Let indexedPosition be ? ValidateAtomicAccess(typedArray, index).
    auto indexed_position = TRY(validate_atomic_access(vm, *typed_array, vm.argument(1)));

    auto value = vm.argument(2);
    Value value_to_set;

    // 3. If typedArray.[[ContentType]] is BigInt, let v be ? ToBigInt(value).
    if (typed_array->content_type() == TypedArrayBase::ContentType::BigInt)
        value_to_set = TRY(value.to_bigint(vm));
    // 4. Otherwise, let v be 𝔽(? ToIntegerOrInfinity(value)).
    else
        value_to_set = Value(TRY(value.to_integer_or_infinity(vm)));

    // 5. Let getBufferByteLength be MakeIdempotentArrayBufferByteLengthGetter(Unordered).
    auto get_buffer_byte_length = make_idempotent_array_buffer_byte_length_getter(ArrayBuffer::Order::Unordered);

    // FIXME: 6. NOTE: Bounds checking is not a synchronizing operation when typedArray's backing buffer is a growable SharedArrayBuffer.

    // NOTE: This below steps (7 and 8) seems to be a spec bug as we want byte length not the object length as validate_atomic_access returns a byte index
    // 7. Let length be IntegerIndexedObjectLength(typedArray, getBufferByteLength).
    auto length = integer_indexed_object_byte_length(vm, *typed_array, get_buffer_byte_length);

    // 8. If length is out-of-bounds, throw a TypeError exception.

    // 9. If indexedPosition ≥ length, throw a RangeError exception.
    if (indexed_position >= length)
        return vm.throw_completion<TypeError>(ErrorType::IndexOutOfRange, indexed_position, length);

    // 10. NOTE: The above check is not redundant with the check in ValidateIntegerTypedArray because the call to ToBigInt or ToIntegerOrInfinity on the preceding lines can have arbitrary side effects, which could cause the buffer to become detached.

    // 11. Let elementType be TypedArrayElementType(typedArray).
    // 12. Perform SetValueInBuffer(buffer, indexedPosition, elementType, v, true, SeqCst).
    typed_array->set_value_in_buffer(indexed_position, value_to_set, ArrayBuffer::Order::SeqCst, true);

    // 13. Return v.
    return value_to_set;
}

// 25.4.11 Atomics.sub ( typedArray, index, value ), https://tc39.es/ecma262/#sec-atomics.sub
JS_DEFINE_NATIVE_FUNCTION(AtomicsObject::sub)
{
    auto* typed_array = TRY(typed_array_from(vm, vm.argument(0)));

    auto atomic_sub = [](auto* storage, auto value) { return AK::atomic_fetch_sub(storage, value); };

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, Type) \
    if (is<ClassName>(typed_array))                                                 \
        return TRY(perform_atomic_operation<Type>(vm, *typed_array, move(atomic_sub)));
    JS_ENUMERATE_TYPED_ARRAYS
#undef __JS_ENUMERATE

    VERIFY_NOT_REACHED();
}

// 25.4.14 Atomics.xor ( typedArray, index, value ), https://tc39.es/ecma262/#sec-atomics.xor
JS_DEFINE_NATIVE_FUNCTION(AtomicsObject::xor_)
{
    auto* typed_array = TRY(typed_array_from(vm, vm.argument(0)));

    auto atomic_xor = [](auto* storage, auto value) { return AK::atomic_fetch_xor(storage, value); };

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, Type) \
    if (is<ClassName>(typed_array))                                                 \
        return TRY(perform_atomic_operation<Type>(vm, *typed_array, move(atomic_xor)));
    JS_ENUMERATE_TYPED_ARRAYS
#undef __JS_ENUMERATE

    VERIFY_NOT_REACHED();
}

}

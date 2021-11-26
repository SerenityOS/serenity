/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Atomic.h>
#include <AK/ByteBuffer.h>
#include <AK/Endian.h>
#include <LibJS/Runtime/AtomicsObject.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/TypedArray.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

// 25.4.2.1 ValidateIntegerTypedArray ( typedArray [ , waitable ] ), https://tc39.es/ecma262/#sec-validateintegertypedarray
static ThrowCompletionOr<ArrayBuffer*> validate_integer_typed_array(GlobalObject& global_object, TypedArrayBase& typed_array, bool waitable = false)
{
    auto& vm = global_object.vm();

    // 1. If waitable is not present, set waitable to false.

    // 2. Perform ? ValidateTypedArray(typedArray).
    TRY(validate_typed_array(global_object, typed_array));

    // 3. Let buffer be typedArray.[[ViewedArrayBuffer]].
    auto* buffer = typed_array.viewed_array_buffer();

    // 4. Let typeName be typedArray.[[TypedArrayName]].
    auto type_name = typed_array.element_name();

    // 5. Let type be the Element Type value in Table 72 for typeName.

    // 6. If waitable is true, then
    if (waitable) {
        // a. If typeName is not "Int32Array" or "BigInt64Array", throw a TypeError exception.
        if ((type_name != "Int32Array"sv) && (type_name != "BigInt64Array"sv))
            return vm.throw_completion<TypeError>(global_object, ErrorType::TypedArrayTypeIsNot, type_name, "Int32 or BigInt64"sv);
    }
    // 7. Else,
    else {
        // a. If ! IsUnclampedIntegerElementType(type) is false and ! IsBigIntElementType(type) is false, throw a TypeError exception.
        if (!typed_array.is_unclamped_integer_element_type() && !typed_array.is_bigint_element_type())
            return vm.throw_completion<TypeError>(global_object, ErrorType::TypedArrayTypeIsNot, type_name, "an unclamped integer or BigInt"sv);
    }

    // 8. Return buffer.
    return buffer;
}

// 25.4.2.2 ValidateAtomicAccess ( typedArray, requestIndex ), https://tc39.es/ecma262/#sec-validateatomicaccess
static ThrowCompletionOr<size_t> validate_atomic_access(GlobalObject& global_object, TypedArrayBase& typed_array, Value request_index)
{
    auto& vm = global_object.vm();

    // 1. Let length be typedArray.[[ArrayLength]].
    auto length = typed_array.array_length();

    // 2. Let accessIndex be ? ToIndex(requestIndex).
    auto access_index = TRY(request_index.to_index(global_object));

    // 3. Assert: accessIndex ≥ 0.

    // 4. If accessIndex ≥ length, throw a RangeError exception.
    if (access_index >= length)
        return vm.throw_completion<RangeError>(global_object, ErrorType::IndexOutOfRange, access_index, typed_array.array_length());

    // 5. Let arrayTypeName be typedArray.[[TypedArrayName]].
    // 6. Let elementSize be the Element Size value specified in Table 72 for arrayTypeName.
    auto element_size = typed_array.element_size();

    // 7. Let offset be typedArray.[[ByteOffset]].
    auto offset = typed_array.byte_offset();

    // 8. Return (accessIndex × elementSize) + offset.
    return (access_index * element_size) + offset;
}

// 25.4.2.11 AtomicReadModifyWrite ( typedArray, index, value, op ), https://tc39.es/ecma262/#sec-atomicreadmodifywrite
static ThrowCompletionOr<Value> atomic_read_modify_write(GlobalObject& global_object, TypedArrayBase& typed_array, Value index, Value value, ReadWriteModifyFunction operation)
{
    auto& vm = global_object.vm();

    // 1. Let buffer be ? ValidateIntegerTypedArray(typedArray).
    auto* buffer = TRY(validate_integer_typed_array(global_object, typed_array));

    // 2. Let indexedPosition be ? ValidateAtomicAccess(typedArray, index).
    auto indexed_position = TRY(validate_atomic_access(global_object, typed_array, index));

    // 3. Let arrayTypeName be typedArray.[[TypedArrayName]].

    Value value_to_set;

    // 4. If typedArray.[[ContentType]] is BigInt, let v be ? ToBigInt(value).
    if (typed_array.content_type() == TypedArrayBase::ContentType::BigInt)
        value_to_set = TRY(value.to_bigint(global_object));
    // 5. Otherwise, let v be 𝔽(? ToIntegerOrInfinity(value)).
    else
        value_to_set = Value(TRY(value.to_integer_or_infinity(global_object)));

    // 6. If IsDetachedBuffer(buffer) is true, throw a TypeError exception.
    if (buffer->is_detached())
        return vm.throw_completion<TypeError>(global_object, ErrorType::DetachedArrayBuffer);

    // 7. NOTE: The above check is not redundant with the check in ValidateIntegerTypedArray because the call to ToBigInt or ToIntegerOrInfinity on the preceding lines can have arbitrary side effects, which could cause the buffer to become detached.

    // 8. Let elementType be the Element Type value in Table 72 for arrayTypeName.

    // 9. Return GetModifySetValueInBuffer(buffer, indexedPosition, elementType, v, op).
    return typed_array.get_modify_set_value_in_buffer(indexed_position, value_to_set, move(operation));
}

template<typename T, typename AtomicFunction>
static ThrowCompletionOr<Value> perform_atomic_operation(GlobalObject& global_object, TypedArrayBase& typed_array, AtomicFunction&& operation)
{
    auto& vm = global_object.vm();
    auto index = vm.argument(1);
    auto value = vm.argument(2);

    auto operation_wrapper = [&, operation = forward<AtomicFunction>(operation)](ByteBuffer x_bytes, ByteBuffer y_bytes) -> ByteBuffer {
        if constexpr (IsFloatingPoint<T>) {
            VERIFY_NOT_REACHED();
        } else {
            using U = Conditional<IsSame<ClampedU8, T>, u8, T>;

            auto* x = reinterpret_cast<U*>(x_bytes.data());
            auto* y = reinterpret_cast<U*>(y_bytes.data());
            operation(x, *y);

            return x_bytes;
        }
    };

    return atomic_read_modify_write(global_object, typed_array, index, value, move(operation_wrapper));
}

AtomicsObject::AtomicsObject(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void AtomicsObject::initialize(GlobalObject& global_object)
{
    Object::initialize(global_object);
    auto& vm = this->vm();

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.add, add, 3, attr);
    define_native_function(vm.names.and_, and_, 3, attr);
    define_native_function(vm.names.compareExchange, compare_exchange, 4, attr);
    define_native_function(vm.names.exchange, exchange, 3, attr);
    define_native_function(vm.names.isLockFree, is_lock_free, 1, attr);
    define_native_function(vm.names.load, load, 2, attr);
    define_native_function(vm.names.or_, or_, 3, attr);
    define_native_function(vm.names.store, store, 3, attr);
    define_native_function(vm.names.sub, sub, 3, attr);
    define_native_function(vm.names.xor_, xor_, 3, attr);

    // 25.4.15 Atomics [ @@toStringTag ], https://tc39.es/ecma262/#sec-atomics-@@tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(global_object.heap(), "Atomics"), Attribute::Configurable);
}

// 25.4.3 Atomics.add ( typedArray, index, value ), https://tc39.es/ecma262/#sec-atomics.add
JS_DEFINE_NATIVE_FUNCTION(AtomicsObject::add)
{
    auto* typed_array = TRY(typed_array_from(global_object, vm.argument(0)));

    auto atomic_add = [](auto* storage, auto value) { return AK::atomic_fetch_add(storage, value); };

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, Type) \
    if (is<ClassName>(typed_array))                                                 \
        return TRY(perform_atomic_operation<Type>(global_object, *typed_array, move(atomic_add)));
    JS_ENUMERATE_TYPED_ARRAYS
#undef __JS_ENUMERATE

    VERIFY_NOT_REACHED();
}

// 25.4.4 Atomics.and ( typedArray, index, value ), https://tc39.es/ecma262/#sec-atomics.and
JS_DEFINE_NATIVE_FUNCTION(AtomicsObject::and_)
{
    auto* typed_array = TRY(typed_array_from(global_object, vm.argument(0)));

    auto atomic_and = [](auto* storage, auto value) { return AK::atomic_fetch_and(storage, value); };

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, Type) \
    if (is<ClassName>(typed_array))                                                 \
        return TRY(perform_atomic_operation<Type>(global_object, *typed_array, move(atomic_and)));
    JS_ENUMERATE_TYPED_ARRAYS
#undef __JS_ENUMERATE

    VERIFY_NOT_REACHED();
}

// Implementation of 25.4.5 Atomics.compareExchange ( typedArray, index, expectedValue, replacementValue ), https://tc39.es/ecma262/#sec-atomics.compareexchange
template<typename T>
static ThrowCompletionOr<Value> atomic_compare_exchange_impl(GlobalObject& global_object, TypedArrayBase& typed_array)
{
    auto& vm = global_object.vm();

    // 1. Let buffer be ? ValidateIntegerTypedArray(typedArray).
    auto* buffer = TRY(validate_integer_typed_array(global_object, typed_array));

    // 2. Let block be buffer.[[ArrayBufferData]].
    auto& block = buffer->buffer();

    // 3. Let indexedPosition be ? ValidateAtomicAccess(typedArray, index).
    auto indexed_position = TRY(validate_atomic_access(global_object, typed_array, vm.argument(1)));

    // 4. Let arrayTypeName be typedArray.[[TypedArrayName]].

    Value expected;
    Value replacement;

    // 5. If typedArray.[[ContentType]] is BigInt, then
    if (typed_array.content_type() == TypedArrayBase::ContentType::BigInt) {
        // a. Let expected be ? ToBigInt(expectedValue).
        expected = TRY(vm.argument(2).to_bigint(global_object));

        // b. Let replacement be ? ToBigInt(replacementValue).
        replacement = TRY(vm.argument(3).to_bigint(global_object));
    }
    // 6. Else,
    else {
        // a. Let expected be 𝔽(? ToIntegerOrInfinity(expectedValue)).
        expected = Value(TRY(vm.argument(2).to_integer_or_infinity(global_object)));

        // b. Let replacement be 𝔽(? ToIntegerOrInfinity(replacementValue)).
        replacement = Value(TRY(vm.argument(3).to_integer_or_infinity(global_object)));
    }

    // 7. If IsDetachedBuffer(buffer) is true, throw a TypeError exception.
    if (buffer->is_detached())
        return vm.template throw_completion<TypeError>(global_object, ErrorType::DetachedArrayBuffer);

    // 8. NOTE: The above check is not redundant with the check in ValidateIntegerTypedArray because the call to ToBigInt or ToIntegerOrInfinity on the preceding lines can have arbitrary side effects, which could cause the buffer to become detached.

    // 9. Let elementType be the Element Type value in Table 72 for arrayTypeName.
    // 10. Let elementSize be the Element Size value specified in Table 72 for Element Type elementType.

    // 11. Let isLittleEndian be the value of the [[LittleEndian]] field of the surrounding agent's Agent Record.
    constexpr bool is_little_endian = __BYTE_ORDER__ == __ORDER_LITTLE_ENDIAN__;

    // 12. Let expectedBytes be NumericToRawBytes(elementType, expected, isLittleEndian).
    auto expected_bytes = numeric_to_raw_bytes<T>(global_object, expected, is_little_endian);

    // 13. Let replacementBytes be NumericToRawBytes(elementType, replacement, isLittleEndian).
    auto replacement_bytes = numeric_to_raw_bytes<T>(global_object, replacement, is_little_endian);

    // FIXME: Implement SharedArrayBuffer case.
    // 14. If IsSharedArrayBuffer(buffer) is true, then
    //     a-i.
    // 15. Else,

    // a. Let rawBytesRead be a List of length elementSize whose elements are the sequence of elementSize bytes starting with block[indexedPosition].
    auto raw_bytes_read = block.slice(indexed_position, sizeof(T));

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

    // 16. Return RawBytesToNumeric(elementType, rawBytesRead, isLittleEndian).
    return raw_bytes_to_numeric<T>(global_object, raw_bytes_read, is_little_endian);
}

// 25.4.5 Atomics.compareExchange ( typedArray, index, expectedValue, replacementValue ), https://tc39.es/ecma262/#sec-atomics.compareexchange
JS_DEFINE_NATIVE_FUNCTION(AtomicsObject::compare_exchange)
{
    auto* typed_array = TRY(typed_array_from(global_object, vm.argument(0)));

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, Type) \
    if (is<ClassName>(typed_array))                                                 \
        return TRY(atomic_compare_exchange_impl<Type>(global_object, *typed_array));
    JS_ENUMERATE_TYPED_ARRAYS
#undef __JS_ENUMERATE

    VERIFY_NOT_REACHED();
}

// 25.4.6 Atomics.exchange ( typedArray, index, value ), https://tc39.es/ecma262/#sec-atomics.exchange
JS_DEFINE_NATIVE_FUNCTION(AtomicsObject::exchange)
{
    auto* typed_array = TRY(typed_array_from(global_object, vm.argument(0)));

    auto atomic_exchange = [](auto* storage, auto value) { return AK::atomic_exchange(storage, value); };

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, Type) \
    if (is<ClassName>(typed_array))                                                 \
        return TRY(perform_atomic_operation<Type>(global_object, *typed_array, move(atomic_exchange)));
    JS_ENUMERATE_TYPED_ARRAYS
#undef __JS_ENUMERATE

    VERIFY_NOT_REACHED();
}

// 25.4.7 Atomics.isLockFree ( size ), https://tc39.es/ecma262/#sec-atomics.islockfree
JS_DEFINE_NATIVE_FUNCTION(AtomicsObject::is_lock_free)
{
    auto size = TRY(vm.argument(0).to_integer_or_infinity(global_object));
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
    auto* typed_array = TRY(typed_array_from(global_object, vm.argument(0)));

    TRY(validate_integer_typed_array(global_object, *typed_array));

    auto indexed_position = TRY(validate_atomic_access(global_object, *typed_array, vm.argument(1)));

    if (typed_array->viewed_array_buffer()->is_detached())
        return vm.throw_completion<TypeError>(global_object, ErrorType::DetachedArrayBuffer);

    return typed_array->get_value_from_buffer(indexed_position, ArrayBuffer::Order::SeqCst, true);
}

// 25.4.9 Atomics.or ( typedArray, index, value ), https://tc39.es/ecma262/#sec-atomics.or
JS_DEFINE_NATIVE_FUNCTION(AtomicsObject::or_)
{
    auto* typed_array = TRY(typed_array_from(global_object, vm.argument(0)));

    auto atomic_or = [](auto* storage, auto value) { return AK::atomic_fetch_or(storage, value); };

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, Type) \
    if (is<ClassName>(typed_array))                                                 \
        return TRY(perform_atomic_operation<Type>(global_object, *typed_array, move(atomic_or)));
    JS_ENUMERATE_TYPED_ARRAYS
#undef __JS_ENUMERATE

    VERIFY_NOT_REACHED();
}

// 25.4.10 Atomics.store ( typedArray, index, value ), https://tc39.es/ecma262/#sec-atomics.store
JS_DEFINE_NATIVE_FUNCTION(AtomicsObject::store)
{
    auto* typed_array = TRY(typed_array_from(global_object, vm.argument(0)));

    TRY(validate_integer_typed_array(global_object, *typed_array));

    auto indexed_position = TRY(validate_atomic_access(global_object, *typed_array, vm.argument(1)));

    auto value = vm.argument(2);
    Value value_to_set;
    if (typed_array->content_type() == TypedArrayBase::ContentType::BigInt)
        value_to_set = TRY(value.to_bigint(global_object));
    else
        value_to_set = Value(TRY(value.to_integer_or_infinity(global_object)));

    if (typed_array->viewed_array_buffer()->is_detached())
        return vm.throw_completion<TypeError>(global_object, ErrorType::DetachedArrayBuffer);

    typed_array->set_value_in_buffer(indexed_position, value_to_set, ArrayBuffer::Order::SeqCst, true);
    return value_to_set;
}

// 25.4.11 Atomics.sub ( typedArray, index, value ), https://tc39.es/ecma262/#sec-atomics.sub
JS_DEFINE_NATIVE_FUNCTION(AtomicsObject::sub)
{
    auto* typed_array = TRY(typed_array_from(global_object, vm.argument(0)));

    auto atomic_sub = [](auto* storage, auto value) { return AK::atomic_fetch_sub(storage, value); };

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, Type) \
    if (is<ClassName>(typed_array))                                                 \
        return TRY(perform_atomic_operation<Type>(global_object, *typed_array, move(atomic_sub)));
    JS_ENUMERATE_TYPED_ARRAYS
#undef __JS_ENUMERATE

    VERIFY_NOT_REACHED();
}

// 25.4.14 Atomics.xor ( typedArray, index, value ), https://tc39.es/ecma262/#sec-atomics.xor
JS_DEFINE_NATIVE_FUNCTION(AtomicsObject::xor_)
{
    auto* typed_array = TRY(typed_array_from(global_object, vm.argument(0)));

    auto atomic_xor = [](auto* storage, auto value) { return AK::atomic_fetch_xor(storage, value); };

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, Type) \
    if (is<ClassName>(typed_array))                                                 \
        return TRY(perform_atomic_operation<Type>(global_object, *typed_array, move(atomic_xor)));
    JS_ENUMERATE_TYPED_ARRAYS
#undef __JS_ENUMERATE

    VERIFY_NOT_REACHED();
}

}

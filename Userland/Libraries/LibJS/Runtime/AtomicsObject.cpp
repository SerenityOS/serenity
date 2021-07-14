/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Atomic.h>
#include <AK/ByteBuffer.h>
#include <LibJS/Runtime/AtomicsObject.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/TypedArray.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

// 25.4.2.1 ValidateIntegerTypedArray ( typedArray [ , waitable ] ), https://tc39.es/ecma262/#sec-validateintegertypedarray
static void validate_integer_typed_array(GlobalObject& global_object, TypedArrayBase& typed_array, bool waitable = false)
{
    auto& vm = global_object.vm();

    validate_typed_array(global_object, typed_array);
    if (vm.exception())
        return;

    auto type_name = typed_array.element_name();

    if (waitable) {
        if ((type_name != "Int32Array"sv) && (type_name != "BigInt64Array"sv))
            vm.throw_exception<TypeError>(global_object, ErrorType::TypedArrayTypeIsNot, type_name, "Int32 or BigInt64"sv);
    } else {
        if (!typed_array.is_unclamped_integer_element_type() && !typed_array.is_bigint_element_type())
            vm.throw_exception<TypeError>(global_object, ErrorType::TypedArrayTypeIsNot, type_name, "an unclamped integer or BigInt"sv);
    }
}

// 25.4.2.2 ValidateAtomicAccess ( typedArray, requestIndex ), https://tc39.es/ecma262/#sec-validateatomicaccess
static Optional<size_t> validate_atomic_access(GlobalObject& global_object, TypedArrayBase& typed_array, Value request_index)
{
    auto& vm = global_object.vm();

    auto access_index = request_index.to_index(global_object);
    if (vm.exception())
        return {};

    if (access_index >= typed_array.array_length()) {
        vm.throw_exception<RangeError>(global_object, ErrorType::IndexOutOfRange, access_index, typed_array.array_length());
        return {};
    }

    return access_index * typed_array.element_size() + typed_array.byte_offset();
}

// 25.4.2.11 AtomicReadModifyWrite ( typedArray, index, value, op ), https://tc39.es/ecma262/#sec-atomicreadmodifywrite
static Value atomic_read_modify_write(GlobalObject& global_object, TypedArrayBase& typed_array, Value index, Value value, ReadWriteModifyFunction operation)
{
    auto& vm = global_object.vm();

    validate_integer_typed_array(global_object, typed_array);
    if (vm.exception())
        return {};

    auto byte_index = validate_atomic_access(global_object, typed_array, index);
    if (!byte_index.has_value())
        return {};

    Value value_to_set;
    if (typed_array.content_type() == TypedArrayBase::ContentType::BigInt) {
        value_to_set = value.to_bigint(global_object);
        if (vm.exception())
            return {};
    } else {
        value_to_set = Value(value.to_integer_or_infinity(global_object));
        if (vm.exception())
            return {};
    }

    if (typed_array.viewed_array_buffer()->is_detached()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::DetachedArrayBuffer);
        return {};
    }

    return typed_array.get_modify_set_value_in_buffer(*byte_index, value_to_set, move(operation));
}

template<typename T, typename AtomicFunction>
static Value perform_atomic_operation(GlobalObject& global_object, TypedArrayBase& typed_array, AtomicFunction&& operation)
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
    define_native_function(vm.names.load, load, 2, attr);
    define_native_function(vm.names.or_, or_, 3, attr);
    define_native_function(vm.names.sub, sub, 3, attr);

    // 25.4.15 Atomics [ @@toStringTag ], https://tc39.es/ecma262/#sec-atomics-@@tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(global_object.heap(), "Atomics"), Attribute::Configurable);
}

// 25.4.3 Atomics.add ( typedArray, index, value ), https://tc39.es/ecma262/#sec-atomics.add
JS_DEFINE_NATIVE_FUNCTION(AtomicsObject::add)
{
    auto* typed_array = typed_array_from(global_object, vm.argument(0));
    if (!typed_array)
        return {};

    auto atomic_add = [](auto* storage, auto value) { return AK::atomic_fetch_add(storage, value); };

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, Type) \
    if (is<ClassName>(typed_array))                                                 \
        return perform_atomic_operation<Type>(global_object, *typed_array, move(atomic_add));
    JS_ENUMERATE_TYPED_ARRAYS
#undef __JS_ENUMERATE

    VERIFY_NOT_REACHED();
}

// 25.4.4 Atomics.and ( typedArray, index, value ), https://tc39.es/ecma262/#sec-atomics.and
JS_DEFINE_NATIVE_FUNCTION(AtomicsObject::and_)
{
    auto* typed_array = typed_array_from(global_object, vm.argument(0));
    if (!typed_array)
        return {};

    auto atomic_and = [](auto* storage, auto value) { return AK::atomic_fetch_and(storage, value); };

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, Type) \
    if (is<ClassName>(typed_array))                                                 \
        return perform_atomic_operation<Type>(global_object, *typed_array, move(atomic_and));
    JS_ENUMERATE_TYPED_ARRAYS
#undef __JS_ENUMERATE

    VERIFY_NOT_REACHED();
}

// 25.4.8 Atomics.load ( typedArray, index ), https://tc39.es/ecma262/#sec-atomics.load
JS_DEFINE_NATIVE_FUNCTION(AtomicsObject::load)
{
    auto* typed_array = typed_array_from(global_object, vm.argument(0));
    if (!typed_array)
        return {};

    validate_integer_typed_array(global_object, *typed_array);
    if (vm.exception())
        return {};

    auto indexed_position = validate_atomic_access(global_object, *typed_array, vm.argument(1));
    if (!indexed_position.has_value())
        return {};

    if (typed_array->viewed_array_buffer()->is_detached()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::DetachedArrayBuffer);
        return {};
    }

    return typed_array->get_value_from_buffer(*indexed_position, ArrayBuffer::Order::SeqCst, true);
}

// 25.4.9 Atomics.or ( typedArray, index, value ), https://tc39.es/ecma262/#sec-atomics.or
JS_DEFINE_NATIVE_FUNCTION(AtomicsObject::or_)
{
    auto* typed_array = typed_array_from(global_object, vm.argument(0));
    if (!typed_array)
        return {};

    auto atomic_or = [](auto* storage, auto value) { return AK::atomic_fetch_or(storage, value); };

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, Type) \
    if (is<ClassName>(typed_array))                                                 \
        return perform_atomic_operation<Type>(global_object, *typed_array, move(atomic_or));
    JS_ENUMERATE_TYPED_ARRAYS
#undef __JS_ENUMERATE

    VERIFY_NOT_REACHED();
}

// 25.4.11 Atomics.sub ( typedArray, index, value ), https://tc39.es/ecma262/#sec-atomics.sub
JS_DEFINE_NATIVE_FUNCTION(AtomicsObject::sub)
{
    auto* typed_array = typed_array_from(global_object, vm.argument(0));
    if (!typed_array)
        return {};

    auto atomic_sub = [](auto* storage, auto value) { return AK::atomic_fetch_sub(storage, value); };

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, Type) \
    if (is<ClassName>(typed_array))                                                 \
        return perform_atomic_operation<Type>(global_object, *typed_array, move(atomic_sub));
    JS_ENUMERATE_TYPED_ARRAYS
#undef __JS_ENUMERATE

    VERIFY_NOT_REACHED();
}

}

/*
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/ArrayIterator.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/TypedArray.h>
#include <LibJS/Runtime/TypedArrayPrototype.h>

namespace JS {

TypedArrayPrototype::TypedArrayPrototype(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void TypedArrayPrototype::initialize(GlobalObject& object)
{
    auto& vm = this->vm();
    Object::initialize(object);
    u8 attr = Attribute::Writable | Attribute::Configurable;

    define_native_accessor(vm.names.length, length_getter, nullptr, Attribute::Configurable);
    define_native_accessor(vm.names.buffer, buffer_getter, nullptr, Attribute::Configurable);
    define_native_accessor(vm.names.byteLength, byte_length_getter, nullptr, Attribute::Configurable);
    define_native_accessor(vm.names.byteOffset, byte_offset_getter, nullptr, Attribute::Configurable);
    define_native_function(vm.names.at, at, 1, attr);
    define_native_function(vm.names.every, every, 1, attr);
    define_native_function(vm.names.find, find, 1, attr);
    define_native_function(vm.names.findIndex, find_index, 1, attr);
    define_native_function(vm.names.forEach, for_each, 1, attr);
    define_native_function(vm.names.some, some, 1, attr);
    define_native_function(vm.names.join, join, 1, attr);
    define_native_function(vm.names.keys, keys, 0, attr);
    define_native_function(vm.names.values, values, 0, attr);
    define_native_function(vm.names.entries, entries, 0, attr);
    define_native_function(vm.names.set, set, 1, attr);
    define_native_function(vm.names.reverse, reverse, 0, attr);

    define_native_accessor(*vm.well_known_symbol_to_string_tag(), to_string_tag_getter, nullptr, Attribute::Configurable);

    // 23.2.3.29 %TypedArray%.prototype.toString ( ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.tostring
    define_property(vm.names.toString, global_object().array_prototype()->get_without_side_effects(vm.names.toString), attr);
}

TypedArrayPrototype::~TypedArrayPrototype()
{
}

static TypedArrayBase* typed_array_from(VM& vm, GlobalObject& global_object)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return nullptr;
    if (!this_object->is_typed_array()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotA, "TypedArray");
        return nullptr;
    }
    auto* typed_array = static_cast<TypedArrayBase*>(this_object);
    if (typed_array->viewed_array_buffer()->is_detached()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::DetachedArrayBuffer);
        return nullptr;
    }
    return typed_array;
}

static FunctionObject* callback_from_args(GlobalObject& global_object, const String& name)
{
    auto& vm = global_object.vm();
    if (vm.argument_count() < 1) {
        vm.throw_exception<TypeError>(global_object, ErrorType::TypedArrayPrototypeOneArg, name);
        return nullptr;
    }
    auto callback = vm.argument(0);
    if (!callback.is_function()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAFunction, callback.to_string_without_side_effects());
        return nullptr;
    }
    return &callback.as_function();
}

static void for_each_item(VM& vm, GlobalObject& global_object, const String& name, Function<IterationDecision(size_t index, Value value, Value callback_result)> callback)
{
    auto* typed_array = typed_array_from(vm, global_object);
    if (!typed_array)
        return;

    auto initial_length = typed_array->array_length();

    auto* callback_function = callback_from_args(global_object, name);
    if (!callback_function)
        return;

    auto this_value = vm.argument(1);

    for (size_t i = 0; i < initial_length; ++i) {
        auto value = typed_array->get(i);
        if (vm.exception())
            return;

        auto callback_result = vm.call(*callback_function, this_value, value, Value((i32)i), typed_array);
        if (vm.exception())
            return;

        if (callback(i, value, callback_result) == IterationDecision::Break)
            break;
    }
}

// 23.2.3.18 get %TypedArray%.prototype.length, https://tc39.es/ecma262/#sec-get-%typedarray%.prototype.length
JS_DEFINE_NATIVE_GETTER(TypedArrayPrototype::length_getter)
{
    auto typed_array = typed_array_from(vm, global_object);
    if (!typed_array)
        return {};
    auto* array_buffer = typed_array->viewed_array_buffer();
    VERIFY(array_buffer);
    if (array_buffer->is_detached())
        return Value(0);
    return Value(typed_array->array_length());
}

// 4.1 %TypedArray%.prototype.at ( index ), https://tc39.es/proposal-relative-indexing-method/#sec-%typedarray%.prototype.at
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::at)
{
    auto typed_array = typed_array_from(vm, global_object);
    if (!typed_array)
        return {};
    auto length = typed_array->array_length();
    auto relative_index = vm.argument(0).to_integer_or_infinity(global_object);
    if (vm.exception())
        return {};
    if (Value(relative_index).is_infinity())
        return js_undefined();
    Checked<size_t> index { 0 };
    if (relative_index >= 0) {
        index += relative_index;
    } else {
        index += length;
        index -= -relative_index;
    }
    if (index.has_overflow() || index.value() >= length)
        return js_undefined();
    return typed_array->get(index.value());
}

// 23.2.3.7 %TypedArray%.prototype.every ( callbackfn [ , thisArg ] ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.every
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::every)
{
    auto result = true;
    for_each_item(vm, global_object, "every", [&](auto, auto, auto callback_result) {
        if (!callback_result.to_boolean()) {
            result = false;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });
    return Value(result);
}

// 23.2.3.10 %TypedArray%.prototype.find ( predicate [ , thisArg ] ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.find
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::find)
{
    auto result = js_undefined();
    for_each_item(vm, global_object, "find", [&](auto, auto value, auto callback_result) {
        if (callback_result.to_boolean()) {
            result = value;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });
    return result;
}

// 23.2.3.11 %TypedArray%.prototype.findIndex ( predicate [ , thisArg ] ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.findindex
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::find_index)
{
    auto result_index = -1;
    for_each_item(vm, global_object, "findIndex", [&](auto index, auto, auto callback_result) {
        if (callback_result.to_boolean()) {
            result_index = index;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });
    return Value(result_index);
}

// 23.2.3.12 %TypedArray%.prototype.forEach ( callbackfn [ , thisArg ] ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.foreach
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::for_each)
{
    for_each_item(vm, global_object, "forEach", [](auto, auto, auto) {
        return IterationDecision::Continue;
    });
    return js_undefined();
}

// 23.2.3.25 %TypedArray%.prototype.some ( callbackfn [ , thisArg ] ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.some
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::some)
{
    auto result = false;
    for_each_item(vm, global_object, "some", [&](auto, auto, auto callback_result) {
        if (callback_result.to_boolean()) {
            result = true;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });
    return Value(result);
}

// 23.2.3.15 %TypedArray%.prototype.join ( separator ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.join
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::join)
{
    auto typed_array = typed_array_from(vm, global_object);
    if (!typed_array)
        return {};
    auto length = typed_array->array_length();
    String separator = ",";
    if (!vm.argument(0).is_undefined()) {
        separator = vm.argument(0).to_string(global_object);
        if (vm.exception())
            return {};
    }

    StringBuilder builder;
    for (size_t i = 0; i < length; ++i) {
        if (i > 0)
            builder.append(separator);
        auto value = typed_array->get(i);
        if (vm.exception())
            return {};
        if (value.is_nullish())
            continue;
        auto string = value.to_string(global_object);
        if (vm.exception())
            return {};
        builder.append(string);
    }

    return js_string(vm, builder.to_string());
}

// 23.2.3.16 %TypedArray%.prototype.keys ( ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.keys
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::keys)
{
    auto typed_array = typed_array_from(vm, global_object);
    if (!typed_array)
        return {};
    return ArrayIterator::create(global_object, typed_array, Object::PropertyKind::Key);
}

// 23.2.3.30 %TypedArray%.prototype.values ( ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.values
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::values)
{
    auto typed_array = typed_array_from(vm, global_object);
    if (!typed_array)
        return {};
    return ArrayIterator::create(global_object, typed_array, Object::PropertyKind::Value);
}

// 23.2.3.6 %TypedArray%.prototype.entries ( ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.entries
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::entries)
{
    auto typed_array = typed_array_from(vm, global_object);
    if (!typed_array)
        return {};
    return ArrayIterator::create(global_object, typed_array, Object::PropertyKind::KeyAndValue);
}

// 23.2.3.1 get %TypedArray%.prototype.buffer, https://tc39.es/ecma262/#sec-get-%typedarray%.prototype.buffer
JS_DEFINE_NATIVE_GETTER(TypedArrayPrototype::buffer_getter)
{
    auto typed_array = typed_array_from(vm, global_object);
    if (!typed_array)
        return {};
    auto* array_buffer = typed_array->viewed_array_buffer();
    VERIFY(array_buffer);
    return Value(array_buffer);
}

// 23.2.3.2 get %TypedArray%.prototype.byteLength, https://tc39.es/ecma262/#sec-get-%typedarray%.prototype.bytelength
JS_DEFINE_NATIVE_GETTER(TypedArrayPrototype::byte_length_getter)
{
    auto typed_array = typed_array_from(vm, global_object);
    if (!typed_array)
        return {};
    auto* array_buffer = typed_array->viewed_array_buffer();
    VERIFY(array_buffer);
    if (array_buffer->is_detached())
        return Value(0);
    return Value(typed_array->byte_length());
}

// 23.2.3.3 get %TypedArray%.prototype.byteOffset, https://tc39.es/ecma262/#sec-get-%typedarray%.prototype.byteoffset
JS_DEFINE_NATIVE_GETTER(TypedArrayPrototype::byte_offset_getter)
{
    auto typed_array = typed_array_from(vm, global_object);
    if (!typed_array)
        return {};
    auto* array_buffer = typed_array->viewed_array_buffer();
    VERIFY(array_buffer);
    if (array_buffer->is_detached())
        return Value(0);
    return Value(typed_array->byte_offset());
}

// 23.2.3.32 get %TypedArray%.prototype [ @@toStringTag ], https://tc39.es/ecma262/#sec-get-%typedarray%.prototype-@@tostringtag
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::to_string_tag_getter)
{
    auto this_value = vm.this_value(global_object);
    if (!this_value.is_object())
        return js_undefined();
    auto& this_object = this_value.as_object();
    if (!this_object.is_typed_array())
        return js_undefined();
    return js_string(vm, static_cast<TypedArrayBase&>(this_object).element_name());
}

// 23.2.3.23 %TypedArray%.prototype.set ( source [ , offset ] ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.set
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::set)
{
    auto* typed_array = typed_array_from(vm, global_object);
    if (!typed_array)
        return {};

    auto source = vm.argument(0);

    auto target_offset = vm.argument(1).to_integer_or_infinity(global_object);
    if (vm.exception())
        return {};

    if (target_offset < 0) {
        vm.throw_exception<JS::RangeError>(global_object, "Invalid target offset");
        return {};
    }

    if (source.is_object() && is<TypedArrayBase>(source.as_object())) {
        auto& source_typed_array = static_cast<TypedArrayBase&>(source.as_object());
        // 23.2.3.23.1 SetTypedArrayFromTypedArray ( target, targetOffset, source ), https://tc39.es/ecma262/#sec-settypedarrayfromtypedarray
        auto target_buffer = typed_array->viewed_array_buffer();
        if (target_buffer->is_detached()) {
            vm.throw_exception<JS::TypeError>(global_object, ErrorType::DetachedArrayBuffer);
            return {};
        }
        auto target_length = typed_array->array_length();
        auto target_byte_offset = typed_array->byte_offset();

        auto source_buffer = source_typed_array.viewed_array_buffer();
        if (source_buffer->is_detached()) {
            vm.throw_exception<JS::TypeError>(global_object, ErrorType::DetachedArrayBuffer);
            return {};
        }
        auto source_length = source_typed_array.array_length();
        auto source_byte_offset = source_typed_array.byte_offset();

        if (isinf(target_offset)) {
            vm.throw_exception<JS::RangeError>(global_object, "Invalid target offset");
            return {};
        }

        Checked<size_t> checked = source_length;
        checked += static_cast<u32>(target_offset);
        if (checked.has_overflow() || checked.value() > target_length) {
            vm.throw_exception<JS::RangeError>(global_object, "Overflow or out of bounds in target length");
            return {};
        }

        if (typed_array->content_type() != source_typed_array.content_type()) {
            vm.throw_exception<JS::TypeError>(global_object, "Copy between arrays of different content types is prohibited");
            return {};
        }

        size_t source_byte_index;
        bool same = false;
        // FIXME: Step 19: If both IsSharedArrayBuffer(srcBuffer) and IsSharedArrayBuffer(targetBuffer) are true...
        same = same_value(source_buffer, target_buffer);
        if (same) {
            // FIXME: Implement this: Step 21
            TODO();
        } else {
            source_byte_index = source_byte_offset;
        }
        Checked<size_t> checked_target_byte_index(static_cast<size_t>(target_offset));
        checked_target_byte_index *= typed_array->element_size();
        checked_target_byte_index += target_byte_offset;
        if (checked_target_byte_index.has_overflow()) {
            vm.throw_exception<JS::RangeError>(global_object, "Overflow in target byte index");
            return {};
        }
        auto target_byte_index = checked_target_byte_index.value();

        Checked<size_t> checked_limit(source_length);
        checked_limit *= typed_array->element_size();
        checked_limit += target_byte_index;
        if (checked_limit.has_overflow()) {
            vm.throw_exception<JS::RangeError>(global_object, "Overflow in target limit");
            return {};
        }
        auto limit = checked_limit.value();

        if (source_typed_array.element_size() == typed_array->element_size()) {
            // FIXME: SharedBuffers use a different mechanism, implement that when SharedBuffers are implemented.
            target_buffer->buffer().overwrite(target_byte_index, source_buffer->buffer().data(), limit - target_byte_index);
        } else {
            while (target_byte_index < limit) {
                auto value = source_typed_array.get_value_from_buffer(source_byte_index, ArrayBuffer::Unordered);
                typed_array->set_value_in_buffer(target_byte_index, value, ArrayBuffer::Unordered);
                source_byte_index += source_typed_array.element_size();
                target_byte_index += typed_array->element_size();
            }
        }
    } else {
        // 23.2.3.23.2 SetTypedArrayFromArrayLike ( target, targetOffset, source ), https://tc39.es/ecma262/#sec-settypedarrayfromarraylike
        auto target_buffer = typed_array->viewed_array_buffer();
        if (target_buffer->is_detached()) {
            vm.throw_exception<JS::TypeError>(global_object, ErrorType::DetachedArrayBuffer);
            return {};
        }
        auto target_length = typed_array->array_length();
        auto target_byte_offset = typed_array->byte_offset();

        auto src = source.to_object(global_object);
        if (vm.exception())
            return {};

        auto source_length = length_of_array_like(global_object, *src);
        if (vm.exception())
            return {};

        if (isinf(target_offset)) {
            vm.throw_exception<JS::RangeError>(global_object, "Invalid target offset");
            return {};
        }

        Checked<size_t> checked = source_length;
        checked += static_cast<u32>(target_offset);
        if (checked.has_overflow() || checked.value() > target_length) {
            vm.throw_exception<JS::RangeError>(global_object, "Overflow or out of bounds in target length");
            return {};
        }

        Checked<size_t> checked_target_byte_index(static_cast<size_t>(target_offset));
        checked_target_byte_index *= typed_array->element_size();
        checked_target_byte_index += target_byte_offset;
        if (checked_target_byte_index.has_overflow()) {
            vm.throw_exception<JS::RangeError>(global_object, "Overflow in target byte index");
            return {};
        }
        auto target_byte_index = checked_target_byte_index.value();

        Checked<size_t> checked_limit(source_length);
        checked_limit *= typed_array->element_size();
        checked_limit += target_byte_index;
        if (checked_limit.has_overflow()) {
            vm.throw_exception<JS::RangeError>(global_object, "Overflow in target limit");
            return {};
        }

        auto limit = checked_limit.value();
        auto k = 0;
        while (target_byte_index < limit) {
            auto value = src->get(k);
            if (vm.exception())
                return {};
            if (typed_array->content_type() == TypedArrayBase::ContentType::BigInt)
                value = value.to_bigint(global_object);
            else
                value = value.to_number(global_object);
            if (vm.exception())
                return {};

            if (target_buffer->is_detached()) {
                vm.throw_exception<JS::TypeError>(global_object, ErrorType::DetachedArrayBuffer);
                return {};
            }

            typed_array->set_value_in_buffer(target_byte_index, value, ArrayBuffer::Unordered);
            ++k;
            target_byte_index += typed_array->element_size();
        }
    }
    return js_undefined();
}

// 23.2.3.22 %TypedArray%.prototype.reverse ( ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.reverse
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::reverse)
{
    // 1. Let O be the this value.
    // 2. Perform ? ValidateTypedArray(O).
    auto* typed_array = typed_array_from(vm, global_object);
    if (!typed_array)
        return {};

    // 3. Let len be O.[[ArrayLength]].
    auto length = typed_array->array_length();

    // 4. Let middle be floor(len / 2).
    auto middle = length / 2;

    // 5. Let lower be 0.
    // 6. Repeat, while lower ≠ middle,
    for (size_t lower = 0; lower != middle; ++lower) {
        // a. Let upper be len - lower - 1.
        auto upper = length - lower - 1;

        // b. Let upperP be ! ToString(𝔽(upper)).
        // d. Let lowerValue be ! Get(O, lowerP).
        auto lower_value = typed_array->get(lower);

        // c. Let lowerP be ! ToString(𝔽(lower)).
        // e. Let upperValue be ! Get(O, upperP).
        auto upper_value = typed_array->get(upper);

        // f. Perform ! Set(O, lowerP, upperValue, true).
        typed_array->set(lower, upper_value, true);

        // g. Perform ! Set(O, upperP, lowerValue, true).
        typed_array->set(upper, lower_value, true);

        // h. Set lower to lower + 1.
    }

    // 7. Return O.
    return typed_array;
}

}

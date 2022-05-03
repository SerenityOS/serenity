/*
 * Copyright (c) 2020-2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2021-2022, Idan Horowitz <idan.horowitz@serenityos.org>
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
    define_native_function(vm.names.fill, fill, 1, attr);
    define_native_function(vm.names.find, find, 1, attr);
    define_native_function(vm.names.findIndex, find_index, 1, attr);
    define_native_function(vm.names.findLast, find_last, 1, attr);
    define_native_function(vm.names.findLastIndex, find_last_index, 1, attr);
    define_native_function(vm.names.forEach, for_each, 1, attr);
    define_native_function(vm.names.includes, includes, 1, attr);
    define_native_function(vm.names.indexOf, index_of, 1, attr);
    define_native_function(vm.names.lastIndexOf, last_index_of, 1, attr);
    define_native_function(vm.names.reduce, reduce, 1, attr);
    define_native_function(vm.names.reduceRight, reduce_right, 1, attr);
    define_native_function(vm.names.some, some, 1, attr);
    define_native_function(vm.names.join, join, 1, attr);
    define_native_function(vm.names.keys, keys, 0, attr);
    define_native_function(vm.names.values, values, 0, attr);
    define_native_function(vm.names.entries, entries, 0, attr);
    define_native_function(vm.names.set, set, 1, attr);
    define_native_function(vm.names.slice, slice, 2, attr);
    define_native_function(vm.names.sort, sort, 1, attr);
    define_native_function(vm.names.subarray, subarray, 2, attr);
    define_native_function(vm.names.reverse, reverse, 0, attr);
    define_native_function(vm.names.copyWithin, copy_within, 2, attr);
    define_native_function(vm.names.filter, filter, 1, attr);
    define_native_function(vm.names.map, map, 1, attr);
    define_native_function(vm.names.toLocaleString, to_locale_string, 0, attr);

    define_native_accessor(*vm.well_known_symbol_to_string_tag(), to_string_tag_getter, nullptr, Attribute::Configurable);

    // 23.2.3.30 %TypedArray%.prototype.toString ( ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.tostring
    define_direct_property(vm.names.toString, global_object().array_prototype()->get_without_side_effects(vm.names.toString), attr);

    // 23.2.3.32 %TypedArray%.prototype [ @@iterator ] ( ), https://tc39.es/ecma262/#sec-%typedarray%.prototype-@@iterator
    define_direct_property(*vm.well_known_symbol_iterator(), get_without_side_effects(vm.names.values), attr);
}

static ThrowCompletionOr<TypedArrayBase*> typed_array_from_this(GlobalObject& global_object)
{
    auto this_value = global_object.vm().this_value(global_object);
    return typed_array_from(global_object, this_value);
}

static ThrowCompletionOr<TypedArrayBase*> validate_typed_array_from_this(GlobalObject& global_object)
{
    auto* typed_array = TRY(typed_array_from_this(global_object));

    TRY(validate_typed_array(global_object, *typed_array));

    return typed_array;
}

static ThrowCompletionOr<FunctionObject*> callback_from_args(GlobalObject& global_object, String const& name)
{
    auto& vm = global_object.vm();
    if (vm.argument_count() < 1)
        return vm.throw_completion<TypeError>(global_object, ErrorType::TypedArrayPrototypeOneArg, name);
    auto callback = vm.argument(0);
    if (!callback.is_function())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAFunction, callback.to_string_without_side_effects());
    return &callback.as_function();
}

static ThrowCompletionOr<void> for_each_item(VM& vm, GlobalObject& global_object, String const& name, Function<IterationDecision(size_t index, Value value, Value callback_result)> callback)
{
    auto* typed_array = TRY(validate_typed_array_from_this(global_object));

    auto initial_length = typed_array->array_length();

    auto* callback_function = TRY(callback_from_args(global_object, name));

    auto this_value = vm.argument(1);

    for (size_t i = 0; i < initial_length; ++i) {
        auto value = TRY(typed_array->get(i));

        auto callback_result = TRY(call(global_object, *callback_function, this_value, value, Value((i32)i), typed_array));

        if (callback(i, value, callback_result) == IterationDecision::Break)
            break;
    }

    return {};
}

static ThrowCompletionOr<void> for_each_item_from_last(VM& vm, GlobalObject& global_object, String const& name, Function<IterationDecision(size_t index, Value value, Value callback_result)> callback)
{
    auto* typed_array = TRY(validate_typed_array_from_this(global_object));

    auto initial_length = typed_array->array_length();

    auto* callback_function = TRY(callback_from_args(global_object, name));

    auto this_value = vm.argument(1);

    for (ssize_t i = (ssize_t)initial_length - 1; i >= 0; --i) {
        auto value = TRY(typed_array->get(i));

        auto callback_result = TRY(call(global_object, *callback_function, this_value, value, Value((i32)i), typed_array));

        if (callback(i, value, callback_result) == IterationDecision::Break)
            break;
    }

    return {};
}

// 23.2.4.1 TypedArraySpeciesCreate ( exemplar, argumentList ), https://tc39.es/ecma262/#typedarray-species-create
static ThrowCompletionOr<TypedArrayBase*> typed_array_species_create(GlobalObject& global_object, TypedArrayBase const& exemplar, MarkedVector<Value> arguments)
{
    auto& vm = global_object.vm();

    TypedArrayConstructor* typed_array_default_constructor = nullptr;

    // FIXME: This kinda sucks.
#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, Type) \
    if (is<ClassName>(exemplar))                                                    \
        typed_array_default_constructor = global_object.snake_name##_constructor();
    JS_ENUMERATE_TYPED_ARRAYS
#undef __JS_ENUMERATE

    VERIFY(typed_array_default_constructor);

    auto* constructor = TRY(species_constructor(global_object, exemplar, *typed_array_default_constructor));

    auto* result = TRY(typed_array_create(global_object, *constructor, move(arguments)));

    if (result->content_type() != exemplar.content_type())
        return vm.throw_completion<TypeError>(global_object, ErrorType::TypedArrayContentTypeMismatch, result->class_name(), exemplar.class_name());

    return result;
}

// 23.2.3.19 get %TypedArray%.prototype.length, https://tc39.es/ecma262/#sec-get-%typedarray%.prototype.length
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::length_getter)
{
    auto* typed_array = TRY(typed_array_from_this(global_object));
    auto* array_buffer = typed_array->viewed_array_buffer();
    VERIFY(array_buffer);
    if (array_buffer->is_detached())
        return Value(0);
    return Value(typed_array->array_length());
}

// 23.2.3.1 %TypedArray%.prototype.at ( index ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.at
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::at)
{
    auto* typed_array = TRY(validate_typed_array_from_this(global_object));
    auto length = typed_array->array_length();
    auto relative_index = TRY(vm.argument(0).to_integer_or_infinity(global_object));
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
    return TRY(typed_array->get(index.value()));
}

// 23.2.3.8 %TypedArray%.prototype.every ( callbackfn [ , thisArg ] ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.every
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::every)
{
    auto result = true;
    TRY(for_each_item(vm, global_object, "every", [&](auto, auto, auto callback_result) {
        if (!callback_result.to_boolean()) {
            result = false;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    }));
    return Value(result);
}

// 23.2.3.9 %TypedArray%.prototype.fill ( value [ , start [ , end ] ] ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.fill
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::fill)
{
    auto typed_array = TRY(validate_typed_array_from_this(global_object));

    auto length = typed_array->array_length();

    Value value;
    if (typed_array->content_type() == TypedArrayBase::ContentType::BigInt)
        value = TRY(vm.argument(0).to_bigint(global_object));
    else
        value = TRY(vm.argument(0).to_number(global_object));

    auto relative_start = TRY(vm.argument(1).to_integer_or_infinity(global_object));

    u32 k;
    if (Value(relative_start).is_negative_infinity())
        k = 0;
    else if (relative_start < 0)
        k = max(length + relative_start, 0);
    else
        k = min(relative_start, length);

    double relative_end;
    if (vm.argument(2).is_undefined())
        relative_end = length;
    else
        relative_end = TRY(vm.argument(2).to_integer_or_infinity(global_object));

    u32 final;
    if (Value(relative_end).is_negative_infinity())
        final = 0;
    else if (relative_end < 0)
        final = max(length + relative_end, 0);
    else
        final = min(relative_end, length);

    if (typed_array->viewed_array_buffer()->is_detached())
        return vm.throw_completion<TypeError>(global_object, ErrorType::DetachedArrayBuffer);

    for (; k < final; ++k)
        TRY(typed_array->set(k, value, Object::ShouldThrowExceptions::Yes));

    return typed_array;
}

// 23.2.3.11 %TypedArray%.prototype.find ( predicate [ , thisArg ] ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.find
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::find)
{
    auto result = js_undefined();
    TRY(for_each_item(vm, global_object, "find", [&](auto, auto value, auto callback_result) {
        if (callback_result.to_boolean()) {
            result = value;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    }));
    return result;
}

// 23.2.3.12 %TypedArray%.prototype.findIndex ( predicate [ , thisArg ] ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.findindex
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::find_index)
{
    auto result_index = -1;
    TRY(for_each_item(vm, global_object, "findIndex", [&](auto index, auto, auto callback_result) {
        if (callback_result.to_boolean()) {
            result_index = index;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    }));
    return Value(result_index);
}

// 4 %TypedArray%.prototype.findLast ( predicate [ , thisArg ] ), https://tc39.es/proposal-array-find-from-last/#sec-%typedarray%.prototype.findlast
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::find_last)
{
    auto result = js_undefined();
    TRY(for_each_item_from_last(vm, global_object, "findLast", [&](auto, auto value, auto callback_result) {
        if (callback_result.to_boolean()) {
            result = value;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    }));
    return result;
}

// 5 %TypedArray%.prototype.findLastIndex ( predicate [ , thisArg ] ), https://tc39.es/proposal-array-find-from-last/#sec-%typedarray%.prototype.findlastindex
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::find_last_index)
{
    auto result_index = -1;
    TRY(for_each_item_from_last(vm, global_object, "findLastIndex", [&](auto index, auto, auto callback_result) {
        if (callback_result.to_boolean()) {
            result_index = index;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    }));
    return Value(result_index);
}

// 23.2.3.13 %TypedArray%.prototype.forEach ( callbackfn [ , thisArg ] ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.foreach
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::for_each)
{
    TRY(for_each_item(vm, global_object, "forEach", [](auto, auto, auto) {
        return IterationDecision::Continue;
    }));
    return js_undefined();
}

// 23.2.3.14 %TypedArray%.prototype.includes ( searchElement [ , fromIndex ] ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.includes
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::includes)
{
    auto typed_array = TRY(validate_typed_array_from_this(global_object));

    auto length = typed_array->array_length();

    if (length == 0)
        return Value(false);

    auto n = TRY(vm.argument(1).to_integer_or_infinity(global_object));

    auto value_n = Value(n);
    if (value_n.is_positive_infinity())
        return Value(false);
    else if (value_n.is_negative_infinity())
        n = 0;

    u32 k;
    if (n >= 0) {
        k = n;
    } else {
        auto relative_k = length + n;
        if (relative_k < 0)
            relative_k = 0;
        k = relative_k;
    }

    auto search_element = vm.argument(0);
    for (; k < length; ++k) {
        auto element_k = MUST(typed_array->get(k));

        if (same_value_zero(search_element, element_k))
            return Value(true);
    }

    return Value(false);
}

// 23.2.3.15 %TypedArray%.prototype.indexOf ( searchElement [ , fromIndex ] ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.indexof
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::index_of)
{
    auto typed_array = TRY(validate_typed_array_from_this(global_object));

    auto length = typed_array->array_length();

    if (length == 0)
        return Value(-1);

    auto n = TRY(vm.argument(1).to_integer_or_infinity(global_object));

    auto value_n = Value(n);
    if (value_n.is_positive_infinity())
        return Value(-1);
    else if (value_n.is_negative_infinity())
        n = 0;

    u32 k;
    if (n >= 0) {
        k = n;
    } else {
        auto relative_k = length + n;
        if (relative_k < 0)
            relative_k = 0;
        k = relative_k;
    }

    auto search_element = vm.argument(0);
    for (; k < length; ++k) {
        auto k_present = MUST(typed_array->has_property(k));
        if (k_present) {
            auto element_k = MUST(typed_array->get(k));

            if (is_strictly_equal(search_element, element_k))
                return Value(k);
        }
    }

    return Value(-1);
}

// 23.2.3.18 %TypedArray%.prototype.lastIndexOf ( searchElement [ , fromIndex ] ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.lastindexof
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::last_index_of)
{
    auto typed_array = TRY(validate_typed_array_from_this(global_object));

    auto length = typed_array->array_length();

    if (length == 0)
        return Value(-1);

    double n;
    if (vm.argument_count() > 1)
        n = TRY(vm.argument(1).to_integer_or_infinity(global_object));
    else
        n = length - 1;

    if (Value(n).is_negative_infinity())
        return Value(-1);

    i32 k;
    if (n >= 0) {
        k = min(n, (i32)length - 1);
    } else {
        auto relative_k = length + n;
        if (relative_k < 0) // ensures we dont underflow `k`
            relative_k = -1;
        k = relative_k;
    }

    auto search_element = vm.argument(0);
    for (; k >= 0; --k) {
        auto k_present = MUST(typed_array->has_property(k));
        if (k_present) {
            auto element_k = MUST(typed_array->get(k));

            if (is_strictly_equal(search_element, element_k))
                return Value(k);
        }
    }

    return Value(-1);
}

// 23.2.3.21 %TypedArray%.prototype.reduce ( callbackfn [ , initialValue ] ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.reduce
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::reduce)
{
    auto* typed_array = TRY(validate_typed_array_from_this(global_object));

    auto length = typed_array->array_length();

    auto* callback_function = TRY(callback_from_args(global_object, vm.names.reduce.as_string()));

    if (length == 0 && vm.argument_count() <= 1)
        return vm.throw_completion<TypeError>(global_object, ErrorType::ReduceNoInitial);

    u32 k = 0;
    Value accumulator;
    if (vm.argument_count() > 1) {
        accumulator = vm.argument(1);
    } else {
        accumulator = MUST(typed_array->get(k));
        ++k;
    }

    for (; k < length; ++k) {
        auto k_value = MUST(typed_array->get(k));

        accumulator = TRY(call(global_object, *callback_function, js_undefined(), accumulator, k_value, Value(k), typed_array));
    }

    return accumulator;
}

// 23.2.3.22 %TypedArray%.prototype.reduceRight ( callbackfn [ , initialValue ] ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.reduce
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::reduce_right)
{
    auto* typed_array = TRY(validate_typed_array_from_this(global_object));

    auto length = typed_array->array_length();

    auto* callback_function = TRY(callback_from_args(global_object, vm.names.reduce.as_string()));

    if (length == 0 && vm.argument_count() <= 1)
        return vm.throw_completion<TypeError>(global_object, ErrorType::ReduceNoInitial);

    i32 k = (i32)length - 1;
    Value accumulator;
    if (vm.argument_count() > 1) {
        accumulator = vm.argument(1);
    } else {
        accumulator = MUST(typed_array->get(k));
        --k;
    }

    for (; k >= 0; --k) {
        auto k_value = MUST(typed_array->get(k));

        accumulator = TRY(call(global_object, *callback_function, js_undefined(), accumulator, k_value, Value(k), typed_array));
    }

    return accumulator;
}

// 23.2.3.26 %TypedArray%.prototype.some ( callbackfn [ , thisArg ] ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.some
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::some)
{
    auto result = false;
    TRY(for_each_item(vm, global_object, "some", [&](auto, auto, auto callback_result) {
        if (callback_result.to_boolean()) {
            result = true;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    }));
    return Value(result);
}

// 23.2.3.16 %TypedArray%.prototype.join ( separator ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.join
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::join)
{
    auto* typed_array = TRY(validate_typed_array_from_this(global_object));
    auto length = typed_array->array_length();
    String separator = ",";
    if (!vm.argument(0).is_undefined())
        separator = TRY(vm.argument(0).to_string(global_object));

    StringBuilder builder;
    for (size_t i = 0; i < length; ++i) {
        if (i > 0)
            builder.append(separator);
        auto value = TRY(typed_array->get(i));
        if (value.is_nullish())
            continue;
        auto string = TRY(value.to_string(global_object));
        builder.append(string);
    }

    return js_string(vm, builder.to_string());
}

// 23.2.3.17 %TypedArray%.prototype.keys ( ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.keys
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::keys)
{
    auto* typed_array = TRY(validate_typed_array_from_this(global_object));
    return ArrayIterator::create(global_object, typed_array, Object::PropertyKind::Key);
}

// 23.2.3.31 %TypedArray%.prototype.values ( ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.values
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::values)
{
    auto* typed_array = TRY(typed_array_from_this(global_object));
    return ArrayIterator::create(global_object, typed_array, Object::PropertyKind::Value);
}

// 23.2.3.7 %TypedArray%.prototype.entries ( ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.entries
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::entries)
{
    auto* typed_array = TRY(validate_typed_array_from_this(global_object));
    return ArrayIterator::create(global_object, typed_array, Object::PropertyKind::KeyAndValue);
}

// 23.2.3.2 get %TypedArray%.prototype.buffer, https://tc39.es/ecma262/#sec-get-%typedarray%.prototype.buffer
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::buffer_getter)
{
    auto* typed_array = TRY(typed_array_from_this(global_object));
    auto* array_buffer = typed_array->viewed_array_buffer();
    VERIFY(array_buffer);
    return Value(array_buffer);
}

// 23.2.3.3 get %TypedArray%.prototype.byteLength, https://tc39.es/ecma262/#sec-get-%typedarray%.prototype.bytelength
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::byte_length_getter)
{
    auto* typed_array = TRY(typed_array_from_this(global_object));
    auto* array_buffer = typed_array->viewed_array_buffer();
    VERIFY(array_buffer);
    if (array_buffer->is_detached())
        return Value(0);
    return Value(typed_array->byte_length());
}

// 23.2.3.4 get %TypedArray%.prototype.byteOffset, https://tc39.es/ecma262/#sec-get-%typedarray%.prototype.byteoffset
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::byte_offset_getter)
{
    auto* typed_array = TRY(typed_array_from_this(global_object));
    auto* array_buffer = typed_array->viewed_array_buffer();
    VERIFY(array_buffer);
    if (array_buffer->is_detached())
        return Value(0);
    return Value(typed_array->byte_offset());
}

// 23.2.3.33 get %TypedArray%.prototype [ @@toStringTag ], https://tc39.es/ecma262/#sec-get-%typedarray%.prototype-@@tostringtag
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

// 23.2.3.23.1 SetTypedArrayFromTypedArray ( target, targetOffset, source ), https://tc39.es/ecma262/#sec-settypedarrayfromtypedarray
static ThrowCompletionOr<void> set_typed_array_from_typed_array(GlobalObject& global_object, TypedArrayBase& target, double target_offset, TypedArrayBase& source)
{
    auto& vm = global_object.vm();

    // 1. Let targetBuffer be target.[[ViewedArrayBuffer]].
    auto* target_buffer = target.viewed_array_buffer();

    // 2. If IsDetachedBuffer(targetBuffer) is true, throw a TypeError exception.
    if (target_buffer->is_detached())
        return vm.throw_completion<TypeError>(global_object, ErrorType::DetachedArrayBuffer);

    // 3. Let targetLength be target.[[ArrayLength]].
    auto target_length = target.array_length();

    // 4. Let srcBuffer be source.[[ViewedArrayBuffer]].
    auto* source_buffer = source.viewed_array_buffer();

    // 5. If IsDetachedBuffer(srcBuffer) is true, throw a TypeError exception.
    if (source_buffer->is_detached())
        return vm.throw_completion<TypeError>(global_object, ErrorType::DetachedArrayBuffer);

    // 6. Let targetType be TypedArrayElementType(target).
    // 7. Let targetElementSize be TypedArrayElementSize(target).
    auto target_element_size = target.element_size();

    // 8. Let targetByteOffset be target.[[ByteOffset]].
    auto target_byte_offset = target.byte_offset();

    // 9. Let srcType be TypedArrayElementType(source).
    // 10. Let srcElementSize be TypedArrayElementSize(source).
    auto source_element_size = source.element_size();

    // 11. Let srcLength be source.[[ArrayLength]].
    auto source_length = source.array_length();

    // 12. Let srcByteOffset be source.[[ByteOffset]].
    auto source_byte_offset = source.byte_offset();

    // 13. If targetOffset is +∞, throw a RangeError exception.
    if (isinf(target_offset))
        return vm.throw_completion<RangeError>(global_object, ErrorType::TypedArrayInvalidTargetOffset, "finite");

    // 14. If srcLength + targetOffset > targetLength, throw a RangeError exception.
    Checked<size_t> checked = source_length;
    checked += static_cast<u32>(target_offset);
    if (checked.has_overflow() || checked.value() > target_length)
        return vm.throw_completion<RangeError>(global_object, ErrorType::TypedArrayOverflowOrOutOfBounds, "target length");

    // 15. If target.[[ContentType]] ≠ source.[[ContentType]], throw a TypeError exception.
    if (target.content_type() != source.content_type())
        return vm.throw_completion<TypeError>(global_object, ErrorType::TypedArrayInvalidCopy, target.class_name(), source.class_name());

    // FIXME: 16. If both IsSharedArrayBuffer(srcBuffer) and IsSharedArrayBuffer(targetBuffer) are true, then
    // FIXME: a. If srcBuffer.[[ArrayBufferData]] and targetBuffer.[[ArrayBufferData]] are the same Shared Data Block values, let same be true; else let same be false.

    // 17. Else, let same be SameValue(srcBuffer, targetBuffer).
    auto same = same_value(source_buffer, target_buffer);

    size_t source_byte_index = 0;

    // 18. If same is true, then
    if (same) {
        // a. Let srcByteLength be source.[[ByteLength]].
        auto source_byte_length = source.byte_length();

        // b. Set srcBuffer to ? CloneArrayBuffer(srcBuffer, srcByteOffset, srcByteLength).
        source_buffer = TRY(clone_array_buffer(global_object, *source_buffer, source_byte_offset, source_byte_length));

        // c. Let srcByteIndex be 0.
        source_byte_index = 0;
    }
    // 19. Else, let srcByteIndex be srcByteOffset.
    else {
        source_byte_index = source_byte_offset;
    }

    // 20. Let targetByteIndex be targetOffset × targetElementSize + targetByteOffset.
    Checked<size_t> checked_target_byte_index(static_cast<size_t>(target_offset));
    checked_target_byte_index *= target_element_size;
    checked_target_byte_index += target_byte_offset;
    if (checked_target_byte_index.has_overflow())
        return vm.throw_completion<RangeError>(global_object, ErrorType::TypedArrayOverflow, "target byte index");
    auto target_byte_index = checked_target_byte_index.value();

    // 21. Let limit be targetByteIndex + targetElementSize × srcLength.
    Checked<size_t> checked_limit(source_length);
    checked_limit *= target_element_size;
    checked_limit += target_byte_index;
    if (checked_limit.has_overflow())
        return vm.throw_completion<RangeError>(global_object, ErrorType::TypedArrayOverflow, "target limit");
    auto limit = checked_limit.value();

    // 22. If srcType is the same as targetType, then
    if (source.element_name() == target.element_name()) {
        // a. NOTE: If srcType and targetType are the same, the transfer must be performed in a manner that preserves the bit-level encoding of the source data.
        // b. Repeat, while targetByteIndex < limit,
        //     i. Let value be GetValueFromBuffer(srcBuffer, srcByteIndex, Uint8, true, Unordered).
        //     ii. Perform SetValueInBuffer(targetBuffer, targetByteIndex, Uint8, value, true, Unordered).
        //     iii. Set srcByteIndex to srcByteIndex + 1.
        //     iv. Set targetByteIndex to targetByteIndex + 1.
        target_buffer->buffer().overwrite(target_byte_index, source_buffer->buffer().data() + source_byte_index, limit - target_byte_index);
    }
    // 23. Else,
    else {
        // a. Repeat, while targetByteIndex < limit,
        while (target_byte_index < limit) {
            // i. Let value be GetValueFromBuffer(srcBuffer, srcByteIndex, srcType, true, Unordered).
            auto value = source.get_value_from_buffer(source_byte_index, ArrayBuffer::Unordered);
            // ii. Perform SetValueInBuffer(targetBuffer, targetByteIndex, targetType, value, true, Unordered).
            target.set_value_in_buffer(target_byte_index, value, ArrayBuffer::Unordered);
            // iii. Set srcByteIndex to srcByteIndex + srcElementSize.
            source_byte_index += source_element_size;
            // iv. Set targetByteIndex to targetByteIndex + targetElementSize.
            target_byte_index += target.element_size();
        }
    }

    // 24. Return unused.
    return {};
}

// 23.2.3.23.2 SetTypedArrayFromArrayLike ( target, targetOffset, source ), https://tc39.es/ecma262/#sec-settypedarrayfromarraylike
static ThrowCompletionOr<void> set_typed_array_from_array_like(GlobalObject& global_object, TypedArrayBase& target, double target_offset, Value source)
{
    auto& vm = global_object.vm();

    // 1. Let targetBuffer be target.[[ViewedArrayBuffer]].
    auto* target_buffer = target.viewed_array_buffer();

    // 2. If IsDetachedBuffer(targetBuffer) is true, throw a TypeError exception.
    if (target_buffer->is_detached())
        return vm.throw_completion<TypeError>(global_object, ErrorType::DetachedArrayBuffer);

    // 3. Let targetLength be target.[[ArrayLength]].
    auto target_length = target.array_length();

    // 4. Let src be ? ToObject(source).
    auto* src = TRY(source.to_object(global_object));

    // 5. Let srcLength be ? LengthOfArrayLike(src).
    auto source_length = TRY(length_of_array_like(global_object, *src));

    // 6. If targetOffset is +∞, throw a RangeError exception.
    if (isinf(target_offset))
        return vm.throw_completion<RangeError>(global_object, ErrorType::TypedArrayInvalidTargetOffset, "finite");

    // 7. If srcLength + targetOffset > targetLength, throw a RangeError exception.
    Checked<size_t> checked = source_length;
    checked += static_cast<u32>(target_offset);
    if (checked.has_overflow() || checked.value() > target_length)
        return vm.throw_completion<RangeError>(global_object, ErrorType::TypedArrayOverflowOrOutOfBounds, "target length");

    // 8. Let k be 0.
    size_t k = 0;

    // 9. Repeat, while k < srcLength,
    while (k < source_length) {
        // a. Let Pk be ! ToString(𝔽(k)).
        // b. Let value be ? Get(src, Pk).
        auto value = TRY(src->get(k));

        // c. Let targetIndex be 𝔽(targetOffset + k).
        CanonicalIndex target_index(CanonicalIndex::Type::Index, target_offset + k);

        // d. Perform ? IntegerIndexedElementSet(target, targetIndex, value).
        // FIXME: This is very awkward.
#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, Type) \
    if (is<ClassName>(target))                                                      \
        TRY(integer_indexed_element_set<Type>(target, target_index, value));
        JS_ENUMERATE_TYPED_ARRAYS
#undef __JS_ENUMERATE

        // e. Set k to k + 1.
        ++k;
    }

    // 10. Return unused.
    return {};
}

// 23.2.3.24 %TypedArray%.prototype.set ( source [ , offset ] ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.set
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::set)
{
    auto source = vm.argument(0);

    // 1. Let target be the this value.
    // 2. Perform ? RequireInternalSlot(target, [[TypedArrayName]]).
    auto* typed_array = TRY(typed_array_from_this(global_object));

    // 3. Assert: target has a [[ViewedArrayBuffer]] internal slot.

    // 4. Let targetOffset be ? ToIntegerOrInfinity(offset).
    auto target_offset = TRY(vm.argument(1).to_integer_or_infinity(global_object));

    // 5. If targetOffset < 0, throw a RangeError exception.
    if (target_offset < 0)
        return vm.throw_completion<RangeError>(global_object, ErrorType::TypedArrayInvalidTargetOffset, "positive");

    // 6. If source is an Object that has a [[TypedArrayName]] internal slot, then
    if (source.is_object() && is<TypedArrayBase>(source.as_object())) {
        auto& source_typed_array = static_cast<TypedArrayBase&>(source.as_object());

        // a. Perform ? SetTypedArrayFromTypedArray(target, targetOffset, source).
        TRY(set_typed_array_from_typed_array(global_object, *typed_array, target_offset, source_typed_array));
    }
    // 7. Else,
    else {
        // a. Perform ? SetTypedArrayFromArrayLike(target, targetOffset, source).
        TRY(set_typed_array_from_array_like(global_object, *typed_array, target_offset, source));
    }

    // 8. Return undefined.
    return js_undefined();
}

// 23.2.3.25 %TypedArray%.prototype.slice ( start, end ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.slice
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::slice)
{
    auto start = vm.argument(0);
    auto end = vm.argument(1);

    // 1. Let O be the this value.
    // 2. Perform ? ValidateTypedArray(O).
    auto* typed_array = TRY(validate_typed_array_from_this(global_object));

    // 3. Let len be O.[[ArrayLength]].
    auto length = typed_array->array_length();

    // 4. Let relativeStart be ? ToIntegerOrInfinity(start).
    auto relative_start = TRY(start.to_integer_or_infinity(global_object));

    i32 k = 0;

    // 5. If relativeStart is -∞, let k be 0.
    if (Value(relative_start).is_negative_infinity())
        k = 0;
    // 6. Else if relativeStart < 0, let k be max(len + relativeStart, 0).
    else if (relative_start < 0)
        k = max(length + relative_start, 0);
    // 7. Else, let k be min(relativeStart, len).
    else
        k = min(relative_start, length);

    double relative_end = 0;

    // 8. If end is undefined, let relativeEnd be len; else let relativeEnd be ? ToIntegerOrInfinity(end).
    if (end.is_undefined())
        relative_end = length;
    else
        relative_end = TRY(end.to_integer_or_infinity(global_object));

    i32 final = 0;

    // 9. If relativeEnd is -∞, let final be 0.
    if (Value(relative_end).is_negative_infinity())
        final = 0;
    // 10. Else if relativeEnd < 0, let final be max(len + relativeEnd, 0).
    else if (relative_end < 0)
        final = max(length + relative_end, 0);
    // 11. Else, let final be min(relativeEnd, len).
    else
        final = min(relative_end, length);

    // 12. Let count be max(final - k, 0).
    auto count = max(final - k, 0);

    // 13. Let A be ? TypedArraySpeciesCreate(O, « 𝔽(count) »).
    MarkedVector<Value> arguments(vm.heap());
    arguments.empend(count);
    auto* new_array = TRY(typed_array_species_create(global_object, *typed_array, move(arguments)));

    // 14. If count > 0, then
    if (count > 0) {
        // a. If IsDetachedBuffer(O.[[ViewedArrayBuffer]]) is true, throw a TypeError exception.
        if (typed_array->viewed_array_buffer()->is_detached())
            return vm.throw_completion<TypeError>(global_object, ErrorType::DetachedArrayBuffer);

        // b. Let srcType be TypedArrayElementType(O).
        // c. Let targetType be TypedArrayElementType(A).

        // d. If srcType is different from targetType, then
        if (typed_array->element_name() != new_array->element_name()) {
            // i. Let n be 0.
            // ii. Repeat, while k < final,
            for (i32 n = 0; k < final; ++k, ++n) {
                // 1. Let Pk be ! ToString(𝔽(k)).
                // 2. Let kValue be ! Get(O, Pk).
                auto k_value = MUST(typed_array->get(k));

                // 3. Perform ! Set(A, ! ToString(𝔽(n)), kValue, true).
                MUST(new_array->set(n, k_value, Object::ShouldThrowExceptions::Yes));

                // 4. Set k to k + 1.
                // 5. Set n to n + 1.
            }
        }
        // e. Else,
        else {
            // i. Let srcBuffer be O.[[ViewedArrayBuffer]].
            auto& source_buffer = *typed_array->viewed_array_buffer();

            // ii. Let targetBuffer be A.[[ViewedArrayBuffer]].
            auto& target_buffer = *new_array->viewed_array_buffer();

            // iii. Let elementSize be TypedArrayElementSize(O).
            auto element_size = typed_array->element_size();

            // iv. NOTE: If srcType and targetType are the same, the transfer must be performed in a manner that preserves the bit-level encoding of the source data.

            // v. Let srcByteOffset be O.[[ByteOffset]].
            auto source_byte_offset = typed_array->byte_offset();

            // vi. Let targetByteIndex be A.[[ByteOffset]].
            auto target_byte_index = new_array->byte_offset();

            // vii. Let srcByteIndex be (k × elementSize) + srcByteOffset.
            Checked<u32> source_byte_index = k;
            source_byte_index *= element_size;
            source_byte_index += source_byte_offset;
            if (source_byte_index.has_overflow()) {
                dbgln("TypedArrayPrototype::slice: source_byte_index overflowed, returning as if succeeded.");
                return new_array;
            }

            // viii. Let limit be targetByteIndex + count × elementSize.
            Checked<u32> limit = count;
            limit *= element_size;
            limit += target_byte_index;
            if (limit.has_overflow()) {
                dbgln("TypedArrayPrototype::slice: limit overflowed, returning as if succeeded.");
                return new_array;
            }

            // ix. Repeat, while targetByteIndex < limit,
            for (; target_byte_index < limit.value(); ++source_byte_index, ++target_byte_index) {
                // 1. Let value be GetValueFromBuffer(srcBuffer, srcByteIndex, Uint8, true, Unordered).
                auto value = source_buffer.get_value<u8>(source_byte_index.value(), true, ArrayBuffer::Unordered);

                // 2. Perform SetValueInBuffer(targetBuffer, targetByteIndex, Uint8, value, true, Unordered).
                target_buffer.set_value<u8>(target_byte_index, value, true, ArrayBuffer::Unordered);

                // 3. Set srcByteIndex to srcByteIndex + 1.
                // 4. Set targetByteIndex to targetByteIndex + 1.
            }
        }
    }

    // 15. Return A.
    return new_array;
}

static ThrowCompletionOr<void> typed_array_merge_sort(GlobalObject& global_object, FunctionObject* compare_function, ArrayBuffer& buffer, MarkedVector<Value>& arr_to_sort)
{
    auto& vm = global_object.vm();
    if (arr_to_sort.size() <= 1)
        return {};

    MarkedVector<Value> left(vm.heap());
    MarkedVector<Value> right(vm.heap());

    left.ensure_capacity(arr_to_sort.size() / 2);
    right.ensure_capacity(arr_to_sort.size() / 2 + (arr_to_sort.size() & 1));

    for (size_t i = 0; i < arr_to_sort.size(); ++i) {
        if (i < arr_to_sort.size() / 2) {
            left.append(arr_to_sort[i]);
        } else {
            right.append(arr_to_sort[i]);
        }
    }

    TRY(typed_array_merge_sort(global_object, compare_function, buffer, left));
    TRY(typed_array_merge_sort(global_object, compare_function, buffer, right));

    arr_to_sort.clear();

    size_t left_index = 0, right_index = 0;

    while (left_index < left.size() && right_index < right.size()) {
        auto x = left[left_index];
        auto y = right[right_index];

        bool number_comparison = x.is_number();
        double comparison_result;

        if (compare_function) {
            auto result = TRY(call(global_object, *compare_function, js_undefined(), x, y));

            auto value = TRY(result.to_number(global_object));

            if (value.is_nan())
                comparison_result = 0;
            else
                comparison_result = value.as_double();
        } else if (x.is_nan() && y.is_nan()) {
            comparison_result = 0;
        } else if (x.is_nan()) {
            comparison_result = 1;
        } else if (y.is_nan()) {
            comparison_result = -1;
        } else if (number_comparison ? (x.as_double() < y.as_double()) : (x.as_bigint().big_integer() < y.as_bigint().big_integer())) {
            comparison_result = -1;
        } else if (number_comparison ? (x.as_double() > y.as_double()) : (x.as_bigint().big_integer() > y.as_bigint().big_integer())) {
            comparison_result = 1;
        } else if (x.is_negative_zero() && y.is_positive_zero()) {
            comparison_result = -1;
        } else if (x.is_positive_zero() && y.is_negative_zero()) {
            comparison_result = 1;
        } else {
            comparison_result = 0;
        }

        if (comparison_result <= 0) {
            arr_to_sort.append(left[left_index]);
            left_index++;
        } else {
            arr_to_sort.append(right[right_index]);
            right_index++;
        }
    }

    while (left_index < left.size()) {
        arr_to_sort.append(left[left_index]);
        left_index++;
    }

    while (right_index < right.size()) {
        arr_to_sort.append(right[right_index]);
        right_index++;
    }

    return {};
}

// 23.2.3.27 %TypedArray%.prototype.sort ( comparefn ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.sort
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::sort)
{
    auto compare_fn = vm.argument(0);
    if (!compare_fn.is_undefined() && !compare_fn.is_function())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAFunction, compare_fn.to_string_without_side_effects());

    auto* typed_array = TRY(validate_typed_array_from_this(global_object));

    auto length = typed_array->array_length();

    MarkedVector<Value> items(vm.heap());
    for (u32 k = 0; k < length; ++k) {
        auto k_present = TRY(typed_array->has_property(k));

        if (k_present) {
            auto k_value = TRY(typed_array->get(k));
            items.append(k_value);
        }
    }

    TRY(typed_array_merge_sort(global_object, compare_fn.is_undefined() ? nullptr : &compare_fn.as_function(), *typed_array->viewed_array_buffer(), items));

    u32 j;
    for (j = 0; j < items.size(); ++j)
        TRY(typed_array->set(j, items[j], Object::ShouldThrowExceptions::Yes));

    for (; j < length; ++j)
        TRY(typed_array->delete_property_or_throw(j));

    return typed_array;
}

// 23.2.3.28 %TypedArray%.prototype.subarray ( begin, end ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.subarray
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::subarray)
{
    auto begin = vm.argument(0);
    auto end = vm.argument(1);

    // 1. Let O be the this value.
    // 2. Perform ? RequireInternalSlot(O, [[TypedArrayName]]).
    auto* typed_array = TRY(typed_array_from_this(global_object));

    // 3. Assert: O has a [[ViewedArrayBuffer]] internal slot.
    // 4. Let buffer be O.[[ViewedArrayBuffer]].
    auto* buffer = typed_array->viewed_array_buffer();

    // 5. Let srcLength be O.[[ArrayLength]].
    auto source_length = typed_array->array_length();

    // 6. Let relativeBegin be ? ToIntegerOrInfinity(begin).
    auto relative_begin = TRY(begin.to_integer_or_infinity(global_object));

    i32 begin_index = 0;

    // 7. If relativeBegin is -∞, let beginIndex be 0.
    if (Value(relative_begin).is_negative_infinity())
        begin_index = 0;
    // 8. Else if relativeBegin < 0, let beginIndex be max(srcLength + relativeBegin, 0).
    else if (relative_begin < 0)
        begin_index = max(source_length + relative_begin, 0);
    // 9. Else, let beginIndex be min(relativeBegin, srcLength).
    else
        begin_index = min(relative_begin, source_length);

    double relative_end = 0;

    // 10. If end is undefined, let relativeEnd be srcLength; else let relativeEnd be ? ToIntegerOrInfinity(end).
    if (end.is_undefined())
        relative_end = source_length;
    else
        relative_end = TRY(end.to_integer_or_infinity(global_object));

    i32 end_index = 0;

    // 11. If relativeEnd is -∞, let endIndex be 0.
    if (Value(relative_end).is_negative_infinity())
        end_index = 0;
    // 12. Else if relativeEnd < 0, let endIndex be max(srcLength + relativeEnd, 0).
    else if (relative_end < 0)
        end_index = max(source_length + relative_end, 0);
    // 13. Else, let endIndex be min(relativeEnd, srcLength).
    else
        end_index = min(relative_end, source_length);

    // 14. Let newLength be max(endIndex - beginIndex, 0).
    auto new_length = max(end_index - begin_index, 0);

    // 15. Let elementSize be TypedArrayElementSize(O).
    // 16. Let srcByteOffset be O.[[ByteOffset]].
    // 17. Let beginByteOffset be srcByteOffset + beginIndex × elementSize.
    Checked<u32> begin_byte_offset = begin_index;
    begin_byte_offset *= typed_array->element_size();
    begin_byte_offset += typed_array->byte_offset();
    if (begin_byte_offset.has_overflow()) {
        dbgln("TypedArrayPrototype::begin_byte_offset: limit overflowed, returning as if succeeded.");
        return typed_array;
    }

    // 18. Let argumentsList be « buffer, 𝔽(beginByteOffset), 𝔽(newLength) ».
    MarkedVector<Value> arguments(vm.heap());
    arguments.empend(buffer);
    arguments.empend(begin_byte_offset.value());
    arguments.empend(new_length);

    // 19. Return ? TypedArraySpeciesCreate(O, argumentsList).
    return TRY(typed_array_species_create(global_object, *typed_array, move(arguments)));
}

// 23.2.3.23 %TypedArray%.prototype.reverse ( ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.reverse
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::reverse)
{
    // 1. Let O be the this value.
    // 2. Perform ? ValidateTypedArray(O).
    auto* typed_array = TRY(validate_typed_array_from_this(global_object));

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
        auto lower_value = MUST(typed_array->get(lower));

        // c. Let lowerP be ! ToString(𝔽(lower)).
        // e. Let upperValue be ! Get(O, upperP).
        auto upper_value = MUST(typed_array->get(upper));

        // f. Perform ! Set(O, lowerP, upperValue, true).
        MUST(typed_array->set(lower, upper_value, Object::ShouldThrowExceptions::Yes));

        // g. Perform ! Set(O, upperP, lowerValue, true).
        MUST(typed_array->set(upper, lower_value, Object::ShouldThrowExceptions::Yes));

        // h. Set lower to lower + 1.
    }

    // 7. Return O.
    return typed_array;
}

// 23.2.3.6 %TypedArray%.prototype.copyWithin ( target, start [ , end ] ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.copywithin
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::copy_within)
{
    // 1. Let O be the this value.
    // 2. Perform ? ValidateTypedArray(O).
    auto* typed_array = TRY(validate_typed_array_from_this(global_object));

    // 3. Let len be O.[[ArrayLength]].
    auto length = typed_array->array_length();

    // 4. Let relativeTarget be ? ToIntegerOrInfinity(target).
    auto relative_target = TRY(vm.argument(0).to_integer_or_infinity(global_object));

    double to;
    if (Value(relative_target).is_negative_infinity()) {
        // 5. If relativeTarget is -∞, let to be 0.
        to = 0.0;
    } else if (relative_target < 0) {
        // 6. Else if relativeTarget < 0, let to be max(len + relativeTarget, 0)
        to = max(length + relative_target, 0.0);
    } else {
        // 7. Else, let to be min(relativeTarget, len).
        to = min(relative_target, (double)length);
    }

    // 8. Let relativeStart be ? ToIntegerOrInfinity(start).
    auto relative_start = TRY(vm.argument(1).to_integer_or_infinity(global_object));

    double from;
    if (Value(relative_start).is_negative_infinity()) {
        // 9. If relativeStart is -∞, let from be 0.
        from = 0.0;
    } else if (relative_start < 0) {
        // 10. Else if relativeStart < 0, let from be max(len + relativeStart, 0).
        from = max(length + relative_start, 0.0);
    } else {
        // 11. Else, let from be min(relativeStart, len).
        from = min(relative_start, (double)length);
    }

    double relative_end;

    // 12. If end is undefined, let relativeEnd be len; else let relativeEnd be ? ToIntegerOrInfinity(end).
    if (vm.argument(2).is_undefined())
        relative_end = length;
    else
        relative_end = TRY(vm.argument(2).to_integer_or_infinity(global_object));

    double final;
    if (Value(relative_end).is_negative_infinity()) {
        // 13. If relativeEnd is -∞, let final be 0.
        final = 0.0;
    } else if (relative_end < 0) {
        // 14. Else if relativeEnd < 0, let final be max(len + relativeEnd, 0).
        final = max(length + relative_end, 0.0);
    } else {
        // 15. Else, let final be min(relativeEnd, len).
        final = min(relative_end, (double)length);
    }

    // 16. Let count be min(final - from, len - to).
    double count = min(final - from, length - to);

    // 17. If count > 0, then
    if (count > 0.0) {
        // a. NOTE: The copying must be performed in a manner that preserves the bit-level encoding of the source data.

        // b. Let buffer be O.[[ViewedArrayBuffer]].
        auto buffer = typed_array->viewed_array_buffer();

        // c. If IsDetachedBuffer(buffer) is true, throw a TypeError exception.
        if (buffer->is_detached())
            return vm.throw_completion<TypeError>(global_object, ErrorType::DetachedArrayBuffer);

        // d. Let elementSize be TypedArrayElementSize(O).
        auto element_size = typed_array->element_size();

        // e. Let byteOffset be O.[[ByteOffset]].
        auto byte_offset = typed_array->byte_offset();

        // FIXME: Not exactly sure what we should do when overflow occurs.
        //        Just return as if succeeded for now. (This goes for steps g to j)

        // f. Let toByteIndex be to × elementSize + byteOffset.
        Checked<size_t> to_byte_index_checked = static_cast<size_t>(to);
        to_byte_index_checked *= element_size;
        to_byte_index_checked += byte_offset;
        if (to_byte_index_checked.has_overflow()) {
            dbgln("TypedArrayPrototype::copy_within: to_byte_index overflowed, returning as if succeeded.");
            return typed_array;
        }

        // g. Let fromByteIndex be from × elementSize + byteOffset.
        Checked<size_t> from_byte_index_checked = static_cast<size_t>(from);
        from_byte_index_checked *= element_size;
        from_byte_index_checked += byte_offset;
        if (from_byte_index_checked.has_overflow()) {
            dbgln("TypedArrayPrototype::copy_within: from_byte_index_checked overflowed, returning as if succeeded.");
            return typed_array;
        }

        // h. Let countBytes be count × elementSize.
        Checked<size_t> count_bytes_checked = static_cast<size_t>(count);
        count_bytes_checked *= element_size;
        if (count_bytes_checked.has_overflow()) {
            dbgln("TypedArrayPrototype::copy_within: count_bytes_checked overflowed, returning as if succeeded.");
            return typed_array;
        }

        auto to_byte_index = to_byte_index_checked.value();
        auto from_byte_index = from_byte_index_checked.value();
        auto count_bytes = count_bytes_checked.value();

        Checked<size_t> from_plus_count = from_byte_index;
        from_plus_count += count_bytes;
        if (from_plus_count.has_overflow()) {
            dbgln("TypedArrayPrototype::copy_within: from_plus_count overflowed, returning as if succeeded.");
            return typed_array;
        }

        i8 direction;

        // i. If fromByteIndex < toByteIndex and toByteIndex < fromByteIndex + countBytes, then
        if (from_byte_index < to_byte_index && to_byte_index < from_plus_count.value()) {
            // i. Let direction be -1.
            direction = -1;

            // ii. Set fromByteIndex to fromByteIndex + countBytes - 1.
            from_byte_index = from_plus_count.value() - 1;

            Checked<size_t> to_plus_count = to_byte_index;
            to_plus_count += count_bytes;
            if (to_plus_count.has_overflow()) {
                dbgln("TypedArrayPrototype::copy_within: to_plus_count overflowed, returning as if succeeded.");
                return typed_array;
            }

            // iii. Set toByteIndex to toByteIndex + countBytes - 1.
            to_byte_index = to_plus_count.value() - 1;
        }
        // j. Else,
        else {
            // i. Let direction be 1.
            direction = 1;
        }

        // k. Repeat, while countBytes > 0,
        for (; count_bytes > 0; --count_bytes) {
            // i. Let value be GetValueFromBuffer(buffer, fromByteIndex, Uint8, true, Unordered).
            auto value = buffer->get_value<u8>(from_byte_index, true, ArrayBuffer::Order::Unordered);

            // ii. Perform SetValueInBuffer(buffer, toByteIndex, Uint8, value, true, Unordered).
            buffer->set_value<u8>(to_byte_index, value, true, ArrayBuffer::Order::Unordered);

            // iii. Set fromByteIndex to fromByteIndex + direction.
            from_byte_index += direction;

            // iv. Set toByteIndex to toByteIndex + direction.
            to_byte_index += direction;

            // v. Set countBytes to countBytes - 1.
        }
    }

    // 18. Return O.
    return typed_array;
}

// 23.2.3.10 %TypedArray%.prototype.filter ( callbackfn [ , thisArg ] ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.filter
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::filter)
{
    // 1. Let O be the this value.
    // 2. Perform ? ValidateTypedArray(O).
    auto* typed_array = TRY(validate_typed_array_from_this(global_object));

    // 3. Let len be O.[[ArrayLength]].
    auto initial_length = typed_array->array_length();

    // 4. If IsCallable(callbackfn) is false, throw a TypeError exception.
    auto* callback_function = TRY(callback_from_args(global_object, "filter"));

    // 5. Let kept be a new empty List.
    MarkedVector<Value> kept(vm.heap());

    // 7. Let captured be 0.
    size_t captured = 0;

    auto this_value = vm.argument(1);

    // 5. Let k be 0.
    // 8. Repeat, while k < len,
    for (size_t i = 0; i < initial_length; ++i) {
        // a. Let Pk be ! ToString(𝔽(k)).
        // b. Let kValue be ! Get(O, Pk).
        auto value = MUST(typed_array->get(i));

        // c. Let selected be ToBoolean(? Call(callbackfn, thisArg, « kValue, 𝔽(k), O »)).
        auto callback_result = TRY(call(global_object, *callback_function, this_value, value, Value((i32)i), typed_array)).to_boolean();

        // d. If selected is true, then
        if (callback_result) {
            // i. Append kValue to the end of kept.
            kept.append(value);

            // ii. Set captured to captured + 1.
            ++captured;
        }

        // e. Set k to k + 1.
    }

    // 9. Let A be ? TypedArraySpeciesCreate(O, « 𝔽(captured) »).
    MarkedVector<Value> arguments(vm.heap());
    arguments.empend(captured);
    auto* filter_array = TRY(typed_array_species_create(global_object, *typed_array, move(arguments)));

    // 10. Let n be 0.
    size_t index = 0;

    // 11. For each element e of kept, do
    for (auto& value : kept) {
        // a. Perform ! Set(A, ! ToString(𝔽(n)), e, true).
        MUST(filter_array->set(index, value, Object::ShouldThrowExceptions::Yes));

        // b. Set n to n + 1.
        ++index;
    }

    // 12. Return A.
    return filter_array;
}

// 23.2.3.20 %TypedArray%.prototype.map ( callbackfn [ , thisArg ] ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.map
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::map)
{
    // 1. Let O be the this value.
    // 2. Perform ? ValidateTypedArray(O).
    auto* typed_array = TRY(validate_typed_array_from_this(global_object));

    // 3. Let len be O.[[ArrayLength]].
    auto initial_length = typed_array->array_length();

    // 4. If IsCallable(callbackfn) is false, throw a TypeError exception.
    auto* callback_function = TRY(callback_from_args(global_object, "map"));

    // 5. Let A be ? TypedArraySpeciesCreate(O, « 𝔽(len) »).
    MarkedVector<Value> arguments(vm.heap());
    arguments.empend(initial_length);
    auto* return_array = TRY(typed_array_species_create(global_object, *typed_array, move(arguments)));

    auto this_value = vm.argument(1);

    // 6. Let k be 0.
    // 7. Repeat, while k < len,
    for (size_t i = 0; i < initial_length; ++i) {
        // a. Let Pk be ! ToString(𝔽(k)).
        // b. Let kValue be ! Get(O, Pk).
        auto value = MUST(typed_array->get(i));

        // c. Let mappedValue be ? Call(callbackfn, thisArg, « kValue, 𝔽(k), O »).
        auto mapped_value = TRY(call(global_object, *callback_function, this_value, value, Value((i32)i), typed_array));

        // d. Perform ? Set(A, Pk, mappedValue, true).
        TRY(return_array->set(i, mapped_value, Object::ShouldThrowExceptions::Yes));

        // e. Set k to k + 1.
    }

    // 8. Return A.
    return return_array;
}

// 23.2.3.29 %TypedArray%.prototype.toLocaleString ( [ reserved1 [ , reserved2 ] ] ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.tolocalestring
// 19.5.1 Array.prototype.toLocaleString ( [ locales [ , options ] ] ), https://tc39.es/ecma402/#sup-array.prototype.tolocalestring
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::to_locale_string)
{
    auto locales = vm.argument(0);
    auto options = vm.argument(1);

    // This function is not generic. ValidateTypedArray is applied to the this value prior to evaluating the algorithm.
    // If its result is an abrupt completion that exception is thrown instead of evaluating the algorithm.
    auto* typed_array = TRY(validate_typed_array_from_this(global_object));

    // 1. Let array be ? ToObject(this value).
    // NOTE: Handled by ValidateTypedArray

    // 2. Let len be ? ToLength(? Get(array, "length")).
    // The implementation of the algorithm may be optimized with the knowledge that the this value is an object that
    // has a fixed length and whose integer-indexed properties are not sparse.
    auto length = typed_array->array_length();

    // 3. Let separator be the implementation-defined list-separator String value appropriate for the host environment's current locale (such as ", ").
    constexpr auto separator = ',';

    // 4. Let R be the empty String.
    StringBuilder builder;

    // 5. Let k be 0.
    // 6. Repeat, while k < len,
    for (size_t k = 0; k < length; ++k) {
        // a. If k > 0, then
        if (k > 0) {
            // i. Set R to the string-concatenation of R and separator.
            builder.append(separator);
        }

        // b. Let nextElement be ? Get(array, ! ToString(k)).
        auto next_element = TRY(typed_array->get(k));

        // c. If nextElement is not undefined or null, then
        if (!next_element.is_nullish()) {
            // i. Let S be ? ToString(? Invoke(nextElement, "toLocaleString", « locales, options »)).
            auto locale_string_value = TRY(next_element.invoke(global_object, vm.names.toLocaleString, locales, options));
            auto locale_string = TRY(locale_string_value.to_string(global_object));

            // ii. Set R to the string-concatenation of R and S.
            builder.append(locale_string);
        }

        // d. Increase k by 1.
    }

    // 7. Return R.
    return js_string(vm, builder.to_string());
}

}

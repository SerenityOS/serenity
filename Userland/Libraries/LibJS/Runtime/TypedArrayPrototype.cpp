/*
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
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
}

TypedArrayPrototype::~TypedArrayPrototype()
{
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

static ThrowCompletionOr<FunctionObject*> callback_from_args(GlobalObject& global_object, const String& name)
{
    auto& vm = global_object.vm();
    if (vm.argument_count() < 1)
        return vm.throw_completion<TypeError>(global_object, ErrorType::TypedArrayPrototypeOneArg, name);
    auto callback = vm.argument(0);
    if (!callback.is_function())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAFunction, callback.to_string_without_side_effects());
    return &callback.as_function();
}

static ThrowCompletionOr<void> for_each_item(VM& vm, GlobalObject& global_object, const String& name, Function<IterationDecision(size_t index, Value value, Value callback_result)> callback)
{
    auto* typed_array = TRY(validate_typed_array_from_this(global_object));

    auto initial_length = typed_array->array_length();

    auto* callback_function = TRY(callback_from_args(global_object, name));

    auto this_value = vm.argument(1);

    for (size_t i = 0; i < initial_length; ++i) {
        auto value = TRY(typed_array->get(i));

        auto callback_result = TRY(vm.call(*callback_function, this_value, value, Value((i32)i), typed_array));

        if (callback(i, value, callback_result) == IterationDecision::Break)
            break;
    }

    return {};
}

static ThrowCompletionOr<void> for_each_item_from_last(VM& vm, GlobalObject& global_object, const String& name, Function<IterationDecision(size_t index, Value value, Value callback_result)> callback)
{
    auto* typed_array = TRY(validate_typed_array_from_this(global_object));

    auto initial_length = typed_array->array_length();

    auto* callback_function = TRY(callback_from_args(global_object, name));

    auto this_value = vm.argument(1);

    for (ssize_t i = (ssize_t)initial_length - 1; i >= 0; --i) {
        auto value = TRY(typed_array->get(i));

        auto callback_result = TRY(vm.call(*callback_function, this_value, value, Value((i32)i), typed_array));

        if (callback(i, value, callback_result) == IterationDecision::Break)
            break;
    }

    return {};
}

// 23.2.4.1 TypedArraySpeciesCreate ( exemplar, argumentList ), https://tc39.es/ecma262/#typedarray-species-create
static ThrowCompletionOr<TypedArrayBase*> typed_array_species_create(GlobalObject& global_object, TypedArrayBase const& exemplar, MarkedValueList arguments)
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
    for_each_item(vm, global_object, "every", [&](auto, auto, auto callback_result) {
        if (!callback_result.to_boolean()) {
            result = false;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });
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
    for_each_item(vm, global_object, "find", [&](auto, auto value, auto callback_result) {
        if (callback_result.to_boolean()) {
            result = value;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });
    return result;
}

// 23.2.3.12 %TypedArray%.prototype.findIndex ( predicate [ , thisArg ] ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.findindex
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

// 4 %TypedArray%.prototype.findLast ( predicate [ , thisArg ] ), https://tc39.es/proposal-array-find-from-last/#sec-%typedarray%.prototype.findlast
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::find_last)
{
    auto result = js_undefined();
    for_each_item_from_last(vm, global_object, "findLast", [&](auto, auto value, auto callback_result) {
        if (callback_result.to_boolean()) {
            result = value;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });
    return result;
}

// 5 %TypedArray%.prototype.findLastIndex ( predicate [ , thisArg ] ), https://tc39.es/proposal-array-find-from-last/#sec-%typedarray%.prototype.findlastindex
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::find_last_index)
{
    auto result_index = -1;
    for_each_item_from_last(vm, global_object, "findLastIndex", [&](auto index, auto, auto callback_result) {
        if (callback_result.to_boolean()) {
            result_index = index;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });
    return Value(result_index);
}

// 23.2.3.13 %TypedArray%.prototype.forEach ( callbackfn [ , thisArg ] ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.foreach
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::for_each)
{
    for_each_item(vm, global_object, "forEach", [](auto, auto, auto) {
        return IterationDecision::Continue;
    });
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

        accumulator = TRY(vm.call(*callback_function, js_undefined(), accumulator, k_value, Value(k), typed_array));
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

        accumulator = TRY(vm.call(*callback_function, js_undefined(), accumulator, k_value, Value(k), typed_array));
    }

    return accumulator;
}

// 23.2.3.26 %TypedArray%.prototype.some ( callbackfn [ , thisArg ] ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.some
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

// 23.2.3.24 %TypedArray%.prototype.set ( source [ , offset ] ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.set
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::set)
{
    auto* typed_array = TRY(typed_array_from_this(global_object));

    auto source = vm.argument(0);

    auto target_offset = TRY(vm.argument(1).to_integer_or_infinity(global_object));
    if (target_offset < 0)
        return vm.throw_completion<JS::RangeError>(global_object, "Invalid target offset");

    if (source.is_object() && is<TypedArrayBase>(source.as_object())) {
        auto& source_typed_array = static_cast<TypedArrayBase&>(source.as_object());
        // 23.2.3.23.1 SetTypedArrayFromTypedArray ( target, targetOffset, source ), https://tc39.es/ecma262/#sec-settypedarrayfromtypedarray
        auto target_buffer = typed_array->viewed_array_buffer();
        if (target_buffer->is_detached())
            return vm.throw_completion<JS::TypeError>(global_object, ErrorType::DetachedArrayBuffer);
        auto target_length = typed_array->array_length();
        auto target_byte_offset = typed_array->byte_offset();

        auto source_buffer = source_typed_array.viewed_array_buffer();
        if (source_buffer->is_detached())
            return vm.throw_completion<JS::TypeError>(global_object, ErrorType::DetachedArrayBuffer);
        auto source_length = source_typed_array.array_length();
        auto source_byte_offset = source_typed_array.byte_offset();

        if (isinf(target_offset))
            return vm.throw_completion<JS::RangeError>(global_object, "Invalid target offset");

        Checked<size_t> checked = source_length;
        checked += static_cast<u32>(target_offset);
        if (checked.has_overflow() || checked.value() > target_length)
            return vm.throw_completion<JS::RangeError>(global_object, "Overflow or out of bounds in target length");

        if (typed_array->content_type() != source_typed_array.content_type())
            return vm.throw_completion<JS::TypeError>(global_object, "Copy between arrays of different content types is prohibited");

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
        if (checked_target_byte_index.has_overflow())
            return vm.throw_completion<JS::RangeError>(global_object, "Overflow in target byte index");
        auto target_byte_index = checked_target_byte_index.value();

        Checked<size_t> checked_limit(source_length);
        checked_limit *= typed_array->element_size();
        checked_limit += target_byte_index;
        if (checked_limit.has_overflow())
            return vm.throw_completion<JS::RangeError>(global_object, "Overflow in target limit");
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
        if (target_buffer->is_detached())
            return vm.throw_completion<JS::TypeError>(global_object, ErrorType::DetachedArrayBuffer);
        auto target_length = typed_array->array_length();
        auto target_byte_offset = typed_array->byte_offset();

        auto src = TRY(source.to_object(global_object));
        auto source_length = TRY(length_of_array_like(global_object, *src));

        if (isinf(target_offset))
            return vm.throw_completion<JS::RangeError>(global_object, "Invalid target offset");

        Checked<size_t> checked = source_length;
        checked += static_cast<u32>(target_offset);
        if (checked.has_overflow() || checked.value() > target_length)
            return vm.throw_completion<JS::RangeError>(global_object, "Overflow or out of bounds in target length");

        Checked<size_t> checked_target_byte_index(static_cast<size_t>(target_offset));
        checked_target_byte_index *= typed_array->element_size();
        checked_target_byte_index += target_byte_offset;
        if (checked_target_byte_index.has_overflow())
            return vm.throw_completion<JS::RangeError>(global_object, "Overflow in target byte index");
        auto target_byte_index = checked_target_byte_index.value();

        Checked<size_t> checked_limit(source_length);
        checked_limit *= typed_array->element_size();
        checked_limit += target_byte_index;
        if (checked_limit.has_overflow())
            return vm.throw_completion<JS::RangeError>(global_object, "Overflow in target limit");

        auto limit = checked_limit.value();
        auto k = 0;
        while (target_byte_index < limit) {
            auto value = TRY(src->get(k));
            if (typed_array->content_type() == TypedArrayBase::ContentType::BigInt)
                value = TRY(value.to_bigint(global_object));
            else
                value = TRY(value.to_number(global_object));

            if (target_buffer->is_detached())
                return vm.throw_completion<JS::TypeError>(global_object, ErrorType::DetachedArrayBuffer);

            typed_array->set_value_in_buffer(target_byte_index, value, ArrayBuffer::Unordered);
            ++k;
            target_byte_index += typed_array->element_size();
        }
    }
    return js_undefined();
}

// 23.2.3.25 %TypedArray%.prototype.slice ( start, end ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.slice
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::slice)
{
    auto* typed_array = TRY(validate_typed_array_from_this(global_object));

    auto length = typed_array->array_length();

    auto relative_start = TRY(vm.argument(0).to_integer_or_infinity(global_object));

    i32 k;
    if (Value(relative_start).is_negative_infinity())
        k = 0;
    else if (relative_start < 0)
        k = max(length + relative_start, 0);
    else
        k = min(relative_start, length);

    double relative_end;
    if (vm.argument(1).is_undefined())
        relative_end = length;
    else
        relative_end = TRY(vm.argument(1).to_integer_or_infinity(global_object));

    i32 final;
    if (Value(relative_end).is_negative_infinity())
        final = 0;
    else if (relative_end < 0)
        final = max(length + relative_end, 0);
    else
        final = min(relative_end, length);

    auto count = max(final - k, 0);

    MarkedValueList arguments(vm.heap());
    arguments.empend(count);
    auto* new_array = TRY(typed_array_species_create(global_object, *typed_array, move(arguments)));

    if (count > 0) {
        if (typed_array->viewed_array_buffer()->is_detached())
            return vm.throw_completion<TypeError>(global_object, ErrorType::DetachedArrayBuffer);

        if (typed_array->element_name() != new_array->element_name()) {
            for (i32 n = 0; k < final; ++k, ++n) {
                auto k_value = MUST(typed_array->get(k));
                MUST(new_array->set(n, k_value, Object::ShouldThrowExceptions::Yes));
            }
        } else {
            auto element_size = typed_array->element_size();

            Checked<u32> source_byte_index = k;
            source_byte_index *= element_size;
            source_byte_index += typed_array->byte_offset();
            if (source_byte_index.has_overflow()) {
                dbgln("TypedArrayPrototype::slice: source_byte_index overflowed, returning as if succeeded.");
                return new_array;
            }

            auto target_byte_index = new_array->byte_offset();

            Checked<u32> limit = count;
            limit *= element_size;
            limit += target_byte_index;
            if (limit.has_overflow()) {
                dbgln("TypedArrayPrototype::slice: limit overflowed, returning as if succeeded.");
                return new_array;
            }

            auto& source_buffer = *typed_array->viewed_array_buffer();
            auto& target_buffer = *new_array->viewed_array_buffer();
            for (; target_byte_index < limit.value(); ++source_byte_index, ++target_byte_index) {
                auto value = source_buffer.get_value<u8>(source_byte_index.value(), true, ArrayBuffer::Unordered);
                target_buffer.set_value<u8>(target_byte_index, value, true, ArrayBuffer::Unordered);
            }
        }
    }

    return new_array;
}

static ThrowCompletionOr<void> typed_array_merge_sort(GlobalObject& global_object, FunctionObject* compare_function, ArrayBuffer& buffer, MarkedValueList& arr_to_sort)
{
    auto& vm = global_object.vm();
    if (arr_to_sort.size() <= 1)
        return {};

    MarkedValueList left(vm.heap());
    MarkedValueList right(vm.heap());

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
            auto result = TRY(vm.call(*compare_function, js_undefined(), x, y));

            auto value = TRY(result.to_number(global_object));

            if (buffer.is_detached())
                return vm.throw_completion<TypeError>(global_object, ErrorType::DetachedArrayBuffer);

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

    MarkedValueList items(vm.heap());
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
    auto* typed_array = TRY(typed_array_from_this(global_object));

    auto length = typed_array->array_length();

    auto relative_begin = TRY(vm.argument(0).to_integer_or_infinity(global_object));

    i32 begin_index;
    if (Value(relative_begin).is_negative_infinity())
        begin_index = 0;
    else if (relative_begin < 0)
        begin_index = max(length + relative_begin, 0);
    else
        begin_index = min(relative_begin, length);

    double relative_end;
    if (vm.argument(1).is_undefined())
        relative_end = length;
    else
        relative_end = TRY(vm.argument(1).to_integer_or_infinity(global_object));

    i32 end_index;
    if (Value(relative_end).is_negative_infinity())
        end_index = 0;
    else if (relative_end < 0)
        end_index = max(length + relative_end, 0);
    else
        end_index = min(relative_end, length);

    auto new_length = max(end_index - begin_index, 0);

    Checked<u32> begin_byte_offset = begin_index;
    begin_byte_offset *= typed_array->element_size();
    begin_byte_offset += typed_array->byte_offset();
    if (begin_byte_offset.has_overflow()) {
        dbgln("TypedArrayPrototype::begin_byte_offset: limit overflowed, returning as if succeeded.");
        return typed_array;
    }

    MarkedValueList arguments(vm.heap());
    arguments.empend(typed_array->viewed_array_buffer());
    arguments.empend(begin_byte_offset.value());
    arguments.empend(new_length);
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

        // d. Let typedArrayName be the String value of O.[[TypedArrayName]].
        // e. Let elementSize be the Element Size value specified in Table 64 for typedArrayName.
        auto element_size = typed_array->element_size();

        // f. Let byteOffset be O.[[ByteOffset]].
        auto byte_offset = typed_array->byte_offset();

        // FIXME: Not exactly sure what we should do when overflow occurs.
        //        Just return as if succeeded for now. (This goes for steps g to j)

        // g. Let toByteIndex be to × elementSize + byteOffset.
        Checked<size_t> to_byte_index_checked = static_cast<size_t>(to);
        to_byte_index_checked *= element_size;
        to_byte_index_checked += byte_offset;
        if (to_byte_index_checked.has_overflow()) {
            dbgln("TypedArrayPrototype::copy_within: to_byte_index overflowed, returning as if succeeded.");
            return typed_array;
        }

        // h. Let fromByteIndex be from × elementSize + byteOffset.
        Checked<size_t> from_byte_index_checked = static_cast<size_t>(from);
        from_byte_index_checked *= element_size;
        from_byte_index_checked += byte_offset;
        if (from_byte_index_checked.has_overflow()) {
            dbgln("TypedArrayPrototype::copy_within: from_byte_index_checked overflowed, returning as if succeeded.");
            return typed_array;
        }

        // i. Let countBytes be count × elementSize.
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

        // j. If fromByteIndex < toByteIndex and toByteIndex < fromByteIndex + countBytes, then
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
        } else {
            // k. Else,
            // i. Let direction be 1.
            direction = 1;
        }

        // l. Repeat, while countBytes > 0,
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
    MarkedValueList kept(vm.heap());

    // 7. Let captured be 0.
    size_t captured = 0;

    auto this_value = vm.argument(1);

    // 5. Let k be 0.
    // 8. Repeat, while k < len,
    for (size_t i = 0; i < initial_length; ++i) {
        // a. Let Pk be ! ToString(𝔽(k)).
        // b. Let kValue be ! Get(O, Pk).
        auto value = MUST(typed_array->get(i));

        // c. Let selected be ! ToBoolean(? Call(callbackfn, thisArg, « kValue, 𝔽(k), O »)).
        auto callback_result = TRY(vm.call(*callback_function, this_value, value, Value((i32)i), typed_array)).to_boolean();

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
    MarkedValueList arguments(vm.heap());
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
    MarkedValueList arguments(vm.heap());
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
        auto mapped_value = TRY(vm.call(*callback_function, this_value, value, Value((i32)i), typed_array));

        // d. Perform ? Set(A, Pk, mappedValue, true).
        TRY(return_array->set(i, mapped_value, Object::ShouldThrowExceptions::Yes));

        // e. Set k to k + 1.
    }

    // 8. Return A.
    return return_array;
}

// 23.2.3.29 %TypedArray%.prototype.toLocaleString ( [ reserved1 [ , reserved2 ] ] ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.tolocalestring
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::to_locale_string)
{
    auto* typed_array = TRY(validate_typed_array_from_this(global_object));

    auto length = typed_array->array_length();

    StringBuilder builder;
    for (u32 k = 0; k < length; ++k) {
        if (k > 0)
            builder.append(','); // NOTE: Until we implement ECMA-402 (Intl) this is implementation specific.
        auto value = TRY(typed_array->get(k));
        if (value.is_nullish())
            continue;
        auto locale_string_result = TRY(value.invoke(global_object, vm.names.toLocaleString));
        auto string = TRY(locale_string_result.to_string(global_object));
        builder.append(string);
    }
    return js_string(vm, builder.to_string());
}

}

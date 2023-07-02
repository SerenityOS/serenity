/*
 * Copyright (c) 2020-2023, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2021-2022, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TypeCasts.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/ArrayIterator.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/TypedArray.h>
#include <LibJS/Runtime/TypedArrayPrototype.h>

namespace JS {

TypedArrayPrototype::TypedArrayPrototype(Realm& realm)
    : Object(ConstructWithPrototypeTag::Tag, realm.intrinsics().object_prototype())
{
}

ThrowCompletionOr<void> TypedArrayPrototype::initialize(Realm& realm)
{
    auto& vm = this->vm();
    MUST_OR_THROW_OOM(Base::initialize(realm));
    u8 attr = Attribute::Writable | Attribute::Configurable;

    define_native_accessor(realm, vm.names.buffer, buffer_getter, nullptr, Attribute::Configurable);
    define_native_accessor(realm, vm.names.byteLength, byte_length_getter, nullptr, Attribute::Configurable);
    define_native_accessor(realm, vm.names.byteOffset, byte_offset_getter, nullptr, Attribute::Configurable);
    define_native_accessor(realm, vm.names.length, length_getter, nullptr, Attribute::Configurable);

    define_native_function(realm, vm.names.at, at, 1, attr);
    define_native_function(realm, vm.names.copyWithin, copy_within, 2, attr);
    define_native_function(realm, vm.names.entries, entries, 0, attr);
    define_native_function(realm, vm.names.every, every, 1, attr);
    define_native_function(realm, vm.names.fill, fill, 1, attr);
    define_native_function(realm, vm.names.filter, filter, 1, attr);
    define_native_function(realm, vm.names.find, find, 1, attr);
    define_native_function(realm, vm.names.findIndex, find_index, 1, attr);
    define_native_function(realm, vm.names.findLast, find_last, 1, attr);
    define_native_function(realm, vm.names.findLastIndex, find_last_index, 1, attr);
    define_native_function(realm, vm.names.forEach, for_each, 1, attr);
    define_native_function(realm, vm.names.includes, includes, 1, attr);
    define_native_function(realm, vm.names.indexOf, index_of, 1, attr);
    define_native_function(realm, vm.names.join, join, 1, attr);
    define_native_function(realm, vm.names.keys, keys, 0, attr);
    define_native_function(realm, vm.names.lastIndexOf, last_index_of, 1, attr);
    define_native_function(realm, vm.names.map, map, 1, attr);
    define_native_function(realm, vm.names.reduce, reduce, 1, attr);
    define_native_function(realm, vm.names.reduceRight, reduce_right, 1, attr);
    define_native_function(realm, vm.names.reverse, reverse, 0, attr);
    define_native_function(realm, vm.names.set, set, 1, attr);
    define_native_function(realm, vm.names.slice, slice, 2, attr);
    define_native_function(realm, vm.names.some, some, 1, attr);
    define_native_function(realm, vm.names.sort, sort, 1, attr);
    define_native_function(realm, vm.names.subarray, subarray, 2, attr);
    define_native_function(realm, vm.names.toLocaleString, to_locale_string, 0, attr);
    define_native_function(realm, vm.names.toReversed, to_reversed, 0, attr);
    define_native_function(realm, vm.names.toSorted, to_sorted, 1, attr);
    define_native_function(realm, vm.names.with, with, 2, attr);
    define_native_function(realm, vm.names.values, values, 0, attr);

    define_native_accessor(realm, vm.well_known_symbol_to_string_tag(), to_string_tag_getter, nullptr, Attribute::Configurable);

    // 23.2.3.34 %TypedArray%.prototype.toString ( ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.tostring
    define_direct_property(vm.names.toString, realm.intrinsics().array_prototype()->get_without_side_effects(vm.names.toString), attr);

    // 23.2.3.37 %TypedArray%.prototype [ @@iterator ] ( ), https://tc39.es/ecma262/#sec-%typedarray%.prototype-@@iterator
    define_direct_property(vm.well_known_symbol_iterator(), get_without_side_effects(vm.names.values), attr);

    return {};
}

static ThrowCompletionOr<TypedArrayBase*> typed_array_from_this(VM& vm)
{
    auto this_value = vm.this_value();
    return typed_array_from(vm, this_value);
}

static ThrowCompletionOr<TypedArrayBase*> validate_typed_array_from_this(VM& vm)
{
    auto* typed_array = TRY(typed_array_from_this(vm));

    TRY(validate_typed_array(vm, *typed_array));

    return typed_array;
}

static ThrowCompletionOr<FunctionObject*> callback_from_args(VM& vm, DeprecatedString const& name)
{
    if (vm.argument_count() < 1)
        return vm.throw_completion<TypeError>(ErrorType::TypedArrayPrototypeOneArg, name);
    auto callback = vm.argument(0);
    if (!callback.is_function())
        return vm.throw_completion<TypeError>(ErrorType::NotAFunction, TRY_OR_THROW_OOM(vm, callback.to_string_without_side_effects()));
    return &callback.as_function();
}

static ThrowCompletionOr<void> for_each_item(VM& vm, DeprecatedString const& name, Function<IterationDecision(size_t index, Value value, Value callback_result)> callback)
{
    auto* typed_array = TRY(validate_typed_array_from_this(vm));

    auto initial_length = typed_array->idempotent_array_length();

    auto* callback_function = TRY(callback_from_args(vm, name));

    auto this_value = vm.argument(1);

    for (size_t i = 0; i < initial_length; ++i) {
        auto value = TRY(typed_array->get(i));

        auto callback_result = TRY(call(vm, *callback_function, this_value, value, Value((i32)i), typed_array));

        if (callback(i, value, callback_result) == IterationDecision::Break)
            break;
    }

    return {};
}

static ThrowCompletionOr<void> for_each_item_from_last(VM& vm, DeprecatedString const& name, Function<IterationDecision(size_t index, Value value, Value callback_result)> callback)
{
    auto* typed_array = TRY(validate_typed_array_from_this(vm));

    auto initial_length = typed_array->idempotent_array_length();

    auto* callback_function = TRY(callback_from_args(vm, name));

    auto this_value = vm.argument(1);

    for (ssize_t i = (ssize_t)initial_length - 1; i >= 0; --i) {
        auto value = TRY(typed_array->get(i));

        auto callback_result = TRY(call(vm, *callback_function, this_value, value, Value((i32)i), typed_array));

        if (callback(i, value, callback_result) == IterationDecision::Break)
            break;
    }

    return {};
}

// 23.2.4.1 TypedArraySpeciesCreate ( exemplar, argumentList ), https://tc39.es/ecma262/#typedarray-species-create
static ThrowCompletionOr<TypedArrayBase*> typed_array_species_create(VM& vm, TypedArrayBase const& exemplar, MarkedVector<Value> arguments)
{
    auto& realm = *vm.current_realm();

    // 1. Let defaultConstructor be the intrinsic object listed in column one of Table 72 for exemplar.[[TypedArrayName]].
    auto default_constructor = (realm.intrinsics().*exemplar.intrinsic_constructor())();

    // 2. Let constructor be ? SpeciesConstructor(exemplar, defaultConstructor).
    auto* constructor = TRY(species_constructor(vm, exemplar, *default_constructor));

    // 3. Let result be ? TypedArrayCreate(constructor, argumentList).
    auto* result = TRY(typed_array_create(vm, *constructor, move(arguments)));

    // 4. Assert: result has [[TypedArrayName]] and [[ContentType]] internal slots.
    // 5. If result.[[ContentType]] ≠ exemplar.[[ContentType]], throw a TypeError exception.
    if (result->content_type() != exemplar.content_type())
        return vm.throw_completion<TypeError>(ErrorType::TypedArrayContentTypeMismatch, result->class_name(), exemplar.class_name());

    // 6. Return result.
    return result;
}

// 23.2.3.1 %TypedArray%.prototype.at ( index ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.at
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::at)
{
    // 1. Let O be the this value.
    // 2. Perform ? ValidateTypedArray(O).
    auto* typed_array = TRY(validate_typed_array_from_this(vm));

    // 3. Let len be O.[[ArrayLength]].
    auto length = typed_array->idempotent_array_length();

    // 4. Let relativeIndex be ? ToIntegerOrInfinity(index).
    auto relative_index = TRY(vm.argument(0).to_integer_or_infinity(vm));

    if (Value(relative_index).is_infinity())
        return js_undefined();

    Checked<size_t> index { 0 };

    // 5. If relativeIndex ≥ 0, then
    if (relative_index >= 0) {
        // a. Let k be relativeIndex.
        index += relative_index;
    }
    // 6. Else,
    else {
        // a. Let k be len + relativeIndex.
        index += length;
        index -= -relative_index;
    }

    // 7. If k < 0 or k ≥ len, return undefined.
    if (index.has_overflow() || index.value() >= length)
        return js_undefined();

    // 8. Return ! Get(O, ! ToString(𝔽(k))).
    return MUST(typed_array->get(index.value()));
}

// 23.2.3.2 get %TypedArray%.prototype.buffer, https://tc39.es/ecma262/#sec-get-%typedarray%.prototype.buffer
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::buffer_getter)
{
    // 1. Let O be the this value.
    // 2. Perform ? RequireInternalSlot(O, [[TypedArrayName]]).
    auto* typed_array = TRY(typed_array_from_this(vm));

    // 3. Assert: O has a [[ViewedArrayBuffer]] internal slot.
    // 4. Let buffer be O.[[ViewedArrayBuffer]].
    auto* buffer = typed_array->viewed_array_buffer();
    VERIFY(buffer);

    // 5. Return buffer.
    return Value(buffer);
}

// 23.2.3.3 get %TypedArray%.prototype.byteLength, https://tc39.es/proposal-resizablearraybuffer/#sec-get-%typedarray%.prototype.bytelength
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::byte_length_getter)
{
    // 1. Let O be the this value.
    // 2. Perform ? RequireInternalSlot(O, [[TypedArrayName]]).
    auto* typed_array = TRY(typed_array_from_this(vm));

    // 3. Assert: O has a [[ViewedArrayBuffer]] internal slot.
    // 4. Let buffer be O.[[ViewedArrayBuffer]].
    auto* buffer = typed_array->viewed_array_buffer();
    VERIFY(buffer);

    // 5. Let getBufferByteLength be MakeIdempotentArrayBufferByteLengthGetter(SeqCst).
    auto get_buffer_byte_length = make_idempotent_array_buffer_byte_length_getter(ArrayBuffer::Order::SeqCst);

    // 6. Let size be IntegerIndexedObjectByteLength(O, getBufferByteLength).
    auto size = integer_indexed_object_byte_length(vm, *typed_array, get_buffer_byte_length);

    // 7. Return 𝔽(size).
    return Value(size);
}

// 23.2.3.4 get %TypedArray%.prototype.byteOffset, https://tc39.es/proposal-resizablearraybuffer/#sec-get-%typedarray%.prototype.bytelength
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::byte_offset_getter)
{
    // 1. Let O be the this value.
    // 2. Perform ? RequireInternalSlot(O, [[TypedArrayName]]).
    auto* typed_array = TRY(typed_array_from_this(vm));

    // 3. Assert: O has a [[ViewedArrayBuffer]] internal slot.
    // 4. Let buffer be O.[[ViewedArrayBuffer]].
    auto* buffer = typed_array->viewed_array_buffer();
    VERIFY(buffer);

    // 5. Let getBufferByteLength be MakeIdempotentArrayBufferByteLengthGetter(SeqCst).
    auto get_buffer_byte_length = make_idempotent_array_buffer_byte_length_getter(ArrayBuffer::Order::SeqCst);

    // 6. If IsIntegerIndexedObjectOutOfBounds(O, getBufferByteLength) is true, return +0𝔽.
    if (is_integer_indexed_object_out_of_bounds(vm, *typed_array, get_buffer_byte_length))
        return Value(0);

    // 7. Let offset be O.[[ByteOffset]].
    auto offset = typed_array->byte_offset();

    // 8. Return 𝔽(offset).
    return Value(offset);
}

// 23.2.3.6 %TypedArray%.prototype.copyWithin ( target, start [ , end ] ), https://tc39.es/proposal-resizablearraybuffer/#sec-%typedarray%.prototype.copywithin
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::copy_within)
{
    // 1. Let O be the this value.
    // 2. Perform ? ValidateTypedArray(O).
    auto* typed_array = TRY(validate_typed_array_from_this(vm));

    // 3. Let getBufferByteLength be MakeIdempotentArrayBufferByteLengthGetter(SeqCst).
    auto get_buffer_byte_length = make_idempotent_array_buffer_byte_length_getter(ArrayBuffer::Order::SeqCst);

    // 4. Let len be IntegerIndexedObjectLength(O, getBufferByteLength).
    auto length = integer_indexed_object_length(vm, *typed_array, get_buffer_byte_length);

    // 5. Assert: len is not out-of-bounds.
    VERIFY(length.has_value());

    // 6. Let relativeTarget be ? ToIntegerOrInfinity(target).
    auto relative_target = TRY(vm.argument(0).to_integer_or_infinity(vm));

    double to;
    if (Value(relative_target).is_negative_infinity()) {
        // 7. If relativeTarget is -∞, let to be 0.
        to = 0.0;
    } else if (relative_target < 0) {
        // 8. Else if relativeTarget < 0, let to be max(len + relativeTarget, 0)
        to = max(length.value() + relative_target, 0.0);
    } else {
        // 9. Else, let to be min(relativeTarget, len).
        to = min(relative_target, (double)length.value());
    }

    // 10. Let relativeStart be ? ToIntegerOrInfinity(start).
    auto relative_start = TRY(vm.argument(1).to_integer_or_infinity(vm));

    double from;
    if (Value(relative_start).is_negative_infinity()) {
        // 11. If relativeStart is -∞, let from be 0.
        from = 0.0;
    } else if (relative_start < 0) {
        // 12. Else if relativeStart < 0, let from be max(len + relativeStart, 0).
        from = max(length.value() + relative_start, 0.0);
    } else {
        // 13. Else, let from be min(relativeStart, len).
        from = min(relative_start, (double)length.value());
    }

    double relative_end;

    // 14. If end is undefined, let relativeEnd be len; else let relativeEnd be ? ToIntegerOrInfinity(end).
    if (vm.argument(2).is_undefined())
        relative_end = length.value();
    else
        relative_end = TRY(vm.argument(2).to_integer_or_infinity(vm));

    double final;
    if (Value(relative_end).is_negative_infinity()) {
        // 15. If relativeEnd is -∞, let final be 0.
        final = 0.0;
    } else if (relative_end < 0) {
        // 16. Else if relativeEnd < 0, let final be max(len + relativeEnd, 0).
        final = max(length.value() + relative_end, 0.0);
    } else {
        // 17. Else, let final be min(relativeEnd, len).
        final = min(relative_end, (double)length.value());
    }

    // 18. Let count be min(final - from, len - to).
    double count = min(final - from, length.value() - to);

    // 19. If count > 0, then
    if (count > 0.0) {
        // a. NOTE: The copying must be performed in a manner that preserves the bit-level encoding of the source data.

        // b. Let buffer be O.[[ViewedArrayBuffer]].
        auto buffer = typed_array->viewed_array_buffer();

        // c. set getBufferByteLength be MakeIdempotentArrayBufferByteLengthGetter(SeqCst).
        get_buffer_byte_length = make_idempotent_array_buffer_byte_length_getter(ArrayBuffer::Order::SeqCst);

        // d. Let len be IntegerIndexedObjectLength(O, getBufferByteLength).
        length = integer_indexed_object_length(vm, *typed_array, get_buffer_byte_length);

        // e. If len is out-of-bounds, throw a TypeError exception.
        if (!length.has_value())
            return vm.throw_completion<TypeError>(ErrorType::TypedArrayOverflowOrOutOfBounds, "length");

        // f. Let typedArrayName be the String value of O.[[TypedArrayName]].
        // g. Let elementSize be the Element Size value specified in Table 67 for typedArrayName.
        auto element_size = typed_array->element_size();

        // FIXME: Not exactly sure what we should do when overflow occurs.
        //        Just return as if succeeded for now. (This goes for steps h to m)

        // h. Let bufferByteLen be len × elementSize.
        Checked<size_t> buffer_byte_length = length.value();
        buffer_byte_length *= element_size;
        if (buffer_byte_length.has_overflow()) {
            dbgln("TypedArrayPrototype::copy_within: buffer_byte_length overflowed, returning as if succeeded.");
            return typed_array;
        }

        // i. Let byteOffset be O.[[ByteOffset]].
        auto byte_offset = typed_array->byte_offset();

        // j. Let toByteIndex be to × elementSize + byteOffset.
        Checked<size_t> to_byte_index_checked = static_cast<size_t>(to);
        to_byte_index_checked *= element_size;
        to_byte_index_checked += byte_offset;
        if (to_byte_index_checked.has_overflow()) {
            dbgln("TypedArrayPrototype::copy_within: to_byte_index overflowed, returning as if succeeded.");
            return typed_array;
        }

        // k. Let fromByteIndex be from × elementSize + byteOffset.
        Checked<size_t> from_byte_index_checked = static_cast<size_t>(from);
        from_byte_index_checked *= element_size;
        from_byte_index_checked += byte_offset;
        if (from_byte_index_checked.has_overflow()) {
            dbgln("TypedArrayPrototype::copy_within: from_byte_index_checked overflowed, returning as if succeeded.");
            return typed_array;
        }

        // l. Let countBytes be count × elementSize.
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

        // m. If fromByteIndex < toByteIndex and toByteIndex < fromByteIndex + countBytes, then
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
        // l. Else,
        else {
            // i. Let direction be 1.
            direction = 1;
        }

        // o. Repeat, while countBytes > 0,
        for (; count_bytes > 0; --count_bytes) {
            // i. If fromByteIndex < bufferByteLen and toByteIndex < bufferByteLen, then
            if (from_byte_index < buffer_byte_length && to_byte_index < buffer_byte_length) {
                // 1. Let value be GetValueFromBuffer(buffer, fromByteIndex, Uint8, true, Unordered).
                auto value = buffer->get_value<u8>(from_byte_index, true, ArrayBuffer::Order::Unordered);

                // 2. Perform SetValueInBuffer(buffer, toByteIndex, Uint8, value, true, Unordered).
                buffer->set_value<u8>(to_byte_index, value, true, ArrayBuffer::Order::Unordered);

                // 3. Set fromByteIndex to fromByteIndex + direction.
                from_byte_index += direction;

                // 4. Set toByteIndex to toByteIndex + direction.
                to_byte_index += direction;
            }

            // ii. Set countBytes to countBytes - 1.
        }
    }

    // 20. Return O.
    return typed_array;
}

// 23.2.3.7 %TypedArray%.prototype.entries ( ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.entries
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::entries)
{
    auto& realm = *vm.current_realm();

    // 1. Let O be the this value.
    // 2. Perform ? ValidateTypedArray(O).
    auto* typed_array = TRY(validate_typed_array_from_this(vm));

    // 3. Return CreateArrayIterator(O, key+value).
    return ArrayIterator::create(realm, typed_array, Object::PropertyKind::KeyAndValue);
}

// 23.2.3.8 %TypedArray%.prototype.every ( callbackfn [ , thisArg ] ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.every
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::every)
{
    auto result = true;
    TRY(for_each_item(vm, "every", [&](auto, auto, auto callback_result) {
        if (!callback_result.to_boolean()) {
            result = false;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    }));
    return Value(result);
}

// 23.2.3.9 %TypedArray%.prototype.fill ( value [ , start [ , end ] ] ), https://tc39.es/proposal-resizablearraybuffer/#sec-%typedarray%.prototype.fill
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::fill)
{
    // 1. Let O be the this value.
    // 2. Perform ? ValidateTypedArray(O).
    auto typed_array = TRY(validate_typed_array_from_this(vm));

    // 3. Let getBufferByteLength be MakeIdempotentArrayBufferByteLengthGetter(SeqCst).
    auto get_buffer_byte_length = make_idempotent_array_buffer_byte_length_getter(ArrayBuffer::Order::SeqCst);

    // 4. Let len be IntegerIndexedObjectLength(O, getBufferByteLength).
    auto length = integer_indexed_object_length(vm, *typed_array, get_buffer_byte_length);

    // 5. Assert: len is not out-of-bounds.
    VERIFY(length.has_value());

    Value value;
    // 6. If O.[[ContentType]] is BigInt, set value to ? ToBigInt(value).
    if (typed_array->content_type() == TypedArrayBase::ContentType::BigInt)
        value = TRY(vm.argument(0).to_bigint(vm));
    // 7. Otherwise, set value to ? ToNumber(value).
    else
        value = TRY(vm.argument(0).to_number(vm));

    // 8. Let relativeStart be ? ToIntegerOrInfinity(start).
    auto relative_start = TRY(vm.argument(1).to_integer_or_infinity(vm));

    u32 k;
    // 9. If relativeStart is -∞, let k be 0.
    if (Value(relative_start).is_negative_infinity())
        k = 0;
    // 10. Else if relativeStart < 0, let k be max(len + relativeStart, 0).
    else if (relative_start < 0)
        k = max(length.value() + relative_start, 0);
    // 11. Else, let k be min(relativeStart, len).
    else
        k = min(relative_start, length.value());

    // 12. If end is undefined, let relativeEnd be len; else let relativeEnd be ? ToIntegerOrInfinity(end).
    double relative_end;
    if (vm.argument(2).is_undefined())
        relative_end = length.value();
    else
        relative_end = TRY(vm.argument(2).to_integer_or_infinity(vm));

    u32 final;
    // 13. If relativeEnd is -∞, let final be 0.
    if (Value(relative_end).is_negative_infinity())
        final = 0;
    // 14. Else if relativeEnd < 0, let final be max(len + relativeEnd, 0).
    else if (relative_end < 0)
        final = max(length.value() + relative_end, 0);
    // 15. Else, let final be min(relativeEnd, len).
    else
        final = min(relative_end, length.value());

    // 16. Set getBufferByteLength to MakeIdempotentArrayBufferByteLengthGetter(SeqCst).
    get_buffer_byte_length = make_idempotent_array_buffer_byte_length_getter(ArrayBuffer::Order::SeqCst);

    // 17. Set len to IntegerIndexedObjectLength(O, getBufferByteLength).
    length = integer_indexed_object_length(vm, *typed_array, get_buffer_byte_length);

    // 18. If len is out-of-bounds, throw a TypeError exception.
    if (!length.has_value())
        return vm.throw_completion<TypeError>(ErrorType::TypedArrayOverflowOrOutOfBounds, "length");

    // 19. Set final to min(final, len).
    final = min(final, length.value());

    // 20. Repeat, while k < final,
    for (; k < final; ++k) {
        // a. Let Pk be ! ToString(𝔽(k)).
        // b. Perform ! Set(O, Pk, value, true).
        MUST(typed_array->set(k, value, Object::ShouldThrowExceptions::Yes));

        // c. Set k to k + 1.
    }

    // 21. Return O.
    return typed_array;
}

// 23.2.3.10 %TypedArray%.prototype.filter ( callbackfn [ , thisArg ] ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.filter
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::filter)
{
    // 1. Let O be the this value.
    // 2. Perform ? ValidateTypedArray(O).
    auto* typed_array = TRY(validate_typed_array_from_this(vm));

    // 3. Let len be O.[[ArrayLength]].
    auto initial_length = typed_array->idempotent_array_length();

    // 4. If IsCallable(callbackfn) is false, throw a TypeError exception.
    auto* callback_function = TRY(callback_from_args(vm, "filter"));

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
        auto callback_result = TRY(call(vm, *callback_function, this_value, value, Value((i32)i), typed_array)).to_boolean();

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
    auto* filter_array = TRY(typed_array_species_create(vm, *typed_array, move(arguments)));

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

// 23.2.3.11 %TypedArray%.prototype.find ( predicate [ , thisArg ] ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.find
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::find)
{
    auto result = js_undefined();
    TRY(for_each_item(vm, "find", [&](auto, auto value, auto callback_result) {
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
    TRY(for_each_item(vm, "findIndex", [&](auto index, auto, auto callback_result) {
        if (callback_result.to_boolean()) {
            result_index = index;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    }));
    return Value(result_index);
}

// 23.2.3.13 %TypedArray%.prototype.findLast ( predicate [ , thisArg ] ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.findlast
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::find_last)
{
    auto result = js_undefined();
    TRY(for_each_item_from_last(vm, "findLast", [&](auto, auto value, auto callback_result) {
        if (callback_result.to_boolean()) {
            result = value;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    }));
    return result;
}

// 23.2.3.14 %TypedArray%.prototype.findLastIndex ( predicate [ , thisArg ] ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.findlastindex
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::find_last_index)
{
    auto result_index = -1;
    TRY(for_each_item_from_last(vm, "findLastIndex", [&](auto index, auto, auto callback_result) {
        if (callback_result.to_boolean()) {
            result_index = index;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    }));
    return Value(result_index);
}

// 23.2.3.15 %TypedArray%.prototype.forEach ( callbackfn [ , thisArg ] ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.foreach
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::for_each)
{
    TRY(for_each_item(vm, "forEach", [](auto, auto, auto) {
        return IterationDecision::Continue;
    }));
    return js_undefined();
}

// 23.2.3.16 %TypedArray%.prototype.includes ( searchElement [ , fromIndex ] ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.includes
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::includes)
{
    // 1. Let O be the this value.
    // 2. Perform ? ValidateTypedArray(O).
    auto typed_array = TRY(validate_typed_array_from_this(vm));

    // 3. Let len be O.[[ArrayLength]].
    auto length = typed_array->idempotent_array_length();

    // 4. If len is 0, return false.
    if (length == 0)
        return Value(false);

    // 5. Let n be ? ToIntegerOrInfinity(fromIndex).
    auto n = TRY(vm.argument(1).to_integer_or_infinity(vm));

    // 6. Assert: If fromIndex is undefined, then n is 0.
    if (vm.argument(1).is_undefined())
        VERIFY(n == 0);

    auto value_n = Value(n);
    // 7. If n is +∞, return false.
    if (value_n.is_positive_infinity())
        return Value(false);
    // 8. Else if n is -∞, set n to 0.
    else if (value_n.is_negative_infinity())
        n = 0;

    u32 k;
    // 9. If n ≥ 0, then
    if (n >= 0) {
        // a. Let k be n.
        k = n;
    }
    // 10. Else,
    else {
        // a. Let k be len + n.
        auto relative_k = length + n;

        // b. If k < 0, set k to 0.
        if (relative_k < 0)
            relative_k = 0;
        k = relative_k;
    }

    auto search_element = vm.argument(0);
    // 11. Repeat, while k < len,
    for (; k < length; ++k) {
        // a. Let elementK be ! Get(O, ! ToString(𝔽(k))).
        auto element_k = MUST(typed_array->get(k));

        // b. If SameValueZero(searchElement, elementK) is true, return true.
        if (same_value_zero(search_element, element_k))
            return Value(true);

        // c. Set k to k + 1.
    }

    // 12. Return false.
    return Value(false);
}

// 23.2.3.17 %TypedArray%.prototype.indexOf ( searchElement [ , fromIndex ] ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.indexof
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::index_of)
{
    // 1. Let O be the this value.
    // 2. Perform ? ValidateTypedArray(O).
    auto typed_array = TRY(validate_typed_array_from_this(vm));

    // 3. Let len be O.[[ArrayLength]].
    auto length = typed_array->idempotent_array_length();

    // 4. If len is 0, return -1𝔽.
    if (length == 0)
        return Value(-1);

    // 5. Let n be ? ToIntegerOrInfinity(fromIndex).
    auto n = TRY(vm.argument(1).to_integer_or_infinity(vm));

    // 6. Assert: If fromIndex is undefined, then n is 0.
    if (vm.argument(1).is_undefined())
        VERIFY(n == 0);

    auto value_n = Value(n);
    // 7. If n is +∞, return -1𝔽.
    if (value_n.is_positive_infinity())
        return Value(-1);
    // 8. Else if n is -∞, set n to 0.
    else if (value_n.is_negative_infinity())
        n = 0;

    u32 k;
    // 9. If n ≥ 0, then
    if (n >= 0) {
        // a. Let k be n.
        k = n;
    }
    // 10. Else,
    else {
        // a. Let k be len + n.
        auto relative_k = length + n;

        // b. If k < 0, set k to 0.
        if (relative_k < 0)
            relative_k = 0;
        k = relative_k;
    }

    // 11. Repeat, while k < len,
    auto search_element = vm.argument(0);
    for (; k < length; ++k) {
        // a. Let kPresent be ! HasProperty(O, ! ToString(𝔽(k))).
        auto k_present = MUST(typed_array->has_property(k));

        // b. If kPresent is true, then
        if (k_present) {
            // i. Let elementK be ! Get(O, ! ToString(𝔽(k))).
            auto element_k = MUST(typed_array->get(k));

            // ii. Let same be IsStrictlyEqual(searchElement, elementK).
            // iii. If same is true, return 𝔽(k).
            if (is_strictly_equal(search_element, element_k))
                return Value(k);
        }

        // c. Set k to k + 1.
    }

    // 12. Return -1𝔽.
    return Value(-1);
}

// 23.2.3.18 %TypedArray%.prototype.join ( separator ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.join
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::join)
{
    // 1. Let O be the this value.
    // 2. Perform ? ValidateTypedArray(O).
    auto* typed_array = TRY(validate_typed_array_from_this(vm));

    // 3. Let len be O.[[ArrayLength]].
    auto length = typed_array->idempotent_array_length();

    // 4. If separator is undefined, let sep be ",".
    // 5. Else, let sep be ? ToString(separator).
    DeprecatedString separator = ",";
    if (!vm.argument(0).is_undefined())
        separator = TRY(vm.argument(0).to_deprecated_string(vm));

    // 6. Let R be the empty String.
    StringBuilder builder;

    // 7. Let k be 0.
    // 8. Repeat, while k < len,
    for (size_t i = 0; i < length; ++i) {
        // a. If k > 0, set R to the string-concatenation of R and sep.
        if (i > 0)
            builder.append(separator);

        // b. Let element be ! Get(O, ! ToString(𝔽(k))).
        auto element = MUST(typed_array->get(i));

        // c. If element is undefined, let next be the empty String; otherwise, let next be ! ToString(element).
        if (element.is_undefined())
            continue;
        auto next = MUST(element.to_deprecated_string(vm));

        // d. Set R to the string-concatenation of R and next.
        builder.append(next);

        // e. Set k to k + 1.
    }

    // 9. Return R.
    return PrimitiveString::create(vm, builder.to_deprecated_string());
}

// 23.2.3.19 %TypedArray%.prototype.keys ( ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.keys
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::keys)
{
    auto& realm = *vm.current_realm();

    // 1. Let O be the this value.
    // 2. Perform ? ValidateTypedArray(O).
    auto* typed_array = TRY(validate_typed_array_from_this(vm));

    // 3. Return CreateArrayIterator(O, key).
    return ArrayIterator::create(realm, typed_array, Object::PropertyKind::Key);
}

// 23.2.3.20 %TypedArray%.prototype.lastIndexOf ( searchElement [ , fromIndex ] ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.lastindexof
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::last_index_of)
{
    auto search_element = vm.argument(0);
    auto from_index = vm.argument(1);

    // 1. Let O be the this value.
    // 2. Perform ? ValidateTypedArray(O).
    auto* typed_array = TRY(validate_typed_array_from_this(vm));

    // 3. Let len be O.[[ArrayLength]].
    auto length = typed_array->idempotent_array_length();

    // 4. If len = 0, return -1𝔽.
    if (length == 0)
        return Value(-1);

    double n;

    // 5. If fromIndex is present, let n be ? ToIntegerOrInfinity(fromIndex); else let n be len - 1.
    if (vm.argument_count() > 1)
        n = TRY(from_index.to_integer_or_infinity(vm));
    else
        n = length - 1;

    // 6. If n = -∞, return -1𝔽.
    if (Value(n).is_negative_infinity())
        return Value(-1);

    i32 k;

    // 7. If n ≥ 0, then
    if (n >= 0) {
        // a. Let k be min(n, len - 1).
        k = min(n, (i32)length - 1);
    }
    // 8. Else,
    else {
        // a. Let k be len + n.
        auto relative_k = length + n;
        if (relative_k < 0) // ensures we dont underflow `k`
            relative_k = -1;
        k = relative_k;
    }

    // 9. Repeat, while k ≥ 0,
    for (; k >= 0; --k) {
        // a. Let kPresent be ! HasProperty(O, ! ToString(𝔽(k))).
        auto k_present = MUST(typed_array->has_property(k));

        // b. If kPresent is true, then
        if (k_present) {
            // i. Let elementK be ! Get(O, ! ToString(𝔽(k))).
            auto element_k = MUST(typed_array->get(k));

            // ii. If IsStrictlyEqual(searchElement, elementK) is true, return 𝔽(k).
            if (is_strictly_equal(search_element, element_k))
                return Value(k);
        }

        // c. Set k to k - 1.
    }

    // 10. Return -1𝔽.
    return Value(-1);
}

// 23.2.3.21 get %TypedArray%.prototype.length, https://tc39.es/proposal-resizablearraybuffer/#sec-get-%typedarray%.prototype.length
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::length_getter)
{
    // 1. Let O be the this value.
    // 2. Perform ? RequireInternalSlot(O, [[TypedArrayName]]).
    auto* typed_array = TRY(typed_array_from_this(vm));

    // 3. Assert: O has [[ViewedArrayBuffer]] and [[ArrayLength]] internal slots.
    // 4. Let buffer be O.[[ViewedArrayBuffer]].
    auto* buffer = typed_array->viewed_array_buffer();
    VERIFY(buffer);

    // 5. Let getBufferByteLength be MakeIdempotentArrayBufferByteLengthGetter(SeqCst).
    auto get_buffer_byte_length = make_idempotent_array_buffer_byte_length_getter(ArrayBuffer::Order::SeqCst);

    // 6. Let length be IntegerIndexedObjectLength(O, getBufferByteLength).
    auto length = integer_indexed_object_length(vm, *typed_array, get_buffer_byte_length);

    // 7. If length is out-of-bounds, set length to 0.
    if (!length.has_value())
        return Value(0);

    // 8. Return 𝔽(length).
    return Value(length.value());
}

// 23.2.3.22 %TypedArray%.prototype.map ( callbackfn [ , thisArg ] ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.map
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::map)
{
    // 1. Let O be the this value.
    // 2. Perform ? ValidateTypedArray(O).
    auto* typed_array = TRY(validate_typed_array_from_this(vm));

    // 3. Let len be O.[[ArrayLength]].
    auto initial_length = typed_array->idempotent_array_length();

    // 4. If IsCallable(callbackfn) is false, throw a TypeError exception.
    auto* callback_function = TRY(callback_from_args(vm, "map"));

    // 5. Let A be ? TypedArraySpeciesCreate(O, « 𝔽(len) »).
    MarkedVector<Value> arguments(vm.heap());
    arguments.empend(initial_length);
    auto* return_array = TRY(typed_array_species_create(vm, *typed_array, move(arguments)));

    auto this_value = vm.argument(1);

    // 6. Let k be 0.
    // 7. Repeat, while k < len,
    for (size_t i = 0; i < initial_length; ++i) {
        // a. Let Pk be ! ToString(𝔽(k)).
        // b. Let kValue be ! Get(O, Pk).
        auto value = MUST(typed_array->get(i));

        // c. Let mappedValue be ? Call(callbackfn, thisArg, « kValue, 𝔽(k), O »).
        auto mapped_value = TRY(call(vm, *callback_function, this_value, value, Value((i32)i), typed_array));

        // d. Perform ? Set(A, Pk, mappedValue, true).
        TRY(return_array->set(i, mapped_value, Object::ShouldThrowExceptions::Yes));

        // e. Set k to k + 1.
    }

    // 8. Return A.
    return return_array;
}

// 23.2.3.23 %TypedArray%.prototype.reduce ( callbackfn [ , initialValue ] ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.reduce
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::reduce)
{
    // 1. Let O be the this value.
    // 2. Perform ? ValidateTypedArray(O).
    auto* typed_array = TRY(validate_typed_array_from_this(vm));

    // 3. Let len be O.[[ArrayLength]].
    auto length = typed_array->idempotent_array_length();

    // 4. If IsCallable(callbackfn) is false, throw a TypeError exception.
    auto* callback_function = TRY(callback_from_args(vm, vm.names.reduce.as_string()));

    // 5. If len = 0 and initialValue is not present, throw a TypeError exception.
    if (length == 0 && vm.argument_count() <= 1)
        return vm.throw_completion<TypeError>(ErrorType::ReduceNoInitial);

    // 6. Let k be 0.
    u32 k = 0;

    // 7. Let accumulator be undefined.
    Value accumulator;

    // 8. If initialValue is present, then
    if (vm.argument_count() > 1) {
        // a. Set accumulator to initialValue.
        accumulator = vm.argument(1);
    }
    // 9. Else,
    else {
        // a. Let Pk be ! ToString(𝔽(k)).
        // b. Set accumulator to ! Get(O, Pk).
        accumulator = MUST(typed_array->get(k));

        // c. Set k to k + 1.
        ++k;
    }

    // 10. Repeat, while k < len,
    for (; k < length; ++k) {
        // a. Let Pk be ! ToString(𝔽(k)).
        // b. Let kValue be ! Get(O, Pk).
        auto k_value = MUST(typed_array->get(k));

        // c. Set accumulator to ? Call(callbackfn, undefined, « accumulator, kValue, 𝔽(k), O »).
        accumulator = TRY(call(vm, *callback_function, js_undefined(), accumulator, k_value, Value(k), typed_array));

        // d. Set k to k + 1.
    }

    // 11. Return accumulator.
    return accumulator;
}

// 23.2.3.24 %TypedArray%.prototype.reduceRight ( callbackfn [ , initialValue ] ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.reduce
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::reduce_right)
{
    // 1. Let O be the this value.
    // 2. Perform ? ValidateTypedArray(O).
    auto* typed_array = TRY(validate_typed_array_from_this(vm));

    // 3. Let len be O.[[ArrayLength]].
    auto length = typed_array->idempotent_array_length();

    // 4. If IsCallable(callbackfn) is false, throw a TypeError exception.
    auto* callback_function = TRY(callback_from_args(vm, vm.names.reduce.as_string()));

    // 5. If len is 0 and initialValue is not present, throw a TypeError exception.
    if (length == 0 && vm.argument_count() <= 1)
        return vm.throw_completion<TypeError>(ErrorType::ReduceNoInitial);

    // 6. Let k be len - 1.
    i32 k = (i32)length - 1;

    // 7. Let accumulator be undefined.
    Value accumulator;

    // 8. If initialValue is present, then
    if (vm.argument_count() > 1) {
        // a. Set accumulator to initialValue.
        accumulator = vm.argument(1);
    }
    // 9. Else,
    else {
        // a. Let Pk be ! ToString(𝔽(k)).
        // b. Set accumulator to ! Get(O, Pk).
        accumulator = MUST(typed_array->get(k));

        // c. Set k to k - 1.
        --k;
    }

    // 10. Repeat, while k ≥ 0,
    for (; k >= 0; --k) {
        // a. Let Pk be ! ToString(𝔽(k)).
        // b. Let kValue be ! Get(O, Pk).
        auto k_value = MUST(typed_array->get(k));

        // c. Set accumulator to ? Call(callbackfn, undefined, « accumulator, kValue, 𝔽(k), O »).
        accumulator = TRY(call(vm, *callback_function, js_undefined(), accumulator, k_value, Value(k), typed_array));

        // d. Set k to k - 1.
    }

    // 11. Return accumulator.
    return accumulator;
}

// 23.2.3.25 %TypedArray%.prototype.reverse ( ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.reverse
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::reverse)
{
    // 1. Let O be the this value.
    // 2. Perform ? ValidateTypedArray(O).
    auto* typed_array = TRY(validate_typed_array_from_this(vm));

    // 3. Let len be O.[[ArrayLength]].
    auto length = typed_array->idempotent_array_length();

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

// 23.2.3.26.1 SetTypedArrayFromTypedArray ( target, targetOffset, source ), https://tc39.es/proposal-resizablearraybuffer/#sec-settypedarrayfromtypedarray
static ThrowCompletionOr<void> set_typed_array_from_typed_array(VM& vm, TypedArrayBase& target, double target_offset, TypedArrayBase& source)
{
    // 1. Let targetBuffer be target.[[ViewedArrayBuffer]].
    auto* target_buffer = target.viewed_array_buffer();

    // 2. Let getTargetBufferByteLength be MakeIdempotentArrayBufferByteLengthGetter(SeqCst).
    auto get_target_buffer_byte_length = make_idempotent_array_buffer_byte_length_getter(ArrayBuffer::Order::SeqCst);

    // 3. Let targetLength be IntegerIndexedObjectLength(target, getTargetBufferByteLength).
    auto target_length = integer_indexed_object_length(vm, target, get_target_buffer_byte_length);

    // 4. If targetLength is out-of-bounds, throw a TypeError exception.
    if (!target_length.has_value())
        return vm.throw_completion<TypeError>(ErrorType::TypedArrayOverflowOrOutOfBounds, "target length");

    // 5. Let srcBuffer be source.[[ViewedArrayBuffer]].
    auto* source_buffer = source.viewed_array_buffer();

    // 6. Let targetType be TypedArrayElementType(target).
    // 7. Let targetElementSize be TypedArrayElementSize(target).
    auto target_element_size = target.element_size();

    // 8. Let targetByteOffset be target.[[ByteOffset]].
    auto target_byte_offset = target.byte_offset();

    // 9. Let srcType be TypedArrayElementType(source).
    // 10. Let srcElementSize be TypedArrayElementSize(source).
    auto source_element_size = source.element_size();

    // 11. Let getSrcBufferByteLength be MakeIdempotentArrayBufferByteLengthGetter(SeqCst).
    auto get_source_buffer_byte_length = make_idempotent_array_buffer_byte_length_getter(ArrayBuffer::Order::SeqCst);

    // 12. Let srcLength be IntegerIndexedObjectLength(source, getSrcBufferByteLength).
    auto source_length = integer_indexed_object_length(vm, source, get_source_buffer_byte_length);

    // 13. If srcLength is out-of-bounds, throw a TypeError exception.
    if (!source_length.has_value())
        return vm.throw_completion<TypeError>(ErrorType::TypedArrayOverflowOrOutOfBounds, "source length");

    // 14. Let srcByteOffset be source.[[ByteOffset]].
    auto source_byte_offset = source.byte_offset();

    // 15. If targetOffset is +∞, throw a RangeError exception.
    if (isinf(target_offset))
        return vm.throw_completion<RangeError>(ErrorType::TypedArrayInvalidTargetOffset, "finite");

    // 16. If srcLength + targetOffset > targetLength, throw a RangeError exception.
    Checked<size_t> checked = source_length.value();
    checked += static_cast<u32>(target_offset);
    if (checked.has_overflow() || checked > target_length.value())
        return vm.throw_completion<RangeError>(ErrorType::TypedArrayOverflowOrOutOfBounds, "target length");

    // 17. If target.[[ContentType]] ≠ source.[[ContentType]], throw a TypeError exception.
    if (target.content_type() != source.content_type())
        return vm.throw_completion<TypeError>(ErrorType::TypedArrayInvalidCopy, target.class_name(), source.class_name());

    // FIXME: 18. If both IsSharedArrayBuffer(srcBuffer) and IsSharedArrayBuffer(targetBuffer) are true, then
    // FIXME: a. If srcBuffer.[[ArrayBufferData]] and targetBuffer.[[ArrayBufferData]] are the same Shared Data Block values, let same be true; else let same be false.

    // 19. Else, let same be SameValue(srcBuffer, targetBuffer).
    auto same = same_value(source_buffer, target_buffer);

    size_t source_byte_index = 0;

    // 20. If same is true, then
    if (same) {
        // a. Let srcByteLength be IntegerIndexedObjectByteLength(source, getSrcBufferByteLength).
        auto source_byte_length = integer_indexed_object_byte_length(vm, source, get_source_buffer_byte_length);

        // b. Set srcBuffer to ? CloneArrayBuffer(srcBuffer, srcByteOffset, srcByteLength).
        source_buffer = TRY(clone_array_buffer(vm, *source_buffer, source_byte_offset, source_byte_length));

        // c. Let srcByteIndex be 0.
        source_byte_index = 0;
    }
    // 21. Else, let srcByteIndex be srcByteOffset.
    else {
        source_byte_index = source_byte_offset;
    }

    // 22. Let targetByteIndex be targetOffset × targetElementSize + targetByteOffset.
    Checked<size_t> checked_target_byte_index(static_cast<size_t>(target_offset));
    checked_target_byte_index *= target_element_size;
    checked_target_byte_index += target_byte_offset;
    if (checked_target_byte_index.has_overflow())
        return vm.throw_completion<RangeError>(ErrorType::TypedArrayOverflow, "target byte index");
    auto target_byte_index = checked_target_byte_index.value();

    // 23. Let limit be targetByteIndex + targetElementSize × srcLength.
    Checked<size_t> checked_limit(source_length.value());
    checked_limit *= target_element_size;
    checked_limit += target_byte_index;
    if (checked_limit.has_overflow())
        return vm.throw_completion<RangeError>(ErrorType::TypedArrayOverflow, "target limit");
    auto limit = checked_limit.value();

    // 24. If srcType is the same as targetType, then
    if (source.element_name() == target.element_name()) {
        // a. NOTE: If srcType and targetType are the same, the transfer must be performed in a manner that preserves the bit-level encoding of the source data.
        // b. Repeat, while targetByteIndex < limit,
        //     i. Let value be GetValueFromBuffer(srcBuffer, srcByteIndex, Uint8, true, Unordered).
        //     ii. Perform SetValueInBuffer(targetBuffer, targetByteIndex, Uint8, value, true, Unordered).
        //     iii. Set srcByteIndex to srcByteIndex + 1.
        //     iv. Set targetByteIndex to targetByteIndex + 1.
        target_buffer->buffer().overwrite(target_byte_index, source_buffer->buffer().data() + source_byte_index, limit - target_byte_index);
    }
    // 25. Else,
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

    // 26. Return unused.
    return {};
}

// 23.2.3.26.2 SetTypedArrayFromArrayLike ( target, targetOffset, source ), https://tc39.es/ecma262/#sec-settypedarrayfromarraylike
static ThrowCompletionOr<void> set_typed_array_from_array_like(VM& vm, TypedArrayBase& target, double target_offset, Value source)
{
    // 1. Let targetBuffer be target.[[ViewedArrayBuffer]].
    auto* target_buffer = target.viewed_array_buffer();

    // 2. If IsDetachedBuffer(targetBuffer) is true, throw a TypeError exception.
    if (target_buffer->is_detached())
        return vm.throw_completion<TypeError>(ErrorType::DetachedArrayBuffer);

    // 3. Let targetLength be target.[[ArrayLength]].
    auto target_length = target.idempotent_array_length();

    // 4. Let src be ? ToObject(source).
    auto src = TRY(source.to_object(vm));

    // 5. Let srcLength be ? LengthOfArrayLike(src).
    auto source_length = TRY(length_of_array_like(vm, src));

    // 6. If targetOffset is +∞, throw a RangeError exception.
    if (isinf(target_offset))
        return vm.throw_completion<RangeError>(ErrorType::TypedArrayInvalidTargetOffset, "finite");

    // 7. If srcLength + targetOffset > targetLength, throw a RangeError exception.
    Checked<size_t> checked = source_length;
    checked += static_cast<u32>(target_offset);
    if (checked.has_overflow() || checked.value() > target_length)
        return vm.throw_completion<RangeError>(ErrorType::TypedArrayOverflowOrOutOfBounds, "target length");

    // 8. Let k be 0.
    size_t k = 0;

    // 9. Repeat, while k < srcLength,
    while (k < source_length) {
        // a. Let Pk be ! ToString(𝔽(k)).
        // b. Let value be ? Get(src, Pk).
        auto value = TRY(src->get(k));

        // c. Let targetIndex be 𝔽(targetOffset + k).
        // NOTE: We verify above that target_offset + source_length is valid, so this cannot fail.
        auto target_index = MUST(CanonicalIndex::from_double(vm, CanonicalIndex::Type::Index, target_offset + k));

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

// 23.2.3.26 %TypedArray%.prototype.set ( source [ , offset ] ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.set
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::set)
{
    auto source = vm.argument(0);

    // 1. Let target be the this value.
    // 2. Perform ? RequireInternalSlot(target, [[TypedArrayName]]).
    auto* typed_array = TRY(typed_array_from_this(vm));

    // 3. Assert: target has a [[ViewedArrayBuffer]] internal slot.

    // 4. Let targetOffset be ? ToIntegerOrInfinity(offset).
    auto target_offset = TRY(vm.argument(1).to_integer_or_infinity(vm));

    // 5. If targetOffset < 0, throw a RangeError exception.
    if (target_offset < 0)
        return vm.throw_completion<RangeError>(ErrorType::TypedArrayInvalidTargetOffset, "positive");

    // 6. If source is an Object that has a [[TypedArrayName]] internal slot, then
    if (source.is_object() && is<TypedArrayBase>(source.as_object())) {
        auto& source_typed_array = static_cast<TypedArrayBase&>(source.as_object());

        // a. Perform ? SetTypedArrayFromTypedArray(target, targetOffset, source).
        TRY(set_typed_array_from_typed_array(vm, *typed_array, target_offset, source_typed_array));
    }
    // 7. Else,
    else {
        // a. Perform ? SetTypedArrayFromArrayLike(target, targetOffset, source).
        TRY(set_typed_array_from_array_like(vm, *typed_array, target_offset, source));
    }

    // 8. Return undefined.
    return js_undefined();
}

// 23.2.3.27 %TypedArray%.prototype.slice ( start, end ), https://tc39.es/proposal-resizablearraybuffer/#sec-%typedarray%.prototype.slice
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::slice)
{
    auto start = vm.argument(0);
    auto end = vm.argument(1);

    // 1. Let O be the this value.
    // 2. Perform ? ValidateTypedArray(O).
    auto* typed_array = TRY(validate_typed_array_from_this(vm));

    // 3. Let getBufferByteLength be MakeIdempotentArrayBufferByteLengthGetter(SeqCst).
    auto get_buffer_byte_length = make_idempotent_array_buffer_byte_length_getter(ArrayBuffer::Order::SeqCst);

    // NOTE: Seems like there's an implicit assert here as length is not checked for oob
    // 4. Let len be IntegerIndexedObjectLength(O, getBufferByteLength).
    auto length = integer_indexed_object_length(vm, *typed_array, get_buffer_byte_length);

    // 5. Let relativeStart be ? ToIntegerOrInfinity(start).
    auto relative_start = TRY(start.to_integer_or_infinity(vm));

    i32 k = 0;

    // 6. If relativeStart is -∞, let k be 0.
    if (Value(relative_start).is_negative_infinity())
        k = 0;
    // 7. Else if relativeStart < 0, let k be max(len + relativeStart, 0).
    else if (relative_start < 0)
        k = max(length.value() + relative_start, 0);
    // 8. Else, let k be min(relativeStart, len).
    else
        k = min(relative_start, length.value());

    double relative_end = 0;

    // 9. If end is undefined, let relativeEnd be len; else let relativeEnd be ? ToIntegerOrInfinity(end).
    if (end.is_undefined())
        relative_end = length.value();
    else
        relative_end = TRY(end.to_integer_or_infinity(vm));

    i32 final = 0;

    // 10. If relativeEnd is -∞, let final be 0.
    if (Value(relative_end).is_negative_infinity())
        final = 0;
    // 11. Else if relativeEnd < 0, let final be max(len + relativeEnd, 0).
    else if (relative_end < 0)
        final = max(length.value() + relative_end, 0);
    // 12. Else, let final be min(relativeEnd, len).
    else
        final = min(relative_end, length.value());

    // 13. Let count be max(final - k, 0).
    auto count = max(final - k, 0);

    // 14. Let A be ? TypedArraySpeciesCreate(O, « 𝔽(count) »).
    MarkedVector<Value> arguments(vm.heap());
    arguments.empend(count);
    auto* new_array = TRY(typed_array_species_create(vm, *typed_array, move(arguments)));

    // 15. If count > 0, then
    if (count > 0) {
        // a. Set getBufferByteLength to MakeIdempotentArrayBufferByteLengthGetter(SeqCst).
        get_buffer_byte_length = make_idempotent_array_buffer_byte_length_getter(ArrayBuffer::Order::SeqCst);

        // b. Set len to IntegerIndexedObjectLength(O, getBufferByteLength).
        length = integer_indexed_object_length(vm, *typed_array, get_buffer_byte_length);

        // c. If len is out-of-bounds, throw a TypeError exception.
        if (!length.has_value())
            return vm.throw_completion<TypeError>(ErrorType::TypedArrayOverflowOrOutOfBounds, "length");

        // d. Set final to min(final, len).
        final = min(final, length.value());

        // e. Let srcType be TypedArrayElementType(O).
        // f. Let targetType be TypedArrayElementType(A).

        // g. If srcType is different from targetType, then
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
        // h. Else,
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

            // NOTE: This step causes an out of bounds the spec doesn't take into acount the source byte index
            // We currently do viii. Let limit be min(targetByteIndex + count × elementSize, len × elementSize - source_byte_index).
            // viii. Let limit be min(targetByteIndex + count × elementSize, len × elementSize).
            Checked<u32> limit = count;
            limit *= element_size;
            limit += target_byte_index;
            if (limit.has_overflow()) {
                dbgln("TypedArrayPrototype::slice: limit overflowed, returning as if succeeded.");
                return new_array;
            }

            Checked<u32> bytes_to_end_from_source = length.value();
            bytes_to_end_from_source *= element_size;
            bytes_to_end_from_source -= source_byte_index;
            if (bytes_to_end_from_source.has_overflow()) {
                dbgln("TypedArrayPrototype::slice: bytes_to_end_from_source overflowed, returning as if succeeded.");
                return new_array;
            }

            limit = min(limit.value(), bytes_to_end_from_source.value());

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

// 23.2.3.28 %TypedArray%.prototype.some ( callbackfn [ , thisArg ] ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.some
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::some)
{
    auto result = false;
    TRY(for_each_item(vm, "some", [&](auto, auto, auto callback_result) {
        if (callback_result.to_boolean()) {
            result = true;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    }));
    return Value(result);
}

// 23.2.3.29 %TypedArray%.prototype.sort ( comparefn ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.sort
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::sort)
{
    // 1. If comparefn is not undefined and IsCallable(comparefn) is false, throw a TypeError exception.
    auto compare_fn = vm.argument(0);
    if (!compare_fn.is_undefined() && !compare_fn.is_function())
        return vm.throw_completion<TypeError>(ErrorType::NotAFunction, TRY_OR_THROW_OOM(vm, compare_fn.to_string_without_side_effects()));

    // 2. Let obj be the this value.
    // 3. Perform ? ValidateTypedArray(obj).
    auto* typed_array = TRY(validate_typed_array_from_this(vm));

    // 4. Let len be obj.[[ArrayLength]].
    auto length = typed_array->idempotent_array_length();

    // 5. NOTE: The following closure performs a numeric comparison rather than the string comparison used in 23.1.3.30.
    // 6. Let SortCompare be a new Abstract Closure with parameters (x, y) that captures comparefn and performs the following steps when called:
    Function<ThrowCompletionOr<double>(Value, Value)> sort_compare = [&](auto x, auto y) -> ThrowCompletionOr<double> {
        // a. Return ? CompareTypedArrayElements(x, y, comparefn).
        return TRY(compare_typed_array_elements(vm, x, y, compare_fn.is_undefined() ? nullptr : &compare_fn.as_function()));
    };

    // 7. Let sortedList be ? SortIndexedProperties(obj, len, SortCompare, read-through-holes).
    auto sorted_list = TRY(sort_indexed_properties(vm, *typed_array, length, sort_compare, Holes::ReadThroughHoles));

    // 8. Let j be 0.
    // 9. Repeat, while j < len,
    for (size_t j = 0; j < length; j++) {
        // a. Perform ! Set(obj, ! ToString(𝔽(j)), sortedList[j], true).
        MUST(typed_array->set(j, sorted_list[j], Object::ShouldThrowExceptions::Yes));
        // b. Set j to j + 1.
    }

    // 10. Return obj.
    return typed_array;
}

// 23.2.3.30 %TypedArray%.prototype.subarray ( begin, end ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.subarray
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::subarray)
{
    auto begin = vm.argument(0);
    auto end = vm.argument(1);

    // 1. Let O be the this value.
    // 2. Perform ? RequireInternalSlot(O, [[TypedArrayName]]).
    auto* typed_array = TRY(typed_array_from_this(vm));

    // 3. Assert: O has a [[ViewedArrayBuffer]] internal slot.
    // 4. Let buffer be O.[[ViewedArrayBuffer]].
    auto* buffer = typed_array->viewed_array_buffer();

    // 5. Let getSrcBufferByteLength be MakeIdempotentArrayBufferByteLengthGetter(SeqCst).
    auto get_src_buffer_byte_length = make_idempotent_array_buffer_byte_length_getter(ArrayBuffer::Order::SeqCst);

    // 6. Let srcLength be IntegerIndexedObjectLength(O, getSrcBufferByteLength).
    // 7. If srcLength is out-of-bounds, set srcLength to 0.
    auto source_length_or_oob = integer_indexed_object_length(vm, *typed_array, get_src_buffer_byte_length);
    auto source_length = source_length_or_oob.has_value() ? source_length_or_oob.value() : 0;

    // 8. Let relativeBegin be ? ToIntegerOrInfinity(begin).
    auto relative_begin = TRY(begin.to_integer_or_infinity(vm));

    i32 begin_index = 0;

    // 9. If relativeBegin is -∞, let beginIndex be 0.
    if (Value(relative_begin).is_negative_infinity())
        begin_index = 0;
    // 10. Else if relativeBegin < 0, let beginIndex be max(srcLength + relativeBegin, 0).
    else if (relative_begin < 0)
        begin_index = max(source_length + relative_begin, 0);
    // 11. Else, let beginIndex be min(relativeBegin, srcLength).
    else
        begin_index = min(relative_begin, source_length);

    double relative_end = 0;

    // 12. If O.[[ArrayLength]] is auto and end is undefined, then
    Value new_length;
    if (typed_array->is_array_length_auto() && end.is_undefined()) {
        // a. Let newLength be undefined.
        new_length = js_undefined();
    }
    // 13. Else,
    else {
        // a. If end is undefined, let relativeEnd be srcLength; else let relativeEnd be ? ToIntegerOrInfinity(end).
        if (end.is_undefined())
            relative_end = source_length;
        else
            relative_end = TRY(end.to_integer_or_infinity(vm));

        i32 end_index = 0;

        // b. If relativeEnd is -∞, let endIndex be 0.
        if (Value(relative_end).is_negative_infinity())
            end_index = 0;
        // c. Else if relativeEnd < 0, let endIndex be max(srcLength + relativeEnd, 0).
        else if (relative_end < 0)
            end_index = max(source_length + relative_end, 0);
        // d. Else, let endIndex be min(relativeEnd, srcLength).
        else
            end_index = min(relative_end, source_length);

        // e. Let newLength be max(endIndex - beginIndex, 0).
        new_length = Value(max(end_index - begin_index, 0));
    }

    // 14. Let elementSize be TypedArrayElementSize(O).
    // 15. Let srcByteOffset be O.[[ByteOffset]].
    // 16. Let beginByteOffset be srcByteOffset + beginIndex × elementSize.
    Checked<u32> begin_byte_offset = begin_index;
    begin_byte_offset *= typed_array->element_size();
    begin_byte_offset += typed_array->byte_offset();
    if (begin_byte_offset.has_overflow()) {
        dbgln("TypedArrayPrototype::begin_byte_offset: limit overflowed, returning as if succeeded.");
        return typed_array;
    }

    // 17. If newLength is undefined, then
    // a. Let argumentsList be « buffer, 𝔽(beginByteOffset) ».
    // 18. Else,
    // a. Let argumentsList be « buffer, 𝔽(beginByteOffset), 𝔽(newLength) ».
    MarkedVector<Value> arguments(vm.heap());
    arguments.empend(buffer);
    arguments.empend(begin_byte_offset.value());
    if (!new_length.is_undefined())
        arguments.empend(new_length);

    // 18. Return ? TypedArraySpeciesCreate(O, argumentsList).
    return TRY(typed_array_species_create(vm, *typed_array, move(arguments)));
}

// 23.2.3.31 %TypedArray%.prototype.toLocaleString ( [ reserved1 [ , reserved2 ] ] ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.tolocalestring
// 19.5.1 Array.prototype.toLocaleString ( [ locales [ , options ] ] ), https://tc39.es/ecma402/#sup-array.prototype.tolocalestring
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::to_locale_string)
{
    auto locales = vm.argument(0);
    auto options = vm.argument(1);

    // This function is not generic. ValidateTypedArray is applied to the this value prior to evaluating the algorithm.
    // If its result is an abrupt completion that exception is thrown instead of evaluating the algorithm.
    auto* typed_array = TRY(validate_typed_array_from_this(vm));

    // 1. Let array be ? ToObject(this value).
    // NOTE: Handled by ValidateTypedArray

    // 2. Let len be ? ToLength(? Get(array, "length")).
    // The implementation of the algorithm may be optimized with the knowledge that the this value is an object that
    // has a fixed length and whose integer-indexed properties are not sparse.
    auto length = typed_array->idempotent_array_length();

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
            auto locale_string_value = TRY(next_element.invoke(vm, vm.names.toLocaleString, locales, options));
            auto locale_string = TRY(locale_string_value.to_deprecated_string(vm));

            // ii. Set R to the string-concatenation of R and S.
            builder.append(locale_string);
        }

        // d. Increase k by 1.
    }

    // 7. Return R.
    return PrimitiveString::create(vm, builder.to_deprecated_string());
}

// 23.2.3.32 %TypedArray%.prototype.toReversed ( ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.toreversed
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::to_reversed)
{
    // 1. Let O be the this value.
    // 2. Perform ? ValidateTypedArray(O).
    auto* typed_array = TRY(validate_typed_array_from_this(vm));

    // 3. Let length be O.[[ArrayLength]].
    auto length = typed_array->idempotent_array_length();

    // 4. Let A be ? TypedArrayCreateSameType(O, « 𝔽(length) »).
    MarkedVector<Value> arguments(vm.heap());
    arguments.empend(length);
    auto* return_array = TRY(typed_array_create_same_type(vm, *typed_array, move(arguments)));

    // 5. Let k be 0.
    // 6. Repeat, while k < length
    for (size_t k = 0; k < length; k++) {
        // a. Let from be ! ToString(𝔽(length - k - 1)).
        auto from = PropertyKey { length - k - 1 };

        // b. Let Pk be ! ToString(𝔽(k)).
        auto property_key = PropertyKey { k };

        // c. Let fromValue be ! Get(O, from).
        auto from_value = MUST(typed_array->get(from));

        // d. Perform ! Set(A, Pk, fromValue, true).
        MUST(return_array->set(property_key, from_value, Object::ShouldThrowExceptions::Yes));

        // e. Set k to k + 1.
    }

    // 7. Return A.
    return return_array;
}

// 23.2.3.33 %TypedArray%.prototype.toSorted ( comparefn ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.tosorted
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::to_sorted)
{
    auto comparefn = vm.argument(0);

    // 1. If comparefn is not undefined and IsCallable(comparefn) is false, throw a TypeError exception.
    if (!comparefn.is_undefined() && !comparefn.is_function())
        return vm.throw_completion<TypeError>(ErrorType::NotAFunction, comparefn);

    // 2. Let O be the this value.
    auto object = TRY(vm.this_value().to_object(vm));

    // 3. Perform ? ValidateTypedArray(O).
    auto* typed_array = TRY(validate_typed_array_from_this(vm));

    // 4. Let len be O.[[ArrayLength]].
    auto length = typed_array->idempotent_array_length();

    // 5. Let A be ? TypedArrayCreateSameType(O, « 𝔽(len) »).
    MarkedVector<Value> arguments(vm.heap());
    arguments.empend(length);
    auto* return_array = TRY(typed_array_create_same_type(vm, *typed_array, move(arguments)));

    // 6. NOTE: The following closure performs a numeric comparison rather than the string comparison used in  Array.prototype.toSorted
    // 7. Let SortCompare be a new Abstract Closure with parameters (x, y) that captures comparefn and performs the following steps when called:
    Function<ThrowCompletionOr<double>(Value, Value)> sort_compare = [&](auto x, auto y) -> ThrowCompletionOr<double> {
        // a. Return ? CompareTypedArrayElements(x, y, comparefn).
        return TRY(compare_typed_array_elements(vm, x, y, comparefn.is_undefined() ? nullptr : &comparefn.as_function()));
    };

    // 8. Let sortedList be ? SortIndexedProperties(O, len, SortCompare, read-through-holes).
    auto sorted_list = TRY(sort_indexed_properties(vm, object, length, sort_compare, Holes::ReadThroughHoles));

    // 9. Let j be 0.
    // 10. Repeat, while j < len,
    for (size_t j = 0; j < length; j++) {
        // a. Perform ! Set(A, ! ToString(𝔽(j)), sortedList[j], true).
        MUST(return_array->set(j, sorted_list[j], Object::ShouldThrowExceptions::Yes));
        // b. Set j to j + 1.
    }

    // 11. Return A.
    return return_array;
}

// 23.2.3.35 %TypedArray%.prototype.values ( ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.values
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::values)
{
    auto& realm = *vm.current_realm();

    // 1. Let O be the this value.
    // 2. Perform ? ValidateTypedArray(O).
    auto* typed_array = TRY(validate_typed_array_from_this(vm));

    // 3. Return CreateArrayIterator(O, value).
    return ArrayIterator::create(realm, typed_array, Object::PropertyKind::Value);
}

// 23.2.3.36 %TypedArray%.prototype.with ( index, value ), https://tc39.es/ecma262/#sec-%typedarray%.prototype.with
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::with)
{
    auto index = vm.argument(0);
    auto value = vm.argument(1);

    // 1. Let O be the this value.
    // 2. Perform ? ValidateTypedArray(O).
    auto* typed_array = TRY(validate_typed_array_from_this(vm));

    // 3. Let len be O.[[ArrayLength]].
    auto length = typed_array->idempotent_array_length();

    // 4. Let relativeIndex be ? ToIntegerOrInfinity(index).
    auto relative_index = TRY(index.to_integer_or_infinity(vm));

    double actual_index = 0;
    // 5. If relativeIndex ≥ 0, let actualIndex be relativeIndex.
    if (relative_index >= 0)
        actual_index = relative_index;
    else
        actual_index = length + relative_index;

    Value numeric_value;
    // 7. If O.[[ContentType]] is BigInt, let numericValue be ? ToBigInt(value).
    if (typed_array->content_type() == TypedArrayBase::ContentType::BigInt)
        numeric_value = TRY(value.to_bigint(vm));
    // 8. Else, let numericValue be ? ToNumber(value).
    else
        numeric_value = TRY(value.to_number(vm));

    // 9. If IsValidIntegerIndex(O, 𝔽(actualIndex)) is false, throw a RangeError exception.
    if (!is_valid_integer_index(*typed_array, TRY(CanonicalIndex::from_double(vm, CanonicalIndex::Type::Index, actual_index))))
        return vm.throw_completion<RangeError>(ErrorType::TypedArrayInvalidIntegerIndex, actual_index);

    // 10. Let A be ? TypedArrayCreateSameType(O, « 𝔽(len) »).
    MarkedVector<Value> arguments(vm.heap());
    arguments.empend(length);
    auto* return_array = TRY(typed_array_create_same_type(vm, *typed_array, move(arguments)));

    // 11. Let k be 0.
    // 12. Repeat, while k < len,
    for (size_t k = 0; k < length; k++) {
        // a. Let Pk be ! ToString(𝔽(k)).
        auto property_key = PropertyKey { k };

        Value from_value;
        // b. If k is actualIndex, let fromValue be numericValue.
        if (k == actual_index)
            from_value = numeric_value;
        // c. Else, let fromValue be ! Get(O, Pk).
        else
            from_value = MUST(typed_array->get(property_key));

        // d. Perform ! Set(A, Pk, fromValue, true).
        MUST(return_array->set(property_key, from_value, Object::ShouldThrowExceptions::Yes));

        // e. Set k to k + 1.
    }

    // 13. Return A.
    return return_array;
}

// 23.2.3.38 get %TypedArray%.prototype [ @@toStringTag ], https://tc39.es/ecma262/#sec-get-%typedarray%.prototype-@@tostringtag
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::to_string_tag_getter)
{
    // 1. Let O be the this value.
    auto this_value = vm.this_value();

    // 2. If O is not an Object, return undefined.
    if (!this_value.is_object())
        return js_undefined();

    auto& this_object = this_value.as_object();

    // 3. If O does not have a [[TypedArrayName]] internal slot, return undefined.
    if (!this_object.is_typed_array())
        return js_undefined();

    // 4. Let name be O.[[TypedArrayName]].
    // 5. Assert: name is a String.
    // 6. Return name.
    return PrimitiveString::create(vm, static_cast<TypedArrayBase&>(this_object).element_name());
}

}
